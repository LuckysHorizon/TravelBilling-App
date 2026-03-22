package com.travelbillpro.service;

import com.travelbillpro.dto.EmployeeBillingDto;
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

@Service
@Slf4j
public class EmployeeInvoiceExcelService {

    private static final String COMPANY_NAME = "RAMNET SOLUTIONS";
    private static final String COMPANY_ADDR_LINE1 = "Shop No. 3134, Road No. 2, MIG PHASE II,";
    private static final String COMPANY_ADDR_LINE2 = "BHEL, Hyderabad - 502032.";
    private static final String GSTIN = "36AMWPB0052D1ZE";
    private static final String PAN = "AMWPB0052D";

    public byte[] generateExcel(EmployeeBillingDto.InvoiceResponse inv) {
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
            cell(r, 0, COMPANY_NAME, bold12);
            merge(s, 0, 0, 0, 2);
            cell(r, 3, "TAX INVOICE", title16);
            merge(s, 0, 0, 3, 5);

            // === ROW 1 (Row 2): Address line 1 ===
            r = s.createRow(rn++);
            cell(r, 0, COMPANY_ADDR_LINE1, normal);
            merge(s, 1, 1, 0, 2);

            // === ROW 2 (Row 3): Address line 2 ===
            r = s.createRow(rn++);
            cell(r, 0, COMPANY_ADDR_LINE2, normal);
            merge(s, 2, 2, 0, 2);

            // Header outer border (rows 0-2, cols 0-5)
            addOuterBorder(s, 0, 2, 0, 5);
            // TAX INVOICE box border
            addOuterBorder(s, 0, 0, 3, 5);

            // === ROW 3 (Row 4): Customer details + GSTIN + PAN ===
            r = s.createRow(rn++);
            cell(r, 0, "Customer details:", boldUndl);
            cell(r, 3, "GSTIN: " + GSTIN, normal);
            cell(r, 5, "PAN No. : " + PAN, normal);

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
            cell(r, 0, "A/C HOLDER NAME: RAMNETSOLUTIONS", normal);
            cell(r, 3, "SGST", boldNormal);
            numCell(r, 4, sgst, numRight);

            // === ROW 25: Bank line 2 ===
            r = s.createRow(rn++);
            cell(r, 0, "CURRENT A/C NO.: 32602154473", normal);

            // === ROW 26: Bank line 3 ===
            r = s.createRow(rn++);
            cell(r, 0, "BANK NAME: SBI, BRANCH: TELLAPUR", normal);

            // === ROW 27: IFSC + Grand Total ===
            r = s.createRow(rn++);
            cell(r, 0, "IFSC : SBIN0013071", normal);
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
            cell(r, 4, "For RAMNET SOLUTIONS", boldRight);
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
     * Generate a styled HTML invoice (for PDF download or in-browser preview).
     */
    public byte[] generatePdf(EmployeeBillingDto.InvoiceResponse inv) {
        try {
            return generateHtmlInvoice(inv).getBytes("UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private String generateHtmlInvoice(EmployeeBillingDto.InvoiceResponse inv) {
        // Parse line items
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

        String[] addrLines = splitAddress(safe(inv.getCompanyAddress()));

        StringBuilder h = new StringBuilder();
        h.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><style>");
        h.append("*{margin:0;padding:0;box-sizing:border-box}");
        h.append("body{font-family:'Calibri','Arial',sans-serif;font-size:11px;padding:20px;max-width:780px;margin:0 auto}");
        h.append("table{border-collapse:collapse;width:100%}");
        h.append("td,th{padding:3px 5px;vertical-align:top}");
        h.append(".b{font-weight:bold}.u{text-decoration:underline}.r{text-align:right}.c{text-align:center}");
        h.append(".bdr{border:1px solid #000}");
        h.append(".bdr-b{border-bottom:1px solid #000}.bdr-l{border-left:1px solid #000}.bdr-r{border-right:1px solid #000}.bdr-t{border-top:1px solid #000}");
        h.append(".title{font-size:18px;font-weight:bold;text-align:center;border:2px solid #000;padding:8px}");
        h.append("@media print{body{padding:0}@page{size:A4;margin:10mm}}");
        h.append("</style></head><body>");

        // HEADER
        h.append("<table><tr>");
        h.append("<td width='50%' style='border:1px solid #000;border-right:none;padding:5px'>");
        h.append("<div class='b' style='font-size:14px'>").append(COMPANY_NAME).append("</div>");
        h.append("<div>").append(COMPANY_ADDR_LINE1).append("</div>");
        h.append("<div>").append(COMPANY_ADDR_LINE2).append("</div>");
        h.append("</td>");
        h.append("<td width='50%' class='title'>TAX INVOICE</td>");
        h.append("</tr></table>");

        // LEFT: Customer + RIGHT: Invoice meta
        h.append("<table><tr><td width='50%' style='vertical-align:top'>");
        h.append("<div class='b u' style='margin-top:4px'>Customer details:</div>");
        h.append("<div>").append(safe(inv.getCompanyName())).append("</div>");
        for (String al : addrLines) h.append("<div>").append(al).append("</div>");
        h.append("<div style='margin-top:4px' class='b'>CUSTOMER GSTIN: ").append(safe(inv.getCompanyGstin())).append("</div>");
        h.append("</td><td width='50%' style='vertical-align:top'>");
        h.append("<table>");
        h.append("<tr><td>GSTIN: ").append(GSTIN).append("</td><td>PAN No.: ").append(PAN).append("</td></tr>");
        h.append("<tr><td class='b'>INVOICE NO:</td><td class='bdr'>").append(safe(inv.getInvoiceNumber())).append("</td></tr>");
        h.append("<tr><td class='b'>DATE:</td><td class='bdr'>").append(fmtDate(inv)).append("</td></tr>");
        h.append("<tr><td class='b'>Passenger Name:</td><td>").append(safe(inv.getPassengerName())).append("</td></tr>");
        h.append("<tr><td class='b'>Mobile No.</td><td>").append(safe(inv.getMobile())).append("</td></tr>");
        h.append("<tr><td class='b'>PNR :</td><td>").append(safe(inv.getPnr())).append("</td></tr>");
        h.append("<tr><td class='b'>DATE OF TRAVEL:</td><td>").append(inv.getDateOfTravel() != null ? inv.getDateOfTravel().format(DateTimeFormatter.ofPattern("d.M.yyyy")) : "").append("</td></tr>");
        h.append("<tr><td class='b'>FROM:</td><td>").append(safe(inv.getFromCity())).append("</td><td>TO : ").append(safe(inv.getToCity())).append("</td></tr>");
        h.append("</table></td></tr></table>");

        // LINE ITEMS TABLE
        h.append("<table style='margin-top:8px'>");
        h.append("<tr class='b c'><th class='bdr'>Particulars</th><th class='bdr'>SAC code</th><th class='bdr'>Taxable Value</th><th class='bdr'>Non Taxable/Exemp</th><th class='bdr' colspan='2'>TOTAL</th></tr>");
        liRow(h, "Air Travel and related charges", sacAir, null, airTravel1, airTravel1);
        liRow(h, "Air Travel and related charges", "", null, airTravel2, airTravel2);
        liRow(h, "Passenger Service Fee", "", null, psf, psf);
        liRow(h, "User Devolopment Charges", "", null, udc, udc);
        liRow(h, "Agent service charges", sacAgent, agentCharges, null, agentCharges);
        liRow(h, "Other charges", "", null, otherCharges, otherCharges);
        BigDecimal discDisp = discount.compareTo(BigDecimal.ZERO) > 0 ? discount.negate() : BigDecimal.ZERO;
        liRow(h, "Discount", "", null, discDisp, discDisp);
        h.append("<tr class='b'><td class='bdr'>TOTAL</td><td class='bdr'></td><td class='bdr r'>").append(fmt(taxableTotal))
                .append("</td><td class='bdr r'>").append(fmt(nonTaxableTotal.subtract(discount)))
                .append("</td><td class='bdr r' colspan='2'>").append(fmt(subTotal)).append("</td></tr>");
        h.append("</table>");

        // BANK + TAX SUMMARY
        h.append("<table style='margin-top:8px'><tr><td width='60%' style='vertical-align:top'>");
        h.append("<div class='b u'>OUR BANK DETAILS:</div>");
        h.append("<div>A/C HOLDER NAME: RAMNETSOLUTIONS</div>");
        h.append("<div>CURRENT A/C NO.: 32602154473</div>");
        h.append("<div>BANK NAME: SBI, BRANCH: TELLAPUR</div>");
        h.append("<div>IFSC : SBIN0013071</div>");
        h.append("</td><td width='40%' style='vertical-align:top'>");
        h.append("<table width='100%'>");
        h.append("<tr><td class='b'>CGST</td><td class='r'>").append(fmt(cgst)).append("</td></tr>");
        h.append("<tr><td class='b'>SGST</td><td class='r'>").append(fmt(sgst)).append("</td></tr>");
        h.append("<tr style='border-top:1px solid #000'><td class='b'>Grand Total</td><td class='r b'>").append(fmt(grandTotal)).append("</td></tr>");
        h.append("</table></td></tr></table>");

        // TOTAL IN WORDS
        h.append("<div class='bdr' style='margin-top:4px;padding:4px'><span class='b'>TOTAL INVOICE VALUE IN WORDS: </span>").append(words).append("</div>");

        // Footer
        h.append("<table style='margin-top:8px'>");
        h.append("<tr><td width='70%' style='font-size:10px'><b>Air Travel and related Charges :-</b> Includes all Charges related to air transportation of passengers</td><td class='r b'>For RAMNET SOLUTIONS</td></tr>");
        h.append("<tr><td style='font-size:10px'><b>Airport Charges :-</b> Includes ADF, UDF and PSF collected on behalf of Airport Operator, as applicable</td><td></td></tr>");
        h.append("<tr><td style='font-size:10px'><b>Misc. Services :-</b> Includes Charges of Lounge Assistance and Travel Certificate</td><td></td></tr>");
        h.append("<tr><td style='font-size:10px'><b>Meal :-</b> Includes all prepaid meals purchased before travel</td><td class='r b'>Authorised Signatory</td></tr>");
        h.append("</table>");

        h.append("</body></html>");
        return h.toString();
    }

    private void liRow(StringBuilder h, String part, String sac, BigDecimal taxable, BigDecimal nonTax, BigDecimal total) {
        h.append("<tr><td class='bdr-l bdr-b bdr-r'>").append(part).append("</td>");
        h.append("<td class='bdr-b bdr-r r'>").append(safe(sac)).append("</td>");
        h.append("<td class='bdr-b bdr-r r'>").append(taxable != null && taxable.compareTo(BigDecimal.ZERO) != 0 ? fmt(taxable) : "").append("</td>");
        h.append("<td class='bdr-b bdr-r r'>").append(nonTax != null && nonTax.compareTo(BigDecimal.ZERO) != 0 ? fmt(nonTax) : "").append("</td>");
        h.append("<td class='bdr-b bdr-r r' colspan='2'>").append(total != null && total.compareTo(BigDecimal.ZERO) != 0 ? fmt(total) : "").append("</td></tr>");
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
