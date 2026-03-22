package com.travelbillpro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

public class EmployeeDto {

    @Data
    public static class CreateEmployeeRequest {
        @NotBlank(message = "Employee name is required")
        private String name;
        private String mobile;
        @NotNull(message = "Company ID is required")
        private Long companyId;
        private Boolean isFrequentFlyer = false;
    }

    @Data
    public static class EmployeeResponse {
        private Long id;
        private String name;
        private String mobile;
        private Long companyId;
        private String companyName;
        private String companyAddress;
        private String companyGstin;
        private Boolean isFrequentFlyer;
        private LocalDateTime createdAt;
    }

    @Data
    public static class EmployeeSearchResult {
        private Long id;
        private String name;
        private String mobile;
        private Long companyId;
        private String companyName;
        private Boolean isFrequentFlyer;
    }
}
