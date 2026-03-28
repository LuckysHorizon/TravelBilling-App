package com.travelbillpro.service;

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
import com.travelbillpro.entity.SystemConfig;
import com.travelbillpro.entity.Ticket;
import com.travelbillpro.exception.BusinessException;
import com.travelbillpro.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceGeneratorService {

    private final SystemConfigRepository systemConfigRepository;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    /** Load all system_config rows into a simple key→value map */
    private java.util.Map<String, String> loadOrgConfig() {
        java.util.Map<String, String> cfg = new java.util.HashMap<>();
        for (SystemConfig sc : systemConfigRepository.findAll()) {
            cfg.put(sc.getKey(), sc.getValue());
        }
        return cfg;
    }

    private String cfg(java.util.Map<String, String> m, String key, String fallback) {
        String v = m.get(key);
        return (v != null && !v.isBlank()) ? v : fallback;
    }

    /**
     * Generates a PDF using the organization's Tax Invoice template.
     * All org details (name, address, GSTIN, PAN, bank) are loaded from system_config.
     */
    public byte[] generatePdf(Invoice invoice, List<Ticket> tickets) {
        log.info("Generating Tax Invoice PDF for Invoice {}", invoice.getInvoiceNumber());

        java.util.Map<String, String> org = loadOrgConfig();
        String orgName   = cfg(org, "agencyName",       "RAMNET SOLUTIONS");
        String orgAddr1  = cfg(org, "orgAddressLine1",  "Shop No. 3134, Road No. 2, MIG PHASE II,");
        String orgAddr2  = cfg(org, "orgAddressLine2",  "BHEL, Hyderabad - 502032.");
        String orgGstin  = cfg(org, "gstin",            "36AMWPB0052D1ZE");
        String orgPan    = cfg(org, "panNumber",        "AMWPB0052D");
        String bankAccName = cfg(org, "bankAccountName",   "RAMNETSOLUTIONS");
        String bankAccNo   = cfg(org, "bankAccountNumber", "32602154473");
        String bankName    = cfg(org, "bankName",          "SBI");
        String bankBranch  = cfg(org, "bankBranch",        "TELLAPUR");
        String bankIfsc    = cfg(org, "bankIfsc",          "SBIN0013071");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf, PageSize.A4);
            doc.setMargins(30, 36, 30, 36);

            Border thinBorder = new SolidBorder(0.5f);

            // ═════════════════════════════════════════════════════
            // HEADER: RAMNET SOLUTIONS | TAX INVOICE
            // ═════════════════════════════════════════════════════
            Table header = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
            Cell leftHeader = new Cell()
                    .add(new Paragraph(orgName).setBold().setFontSize(14))
                    .add(new Paragraph(orgAddr1).setFontSize(9))
                    .add(new Paragraph(orgAddr2).setFontSize(9))
                    .setBorder(thinBorder);
            Cell rightHeader = new Cell()
                    .add(new Paragraph("TAX INVOICE").setBold().setFontSize(18).setTextAlignment(TextAlignment.CENTER))
                    .setBorder(thinBorder)
                    .setTextAlignment(TextAlignment.CENTER);
            header.addCell(leftHeader);
            header.addCell(rightHeader);
            doc.add(header);

            // ═════════════════════════════════════════════════════
            // CUSTOMER & INVOICE DETAILS
            // ═════════════════════════════════════════════════════
            Company company = invoice.getCompany();
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();

            // Left: customer
            Cell custCell = new Cell()
                    .add(new Paragraph("Customer details:").setBold().setUnderline().setFontSize(9))
                    .add(new Paragraph(safe(company.getName())).setBold().setFontSize(10))
                    .add(new Paragraph(safe(company.getAddress())).setFontSize(9))
                    .add(new Paragraph("CUSTOMER GSTIN: " + safe(company.getGstNumber())).setBold().setFontSize(9).setMarginTop(4))
                    .setBorder(Border.NO_BORDER);

            // Right: invoice meta
            Cell metaCell = new Cell()
                    .add(new Paragraph("GSTIN: " + orgGstin + "   PAN No.: " + orgPan).setFontSize(8))
                    .add(metaRow("INVOICE NO:", safe(invoice.getInvoiceNumber())))
                    .add(metaRow("DATE:", invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().format(DATE_FORMAT) : ""))
                    .add(metaRow("BILLING PERIOD:",
                            (invoice.getBillingPeriodStart() != null ? invoice.getBillingPeriodStart().format(DATE_FORMAT) : "") +
                            " to " +
                            (invoice.getBillingPeriodEnd() != null ? invoice.getBillingPeriodEnd().format(DATE_FORMAT) : "")))
                    .add(metaRow("DUE DATE:", invoice.getDueDate() != null ? invoice.getDueDate().format(DATE_FORMAT) : ""))
                    .setBorder(Border.NO_BORDER);

            infoTable.addCell(custCell);
            infoTable.addCell(metaCell);
            doc.add(infoTable);

            // ═════════════════════════════════════════════════════
            // TICKET DETAILS TABLE
            // ═════════════════════════════════════════════════════
            float[] ticketColWidths = {1.2f, 1.5f, 2.5f, 2f, 1.5f, 1.2f, 1.2f, 1.2f, 1.5f};
            Table ticketTable = new Table(UnitValue.createPercentArray(ticketColWidths)).useAllAvailableWidth().setMarginTop(10);

            String[] ticketHeaders = {"Date", "PNR", "Passenger", "Route", "Base Fare", "Service Chg", "CGST", "SGST", "Total"};
            for (String th : ticketHeaders) {
                ticketTable.addHeaderCell(new Cell()
                        .add(new Paragraph(th).setBold().setFontSize(8).setTextAlignment(TextAlignment.CENTER))
                        .setBorder(thinBorder).setPadding(3));
            }

            BigDecimal sumBase = BigDecimal.ZERO, sumSC = BigDecimal.ZERO, sumCGST = BigDecimal.ZERO, sumSGST = BigDecimal.ZERO, sumTotal = BigDecimal.ZERO;

            for (Ticket t : tickets) {
                BigDecimal bf = t.getBaseFare() != null ? t.getBaseFare() : BigDecimal.ZERO;
                BigDecimal sc = t.getServiceCharge() != null ? t.getServiceCharge() : BigDecimal.ZERO;
                BigDecimal cg = t.getCgst() != null ? t.getCgst() : BigDecimal.ZERO;
                BigDecimal sg = t.getSgst() != null ? t.getSgst() : BigDecimal.ZERO;
                BigDecimal tot = t.getTotalAmount() != null ? t.getTotalAmount() : BigDecimal.ZERO;

                sumBase = sumBase.add(bf);
                sumSC = sumSC.add(sc);
                sumCGST = sumCGST.add(cg);
                sumSGST = sumSGST.add(sg);
                sumTotal = sumTotal.add(tot);

                String route = safe(t.getOrigin()) + " → " + safe(t.getDestination());
                addTicketRow(ticketTable, thinBorder,
                        t.getTravelDate() != null ? t.getTravelDate().format(DATE_FORMAT) : "",
                        safe(t.getPnrNumber()), safe(t.getPassengerName()), route,
                        bf, sc, cg, sg, tot);
            }

            // TOTALS row
            Cell totalLabel = new Cell(1, 4).add(new Paragraph("TOTAL").setBold().setFontSize(9)).setBorder(thinBorder).setPadding(3);
            ticketTable.addCell(totalLabel);
            addNumCell(ticketTable, sumBase, true, thinBorder);
            addNumCell(ticketTable, sumSC, true, thinBorder);
            addNumCell(ticketTable, sumCGST, true, thinBorder);
            addNumCell(ticketTable, sumSGST, true, thinBorder);
            addNumCell(ticketTable, sumTotal, true, thinBorder);

            doc.add(ticketTable);

            // ═════════════════════════════════════════════════════
            // BANK DETAILS + TAX SUMMARY
            // ═════════════════════════════════════════════════════
            Table bankTax = new Table(UnitValue.createPercentArray(new float[]{1.4f, 0.6f})).useAllAvailableWidth().setMarginTop(10);

            Cell bankCell = new Cell()
                    .add(new Paragraph("OUR BANK DETAILS:").setBold().setUnderline().setFontSize(9))
                    .add(new Paragraph("A/C HOLDER NAME: " + bankAccName).setFontSize(9))
                    .add(new Paragraph("CURRENT A/C NO.: " + bankAccNo).setFontSize(9))
                    .add(new Paragraph("BANK NAME: " + bankName + ", BRANCH: " + bankBranch).setFontSize(9))
                    .add(new Paragraph("IFSC : " + bankIfsc).setFontSize(9))
                    .setBorder(Border.NO_BORDER);

            // Tax summary
            BigDecimal serviceCharge = invoice.getServiceCharge() != null ? invoice.getServiceCharge() : sumSC;
            BigDecimal cgstTotal = invoice.getCgstTotal() != null ? invoice.getCgstTotal() : sumCGST;
            BigDecimal sgstTotal = invoice.getSgstTotal() != null ? invoice.getSgstTotal() : sumSGST;
            BigDecimal grandTotal = invoice.getGrandTotal() != null ? invoice.getGrandTotal() : sumTotal;

            Table taxSummary = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
            addTaxRow(taxSummary, "Service Charge", serviceCharge);
            addTaxRow(taxSummary, "CGST", cgstTotal);
            addTaxRow(taxSummary, "SGST", sgstTotal);

            // Grand Total with top border
            taxSummary.addCell(new Cell().add(new Paragraph("Grand Total").setBold().setFontSize(10))
                    .setBorder(Border.NO_BORDER).setBorderTop(new SolidBorder(0.5f)));
            taxSummary.addCell(new Cell().add(new Paragraph(fmt(grandTotal)).setBold().setFontSize(10).setTextAlignment(TextAlignment.RIGHT))
                    .setBorder(Border.NO_BORDER).setBorderTop(new SolidBorder(0.5f)));

            Cell taxCell = new Cell().add(taxSummary).setBorder(Border.NO_BORDER);
            bankTax.addCell(bankCell);
            bankTax.addCell(taxCell);
            doc.add(bankTax);

            // ═════════════════════════════════════════════════════
            // TOTAL IN WORDS
            // ═════════════════════════════════════════════════════
            String words = EmployeeBillingService.convertToWords(grandTotal);
            Table wordsTable = new Table(1).useAllAvailableWidth().setMarginTop(4);
            wordsTable.addCell(new Cell()
                    .add(new Paragraph("TOTAL INVOICE VALUE IN WORDS: " + words).setBold().setFontSize(9))
                    .setBorder(thinBorder).setPadding(4));
            doc.add(wordsTable);

            // ═════════════════════════════════════════════════════
            // FOOTER
            // ═════════════════════════════════════════════════════
            Table footer = new Table(UnitValue.createPercentArray(new float[]{3, 1})).useAllAvailableWidth().setMarginTop(8).setFontSize(8);
            footer.addCell(new Cell().add(new Paragraph("Air Travel and related Charges :- Includes all Charges related to air transportation of passengers")).setBorder(Border.NO_BORDER));
            footer.addCell(new Cell().add(new Paragraph("For " + orgName).setBold().setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER));
            footer.addCell(new Cell().add(new Paragraph("Airport Charges :- Includes ADF, UDF and PSF collected on behalf of Airport Operator, as applicable")).setBorder(Border.NO_BORDER));
            footer.addCell(new Cell().add(new Paragraph("")).setBorder(Border.NO_BORDER));
            footer.addCell(new Cell().add(new Paragraph("Misc. Services :- Includes Charges of Lounge Assistance and Travel Certificate")).setBorder(Border.NO_BORDER));
            footer.addCell(new Cell().add(new Paragraph("")).setBorder(Border.NO_BORDER));
            footer.addCell(new Cell().add(new Paragraph("Meal :- Includes all prepaid meals purchased before travel")).setBorder(Border.NO_BORDER));
            footer.addCell(new Cell().add(new Paragraph("Authorised Signatory").setBold().setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER));
            doc.add(footer);

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate RAMNET PDF invoice", e);
            throw new BusinessException("Failed to generate PDF", "PDF_GENERATION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ═════════════════════════════════════════════════════
    // EXCEL GENERATION (unchanged format)
    // ═════════════════════════════════════════════════════

    public byte[] generateExcel(Invoice invoice, List<Ticket> tickets) {
        log.info("Generating Excel for Invoice {}", invoice.getInvoiceNumber());

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Invoice Details");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("#,##0.00"));

            int rowNum = 0;

            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("Invoice Number:");
            row.createCell(1).setCellValue(invoice.getInvoiceNumber());

            row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("Company Name:");
            row.createCell(1).setCellValue(invoice.getCompany().getName());

            row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue("Invoice Date:");
            row.createCell(1).setCellValue(invoice.getInvoiceDate().toString());

            rowNum++;

            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"Travel Date", "PNR", "Type", "Passenger Name", "Origin", "Destination", "Operator", "Base Fare", "Service Charge", "CGST", "SGST", "Total Amount"};
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (Ticket t : tickets) {
                Row tRow = sheet.createRow(rowNum++);
                tRow.createCell(0).setCellValue(t.getTravelDate().toString());
                tRow.createCell(1).setCellValue(t.getPnrNumber());
                tRow.createCell(2).setCellValue(t.getTicketType().name());
                tRow.createCell(3).setCellValue(t.getPassengerName());
                tRow.createCell(4).setCellValue(t.getOrigin());
                tRow.createCell(5).setCellValue(t.getDestination());
                tRow.createCell(6).setCellValue(t.getOperatorName());

                setCurrencyCell(tRow, 7, t.getBaseFare(), currencyStyle);
                setCurrencyCell(tRow, 8, t.getServiceCharge(), currencyStyle);
                setCurrencyCell(tRow, 9, t.getCgst(), currencyStyle);
                setCurrencyCell(tRow, 10, t.getSgst(), currencyStyle);
                setCurrencyCell(tRow, 11, t.getTotalAmount(), currencyStyle);
            }

            rowNum++;

            addSummaryRow(sheet, rowNum++, "Total Base Fare:", invoice.getSubtotal(), currencyStyle);
            addSummaryRow(sheet, rowNum++, "Total Service Charge:", invoice.getServiceCharge(), currencyStyle);
            addSummaryRow(sheet, rowNum++, "Total CGST:", invoice.getCgstTotal(), currencyStyle);
            addSummaryRow(sheet, rowNum++, "Total SGST:", invoice.getSgstTotal(), currencyStyle);

            Row gtRow = sheet.createRow(rowNum);
            gtRow.createCell(10).setCellValue("Grand Total:");
            CellStyle grandTotalStyle = workbook.createCellStyle();
            grandTotalStyle.cloneStyleFrom(currencyStyle);
            Font gtFont = workbook.createFont();
            gtFont.setBold(true);
            grandTotalStyle.setFont(gtFont);
            setCurrencyCell(gtRow, 11, invoice.getGrandTotal(), grandTotalStyle);

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

    // ═════════════════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════════════════

    private Paragraph metaRow(String label, String value) {
        return new Paragraph(label + " " + value).setFontSize(9).setMarginTop(2);
    }

    private void addTicketRow(Table table, Border border, String date, String pnr, String passenger, String route,
                              BigDecimal baseFare, BigDecimal sc, BigDecimal cgst, BigDecimal sgst, BigDecimal total) {
        table.addCell(new Cell().add(new Paragraph(date).setFontSize(8)).setBorder(border).setPadding(2));
        table.addCell(new Cell().add(new Paragraph(pnr).setFontSize(8)).setBorder(border).setPadding(2));
        table.addCell(new Cell().add(new Paragraph(passenger).setFontSize(8)).setBorder(border).setPadding(2));
        table.addCell(new Cell().add(new Paragraph(route).setFontSize(8)).setBorder(border).setPadding(2));
        addNumCell(table, baseFare, false, border);
        addNumCell(table, sc, false, border);
        addNumCell(table, cgst, false, border);
        addNumCell(table, sgst, false, border);
        addNumCell(table, total, false, border);
    }

    private void addNumCell(Table table, BigDecimal value, boolean bold, Border border) {
        Paragraph p = new Paragraph(fmt(value)).setFontSize(8).setTextAlignment(TextAlignment.RIGHT);
        if (bold) p.setBold();
        table.addCell(new Cell().add(p).setBorder(border).setPadding(2));
    }

    private void addTaxRow(Table table, String label, BigDecimal amount) {
        table.addCell(new Cell().add(new Paragraph(label).setBold().setFontSize(9)).setBorder(Border.NO_BORDER));
        table.addCell(new Cell().add(new Paragraph(fmt(amount)).setFontSize(9).setTextAlignment(TextAlignment.RIGHT)).setBorder(Border.NO_BORDER));
    }

    private String fmt(BigDecimal v) {
        return v != null ? String.format("%,.2f", v) : "0.00";
    }

    private String safe(String s) {
        return s != null ? s : "";
    }

    private void setCurrencyCell(Row row, int col, BigDecimal value, CellStyle style) {
        org.apache.poi.ss.usermodel.Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value.doubleValue() : 0);
        cell.setCellStyle(style);
    }

    private void addSummaryRow(Sheet sheet, int rowNum, String label, BigDecimal value, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        row.createCell(10).setCellValue(label);
        setCurrencyCell(row, 11, value, style);
    }
}
