package com.travelbillpro.controller;

import com.travelbillpro.dto.EmployeeBillingDto;
import com.travelbillpro.service.EmployeeBillingService;
import com.travelbillpro.service.EmployeeInvoiceExcelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employee-billing")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
public class EmployeeBillingController {

    private final EmployeeBillingService billingService;
    private final EmployeeInvoiceExcelService excelService;

    /**
     * Get unique passenger names from approved tickets for a company.
     */
    @GetMapping("/passengers")
    public ResponseEntity<List<String>> getPassengersForCompany(@RequestParam Long companyId) {
        return ResponseEntity.ok(billingService.getUniquePassengers(companyId));
    }

    /**
     * Fetch approved tickets for a company + passenger + date range.
     */
    @GetMapping("/tickets")
    public ResponseEntity<List<EmployeeBillingDto.TicketForBilling>> getTicketsForBilling(
            @RequestParam Long companyId,
            @RequestParam String passengerName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(billingService.getTicketsForBilling(companyId, passengerName, from, to));
    }

    @PostMapping("/invoices")
    public ResponseEntity<EmployeeBillingDto.InvoiceResponse> createInvoice(
            @Valid @RequestBody EmployeeBillingDto.CreateInvoiceRequest request) {
        return ResponseEntity.ok(billingService.createInvoice(request));
    }

    @GetMapping("/invoices/{id}")
    public ResponseEntity<EmployeeBillingDto.InvoiceResponse> getInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(billingService.getInvoice(id));
    }

    @GetMapping("/invoices/next-number")
    public ResponseEntity<Map<String, String>> getNextInvoiceNumber() {
        return ResponseEntity.ok(Map.of("nextNumber", billingService.getNextInvoiceNumber()));
    }

    @GetMapping("/invoices/{id}/export/xlsx")
    public ResponseEntity<byte[]> exportExcel(@PathVariable Long id) {
        EmployeeBillingDto.InvoiceResponse invoice = billingService.getInvoice(id);
        byte[] excelBytes = excelService.generateExcel(invoice);

        String filename = String.format("Invoice_%s_%s_%s.xlsx",
                invoice.getInvoiceNumber(),
                invoice.getPassengerName() != null ? invoice.getPassengerName().replace(" ", "_") : "Employee",
                invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM")) : "");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @GetMapping("/invoices/{id}/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        EmployeeBillingDto.InvoiceResponse invoice = billingService.getInvoice(id);
        byte[] pdfBytes = excelService.generatePdf(invoice);

        String filename = String.format("Invoice_%s_%s_%s.pdf",
                invoice.getInvoiceNumber(),
                invoice.getPassengerName() != null ? invoice.getPassengerName().replace(" ", "_") : "Employee",
                invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM")) : "");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
