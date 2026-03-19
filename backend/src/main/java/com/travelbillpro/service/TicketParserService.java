package com.travelbillpro.service;

import com.travelbillpro.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
@Slf4j
public class TicketParserService {

    @Value("${app.storage.base-path}")
    private String basePath;

    public String extractTextFromFile(String relativeFilePath) {
        File file = new File(basePath, relativeFilePath).getAbsoluteFile();
        
        if (!file.exists()) {
            throw new BusinessException("File not found for extraction", "FILE_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        String fileName = file.getName().toLowerCase();
        
        try {
            if (fileName.endsWith(".pdf")) {
                return extractTextFromPdf(file);
            } else if (fileName.matches(".*\\.(png|jpe?g|tiff?)$")) {
                return extractTextFromImage(file);
            } else {
                throw new BusinessException("Unsupported file format", "UNSUPPORTED_FORMAT", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            log.error("Failed to extract text from file: {}", file.getAbsolutePath(), e);
            throw new BusinessException("Failed to extract text from file", "EXTRACTION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String extractTextFromPdf(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            // First try digital text extraction (fast and accurate)
            PDFTextStripper textStripper = new PDFTextStripper();
            String text = textStripper.getText(document);
            
            // If the PDF contains very little text, it might be a scanned PDF (images inside PDF)
            if (text == null || text.trim().length() < 50) {
                log.info("Digital text extraction yielded little text. Falling back to OCR for {}", file.getName());
                return performOcrOnPdf(document);
            }
            
            return text;
        }
    }

    private String performOcrOnPdf(PDDocument document) throws IOException {
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        StringBuilder fullText = new StringBuilder();
        
        // We only process the first 3 pages to avoid memory/time limits on massive documents
        int pagesToProcess = Math.min(document.getNumberOfPages(), 3);
        
        ITesseract tesseract = getTesseractInstance();
        
        for (int parsePageNumber = 0; parsePageNumber < pagesToProcess; ++parsePageNumber) {
            // Render to image at 300 DPI for good OCR quality
            BufferedImage bim = pdfRenderer.renderImageWithDPI(parsePageNumber, 300, ImageType.RGB);
            try {
                String text = tesseract.doOCR(bim);
                fullText.append(text).append("\n");
            } catch (TesseractException e) {
                log.error("OCR failed on page {} of PDF", parsePageNumber + 1, e);
            }
        }
        
        return fullText.toString();
    }

    private String extractTextFromImage(File file) {
        try {
            ITesseract tesseract = getTesseractInstance();
            return tesseract.doOCR(file);
        } catch (TesseractException e) {
            log.error("OCR failed on image: {}", file.getName(), e);
            throw new BusinessException("Failed to OCR image", "OCR_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    private ITesseract getTesseractInstance() {
        ITesseract tesseract = new Tesseract();
        // Fallback or override for datapath if needed. Tesseract looks in TESSDATA_PREFIX env var usually.
        // tesseract.setDatapath("/usr/share/tessdata/"); 
        tesseract.setLanguage("eng"); // Add +hin for Hindi if trained data exists
        return tesseract;
    }
}
