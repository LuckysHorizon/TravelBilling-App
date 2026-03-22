package com.travelbillpro.service;

import com.travelbillpro.entity.Organization;
import com.travelbillpro.entity.User;
import com.travelbillpro.enums.Role;
import com.travelbillpro.exception.BusinessException;
import com.travelbillpro.repository.OrganizationRepository;
import com.travelbillpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {

    private final OrganizationRepository orgRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantProvisioningService provisioningService;

    @Transactional(readOnly = true)
    public List<Organization> listAll() {
        return orgRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Organization getById(Long id) {
        return orgRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Organization not found", "ORG_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public Organization getBySlug(String slug) {
        return orgRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException("Organization not found", "ORG_NOT_FOUND", HttpStatus.NOT_FOUND));
    }

    /**
     * Create a new organization and provision its database.
     */
    @Transactional
    public Organization create(String name, String slug, String dbUrl,
                                String adminUsername, String adminPassword, String adminEmail) {
        // Validate slug uniqueness
        if (orgRepository.existsBySlug(slug)) {
            throw new BusinessException("Slug already exists: " + slug, "SLUG_EXISTS", HttpStatus.CONFLICT);
        }

        // Validate username uniqueness in master DB
        if (userRepository.findByUsername(adminUsername).isPresent()) {
            throw new BusinessException("Username already exists: " + adminUsername, "USERNAME_EXISTS", HttpStatus.CONFLICT);
        }

        // Create org record
        Organization org = new Organization();
        org.setName(name);
        org.setSlug(slug.toLowerCase().replaceAll("[^a-z0-9\\-]", ""));
        org.setDbUrl(dbUrl);
        org.setAdminEmail(adminEmail);
        org.setStatus("PROVISIONING");
        org = orgRepository.save(org);

        // Provision tenant database
        try {
            String provLog = provisioningService.provision(dbUrl, adminUsername, adminPassword, adminEmail);
            org.setProvisioningLog(provLog);
            org.setStatus("ACTIVE");
        } catch (Exception e) {
            org.setProvisioningLog("FAILED: " + e.getMessage());
            org.setStatus("FAILED");
            orgRepository.save(org);
            throw new BusinessException("Provisioning failed: " + e.getMessage(), "PROVISIONING_FAILED", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // Create user record in master DB for auth routing
        User orgAdmin = new User();
        orgAdmin.setUsername(adminUsername);
        orgAdmin.setEmail(adminEmail);
        orgAdmin.setPasswordHash(passwordEncoder.encode(adminPassword));
        orgAdmin.setRole(Role.ADMIN);
        orgAdmin.setOrganization(org);
        userRepository.save(orgAdmin);

        return orgRepository.save(org);
    }

    @Transactional
    public Organization suspend(Long id) {
        Organization org = getById(id);
        if ("default".equals(org.getSlug())) {
            throw new BusinessException("Cannot suspend default organization", "CANNOT_SUSPEND_DEFAULT", HttpStatus.BAD_REQUEST);
        }
        org.setStatus("SUSPENDED");
        return orgRepository.save(org);
    }

    @Transactional
    public Organization activate(Long id) {
        Organization org = getById(id);
        org.setStatus("ACTIVE");
        return orgRepository.save(org);
    }

    /**
     * Delete an organization: drop all tables in tenant DB, remove users, remove org record.
     */
    @Transactional
    public void delete(Long id) {
        Organization org = getById(id);
        if ("default".equals(org.getSlug())) {
            throw new BusinessException("Cannot delete the default organization", "CANNOT_DELETE_DEFAULT", HttpStatus.BAD_REQUEST);
        }

        log.info("Deleting organization: {} ({})", org.getName(), org.getSlug());

        // 1. Drop all tables in tenant DB
        try {
            provisioningService.dropAllTables(org.getDbUrl());
            log.info("✓ Dropped all tables in tenant DB for org {}", org.getSlug());
        } catch (Exception e) {
            log.warn("Could not drop tables in tenant DB (may be unreachable): {}", e.getMessage());
        }

        // 3. Remove all users belonging to this org from master DB
        var orgUsers = userRepository.findAllByOrganization(org);
        if (!orgUsers.isEmpty()) {
            userRepository.deleteAll(orgUsers);
            log.info("✓ Removed {} users from master DB for org {}", orgUsers.size(), org.getSlug());
        }

        // 4. Delete org record
        orgRepository.delete(org);
        log.info("✓ Organization {} deleted from master DB", org.getSlug());
    }
}
