package com.travelbillpro.controller;

import com.travelbillpro.dto.DashboardStatsDto;
import com.travelbillpro.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard-stats")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(reportService.getDashboardStats());
    }

    @GetMapping("/revenue-trend")
    public ResponseEntity<List<Map<String, Object>>> getRevenueTrend() {
        return ResponseEntity.ok(reportService.getRevenueTrend());
    }

    @GetMapping("/client-breakdown")
    public ResponseEntity<List<Map<String, Object>>> getClientBreakdown() {
        return ResponseEntity.ok(reportService.getClientBreakdown());
    }
}
