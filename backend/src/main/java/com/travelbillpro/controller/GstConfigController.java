package com.travelbillpro.controller;

import com.travelbillpro.entity.GstConfig;
import com.travelbillpro.entity.User;
import com.travelbillpro.repository.GstConfigRepository;
import com.travelbillpro.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/gst-config")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class GstConfigController {

    private final GstConfigRepository gstConfigRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCurrentGstConfig() {
        GstConfig config = gstConfigRepository.findActiveConfig().orElse(null);
        Map<String, Object> result = new HashMap<>();
        if (config != null) {
            result.put("id", config.getId());
            result.put("cgstRate", config.getCgstRate());
            result.put("sgstRate", config.getSgstRate());
            result.put("serviceChargePerTicket", config.getServiceChargePerTicket());
            result.put("effectiveFrom", config.getEffectiveFrom());
        }
        return ResponseEntity.ok(result);
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> updateGstConfig(
            @RequestBody Map<String, Object> updates,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userDetails.getUser();

        GstConfig config = new GstConfig();
        config.setCgstRate(toBigDecimal(updates.get("cgstRate"), BigDecimal.ZERO));
        config.setSgstRate(toBigDecimal(updates.get("sgstRate"), BigDecimal.ZERO));
        config.setServiceChargePerTicket(toBigDecimal(updates.get("serviceChargePerTicket"), BigDecimal.ZERO));
        config.setEffectiveFrom(LocalDate.now());
        config.setCreatedById(user.getId());
        gstConfigRepository.save(config);

        return getCurrentGstConfig();
    }

    private BigDecimal toBigDecimal(Object val, BigDecimal fallback) {
        if (val == null) return fallback;
        try {
            return new BigDecimal(val.toString());
        } catch (Exception e) {
            return fallback;
        }
    }
}
