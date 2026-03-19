package com.travelbillpro.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.travelbillpro.entity.Company;
import com.travelbillpro.entity.Invoice;
import com.travelbillpro.entity.Ticket;
import com.travelbillpro.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class InvoiceGeneratorService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    // Theme colors matching UI
    private static final DeviceRgb BRAND_DARK = new DeviceRgb(26, 26, 26);
    private static final DeviceRgb HEADER_GRAY = new DeviceRgb(249, 250, 251);

    /**
     * Generates a PDF byte array for the given invoice and its tickets.
     */
    public byte[] generatePdf(Invoice invoice, List<Ticket> tickets) {
        log.info("Generating PDF for Invoice {}", invoice.getInvoiceNumber());
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(36, 36, 36, 36);

            // 1. Header (Agency Details & Invoice Title)
            Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
            
            Cell agencyCell = new Cell().add(new Paragraph("TRAVELBILL PRO AGENCY")
                    .setBold().setFontSize(16).setFontColor(BRAND_DARK))
                    .add(new Paragraph("123 Business Avenue, Suite 400\nMumbai, MH 400001\nGSTIN: 27AABCT0000A1Z5").setFontSize(10))
                    .setBorder(Border.NO_BORDER);
                    
            Cell invoiceTitleCell = new Cell().add(new Paragraph("TAX INVOICE")
                    .setBold().setFontSize(20).setFontColor(BRAND_DARK).setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(Border.NO_BORDER);
                    
            headerTable.addCell(agencyCell);
            headerTable.addCell(invoiceTitleCell);
            document.add(headerTable);
            
            document.add(new Paragraph("\n"));

            // 2. Bill To & Invoice Info
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
            
            Company company = invoice.getCompany();
            Cell billToCell = new Cell().add(new Paragraph("Bill To:")
                    .setBold().setFontSize(10).setFontColor(BRAND_DARK))
                    .add(new Paragraph(company.getName()).setBold().setFontSize(12))
                    .add(new Paragraph(company.getAddress() != null ? company.getAddress() : "").setFontSize(10))
                    .add(new Paragraph("GSTIN: " + company.getGstNumber()).setFontSize(10))
                    .setBorder(Border.NO_BORDER);
                    
            Cell invDetailsCell = new Cell().add(new Paragraph("Invoice No: " + invoice.getInvoiceNumber())
                    .setBold().setFontSize(10).setTextAlignment(TextAlignment.RIGHT))
                    .add(new Paragraph("Invoice Date: " + invoice.getInvoiceDate().format(DATE_FORMAT))
                    .setFontSize(10).setTextAlignment(TextAlignment.RIGHT))
                    .add(new Paragraph("Billing Period: " + invoice.getBillingPeriodStart().format(DATE_FORMAT) + " to " + invoice.getBillingPeriodEnd().format(DATE_FORMAT))
                    .setFontSize(10).setTextAlignment(TextAlignment.RIGHT))
                    .add(new Paragraph("Due Date: " + invoice.getDueDate().format(DATE_FORMAT))
                    .setBold().setFontSize(10).setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(Border.NO_BORDER);
                    
            infoTable.addCell(billToCell);
            infoTable.addCell(invDetailsCell);
            document.add(infoTable);
            
            document.add(new Paragraph("\n"));

            // 3. Ticket Details Table
            Table itemTable = new Table(UnitValue.createPercentArray(new float[]{1, 2, 3, 2, 2})).useAllAvailableWidth();
            
            // Table Header
            String[] headers = {"Date", "PNR", "Passenger", "Route", "Base Fare"};
            for (String header : headers) {
                itemTable.addHeaderCell(new Cell().add(new Paragraph(header).setBold().setFontSize(9))
                        .setBackgroundColor(HEADER_GRAY)
                        .setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 1)));
            }
            
            // Table Rows
            for (Ticket ticket : tickets) {
                itemTable.addCell(new Cell().add(new Paragraph(ticket.getTravelDate().format(DATE_FORMAT)).setFontSize(9)).setBorder(Border.NO_BORDER));
                itemTable.addCell(new Cell().add(new Paragraph(ticket.getPnrNumber()).setFontSize(9)).setBorder(Border.NO_BORDER));
                itemTable.addCell(new Cell().add(new Paragraph(ticket.getPassengerName()).setFontSize(9)).setBorder(Border.NO_BORDER));
                
                String route = (ticket.getOrigin() != null ? ticket.getOrigin() : "") + 
                               (ticket.getDestination() != null ? " - " + ticket.getDestination() : "");
                               
                itemTable.addCell(new Cell().add(new Paragraph(route).setFontSize(9)).setBorder(Border.NO_BORDER));
                itemTable.addCell(new Cell().add(new Paragraph(String.format("₹%,.2f", ticket.getBaseFare())).setFontSize(9))
                        .setTextAlignment(TextAlignment.RIGHT).setBorder(Border.NO_BORDER));
            }
            
            // Add top border to the row after items
            itemTable.addCell(new Cell(1, 4).add(new Paragraph("")).setBorderTop(new SolidBorder(ColorConstants.LIGHT_GRAY, 1)).setBorderBottom(Border.NO_BORDER).setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER));
            itemTable.addCell(new Cell().add(new Paragraph("")).setBorderTop(new SolidBorder(ColorConstants.LIGHT_GRAY, 1)).setBorderBottom(Border.NO_BORDER).setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER));

            document.add(itemTable);
            document.add(new Paragraph("\n"));

            // 4. Summary Table (Right aligned)
            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{3, 1})).useAllAvailableWidth();
            
            addSummaryRow(summaryTable, "Total Base Fare", invoice.getTotalBaseFare(), false);
            addSummaryRow(summaryTable, "Service Charge", invoice.getTotalServiceCharge(), false);
            addSummaryRow(summaryTable, "CGST", invoice.getTotalCgst(), false);
            addSummaryRow(summaryTable, "SGST", invoice.getTotalSgst(), false);
            
            // Grand Total
            summaryTable.addCell(new Cell().add(new Paragraph("Grand Total").setBold().setFontSize(11))
                    .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));
            summaryTable.addCell(new Cell().add(new Paragraph(String.format("₹%,.2f", invoice.getGrandTotal())).setBold().setFontSize(11))
                    .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT));

            // Wrap summary table to push it right
            Table rightLayoutTable = new Table(UnitValue.createPercentArray(new float[]{2, 2})).useAllAvailableWidth();
            rightLayoutTable.addCell(new Cell().setBorder(Border.NO_BORDER)); // Empty left cell
            rightLayoutTable.addCell(new Cell().add(summaryTable).setBorder(Border.NO_BORDER)); // Summary on right
            
            document.add(rightLayoutTable);

            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            log.error("Failed to generate PDF invoice", e);
            throw new BusinessException("Failed to generate PDF", "PDF_GENERATION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    private void addSummaryRow(Table table, String label, java.math.BigDecimal amount, boolean isBold) {
        Cell labelCell = new Cell().add(new Paragraph(label).setFontSize(10))
                .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        Cell amountCell = new Cell().add(new Paragraph(String.format("₹%,.2f", amount)).setFontSize(10))
                .setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
                
        if (isBold) {
            labelCell.setBold();
            amountCell.setBold();
        }
        
        table.addCell(labelCell);
        table.addCell(amountCell);
    }

    /**
     * Generates an Excel XLSX byte array for the given invoice and its tickets.
     */
    public byte[] generateExcel(Invoice invoice, List<Ticket> tickets) {
        log.info("Generating Excel for Invoice {}", invoice.getInvoiceNumber());
        
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
             
            Sheet sheet = workbook.createSheet("Invoice Details");
            
            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // Currency Style
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("#,##0.00"));

            int rowNum = 0;
            
            // 1. Invoice Meta Info
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("Invoice Number:");
            row.createCell(1).setCellValue(invoice.getInvoiceNumber());
            
            row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("Company Name:");
            row.createCell(1).setCellValue(invoice.getCompany().getName());
            
            row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("Invoice Date:");
            row.createCell(1).setCellValue(invoice.getInvoiceDate().toString());
            
            rowNum++; // Empty row
            
            // 2. Ticket Table Header
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"Travel Date", "PNR", "Type", "Passenger Name", "Origin", "Destination", "Operator", "Base Fare", "Service Charge", "CGST", "SGST", "Total Amount"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 3. Ticket Rows
            for (Ticket t : tickets) {
                Row tRow = sheet.createRow(rowNum++);
                tRow.createCell(0).setCellValue(t.getTravelDate().toString());
                tRow.createCell(1).setCellValue(t.getPnrNumber());
                tRow.createCell(2).setCellValue(t.getTicketType().name());
                tRow.createCell(3).setCellValue(t.getPassengerName());
                tRow.createCell(4).setCellValue(t.getOrigin());
                tRow.createCell(5).setCellValue(t.getDestination());
                tRow.createCell(6).setCellValue(t.getOperatorName());
                
                Cell fareCell = tRow.createCell(7);
                fareCell.setCellValue(t.getBaseFare().doubleValue());
                fareCell.setCellStyle(currencyStyle);
                
                Cell scCell = tRow.createCell(8);
                scCell.setCellValue(t.getServiceCharge().doubleValue());
                scCell.setCellStyle(currencyStyle);
                
                Cell cgstCell = tRow.createCell(9);
                cgstCell.setCellValue(t.getCgst().doubleValue());
                cgstCell.setCellStyle(currencyStyle);
                
                Cell sgstCell = tRow.createCell(10);
                sgstCell.setCellValue(t.getSgst().doubleValue());
                sgstCell.setCellStyle(currencyStyle);
                
                Cell totCell = tRow.createCell(11);
                totCell.setCellValue(t.getTotalAmount().doubleValue());
                totCell.setCellStyle(currencyStyle);
            }
            
            rowNum++; // Empty row
            
            // 4. Summaries
            Row sumRow1 = sheet.createRow(rowNum++);
            sumRow1.createCell(10).setCellValue("Total Base Fare:");
            Cell sumCell1 = sumRow1.createCell(11);
            sumCell1.setCellValue(invoice.getTotalBaseFare().doubleValue());
            sumCell1.setCellStyle(currencyStyle);
            
            Row sumRow2 = sheet.createRow(rowNum++);
            sumRow2.createCell(10).setCellValue("Total Service Charge:");
            Cell sumCell2 = sumRow2.createCell(11);
            sumCell2.setCellValue(invoice.getTotalServiceCharge().doubleValue());
            sumCell2.setCellStyle(currencyStyle);
            
            Row sumRow4 = sheet.createRow(rowNum++);
            sumRow4.createCell(10).setCellValue("Total CGST:");
            Cell sumCell4 = sumRow4.createCell(11);
            sumCell4.setCellValue(invoice.getTotalCgst().doubleValue());
            sumCell4.setCellStyle(currencyStyle);
            
            Row sumRow5 = sheet.createRow(rowNum++);
            sumRow5.createCell(10).setCellValue("Total SGST:");
            Cell sumCell5 = sumRow5.createCell(11);
            sumCell5.setCellValue(invoice.getTotalSgst().doubleValue());
            sumCell5.setCellStyle(currencyStyle);
            
            Row sumRow6 = sheet.createRow(rowNum++);
            sumRow6.createCell(10).setCellValue("Grand Total:");
            Cell sumCell6 = sumRow6.createCell(11);
            sumCell6.setCellValue(invoice.getGrandTotal().doubleValue());
            CellStyle grandTotalStyle = workbook.createCellStyle();
            grandTotalStyle.cloneStyleFrom(currencyStyle);
            Font gtFont = workbook.createFont();
            gtFont.setBold(true);
            grandTotalStyle.setFont(gtFont);
            sumCell6.setCellStyle(grandTotalStyle);
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
             
        } catch (IOException e) {
            log.error("Failed to generate Excel invoice", e);
            throw new BusinessException("Failed to generate Excel", "EXCEL_GENERATION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
