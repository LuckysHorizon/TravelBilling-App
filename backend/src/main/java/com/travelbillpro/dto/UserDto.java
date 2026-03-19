package com.travelbillpro.dto;

import com.travelbillpro.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

public class UserDto {

    @Data
    public static class CreateUserRequest {
        @NotBlank(message = "Username is required")
        private String username;
        
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
        
        @NotBlank(message = "Password is required")
        private String password;
        
        @NotNull(message = "Role is required")
        private Role role;
    }

    @Data
    public static class UpdateUserRequest {
        private String email;
        private Role role;
        private Boolean isActive;
        private Boolean unlockAccount; // If true, sets lockedUntil and failedAttempts to 0
    }
}
