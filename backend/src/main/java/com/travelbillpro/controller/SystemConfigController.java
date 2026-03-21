package com.travelbillpro.controller;

import com.travelbillpro.entity.SystemConfig;
import com.travelbillpro.entity.User;
import com.travelbillpro.repository.SystemConfigRepository;
import com.travelbillpro.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/system-config")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigRepository systemConfigRepository;

    @GetMapping
    public ResponseEntity<Map<String, String>> getAllConfig() {
        List<SystemConfig> configs = systemConfigRepository.findAll();
        Map<String, String> result = new HashMap<>();
        for (SystemConfig config : configs) {
            result.put(config.getKey(), config.getValue());
        }
        return ResponseEntity.ok(result);
    }

    @PutMapping
    public ResponseEntity<Map<String, String>> updateConfig(
            @RequestBody Map<String, String> updates,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            SystemConfig config = systemConfigRepository.findById(entry.getKey())
                    .orElseGet(() -> {
                        SystemConfig newConfig = new SystemConfig();
                        newConfig.setKey(entry.getKey());
                        return newConfig;
                    });
            config.setValue(entry.getValue());
            config.setUpdatedBy(user);
            systemConfigRepository.save(config);
        }
        return getAllConfig();
    }
}
