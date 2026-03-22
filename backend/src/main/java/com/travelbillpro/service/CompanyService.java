package com.travelbillpro.service;

import com.travelbillpro.dto.CompanyDto;
import com.travelbillpro.entity.Company;
import com.travelbillpro.entity.User;
import com.travelbillpro.enums.InvoiceStatus;
import com.travelbillpro.enums.TicketStatus;
import com.travelbillpro.exception.BusinessException;
import com.travelbillpro.repository.CompanyRepository;
import com.travelbillpro.repository.InvoiceRepository;
import com.travelbillpro.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final InvoiceRepository invoiceRepository;
    private final TicketRepository ticketRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<CompanyDto.CompanyResponse> getAllCompanies(String search, Pageable pageable) {
        Page<Company> companies;
        if (search != null && !search.trim().isEmpty()) {
            companies = companyRepository.findByNameContainingIgnoreCase(search, pageable);
        } else {
            companies = companyRepository.findAll(pageable);
        }
        return companies.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Company getCompanyEntity(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Company not found", "COMPANY_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
    
    @Transactional(readOnly = true)
    public CompanyDto.CompanyResponse getCompanyById(Long id) {
        return mapToResponse(getCompanyEntity(id));
    }

    @Transactional
    public CompanyDto.CompanyResponse createCompany(CompanyDto.CreateCompanyRequest request, User user) {
        if (companyRepository.existsByGstNumber(request.getGstNumber())) {
            throw new BusinessException("Company with GST number already exists", "GST_EXISTS", HttpStatus.CONFLICT);
        }

        Company company = new Company();
        company.setName(request.getName());
        company.setGstNumber(request.getGstNumber());
        company.setBillingEmail(request.getBillingEmail());
        company.setAddress(request.getAddress());
        company.setContactName(request.getContactName());
        company.setPhone(request.getPhone());
        company.setCity(request.getCity());
        company.setState(request.getState());
        company.setPinCode(request.getPinCode());
        company.setServiceChargePct(request.getServiceChargePct());
        company.setBillingCycle(request.getBillingCycle());
        company.setCreditLimit(request.getCreditLimit());
        company.setPdfStoragePath(request.getPdfStoragePath());
        company.setCreatedBy(user);
        company.setIsActive(true);

        Company savedCompany = companyRepository.save(company);
        auditService.logAction("COMPANY", savedCompany.getId(), "CREATED", null, request, user);
        
        return mapToResponse(savedCompany);
    }

    @Transactional
    public CompanyDto.CompanyResponse updateCompany(Long id, CompanyDto.CreateCompanyRequest request, User user) {
        Company company = getCompanyEntity(id);

        if (!company.getGstNumber().equals(request.getGstNumber()) &&
                companyRepository.existsByGstNumber(request.getGstNumber())) {
            throw new BusinessException("Company with GST number already exists", "GST_EXISTS", HttpStatus.CONFLICT);
        }

        CompanyDto.CompanyResponse oldValue = mapToResponse(company);

        company.setName(request.getName());
        company.setGstNumber(request.getGstNumber());
        company.setBillingEmail(request.getBillingEmail());
        company.setAddress(request.getAddress());
        company.setContactName(request.getContactName());
        company.setPhone(request.getPhone());
        company.setCity(request.getCity());
        company.setState(request.getState());
        company.setPinCode(request.getPinCode());
        company.setServiceChargePct(request.getServiceChargePct());
        company.setBillingCycle(request.getBillingCycle());
        company.setCreditLimit(request.getCreditLimit());
        company.setPdfStoragePath(request.getPdfStoragePath());

        Company savedCompany = companyRepository.save(company);
        CompanyDto.CompanyResponse newValue = mapToResponse(savedCompany);
        
        auditService.logAction("COMPANY", savedCompany.getId(), "UPDATED", oldValue, newValue, user);
        
        return newValue;
    }

    private CompanyDto.CompanyResponse mapToResponse(Company company) {
        CompanyDto.CompanyResponse response = new CompanyDto.CompanyResponse();
        response.setId(company.getId());
        response.setName(company.getName());
        response.setGstNumber(company.getGstNumber());
        response.setBillingEmail(company.getBillingEmail());
        response.setAddress(company.getAddress());
        response.setContactName(company.getContactName());
        response.setPhone(company.getPhone());
        response.setCity(company.getCity());
        response.setState(company.getState());
        response.setPinCode(company.getPinCode());
        response.setServiceChargePct(company.getServiceChargePct());
        response.setBillingCycle(company.getBillingCycle());
        response.setCreditLimit(company.getCreditLimit());
        response.setPdfStoragePath(company.getPdfStoragePath());
        response.setActive(company.getIsActive());
        response.setCreatedAt(company.getCreatedAt());
        response.setCreatedById(company.getCreatedBy() != null ? company.getCreatedBy().getId() : null);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCompanyStats(Long companyId) {
        getCompanyEntity(companyId); // validate company exists

        LocalDate now = LocalDate.now();
        LocalDate startOfYear = now.withMonth(1).withDayOfMonth(1);
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        // Total billed YTD = sum of all PAID + SENT invoices this year
        BigDecimal totalBilledYtd = invoiceRepository.sumGrandTotalByCompanyIdAndStatusIn(
                companyId, List.of(InvoiceStatus.SENT, InvoiceStatus.PAID));
        if (totalBilledYtd == null) totalBilledYtd = BigDecimal.ZERO;

        // Tickets this month
        long ticketsThisMonth = ticketRepository.countByCompanyIdAndCreatedAtBetween(
                companyId, startOfMonth.atStartOfDay(), endOfMonth.atTime(23, 59, 59));

        // Outstanding = sent but not paid
        BigDecimal outstandingBalance = invoiceRepository.sumGrandTotalByCompanyIdAndStatus(
                companyId, InvoiceStatus.SENT);
        if (outstandingBalance == null) outstandingBalance = BigDecimal.ZERO;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalBilledYtd", totalBilledYtd);
        stats.put("ticketsThisMonth", ticketsThisMonth);
        stats.put("outstandingBalance", outstandingBalance);
        return stats;
    }
}
