package com.travelbillpro.service;

import com.travelbillpro.dto.EmployeeBillingDto;
import com.travelbillpro.entity.SystemConfig;
import com.travelbillpro.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmployeeInvoiceExcelService {

    private final SystemConfigRepository systemConfigRepository;

    /** Load all system_config rows into a simple key→value map */
    private Map<String, String> loadOrgConfig() {
        Map<String, String> cfg = new HashMap<>();
        for (SystemConfig sc : systemConfigRepository.findAll()) {
            cfg.put(sc.getKey(), sc.getValue());
        }
        return cfg;
    }

    private String cfg(Map<String, String> m, String key, String fallback) {
        String v = m.get(key);
        return (v != null && !v.isBlank()) ? v : fallback;
    }

    public byte[] generateExcel(EmployeeBillingDto.InvoiceResponse inv) {
        Map<String, String> org = loadOrgConfig();
        String ORG_NAME      = cfg(org, "agencyName",       "RAMNET SOLUTIONS");
        String ORG_ADDR1     = cfg(org, "orgAddressLine1",  "Shop No. 3134, Road No. 2, MIG PHASE II,");
        String ORG_ADDR2     = cfg(org, "orgAddressLine2",  "BHEL, Hyderabad - 502032.");
        String ORG_GSTIN     = cfg(org, "gstin",            "36AMWPB0052D1ZE");
        String ORG_PAN       = cfg(org, "panNumber",        "AMWPB0052D");
        String BANK_ACC_NAME = cfg(org, "bankAccountName",  "RAMNETSOLUTIONS");
        String BANK_ACC_NO   = cfg(org, "bankAccountNumber","32602154473");
        String BANK_NAME     = cfg(org, "bankName",         "SBI");
        String BANK_BRANCH   = cfg(org, "bankBranch",       "TELLAPUR");
        String BANK_IFSC     = cfg(org, "bankIfsc",         "SBIN0013071");
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet s = wb.createSheet("Tax Invoice");

            // Column widths (in 256ths of char) — 6 columns A-F
            s.setColumnWidth(0, 11000); // A: Particulars/Company
            s.setColumnWidth(1, 3200);  // B: SAC Code
            s.setColumnWidth(2, 4500);  // C: Taxable Value
            s.setColumnWidth(3, 5800);  // D: Non Taxable / labels
            s.setColumnWidth(4, 4000);  // E: TOTAL / values
            s.setColumnWidth(5, 4500);  // F: TO destination

            // === Styles ===
            CellStyle bold12 = mkStyle(wb, true, 12, false, false);
            CellStyle title16 = mkStyle(wb, true, 16, false, true); title16.setAlignment(HorizontalAlignment.CENTER);
            CellStyle normal = mkStyle(wb, false, 10, false, false);
            CellStyle boldNormal = mkStyle(wb, true, 10, false, false);
            CellStyle boldUndl = mkStyle(wb, true, 10, true, false);
            CellStyle bordered = mkBordered(wb, false, 10);
            CellStyle boldBordered = mkBordered(wb, true, 10);
            CellStyle headerCell = mkBordered(wb, true, 10); headerCell.setAlignment(HorizontalAlignment.CENTER);
            CellStyle numBordered = mkBordered(wb, false, 10); numBordered.setAlignment(HorizontalAlignment.RIGHT);
                numBordered.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
            CellStyle numBoldBordered = mkBordered(wb, true, 10); numBoldBordered.setAlignment(HorizontalAlignment.RIGHT);
                numBoldBordered.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
            CellStyle numRight = mkStyle(wb, false, 10, false, false); numRight.setAlignment(HorizontalAlignment.RIGHT);
                numRight.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
            CellStyle numBoldRight = mkStyle(wb, true, 10, false, false); numBoldRight.setAlignment(HorizontalAlignment.RIGHT);
                numBoldRight.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
            CellStyle boldRight = mkStyle(wb, true, 10, false, false); boldRight.setAlignment(HorizontalAlignment.RIGHT);

            // Parse line items into the 8 fixed rows
            BigDecimal airTravel1 = BigDecimal.ZERO, airTravel2 = BigDecimal.ZERO;
            BigDecimal psf = BigDecimal.ZERO, udc = BigDecimal.ZERO;
            BigDecimal agentCharges = BigDecimal.ZERO, otherCharges = BigDecimal.ZERO, discount = BigDecimal.ZERO;
            String sacAir = "996425", sacAgent = "998551";

            List<EmployeeBillingDto.LineItemResponse> items = inv.getLineItems();
            if (items != null) {
                for (EmployeeBillingDto.LineItemResponse li : items) {
                    String p = li.getParticulars() != null ? li.getParticulars().toLowerCase() : "";
                    BigDecimal tot = li.getTotal() != null ? li.getTotal() : BigDecimal.ZERO;
                    BigDecimal tv = li.getTaxableValue() != null ? li.getTaxableValue() : BigDecimal.ZERO;
                    BigDecimal ntv = li.getNonTaxableValue() != null ? li.getNonTaxableValue() : BigDecimal.ZERO;

                    if (p.contains("air travel") && airTravel1.compareTo(BigDecimal.ZERO) == 0) {
                        airTravel1 = ntv.compareTo(BigDecimal.ZERO) != 0 ? ntv : tot;
                    } else if (p.contains("air travel")) {
                        airTravel2 = ntv.compareTo(BigDecimal.ZERO) != 0 ? ntv : tot;
                    } else if (p.contains("passenger service")) {
                        psf = ntv.compareTo(BigDecimal.ZERO) != 0 ? ntv : tot;
                    } else if (p.contains("user") && p.contains("development")) {
                        udc = ntv.compareTo(BigDecimal.ZERO) != 0 ? ntv : tot;
                    } else if (p.contains("agent")) {
                        agentCharges = tv.compareTo(BigDecimal.ZERO) != 0 ? tv : tot;
                    } else if (p.contains("discount")) {
                        discount = tot.abs();
                    } else if (p.contains("other")) {
                        otherCharges = ntv.compareTo(BigDecimal.ZERO) != 0 ? ntv : tot;
                    }
                    if (li.getSacCode() != null && !li.getSacCode().isEmpty()) {
                        if (p.contains("agent")) sacAgent = li.getSacCode();
                        else sacAir = li.getSacCode();
                    }
                }
            }

            // Calculations
            BigDecimal nonTaxableTotal = airTravel1.add(airTravel2).add(psf).add(udc).add(otherCharges);
            BigDecimal taxableTotal = agentCharges;
            BigDecimal subTotal = nonTaxableTotal.add(taxableTotal).subtract(discount);
            BigDecimal cgstRate = inv.getCgstRate() != null ? inv.getCgstRate() : new BigDecimal("9");
            BigDecimal sgstRate = inv.getSgstRate() != null ? inv.getSgstRate() : new BigDecimal("9");
            BigDecimal cgst = agentCharges.multiply(cgstRate).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal sgst = agentCharges.multiply(sgstRate).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal grandTotal = subTotal.add(cgst).add(sgst);

            int rn = 0;
            Row r;

            // === ROW 0 (Row 1): RAMNET SOLUTIONS + TAX INVOICE ===
            r = s.createRow(rn++);
            r.setHeightInPoints(22);
            cell(r, 0, ORG_NAME, bold12);
            merge(s, 0, 0, 0, 2);
            cell(r, 3, "TAX INVOICE", title16);
            merge(s, 0, 0, 3, 5);

            // === ROW 1 (Row 2): Address line 1 ===
            r = s.createRow(rn++);
            cell(r, 0, ORG_ADDR1, normal);
            merge(s, 1, 1, 0, 2);

            // === ROW 2 (Row 3): Address line 2 ===
            r = s.createRow(rn++);
            cell(r, 0, ORG_ADDR2, normal);
            merge(s, 2, 2, 0, 2);

            // Header outer border (rows 0-2, cols 0-5)
            addOuterBorder(s, 0, 2, 0, 5);
            // TAX INVOICE box border
            addOuterBorder(s, 0, 0, 3, 5);

            // === ROW 3 (Row 4): Customer details + GSTIN + PAN ===
            r = s.createRow(rn++);
            cell(r, 0, "Customer details:", boldUndl);
            cell(r, 3, "GSTIN: " + ORG_GSTIN, normal);
            cell(r, 5, "PAN No. : " + ORG_PAN, normal);

            // === ROW 4 (Row 5): Company name + Invoice No ===
            r = s.createRow(rn++);
            String custName = safe(inv.getCompanyName());
            cell(r, 0, custName, normal);
            cell(r, 3, "INVOICE NO:", boldNormal);
            cell(r, 4, safe(inv.getInvoiceNumber()), bordered);

            // === ROW 5 (Row 6): Address line 1 + Date ===
            r = s.createRow(rn++);
            String[] addrLines = splitAddress(safe(inv.getCompanyAddress()));
            cell(r, 0, addrLines.length > 0 ? addrLines[0] : "", normal);
            cell(r, 3, "DATE:", boldNormal);
            cell(r, 4, inv.getInvoiceDate() != null ? inv.getInvoiceDate().format(DateTimeFormatter.ofPattern("d.M.yyyy")) : "", bordered);

            // === ROW 6 (Row 7): Address line 2 + Passenger Name ===
            r = s.createRow(rn++);
            cell(r, 0, addrLines.length > 1 ? addrLines[1] : "", normal);
            cell(r, 3, "Passenger Name:", boldNormal);
            cell(r, 4, safe(inv.getPassengerName()), normal);

            // === ROW 7 (Row 8): Address line 3 + Mobile ===
            r = s.createRow(rn++);
            cell(r, 0, addrLines.length > 2 ? addrLines[2] : "", normal);
            cell(r, 3, "Mobile No.", boldNormal);
            cell(r, 4, safe(inv.getMobile()), normal);

            // === ROW 8 (Row 9): spacer + PNR ===
            r = s.createRow(rn++);
            cell(r, 3, "PNR :", boldNormal);
            cell(r, 4, safe(inv.getPnr()), normal);

            // === ROW 9 (Row 10): Customer GSTIN + Date of Travel ===
            r = s.createRow(rn++);
            cell(r, 0, "CUSTOMER GSTIN: " + safe(inv.getCompanyGstin()), boldNormal);
            merge(s, rn - 1, rn - 1, 0, 2);
            cell(r, 3, "DATE OF TRAVEL:", boldNormal);
            cell(r, 4, inv.getDateOfTravel() != null ? inv.getDateOfTravel().format(DateTimeFormatter.ofPattern("d.M.yyyy")) : "", normal);

            // === ROW 10 (Row 11): FROM / TO ===
            r = s.createRow(rn++);
            cell(r, 3, "FROM:", boldNormal);
            cell(r, 4, safe(inv.getFromCity()), normal);
            cell(r, 5, "TO : " + safe(inv.getToCity()), normal);

            // === ROW 11 (Row 12): spacer ===
            r = s.createRow(rn++);

            // === ROW 12 (Row 13): TABLE HEADER ===
            int tableHeaderRow = rn;
            r = s.createRow(rn++);
            cell(r, 0, "Particulars", headerCell);
            cell(r, 1, "SAC code", headerCell);
            cell(r, 2, "Taxable Value", headerCell);
            cell(r, 3, "Non Taxable/Exemp", headerCell);
            CellStyle totalHeader = mkBordered(wb, true, 10);
            totalHeader.setAlignment(HorizontalAlignment.CENTER);
            cell(r, 4, "TOTAL", totalHeader);
            cell(r, 5, "", totalHeader);
            merge(s, tableHeaderRow, tableHeaderRow, 4, 5);

            // Fixed 8 rows for line items
            int firstDataRow = rn;

            // Row 14: Air Travel and related charges (1)
            r = s.createRow(rn++);
            lineItemRow(r, "Air Travel and related charges", sacAir, null, airTravel1, airTravel1, bordered, numBordered);

            // Row 15: Air Travel and related charges (2)
            r = s.createRow(rn++);
            lineItemRow(r, "Air Travel and related charges", "", null, airTravel2, airTravel2, bordered, numBordered);

            // Row 16: Passenger Service Fee
            r = s.createRow(rn++);
            lineItemRow(r, "Passenger Service Fee", "", null, psf, psf, bordered, numBordered);

            // Row 17: User Development Charges
            r = s.createRow(rn++);
            lineItemRow(r, "User Devolopment Charges", "", null, udc, udc, bordered, numBordered);

            // Row 18: Agent service charges
            r = s.createRow(rn++);
            lineItemRow(r, "Agent service charges", sacAgent, agentCharges, null, agentCharges, bordered, numBordered);

            // Row 19: Other charges
            r = s.createRow(rn++);
            lineItemRow(r, "Other charges", "", null, otherCharges, otherCharges, bordered, numBordered);

            // Row 20: Discount
            r = s.createRow(rn++);
            BigDecimal discountDisplay = discount.compareTo(BigDecimal.ZERO) > 0 ? discount.negate() : BigDecimal.ZERO;
            lineItemRow(r, "Discount", "", null, discountDisplay, discountDisplay, bordered, numBordered);

            // Row 21: TOTAL
            r = s.createRow(rn++);
            cell(r, 0, "TOTAL", boldBordered);
            cell(r, 1, "", boldBordered);
            numCell(r, 2, taxableTotal, numBoldBordered);
            numCell(r, 3, nonTaxableTotal.subtract(discount), numBoldBordered);
            numCell(r, 4, subTotal, numBoldBordered);
            cell(r, 5, "", boldBordered);
            merge(s, rn - 1, rn - 1, 4, 5);

            int lastDataRow = rn - 1;

            // Line items outer border
            addOuterBorder(s, tableHeaderRow, lastDataRow, 0, 5);

            // === ROW 22: spacer ===
            r = s.createRow(rn++);

            // === ROW 23: Bank details + CGST ===
            r = s.createRow(rn++);
            cell(r, 0, "OUR BANK DETAILS:", boldUndl);
            cell(r, 3, "CGST", boldNormal);
            numCell(r, 4, cgst, numRight);

            // === ROW 24: Bank line 1 + SGST ===
            r = s.createRow(rn++);
            cell(r, 0, "A/C HOLDER NAME: " + BANK_ACC_NAME, normal);
            cell(r, 3, "SGST", boldNormal);
            numCell(r, 4, sgst, numRight);

            // === ROW 25: Bank line 2 ===
            r = s.createRow(rn++);
            cell(r, 0, "CURRENT A/C NO.: " + BANK_ACC_NO, normal);

            // === ROW 26: Bank line 3 ===
            r = s.createRow(rn++);
            cell(r, 0, "BANK NAME: " + BANK_NAME + ", BRANCH: " + BANK_BRANCH, normal);

            // === ROW 27: IFSC + Grand Total ===
            r = s.createRow(rn++);
            cell(r, 0, "IFSC : " + BANK_IFSC, normal);
            cell(r, 3, "Grand Total", boldNormal);
            // Grand total with top border
            CellStyle gtStyle = mkStyle(wb, true, 10, false, false);
            gtStyle.setAlignment(HorizontalAlignment.RIGHT);
            gtStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
            gtStyle.setBorderTop(BorderStyle.THIN);
            numCell(r, 4, grandTotal, gtStyle);

            // === ROW 28: Total in words ===
            r = s.createRow(rn++);
            cell(r, 0, "TOTAL INVOICE VALUE IN WORDS:", boldNormal);
            merge(s, rn - 1, rn - 1, 0, 1);
            String words = inv.getTotalInWords() != null ? inv.getTotalInWords() :
                    EmployeeBillingService.convertToWords(grandTotal);
            cell(r, 2, words, boldNormal);
            merge(s, rn - 1, rn - 1, 2, 5);
            addOuterBorder(s, rn - 1, rn - 1, 0, 5);

            // === ROW 29: spacer ===
            r = s.createRow(rn++);

            // === ROW 30-33: Footer notes ===
            r = s.createRow(rn++);
            cell(r, 0, "Air Travel and related Charges :- Includes all Charges related to air transportation of passengers", normal);
            merge(s, rn - 1, rn - 1, 0, 3);
            cell(r, 4, "For " + ORG_NAME, boldRight);
            merge(s, rn - 1, rn - 1, 4, 5);

            r = s.createRow(rn++);
            cell(r, 0, "Airport Charges :- Includes ADF, UDF and PSF collected on behalf of Airport Operator, as applicable", normal);
            merge(s, rn - 1, rn - 1, 0, 3);

            r = s.createRow(rn++);
            cell(r, 0, "Misc. Services :- Includes Charges of Lounge Assistance and Travel Certificate", normal);
            merge(s, rn - 1, rn - 1, 0, 3);

            r = s.createRow(rn++);
            cell(r, 0, "Meal :- Includes all prepaid meals purchased before travel", normal);
            merge(s, rn - 1, rn - 1, 0, 3);
            cell(r, 4, "Authorised Signatory", boldRight);
            merge(s, rn - 1, rn - 1, 4, 5);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate Excel invoice", e);
            throw new RuntimeException("Failed to generate Excel invoice", e);
        }
    }

    /**
     * Generate a real PDF invoice using iText, matching the Tax Invoice template.
     * All org details are loaded from system_config.
     */
    public byte[] generatePdf(EmployeeBillingDto.InvoiceResponse inv) {
        Map<String, String> org = loadOrgConfig();
        String ORG_NAME      = cfg(org, "agencyName",       "RAMNET SOLUTIONS");
        String ORG_ADDR1     = cfg(org, "orgAddressLine1",  "Shop No. 3134, Road No. 2, MIG PHASE II,");
        String ORG_ADDR2     = cfg(org, "orgAddressLine2",  "BHEL, Hyderabad - 502032.");
        String ORG_GSTIN     = cfg(org, "gstin",            "36AMWPB0052D1ZE");
        String ORG_PAN       = cfg(org, "panNumber",        "AMWPB0052D");
        String BANK_ACC_NAME = cfg(org, "bankAccountName",  "RAMNETSOLUTIONS");
        String BANK_ACC_NO   = cfg(org, "bankAccountNumber","32602154473");
        String BANK_NAME     = cfg(org, "bankName",         "SBI");
        String BANK_BRANCH   = cfg(org, "bankBranch",       "TELLAPUR");
        String BANK_IFSC     = cfg(org, "bankIfsc",         "SBIN0013071");
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(baos);
            com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            com.itextpdf.layout.Document doc = new com.itextpdf.layout.Document(pdf, com.itextpdf.kernel.geom.PageSize.A4);
            doc.setMargins(30, 36, 30, 36);

            com.itextpdf.layout.borders.Border thinBorder = new com.itextpdf.layout.borders.SolidBorder(0.5f);

            // Parse line items into the 8 fixed rows
            BigDecimal airTravel1 = BigDecimal.ZERO, airTravel2 = BigDecimal.ZERO;
            BigDecimal psf = BigDecimal.ZERO, udc = BigDecimal.ZERO;
            BigDecimal agentCharges = BigDecimal.ZERO, otherCharges = BigDecimal.ZERO, discount = BigDecimal.ZERO;
            String sacAir = "996425", sacAgent = "998551";

            if (inv.getLineItems() != null) {
                for (EmployeeBillingDto.LineItemResponse li : inv.getLineItems()) {
                    String p = li.getParticulars() != null ? li.getParticulars().toLowerCase() : "";
                    BigDecimal tot = li.getTotal() != null ? li.getTotal() : BigDecimal.ZERO;
                    BigDecimal tv = li.getTaxableValue() != null ? li.getTaxableValue() : BigDecimal.ZERO;
                    BigDecimal ntv = li.getNonTaxableValue() != null ? li.getNonTaxableValue() : BigDecimal.ZERO;
                    if (p.contains("air travel") && airTravel1.compareTo(BigDecimal.ZERO) == 0) {
                        airTravel1 = ntv.compareTo(BigDecimal.ZERO) != 0 ? ntv : tot;
                    } else if (p.contains("air travel")) {
                        airTravel2 = ntv.compareTo(BigDecimal.ZERO) != 0 ? ntv : tot;
                    } else if (p.contains("passenger service")) {
                        psf = ntv.compareTo(BigDecimal.ZERO) != 0 ? ntv : tot;
                    } else if (p.contains("user") && p.contains("development")) {
                        udc = ntv.compareTo(BigDecimal.ZERO) != 0 ? ntv : tot;
                    } else if (p.contains("agent")) {
                        agentCharges = tv.compareTo(BigDecimal.ZERO) != 0 ? tv : tot;
                    } else if (p.contains("discount")) {
                        discount = tot.abs();
                    } else if (p.contains("other")) {
                        otherCharges = ntv.compareTo(BigDecimal.ZERO) != 0 ? ntv : tot;
                    }
                    if (li.getSacCode() != null && !li.getSacCode().isEmpty()) {
                        if (p.contains("agent")) sacAgent = li.getSacCode();
                        else sacAir = li.getSacCode();
                    }
                }
            }

            BigDecimal nonTaxableTotal = airTravel1.add(airTravel2).add(psf).add(udc).add(otherCharges);
            BigDecimal taxableTotal = agentCharges;
            BigDecimal subTotal = nonTaxableTotal.add(taxableTotal).subtract(discount);
            BigDecimal cgstRate = inv.getCgstRate() != null ? inv.getCgstRate() : new BigDecimal("9");
            BigDecimal sgstRate = inv.getSgstRate() != null ? inv.getSgstRate() : new BigDecimal("9");
            BigDecimal cgst = agentCharges.multiply(cgstRate).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal sgst = agentCharges.multiply(sgstRate).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal grandTotal = subTotal.add(cgst).add(sgst);
            String words = inv.getTotalInWords() != null ? inv.getTotalInWords() : EmployeeBillingService.convertToWords(grandTotal);

            // ═══ HEADER ═══
            com.itextpdf.layout.element.Table header = new com.itextpdf.layout.element.Table(
                    com.itextpdf.layout.properties.UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
            com.itextpdf.layout.element.Cell leftH = new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph(ORG_NAME).setBold().setFontSize(14))
                    .add(new com.itextpdf.layout.element.Paragraph(ORG_ADDR1).setFontSize(9))
                    .add(new com.itextpdf.layout.element.Paragraph(ORG_ADDR2).setFontSize(9))
                    .setBorder(thinBorder);
            com.itextpdf.layout.element.Cell rightH = new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph("TAX INVOICE").setBold().setFontSize(18)
                            .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER))
                    .setBorder(thinBorder);
            header.addCell(leftH);
            header.addCell(rightH);
            doc.add(header);

            // ═══ CUSTOMER + INVOICE META ═══
            com.itextpdf.layout.element.Table info = new com.itextpdf.layout.element.Table(
                    com.itextpdf.layout.properties.UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
            String[] addrLines = splitAddress(safe(inv.getCompanyAddress()));
            StringBuilder addrStr = new StringBuilder();
            for (String al : addrLines) addrStr.append(al).append("\n");

            com.itextpdf.layout.element.Cell custCell = new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph("Customer details:").setBold().setFontSize(9))
                    .add(new com.itextpdf.layout.element.Paragraph(safe(inv.getCompanyName())).setBold().setFontSize(10))
                    .add(new com.itextpdf.layout.element.Paragraph(addrStr.toString().trim()).setFontSize(9))
                    .add(new com.itextpdf.layout.element.Paragraph("CUSTOMER GSTIN: " + safe(inv.getCompanyGstin())).setBold().setFontSize(9))
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);

            String dateStr = inv.getInvoiceDate() != null ? inv.getInvoiceDate().format(DateTimeFormatter.ofPattern("d.M.yyyy")) : "";
            String dotStr = inv.getDateOfTravel() != null ? inv.getDateOfTravel().format(DateTimeFormatter.ofPattern("d.M.yyyy")) : "";
            com.itextpdf.layout.element.Cell metaCell = new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph("GSTIN: " + ORG_GSTIN + "   PAN No.: " + ORG_PAN).setFontSize(8))
                    .add(new com.itextpdf.layout.element.Paragraph("INVOICE NO: " + safe(inv.getInvoiceNumber())).setFontSize(9))
                    .add(new com.itextpdf.layout.element.Paragraph("DATE: " + dateStr).setFontSize(9))
                    .add(new com.itextpdf.layout.element.Paragraph("Passenger Name: " + safe(inv.getPassengerName())).setFontSize(9))
                    .add(new com.itextpdf.layout.element.Paragraph("Mobile No. " + safe(inv.getMobile())).setFontSize(9))
                    .add(new com.itextpdf.layout.element.Paragraph("PNR: " + safe(inv.getPnr())).setFontSize(9))
                    .add(new com.itextpdf.layout.element.Paragraph("DATE OF TRAVEL: " + dotStr).setFontSize(9))
                    .add(new com.itextpdf.layout.element.Paragraph("FROM: " + safe(inv.getFromCity()) + "   TO: " + safe(inv.getToCity())).setFontSize(9))
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
            info.addCell(custCell);
            info.addCell(metaCell);
            doc.add(info);

            // ═══ LINE ITEMS TABLE ═══
            float[] colW = {3f, 1.2f, 1.5f, 1.8f, 1.5f};
            com.itextpdf.layout.element.Table liTable = new com.itextpdf.layout.element.Table(
                    com.itextpdf.layout.properties.UnitValue.createPercentArray(colW)).useAllAvailableWidth().setMarginTop(8);
            String[] liHeaders = {"Particulars", "SAC Code", "Taxable Value", "Non Taxable/Exemp", "TOTAL"};
            for (String lh : liHeaders) {
                liTable.addHeaderCell(new com.itextpdf.layout.element.Cell()
                        .add(new com.itextpdf.layout.element.Paragraph(lh).setBold().setFontSize(8)
                                .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER))
                        .setBorder(thinBorder).setPadding(3));
            }

            addLiRow(liTable, thinBorder, "Air Travel and related charges", sacAir, null, airTravel1, airTravel1);
            addLiRow(liTable, thinBorder, "Air Travel and related charges", "", null, airTravel2, airTravel2);
            addLiRow(liTable, thinBorder, "Passenger Service Fee", "", null, psf, psf);
            addLiRow(liTable, thinBorder, "User Devolopment Charges", "", null, udc, udc);
            addLiRow(liTable, thinBorder, "Agent service charges", sacAgent, agentCharges, null, agentCharges);
            addLiRow(liTable, thinBorder, "Other charges", "", null, otherCharges, otherCharges);
            BigDecimal discDisp = discount.compareTo(BigDecimal.ZERO) > 0 ? discount.negate() : BigDecimal.ZERO;
            addLiRow(liTable, thinBorder, "Discount", "", null, discDisp, discDisp);

            // TOTAL row
            liTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("TOTAL").setBold().setFontSize(8)).setBorder(thinBorder).setPadding(3));
            liTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("")).setBorder(thinBorder));
            liTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(fmt(taxableTotal)).setBold().setFontSize(8).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)).setBorder(thinBorder).setPadding(3));
            liTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(fmt(nonTaxableTotal.subtract(discount))).setBold().setFontSize(8).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)).setBorder(thinBorder).setPadding(3));
            liTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(fmt(subTotal)).setBold().setFontSize(8).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)).setBorder(thinBorder).setPadding(3));
            doc.add(liTable);

            // ═══ BANK + TAX SUMMARY ═══
            com.itextpdf.layout.element.Table bankTax = new com.itextpdf.layout.element.Table(
                    com.itextpdf.layout.properties.UnitValue.createPercentArray(new float[]{1.4f, 0.6f})).useAllAvailableWidth().setMarginTop(8);
            com.itextpdf.layout.element.Cell bankCell = new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph("OUR BANK DETAILS:").setBold().setFontSize(9))
                    .add(new com.itextpdf.layout.element.Paragraph("A/C HOLDER NAME: " + BANK_ACC_NAME).setFontSize(9))
                    .add(new com.itextpdf.layout.element.Paragraph("CURRENT A/C NO.: " + BANK_ACC_NO).setFontSize(9))
                    .add(new com.itextpdf.layout.element.Paragraph("BANK NAME: " + BANK_NAME + ", BRANCH: " + BANK_BRANCH).setFontSize(9))
                    .add(new com.itextpdf.layout.element.Paragraph("IFSC : " + BANK_IFSC).setFontSize(9))
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);

            com.itextpdf.layout.element.Table taxTable = new com.itextpdf.layout.element.Table(
                    com.itextpdf.layout.properties.UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
            taxTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("CGST").setBold().setFontSize(9)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            taxTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(fmt(cgst)).setFontSize(9).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            taxTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("SGST").setBold().setFontSize(9)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            taxTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(fmt(sgst)).setFontSize(9).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            taxTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("Grand Total").setBold().setFontSize(10)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).setBorderTop(new com.itextpdf.layout.borders.SolidBorder(0.5f)));
            taxTable.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(fmt(grandTotal)).setBold().setFontSize(10).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).setBorderTop(new com.itextpdf.layout.borders.SolidBorder(0.5f)));

            com.itextpdf.layout.element.Cell taxCell = new com.itextpdf.layout.element.Cell().add(taxTable).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
            bankTax.addCell(bankCell);
            bankTax.addCell(taxCell);
            doc.add(bankTax);

            // ═══ TOTAL IN WORDS ═══
            com.itextpdf.layout.element.Table wordsT = new com.itextpdf.layout.element.Table(1).useAllAvailableWidth().setMarginTop(4);
            wordsT.addCell(new com.itextpdf.layout.element.Cell()
                    .add(new com.itextpdf.layout.element.Paragraph("TOTAL INVOICE VALUE IN WORDS: " + words).setBold().setFontSize(9))
                    .setBorder(thinBorder).setPadding(4));
            doc.add(wordsT);

            // ═══ FOOTER ═══
            com.itextpdf.layout.element.Table footer = new com.itextpdf.layout.element.Table(
                    com.itextpdf.layout.properties.UnitValue.createPercentArray(new float[]{3, 1})).useAllAvailableWidth().setMarginTop(8).setFontSize(8);
            footer.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("Air Travel and related Charges :- Includes all Charges related to air transportation of passengers")).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            footer.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("For " + ORG_NAME).setBold().setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            footer.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("Airport Charges :- Includes ADF, UDF and PSF collected on behalf of Airport Operator, as applicable")).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            footer.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("")).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            footer.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("Misc. Services :- Includes Charges of Lounge Assistance and Travel Certificate")).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            footer.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("")).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            footer.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("Meal :- Includes all prepaid meals purchased before travel")).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            footer.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph("Authorised Signatory").setBold().setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
            doc.add(footer);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF invoice", e);
            throw new RuntimeException("Failed to generate PDF invoice", e);
        }
    }

    private void addLiRow(com.itextpdf.layout.element.Table table, com.itextpdf.layout.borders.Border border,
                          String particulars, String sac, BigDecimal taxable, BigDecimal nonTaxable, BigDecimal total) {
        table.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(particulars).setFontSize(8)).setBorder(border).setPadding(3));
        table.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(safe(sac)).setFontSize(8).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)).setBorder(border).setPadding(3));
        table.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(taxable != null && taxable.compareTo(BigDecimal.ZERO) != 0 ? fmt(taxable) : "").setFontSize(8).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)).setBorder(border).setPadding(3));
        table.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(nonTaxable != null && nonTaxable.compareTo(BigDecimal.ZERO) != 0 ? fmt(nonTaxable) : "").setFontSize(8).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)).setBorder(border).setPadding(3));
        table.addCell(new com.itextpdf.layout.element.Cell().add(new com.itextpdf.layout.element.Paragraph(total != null && total.compareTo(BigDecimal.ZERO) != 0 ? fmt(total) : "").setFontSize(8).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT)).setBorder(border).setPadding(3));
    }

    // === Helpers ===
    private String safe(String s) { return s != null ? s : ""; }
    private String fmt(BigDecimal v) { return v != null ? String.format("%,.2f", v) : ""; }

    private String fmtDate(EmployeeBillingDto.InvoiceResponse inv) {
        return inv.getInvoiceDate() != null ? inv.getInvoiceDate().format(DateTimeFormatter.ofPattern("d.M.yyyy")) : "";
    }

    private String[] splitAddress(String addr) {
        if (addr == null || addr.isEmpty()) return new String[]{};
        // Split on newlines, commas followed by space, or just commas
        String[] parts = addr.split("\\r?\\n|,\\s*");
        java.util.List<String> result = new java.util.ArrayList<>();
        for (String p : parts) {
            p = p.trim();
            if (!p.isEmpty()) result.add(p);
        }
        return result.toArray(new String[0]);
    }

    private void lineItemRow(Row r, String particulars, String sac, BigDecimal taxable, BigDecimal nonTaxable, BigDecimal total, CellStyle textStyle, CellStyle numStyle) {
        cell(r, 0, particulars, textStyle);
        cell(r, 1, sac != null ? sac : "", textStyle);
        if (taxable != null && taxable.compareTo(BigDecimal.ZERO) != 0) numCell(r, 2, taxable, numStyle);
        else cell(r, 2, "", textStyle);
        if (nonTaxable != null && nonTaxable.compareTo(BigDecimal.ZERO) != 0) numCell(r, 3, nonTaxable, numStyle);
        else cell(r, 3, "", textStyle);
        if (total != null && total.compareTo(BigDecimal.ZERO) != 0) numCell(r, 4, total, numStyle);
        else cell(r, 4, "", textStyle);
        cell(r, 5, "", textStyle);
    }

    private void cell(Row r, int col, String val, CellStyle style) {
        Cell c = r.createCell(col); c.setCellValue(val); c.setCellStyle(style);
    }

    private void numCell(Row r, int col, BigDecimal val, CellStyle style) {
        Cell c = r.createCell(col);
        if (val != null) c.setCellValue(val.doubleValue());
        c.setCellStyle(style);
    }

    private void merge(Sheet s, int fr, int lr, int fc, int lc) {
        s.addMergedRegion(new CellRangeAddress(fr, lr, fc, lc));
    }

    private CellStyle mkStyle(Workbook wb, boolean bold, int fontSize, boolean underline, boolean border) {
        CellStyle cs = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(bold);
        f.setFontHeightInPoints((short) fontSize);
        if (underline) f.setUnderline(Font.U_SINGLE);
        cs.setFont(f);
        if (border) {
            cs.setBorderBottom(BorderStyle.THIN); cs.setBorderTop(BorderStyle.THIN);
            cs.setBorderLeft(BorderStyle.THIN); cs.setBorderRight(BorderStyle.THIN);
        }
        return cs;
    }

    private CellStyle mkBordered(Workbook wb, boolean bold, int fontSize) {
        return mkStyle(wb, bold, fontSize, false, true);
    }

    private void addOuterBorder(Sheet s, int firstRow, int lastRow, int firstCol, int lastCol) {
        CellRangeAddress region = new CellRangeAddress(firstRow, lastRow, firstCol, lastCol);
        RegionUtil.setBorderTop(BorderStyle.THIN, region, s);
        RegionUtil.setBorderBottom(BorderStyle.THIN, region, s);
        RegionUtil.setBorderLeft(BorderStyle.THIN, region, s);
        RegionUtil.setBorderRight(BorderStyle.THIN, region, s);
    }
}
