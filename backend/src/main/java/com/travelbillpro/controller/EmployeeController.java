package com.travelbillpro.controller;

import com.travelbillpro.dto.EmployeeDto;
import com.travelbillpro.entity.Company;
import com.travelbillpro.entity.Employee;
import com.travelbillpro.exception.BusinessException;
import com.travelbillpro.repository.CompanyRepository;
import com.travelbillpro.repository.EmployeeRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;

    @GetMapping("/search")
    public ResponseEntity<List<EmployeeDto.EmployeeSearchResult>> searchEmployees(@RequestParam String q) {
        if (q == null || q.trim().length() < 2) {
            return ResponseEntity.ok(List.of());
        }
        List<Employee> employees = employeeRepository.searchByNameOrMobile(q.trim());
        List<EmployeeDto.EmployeeSearchResult> results = employees.stream()
                .map(this::mapToSearchResult)
                .collect(Collectors.toList());
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDto.EmployeeResponse> getEmployee(@PathVariable Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Employee not found", "EMPLOYEE_NOT_FOUND", HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(mapToResponse(employee));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public ResponseEntity<EmployeeDto.EmployeeResponse> createEmployee(@Valid @RequestBody EmployeeDto.CreateEmployeeRequest request) {
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new BusinessException("Company not found", "COMPANY_NOT_FOUND", HttpStatus.NOT_FOUND));

        Employee employee = new Employee();
        employee.setName(request.getName());
        employee.setMobile(request.getMobile());
        employee.setCompany(company);
        employee.setIsFrequentFlyer(request.getIsFrequentFlyer());

        Employee saved = employeeRepository.save(employee);
        return new ResponseEntity<>(mapToResponse(saved), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public ResponseEntity<EmployeeDto.EmployeeResponse> updateEmployee(
            @PathVariable Long id, @Valid @RequestBody EmployeeDto.CreateEmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Employee not found", "EMPLOYEE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!employee.getCompany().getId().equals(request.getCompanyId())) {
            Company newCompany = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new BusinessException("Company not found", "COMPANY_NOT_FOUND", HttpStatus.NOT_FOUND));
            employee.setCompany(newCompany);
        }

        employee.setName(request.getName());
        employee.setMobile(request.getMobile());
        employee.setIsFrequentFlyer(request.getIsFrequentFlyer());

        Employee saved = employeeRepository.save(employee);
        return ResponseEntity.ok(mapToResponse(saved));
    }

    private EmployeeDto.EmployeeResponse mapToResponse(Employee e) {
        EmployeeDto.EmployeeResponse r = new EmployeeDto.EmployeeResponse();
        r.setId(e.getId());
        r.setName(e.getName());
        r.setMobile(e.getMobile());
        r.setCompanyId(e.getCompany().getId());
        r.setCompanyName(e.getCompany().getName());
        r.setCompanyAddress(e.getCompany().getAddress());
        r.setCompanyGstin(e.getCompany().getGstNumber());
        r.setIsFrequentFlyer(e.getIsFrequentFlyer());
        r.setCreatedAt(e.getCreatedAt());
        return r;
    }

    private EmployeeDto.EmployeeSearchResult mapToSearchResult(Employee e) {
        EmployeeDto.EmployeeSearchResult r = new EmployeeDto.EmployeeSearchResult();
        r.setId(e.getId());
        r.setName(e.getName());
        r.setMobile(e.getMobile());
        r.setCompanyId(e.getCompany().getId());
        r.setCompanyName(e.getCompany().getName());
        r.setIsFrequentFlyer(e.getIsFrequentFlyer());
        return r;
    }
}
