package com.travelbillpro.controller;

import com.travelbillpro.entity.Organization;
import com.travelbillpro.service.OrganizationService;
import com.travelbillpro.config.TenantDataSourceManager;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/super-admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final OrganizationService organizationService;
    private final TenantDataSourceManager dataSourceManager;

    /**
     * List all organizations.
     */
    @GetMapping("/organizations")
    public ResponseEntity<List<Organization>> listOrganizations() {
        return ResponseEntity.ok(organizationService.listAll());
    }

    /**
     * Get a single org by ID.
     */
    @GetMapping("/organizations/{id}")
    public ResponseEntity<Organization> getOrganization(@PathVariable Long id) {
        return ResponseEntity.ok(organizationService.getById(id));
    }

    /**
     * Create a new organization and provision its database.
     */
    @PostMapping("/organizations")
    public ResponseEntity<Organization> createOrganization(@RequestBody CreateOrgRequest req) {
        Organization org = organizationService.create(
                req.getName(), req.getSlug(), req.getDbUrl(),
                req.getAdminUsername(), req.getAdminPassword(), req.getAdminEmail()
        );
        return ResponseEntity.ok(org);
    }

    /**
     * Suspend an organization.
     */
    @PutMapping("/organizations/{id}/suspend")
    public ResponseEntity<Organization> suspendOrganization(@PathVariable Long id) {
        return ResponseEntity.ok(organizationService.suspend(id));
    }

    /**
     * Activate an organization.
     */
    @PutMapping("/organizations/{id}/activate")
    public ResponseEntity<Organization> activateOrganization(@PathVariable Long id) {
        return ResponseEntity.ok(organizationService.activate(id));
    }

    /**
     * Test database connection.
     */
    @PostMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection(@RequestBody Map<String, String> body) {
        String dbUrl = body.get("dbUrl");
        Map<String, Object> result = dataSourceManager.testConnection(dbUrl);
        return ResponseEntity.ok(result);
    }

    /**
     * Delete an organization (drops tenant DB tables, removes users, removes org).
     */
    @DeleteMapping("/organizations/{id}")
    public ResponseEntity<Map<String, String>> deleteOrganization(@PathVariable Long id) {
        // Evict cached DataSource before deletion
        dataSourceManager.evict(id);
        organizationService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Organization deleted successfully"));
    }

    @Data
    public static class CreateOrgRequest {
        private String name;
        private String slug;
        private String dbUrl;
        private String adminUsername;
        private String adminPassword;
        private String adminEmail;
    }
}
