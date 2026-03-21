package com.travelbillpro.service;

import com.travelbillpro.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.io.ByteArrayOutputStream;

@Service
@Slf4j
public class TicketParserService {

    private static final int MIN_TEXT_PER_PAGE = 40;

    @Value("${app.storage.base-path}")
    private String basePath;

    // Suppress PDFBox font warnings once at class load
    static {
        java.util.logging.Logger.getLogger("org.apache.pdfbox").setLevel(java.util.logging.Level.SEVERE);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PdfContent — structured per-page text extraction result
    // ═════════════════════════════════════════════════════════════════════════

    public record PageContent(int pageNumber, String text, boolean isImageOnly) {}

    public record PdfContent(
            List<PageContent> pages,
            int totalPages,
            boolean hasTextLayer,
            String fullText
    ) {
        /** Get text for a specific 1-based page index. */
        public String pageText(int oneBasedIndex) {
            return pages.stream()
                    .filter(p -> p.pageNumber() == oneBasedIndex)
                    .map(PageContent::text)
                    .findFirst()
                    .orElse("");
        }

        /** Get concatenated text for a range of pages (1-based, inclusive). */
        public String pagesText(int fromInclusive, int toInclusive) {
            StringBuilder sb = new StringBuilder();
            for (PageContent page : pages) {
                if (page.pageNumber() >= fromInclusive && page.pageNumber() <= toInclusive) {
                    sb.append("--- PAGE ").append(page.pageNumber()).append(" ---\n");
                    sb.append(page.text()).append("\n\n");
                }
            }
            return sb.toString();
        }

        /** Get pages as a String array (0-indexed). */
        public String[] pagesAsArray() {
            String[] arr = new String[totalPages];
            for (PageContent page : pages) {
                arr[page.pageNumber() - 1] = page.text();
            }
            return arr;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  TEXT CLEANING — strips barcode garbage before AI calls (FIX #3)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Cleans extracted page text before sending to AI.
     *
     * Many travel PDFs (esp. IndiGo) contain 2D barcodes that PDFBox extracts as
     * garbled ASCII sequences (e.g., !"#$%&'()*+,-./01234567). These must be removed
     * or the AI returns all-null responses with confidence=0.0.
     *
     * BUG B fix: Previous version used alphanumeric ratio, but barcode lines
     * contain digits (e.g., "./01234567 89:;<=>?@4:A3>;") which made them pass
     * the 20% threshold. Now uses LETTER ratio — real content has letters,
     * barcode dumps do not.
     *
     * Detection strategy:
     * 1. Strip non-ASCII and non-printable characters
     * 2. Skip lines where LETTERS < 15% of total chars (barcode has no letters)
     * 3. Skip lines where symbols > 45% of total chars (barcode is mostly symbols)
     * 4. Collapse whitespace
     *
     * @param raw Raw extracted text from PDFBox
     * @return Cleaned text safe for AI consumption
     */
    public static String cleanPageText(String raw) {
        if (raw == null || raw.isBlank()) return "";

        String[] lines = raw.split("\\r?\\n");
        StringBuilder cleaned = new StringBuilder();

        for (String line : lines) {
            // Step 1: strip non-printable / non-ASCII entirely
            String sanitized = line.replaceAll("[^\\x20-\\x7E]", "").trim();
            
            if (sanitized.isEmpty()) continue;

            // Step 2: skip very short lines — no useful content
            if (sanitized.length() < 3) continue;

            // Step 3: LETTER ratio check (BUG B fix: not alphanumeric — only letters)
            // Real content: "Mr HASAN REZA" → ~75% letters
            // Barcode dump: "./01234567 89:;<=>?@4:A3>" → ~0% letters
            long letterCount = sanitized.chars()
                    .filter(Character::isLetter)   // ← LETTERS ONLY, not digits
                    .count();
            double letterRatio = (double) letterCount / sanitized.length();

            // Skip lines where less than 15% of chars are letters
            if (letterRatio < 0.15 && sanitized.length() > 8) {
                continue;
            }

            // Step 4: skip lines that are predominantly non-word symbols
            // Pattern: line has >45% punctuation/symbols → barcode or encoding garbage
            long symbolCount = sanitized.chars()
                    .filter(c -> !Character.isLetterOrDigit(c) && c != ' ')
                    .count();
            double symbolRatio = (double) symbolCount / sanitized.length();
            if (symbolRatio > 0.45 && sanitized.length() > 8) {
                continue;
            }

            cleaned.append(sanitized).append("\n");
        }

        return cleaned.toString()
                .replaceAll("(?m)^[ \\t]*$\\n", "")   // remove blank lines
                .replaceAll("\\n{3,}", "\n\n")          // collapse triple+ newlines
                .trim();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Primary extraction — returns PdfContent
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Extracts structured text from a stored PDF file.
     * Returns PdfContent with per-page text, hasTextLayer flag, and fullText.
     *
     * CRITICAL (FIX #3): Every page text is automatically cleaned via cleanPageText()
     * to remove barcode garbage before storing in PageContent.
     */
    public PdfContent extractPdfContent(String relativeFilePath) {
        // Normalize basePath
        Path baseDir = Paths.get(basePath).toAbsolutePath().normalize();
        
        // Handle both absolute and relative file paths
        Path filePath;
        if (Paths.get(relativeFilePath).isAbsolute()) {
            // If the stored path is absolute, use it directly
            filePath = Paths.get(relativeFilePath).normalize();
        } else {
            // If relative, resolve from baseDir
            filePath = baseDir.resolve(relativeFilePath).normalize();
        }
        
        File file = filePath.toFile();
        log.debug("Attempting to load PDF: basePath={}, relativeFilePath={}, resolvedPath={}", 
                  baseDir, relativeFilePath, filePath);
        
        if (!file.exists()) {
            log.error("PDF file not found at: {}", filePath);
            throw new BusinessException("File not found for extraction: " + filePath, "FILE_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        try (PDDocument document = PDDocument.load(file)) {
            int totalPages = document.getNumberOfPages();
            List<PageContent> pages = new ArrayList<>();

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            for (int i = 1; i <= totalPages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String rawText = stripper.getText(document).trim();
                String cleanedText = cleanPageText(rawText);  // ← CLEAN HERE (Fix #3)
                boolean imageOnly = cleanedText.length() < MIN_TEXT_PER_PAGE;
                pages.add(new PageContent(i, cleanedText, imageOnly));
            }

            boolean hasText = pages.stream().anyMatch(p -> !p.isImageOnly());
            String fullText = pages.stream()
                    .map(PageContent::text)
                    .reduce((a, b) -> a + "\n\n===PAGE BREAK===\n\n" + b)
                    .orElse("");

            log.info("PDF extracted: {} pages, hasTextLayer={}", totalPages, hasText);
            return new PdfContent(pages, totalPages, hasText, fullText);

        } catch (IOException e) {
            log.error("Failed to parse PDF: {}", file.getAbsolutePath(), e);
            throw new BusinessException("Failed to extract text from PDF", "PDF_PARSE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Legacy extraction — returns String (backwards compat for other callers)
    // ═════════════════════════════════════════════════════════════════════════

    public String extractTextFromFile(String relativeFilePath) {
        File file;
        Path givenPath = Paths.get(relativeFilePath);
        if (givenPath.isAbsolute()) {
            file = givenPath.normalize().toFile();
        } else {
            file = new File(basePath, relativeFilePath).getAbsoluteFile();
        }

        if (!file.exists()) {
            throw new BusinessException("File not found for extraction: " + file.getAbsolutePath(), "FILE_NOT_FOUND", HttpStatus.NOT_FOUND);
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
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("Failed to extract text from file: {}", file.getAbsolutePath(), e);
            throw new BusinessException("Failed to extract text from file", "EXTRACTION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String extractTextFromPdf(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper textStripper = new PDFTextStripper();
            String text = textStripper.getText(document);

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
        int pagesToProcess = Math.min(document.getNumberOfPages(), 3);
        ITesseract tesseract = getTesseractInstance();

        for (int page = 0; page < pagesToProcess; ++page) {
            BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
            try {
                String text = tesseract.doOCR(bim);
                fullText.append(text).append("\n");
            } catch (TesseractException e) {
                log.error("OCR failed on page {} of PDF", page + 1, e);
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
        String tessdataPath = new File("tessdata").getAbsolutePath();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage("eng");
        return tesseract;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Image rendering for vision model
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Renders all pages of a PDF as base64-encoded PNG images for NVIDIA vision model.
     */
    public List<String> renderPagesAsBase64(String relativeFilePath) {
        Path baseDir = Paths.get(basePath).toAbsolutePath().normalize();
        Path filePath;
        if (Paths.get(relativeFilePath).isAbsolute()) {
            filePath = Paths.get(relativeFilePath).normalize();
        } else {
            filePath = baseDir.resolve(relativeFilePath).normalize();
        }

        File file = filePath.toFile();
        if (!file.exists() || !file.getName().toLowerCase().endsWith(".pdf")) {
            return Collections.emptyList();
        }

        List<String> images = new ArrayList<>();
        try (PDDocument document = PDDocument.load(file)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            int pagesToProcess = document.getNumberOfPages();

            for (int i = 0; i < pagesToProcess; i++) {
                BufferedImage bim = pdfRenderer.renderImageWithDPI(i, 180, ImageType.RGB);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bim, "png", baos);
                String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
                images.add(base64);
            }
            log.info("Rendered {} page(s) to base64 images for vision extraction", images.size());
        } catch (IOException e) {
            log.error("Failed to render PDF pages as images: {}", file.getName(), e);
        }
        return images;
    }
}
