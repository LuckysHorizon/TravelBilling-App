package com.travelbillpro.service;

import com.travelbillpro.dto.EmployeeBillingDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class EmployeeInvoiceExcelService {

    private static final String COMPANY_NAME = "RAMNET SOLUTIONS";
    private static final String COMPANY_ADDR_LINE1 = "Shop No. 3134, Road No. 2, MIG PHASE II,";
    private static final String COMPANY_ADDR_LINE2 = "BHEL, Hyderabad - 502032.";
    private static final String GSTIN = "36AMWPB0052D1ZE";
    private static final String PAN = "AMWPB0052D";

    public byte[] generateExcel(EmployeeBillingDto.InvoiceResponse invoice) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Tax Invoice");
            sheet.setDefaultColumnWidth(14);
            // Column widths: A=30, B=14, C=14, D=18, E=14, F=14
            sheet.setColumnWidth(0, 10000);
            sheet.setColumnWidth(1, 4500);
            sheet.setColumnWidth(2, 4500);
            sheet.setColumnWidth(3, 5500);
            sheet.setColumnWidth(4, 5500);
            sheet.setColumnWidth(5, 4500);

            // Styles
            CellStyle boldStyle = createBoldStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle borderStyle = createBorderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle boldBorderStyle = createBoldBorderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);

            int rowNum = 0;

            // === Company Header Block ===
            Row r = sheet.createRow(rowNum++);
            createCell(r, 0, COMPANY_NAME, boldStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));
            createCell(r, 3, "TAX INVOICE", titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 3, 5));

            r = sheet.createRow(rowNum++);
            createCell(r, 0, COMPANY_ADDR_LINE1, borderStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 2));

            r = sheet.createRow(rowNum++);
            createCell(r, 0, COMPANY_ADDR_LINE2, borderStyle);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 2));

            // Row 3: Customer details header + GSTIN/PAN
            r = sheet.createRow(rowNum++);
            createCell(r, 0, "Customer details:", boldStyle);
            createCell(r, 3, "GSTIN: " + GSTIN, borderStyle);
            createCell(r, 5, "PAN No.: " + PAN, borderStyle);

            // Row 4-7: Customer company info + Invoice meta
            r = sheet.createRow(rowNum++);
            createCell(r, 0, invoice.getCompanyName() != null ? invoice.getCompanyName() : "", borderStyle);
            createCell(r, 3, "INVOICE NO:", boldStyle);
            createCell(r, 4, invoice.getInvoiceNumber(), borderStyle);

            r = sheet.createRow(rowNum++);
            createCell(r, 0, invoice.getCompanyAddress() != null ? invoice.getCompanyAddress() : "", borderStyle);
            sheet.addMergedRegion(new CellRangeAddress(5, 7, 0, 2));
            createCell(r, 3, "DATE:", boldStyle);
            createCell(r, 4, invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("d.M.yyyy")) : "", borderStyle);

            r = sheet.createRow(rowNum++);
            createCell(r, 3, "Passenger Name:", boldStyle);
            createCell(r, 4, invoice.getPassengerName() != null ? invoice.getPassengerName() : "", borderStyle);

            r = sheet.createRow(rowNum++);
            createCell(r, 3, "Mobile No.", boldStyle);
            createCell(r, 4, invoice.getMobile() != null ? invoice.getMobile() : "", borderStyle);

            // Row 8: blank
            r = sheet.createRow(rowNum++);

            // Row 9: GSTIN
            r = sheet.createRow(rowNum++);
            createCell(r, 0, "CUSTOMER GSTIN: " + (invoice.getCompanyGstin() != null ? invoice.getCompanyGstin() : ""), boldStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 2));

            // Row 10: blank
            r = sheet.createRow(rowNum++);

            // === Line Items Table Header ===
            r = sheet.createRow(rowNum++);
            createCell(r, 0, "Particulars", headerStyle);
            createCell(r, 1, "SAC Code", headerStyle);
            createCell(r, 2, "Taxable Value", headerStyle);
            createCell(r, 3, "Non Taxable/Exempt", headerStyle);
            createCell(r, 4, "TOTAL", headerStyle);

            // === Line Items ===
            List<EmployeeBillingDto.LineItemResponse> items = invoice.getLineItems();
            if (items != null) {
                for (EmployeeBillingDto.LineItemResponse item : items) {
                    r = sheet.createRow(rowNum++);
                    createCell(r, 0, item.getParticulars() != null ? item.getParticulars() : "", borderStyle);
                    createCell(r, 1, item.getSacCode() != null ? item.getSacCode() : "", borderStyle);
                    createCurrencyCell(r, 2, item.getTaxableValue(), currencyStyle);
                    createCurrencyCell(r, 3, item.getNonTaxableValue(), currencyStyle);
                    createCurrencyCell(r, 4, item.getTotal(), currencyStyle);
                }
            }

            // TOTAL row
            r = sheet.createRow(rowNum++);
            createCell(r, 0, "TOTAL", boldBorderStyle);

            // Calculate totals
            BigDecimal totalTaxable = BigDecimal.ZERO;
            BigDecimal totalNonTaxable = BigDecimal.ZERO;
            BigDecimal totalAmount = BigDecimal.ZERO;
            if (items != null) {
                for (EmployeeBillingDto.LineItemResponse item : items) {
                    totalTaxable = totalTaxable.add(item.getTaxableValue() != null ? item.getTaxableValue() : BigDecimal.ZERO);
                    totalNonTaxable = totalNonTaxable.add(item.getNonTaxableValue() != null ? item.getNonTaxableValue() : BigDecimal.ZERO);
                    totalAmount = totalAmount.add(item.getTotal() != null ? item.getTotal() : BigDecimal.ZERO);
                }
            }
            createCurrencyCell(r, 2, totalTaxable, boldBorderStyle);
            createCurrencyCell(r, 3, totalNonTaxable, boldBorderStyle);
            createCurrencyCell(r, 4, totalAmount, boldBorderStyle);

            // Blank row
            r = sheet.createRow(rowNum++);

            // === Bank Details + Tax Summary ===
            r = sheet.createRow(rowNum++);
            createCell(r, 0, "OUR BANK DETAILS:", boldStyle);
            createCell(r, 3, "CGST", boldStyle);
            createCurrencyCell(r, 4, invoice.getCgstAmount() != null ? invoice.getCgstAmount() : BigDecimal.ZERO, currencyStyle);

            r = sheet.createRow(rowNum++);
            createCell(r, 0, "A/C HOLDER NAME: RAMNETSOLUTIONS", borderStyle);
            createCell(r, 3, "SGST", boldStyle);
            createCurrencyCell(r, 4, invoice.getSgstAmount() != null ? invoice.getSgstAmount() : BigDecimal.ZERO, currencyStyle);

            r = sheet.createRow(rowNum++);
            createCell(r, 0, "CURRENT A/C NO.: 32602154473", borderStyle);

            r = sheet.createRow(rowNum++);
            createCell(r, 0, "BANK NAME: SBI, BRANCH: TELLAPUR", borderStyle);
            createCell(r, 3, "Grand Total", boldStyle);
            createCurrencyCell(r, 4, invoice.getGrandTotal() != null ? invoice.getGrandTotal() : BigDecimal.ZERO, boldBorderStyle);

            r = sheet.createRow(rowNum++);
            createCell(r, 0, "IFSC : SBIN0013071", borderStyle);

            // Total in words
            r = sheet.createRow(rowNum++);
            createCell(r, 0, "TOTAL INVOICE VALUE IN WORDS:", boldStyle);
            createCell(r, 2, invoice.getTotalInWords() != null ? invoice.getTotalInWords() : "", borderStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 2, 5));

            // Blank
            r = sheet.createRow(rowNum++);

            // Footer notes
            r = sheet.createRow(rowNum++);
            createCell(r, 0, "Air Travel and related Charges :- Includes all Charges related to air transportation of passengers", borderStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 3));
            createCell(r, 4, "For RAMNET SOLUTIONS", boldStyle);

            r = sheet.createRow(rowNum++);
            createCell(r, 0, "Airport Charges :- Includes ADF, UDF and PSF collected on behalf of Airport Operator, as applicable", borderStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 3));

            r = sheet.createRow(rowNum++);
            createCell(r, 0, "Misc. Services :- Includes Charges of Lounge Assistance and Travel Certificate", borderStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 3));

            r = sheet.createRow(rowNum++);
            createCell(r, 0, "Meal :- Includes all prepaid meals purchased before travel", borderStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 3));
            createCell(r, 4, "Authorised Signatory", boldStyle);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate Excel invoice", e);
            throw new RuntimeException("Failed to generate Excel invoice", e);
        }
    }

    public byte[] generatePdf(EmployeeBillingDto.InvoiceResponse invoice) {
        // Generate an HTML version and convert to PDF using basic HTML rendering
        String html = generateHtmlInvoice(invoice);
        try {
            // Use a simple approach: write HTML to a byte array
            // For production, consider using Flying Saucer or iText
            // For now, we return the HTML as a pseudo-PDF (browsers can render it)
            return html.getBytes("UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private String generateHtmlInvoice(EmployeeBillingDto.InvoiceResponse invoice) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        sb.append("<style>");
        sb.append("body{font-family:Arial,sans-serif;margin:20px;font-size:12px}");
        sb.append("table{border-collapse:collapse;width:100%}");
        sb.append("td,th{border:1px solid #333;padding:6px 8px}");
        sb.append("th{background:#f0f0f0;font-weight:bold;text-align:center}");
        sb.append(".header{font-size:16px;font-weight:bold}");
        sb.append(".title{font-size:18px;font-weight:bold;text-align:center}");
        sb.append(".right{text-align:right}");
        sb.append(".bold{font-weight:bold}");
        sb.append(".noborder td{border:none}");
        sb.append("</style></head><body>");

        // Company header
        sb.append("<table class='noborder'><tr>");
        sb.append("<td width='50%'><div class='header'>").append(COMPANY_NAME).append("</div>");
        sb.append("<div>").append(COMPANY_ADDR_LINE1).append("</div>");
        sb.append("<div>").append(COMPANY_ADDR_LINE2).append("</div></td>");
        sb.append("<td width='50%'><div class='title'>TAX INVOICE</div></td>");
        sb.append("</tr></table>");

        // Customer + Invoice meta
        sb.append("<table class='noborder'><tr><td width='50%'>");
        sb.append("<div class='bold'>Customer details:</div>");
        sb.append("<div>").append(safe(invoice.getCompanyName())).append("</div>");
        sb.append("<div>").append(safe(invoice.getCompanyAddress())).append("</div>");
        sb.append("<div>CUSTOMER GSTIN: ").append(safe(invoice.getCompanyGstin())).append("</div>");
        sb.append("</td><td width='50%'>");
        sb.append("<div>GSTIN: ").append(GSTIN).append(" &nbsp; PAN No.: ").append(PAN).append("</div>");
        sb.append("<div>INVOICE NO: <b>").append(safe(invoice.getInvoiceNumber())).append("</b></div>");
        sb.append("<div>DATE: ").append(invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().format(DateTimeFormatter.ofPattern("d.M.yyyy")) : "").append("</div>");
        sb.append("<div>Passenger Name: ").append(safe(invoice.getPassengerName())).append("</div>");
        sb.append("<div>Mobile No.: ").append(safe(invoice.getMobile())).append("</div>");
        sb.append("</td></tr></table><br/>");

        // Line items table
        sb.append("<table>");
        sb.append("<tr><th>Particulars</th><th>SAC Code</th><th>Taxable Value</th><th>Non Taxable/Exempt</th><th>TOTAL</th></tr>");

        BigDecimal totalTaxable = BigDecimal.ZERO, totalNonTaxable = BigDecimal.ZERO, totalAmt = BigDecimal.ZERO;
        if (invoice.getLineItems() != null) {
            for (EmployeeBillingDto.LineItemResponse li : invoice.getLineItems()) {
                sb.append("<tr>");
                sb.append("<td>").append(safe(li.getParticulars())).append("</td>");
                sb.append("<td class='right'>").append(safe(li.getSacCode())).append("</td>");
                sb.append("<td class='right'>").append(fmt(li.getTaxableValue())).append("</td>");
                sb.append("<td class='right'>").append(fmt(li.getNonTaxableValue())).append("</td>");
                sb.append("<td class='right'>").append(fmt(li.getTotal())).append("</td>");
                sb.append("</tr>");
                totalTaxable = totalTaxable.add(li.getTaxableValue() != null ? li.getTaxableValue() : BigDecimal.ZERO);
                totalNonTaxable = totalNonTaxable.add(li.getNonTaxableValue() != null ? li.getNonTaxableValue() : BigDecimal.ZERO);
                totalAmt = totalAmt.add(li.getTotal() != null ? li.getTotal() : BigDecimal.ZERO);
            }
        }
        sb.append("<tr class='bold'><td>TOTAL</td><td></td><td class='right'>").append(fmt(totalTaxable))
                .append("</td><td class='right'>").append(fmt(totalNonTaxable))
                .append("</td><td class='right'>").append(fmt(totalAmt)).append("</td></tr>");
        sb.append("</table><br/>");

        // Tax + Bank details
        sb.append("<table class='noborder'><tr><td width='60%'>");
        sb.append("<div class='bold'>OUR BANK DETAILS:</div>");
        sb.append("<div>A/C HOLDER NAME: RAMNETSOLUTIONS</div>");
        sb.append("<div>CURRENT A/C NO.: 32602154473</div>");
        sb.append("<div>BANK NAME: SBI, BRANCH: TELLAPUR</div>");
        sb.append("<div>IFSC : SBIN0013071</div>");
        sb.append("</td><td width='40%'>");
        sb.append("<div>CGST: <b>").append(fmt(invoice.getCgstAmount())).append("</b></div>");
        sb.append("<div>SGST: <b>").append(fmt(invoice.getSgstAmount())).append("</b></div>");
        sb.append("<div class='bold'>Grand Total: ").append(fmt(invoice.getGrandTotal())).append("</div>");
        sb.append("</td></tr></table>");

        sb.append("<div class='bold'>TOTAL INVOICE VALUE IN WORDS: ").append(safe(invoice.getTotalInWords())).append("</div><br/>");
        sb.append("<div style='text-align:right;margin-top:40px'><div>For RAMNET SOLUTIONS</div><br/><div>Authorised Signatory</div></div>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private String safe(String s) { return s != null ? s : ""; }
    private String fmt(BigDecimal v) { return v != null ? String.format("%,.2f", v) : ""; }

    // === Style helpers ===
    private CellStyle createBoldStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        s.setFont(f);
        return s;
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        s.setFont(f);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private CellStyle createBorderStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        return s;
    }

    private CellStyle createCurrencyStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        return s;
    }

    private CellStyle createBoldBorderStyle(Workbook wb) {
        CellStyle s = createBorderStyle(wb);
        Font f = wb.createFont();
        f.setBold(true);
        s.setFont(f);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        return s;
    }

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 16);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createCurrencyCell(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null && value.compareTo(BigDecimal.ZERO) != 0) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }
}
