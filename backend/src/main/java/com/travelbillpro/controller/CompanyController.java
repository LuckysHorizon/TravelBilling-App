package com.travelbillpro.controller;

import com.travelbillpro.dto.CompanyDto;
import com.travelbillpro.security.CustomUserDetails;
import com.travelbillpro.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<Page<CompanyDto.CompanyResponse>> getAllCompanies(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(companyService.getAllCompanies(search, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyDto.CompanyResponse> getCompanyById(@PathVariable Long id) {
        return ResponseEntity.ok(companyService.getCompanyById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public ResponseEntity<CompanyDto.CompanyResponse> createCompany(
            @Valid @RequestBody CompanyDto.CreateCompanyRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return new ResponseEntity<>(companyService.createCompany(request, userDetails.getUser()), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public ResponseEntity<CompanyDto.CompanyResponse> updateCompany(
            @PathVariable Long id,
            @Valid @RequestBody CompanyDto.CreateCompanyRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(companyService.updateCompany(id, request, userDetails.getUser()));
    }
}
