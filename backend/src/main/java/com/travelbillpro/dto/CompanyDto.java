package com.travelbillpro.dto;

import com.travelbillpro.enums.BillingCycle;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CompanyDto {

    @Data
    public static class CreateCompanyRequest {
        @NotBlank(message = "Company name is required")
        private String name;

        @NotBlank(message = "GST Number is required")
        private String gstNumber;

        @NotBlank(message = "Billing Email is required")
        @Email(message = "Invalid email format")
        private String billingEmail;

        private String address;
        private String contactName;
        private String phone;
        private String city;
        private String state;
        private String pinCode;

        @NotNull(message = "Service Charge Percentage is required")
        private BigDecimal serviceChargePct;

        @NotNull(message = "Billing Cycle is required")
        private BillingCycle billingCycle;

        private BigDecimal creditLimit;
        
        private String pdfStoragePath;
    }

    @Data
    public static class CompanyResponse {
        private Long id;
        private String name;
        private String gstNumber;
        private String billingEmail;
        private String address;
        private String contactName;
        private String phone;
        private String city;
        private String state;
        private String pinCode;
        private BigDecimal serviceChargePct;
        private BillingCycle billingCycle;
        private BigDecimal creditLimit;
        private Boolean active; // frontend uses 'active' not 'isActive'
        private String pdfStoragePath;
        private LocalDateTime createdAt;
        private Long createdById;
    }
}
