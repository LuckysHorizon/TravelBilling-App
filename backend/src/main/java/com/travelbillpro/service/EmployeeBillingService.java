package com.travelbillpro.service;

import com.travelbillpro.dto.EmployeeBillingDto;
import com.travelbillpro.entity.*;
import com.travelbillpro.enums.InvoiceStatus;
import com.travelbillpro.enums.TicketStatus;
import com.travelbillpro.exception.BusinessException;
import com.travelbillpro.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeBillingService {

    private final TicketRepository ticketRepository;
    private final CompanyRepository companyRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository lineItemRepository;

    /**
     * Get unique passenger names from APPROVED tickets for a given company.
     */
    @Transactional(readOnly = true)
    public List<String> getUniquePassengers(Long companyId) {
        List<Ticket> tickets = ticketRepository.findByCompanyIdAndStatus(companyId, TicketStatus.APPROVED);
        return tickets.stream()
                .map(Ticket::getPassengerName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Fetch approved tickets for a company + passenger name + date range.
     */
    @Transactional(readOnly = true)
    public List<EmployeeBillingDto.TicketForBilling> getTicketsForBilling(Long companyId, String passengerName, LocalDate from, LocalDate to) {
        List<Ticket> tickets = ticketRepository.findByCompanyIdAndPassengerNameIgnoreCaseAndTravelDateBetween(
                companyId, passengerName, from, to);

        // Also include BILLED tickets if user wants to re-bill (optional, filter APPROVED only)
        // For now, include APPROVED + BILLED
        return tickets.stream().map(this::mapTicketForBilling).collect(Collectors.toList());
    }

    @Transactional
    public EmployeeBillingDto.InvoiceResponse createInvoice(EmployeeBillingDto.CreateInvoiceRequest request) {
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new BusinessException("Company not found", "COMPANY_NOT_FOUND", HttpStatus.NOT_FOUND));

        // Determine invoice number
        String invoiceNumber = request.getInvoiceNumber();
        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            invoiceNumber = getNextInvoiceNumber();
        }

        BigDecimal cgstRate = request.getCgstRate() != null ? request.getCgstRate() : new BigDecimal("9.00");
        BigDecimal sgstRate = request.getSgstRate() != null ? request.getSgstRate() : new BigDecimal("9.00");

        // Calculate totals from line items
        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalNonTaxable = BigDecimal.ZERO;
        BigDecimal subTotal = BigDecimal.ZERO;

        if (request.getLineItems() != null) {
            for (EmployeeBillingDto.LineItemRequest li : request.getLineItems()) {
                BigDecimal tv = li.getTaxableValue() != null ? li.getTaxableValue() : BigDecimal.ZERO;
                BigDecimal ntv = li.getNonTaxableValue() != null ? li.getNonTaxableValue() : BigDecimal.ZERO;
                BigDecimal tot = li.getTotal() != null ? li.getTotal() : tv.add(ntv);
                totalTaxable = totalTaxable.add(tv);
                totalNonTaxable = totalNonTaxable.add(ntv);
                subTotal = subTotal.add(tot);
            }
        }

        BigDecimal cgstAmount = totalTaxable.multiply(cgstRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal sgstAmount = totalTaxable.multiply(sgstRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = subTotal.add(cgstAmount).add(sgstAmount);

        // Create invoice
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setCompany(company);
        invoice.setInvoiceDate(request.getInvoiceDate() != null ? request.getInvoiceDate() : LocalDate.now());
        invoice.setBillingPeriodStart(request.getBillingPeriodFrom());
        invoice.setBillingPeriodEnd(request.getBillingPeriodTo());
        invoice.setBillingMonth(request.getBillingPeriodFrom() != null ?
                request.getBillingPeriodFrom().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")) : "");
        invoice.setDueDate(invoice.getInvoiceDate().plusDays(15));
        invoice.setSubtotal(subTotal);
        invoice.setServiceCharge(totalTaxable);
        invoice.setCgstRate(cgstRate);
        invoice.setSgstRate(sgstRate);
        invoice.setCgstTotal(cgstAmount);
        invoice.setSgstTotal(sgstAmount);
        invoice.setGrandTotal(grandTotal);
        invoice.setTotalInWords(convertToWords(grandTotal));
        invoice.setStatus(InvoiceStatus.DRAFT);

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Create line items
        List<InvoiceLineItem> lineItems = new ArrayList<>();
        if (request.getLineItems() != null) {
            for (EmployeeBillingDto.LineItemRequest liReq : request.getLineItems()) {
                InvoiceLineItem li = new InvoiceLineItem();
                li.setInvoice(savedInvoice);
                if (liReq.getTicketId() != null) {
                    Ticket t = ticketRepository.findById(liReq.getTicketId()).orElse(null);
                    li.setTicket(t);
                }
                li.setParticulars(liReq.getParticulars());
                li.setSacCode(liReq.getSacCode());
                li.setTaxableValue(liReq.getTaxableValue() != null ? liReq.getTaxableValue() : BigDecimal.ZERO);
                li.setNonTaxableValue(liReq.getNonTaxableValue() != null ? liReq.getNonTaxableValue() : BigDecimal.ZERO);
                li.setTotal(liReq.getTotal() != null ? liReq.getTotal() : li.getTaxableValue().add(li.getNonTaxableValue()));
                li.setIsManuallyAdded(liReq.getIsManuallyAdded() != null && liReq.getIsManuallyAdded());
                lineItems.add(li);
            }
        }
        lineItemRepository.saveAll(lineItems);
        savedInvoice.setLineItems(lineItems);

        // Store passenger info in response
        EmployeeBillingDto.InvoiceResponse response = mapToResponse(savedInvoice);
        response.setPassengerName(request.getPassengerName());
        response.setMobile(request.getMobile());
        return response;
    }

    @Transactional(readOnly = true)
    public EmployeeBillingDto.InvoiceResponse getInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Invoice not found", "INVOICE_NOT_FOUND", HttpStatus.NOT_FOUND));
        return mapToResponse(invoice);
    }

    public String getNextInvoiceNumber() {
        String maxNumber = "0";
        try {
            List<Invoice> all = invoiceRepository.findAll();
            for (Invoice inv : all) {
                try {
                    long num = Long.parseLong(inv.getInvoiceNumber());
                    if (num > Long.parseLong(maxNumber)) {
                        maxNumber = String.valueOf(num);
                    }
                } catch (NumberFormatException ignored) {}
            }
        } catch (Exception ignored) {}
        return String.valueOf(Long.parseLong(maxNumber) + 1);
    }

    private EmployeeBillingDto.InvoiceResponse mapToResponse(Invoice invoice) {
        EmployeeBillingDto.InvoiceResponse r = new EmployeeBillingDto.InvoiceResponse();
        r.setId(invoice.getId());
        r.setInvoiceNumber(invoice.getInvoiceNumber());
        r.setInvoiceDate(invoice.getInvoiceDate());
        r.setBillingPeriodFrom(invoice.getBillingPeriodStart());
        r.setBillingPeriodTo(invoice.getBillingPeriodEnd());
        r.setCompanyId(invoice.getCompany().getId());
        r.setCompanyName(invoice.getCompany().getName());
        r.setCompanyAddress(invoice.getCompany().getAddress());
        r.setCompanyGstin(invoice.getCompany().getGstNumber());
        r.setSubtotal(invoice.getSubtotal());
        r.setCgstRate(invoice.getCgstRate());
        r.setSgstRate(invoice.getSgstRate());
        r.setCgstAmount(invoice.getCgstTotal());
        r.setSgstAmount(invoice.getSgstTotal());
        r.setGrandTotal(invoice.getGrandTotal());
        r.setTotalInWords(invoice.getTotalInWords());
        r.setStatus(invoice.getStatus().name());
        if (invoice.getLineItems() != null) {
            r.setLineItems(invoice.getLineItems().stream().map(this::mapLineItem).collect(Collectors.toList()));
        }
        return r;
    }

    private EmployeeBillingDto.LineItemResponse mapLineItem(InvoiceLineItem li) {
        EmployeeBillingDto.LineItemResponse r = new EmployeeBillingDto.LineItemResponse();
        r.setId(li.getId());
        r.setTicketId(li.getTicket() != null ? li.getTicket().getId() : null);
        r.setParticulars(li.getParticulars());
        r.setSacCode(li.getSacCode());
        r.setTaxableValue(li.getTaxableValue());
        r.setNonTaxableValue(li.getNonTaxableValue());
        r.setTotal(li.getTotal());
        r.setIsManuallyAdded(li.getIsManuallyAdded());
        return r;
    }

    private EmployeeBillingDto.TicketForBilling mapTicketForBilling(Ticket t) {
        EmployeeBillingDto.TicketForBilling r = new EmployeeBillingDto.TicketForBilling();
        r.setId(t.getId());
        r.setPnr(t.getPnrNumber());
        r.setDateOfTravel(t.getTravelDate());
        r.setOrigin(t.getOrigin());
        r.setDestination(t.getDestination());
        r.setPassengerName(t.getPassengerName());
        r.setBaseFare(t.getBaseFare() != null ? t.getBaseFare() : BigDecimal.ZERO);
        r.setPassengerServiceFee(t.getPassengerServiceFee() != null ? t.getPassengerServiceFee() : BigDecimal.ZERO);
        r.setUserDevelopmentCharges(t.getUserDevelopmentCharges() != null ? t.getUserDevelopmentCharges() : BigDecimal.ZERO);
        r.setAgentServiceCharges(t.getAgentServiceCharges() != null ? t.getAgentServiceCharges() : BigDecimal.ZERO);
        r.setOtherCharges(t.getOtherCharges() != null ? t.getOtherCharges() : BigDecimal.ZERO);
        r.setDiscount(t.getDiscount() != null ? t.getDiscount() : BigDecimal.ZERO);
        r.setSacCodeAir(t.getSacCodeAir() != null ? t.getSacCodeAir() : "996425");
        r.setSacCodeAgent(t.getSacCodeAgent() != null ? t.getSacCodeAgent() : "998551");
        return r;
    }

    public static String convertToWords(BigDecimal amount) {
        long rupees = amount.longValue();
        int paise = amount.remainder(BigDecimal.ONE).multiply(new BigDecimal(100)).intValue();

        String[] ones = {"", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE",
                "TEN", "ELEVEN", "TWELVE", "THIRTEEN", "FOURTEEN", "FIFTEEN", "SIXTEEN", "SEVENTEEN", "EIGHTEEN", "NINETEEN"};
        String[] tens = {"", "", "TWENTY", "THIRTY", "FORTY", "FIFTY", "SIXTY", "SEVENTY", "EIGHTY", "NINETY"};

        if (rupees == 0) return "ZERO ONLY";

        StringBuilder sb = new StringBuilder();
        if (rupees >= 10000000) {
            sb.append(twoDigitWords(rupees / 10000000, ones, tens)).append(" CRORE ");
            rupees %= 10000000;
        }
        if (rupees >= 100000) {
            sb.append(twoDigitWords(rupees / 100000, ones, tens)).append(" LAKH ");
            rupees %= 100000;
        }
        if (rupees >= 1000) {
            sb.append(twoDigitWords(rupees / 1000, ones, tens)).append(" THOUSAND ");
            rupees %= 1000;
        }
        if (rupees >= 100) {
            sb.append(ones[(int)(rupees / 100)]).append(" HUNDRED ");
            rupees %= 100;
        }
        if (rupees > 0) {
            sb.append(twoDigitWords(rupees, ones, tens)).append(" ");
        }

        sb.append("ONLY");
        if (paise > 0) {
            sb.insert(sb.length() - 4, "AND " + twoDigitWords(paise, ones, tens) + " PAISE ");
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private static String twoDigitWords(long n, String[] ones, String[] tens) {
        if (n < 20) return ones[(int) n];
        return tens[(int)(n / 10)] + (n % 10 != 0 ? " " + ones[(int)(n % 10)] : "");
    }
}
