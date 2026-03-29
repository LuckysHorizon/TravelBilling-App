package com.travelbillpro.service;

import com.travelbillpro.config.TenantContext;
import com.travelbillpro.dto.UserDto;
import com.travelbillpro.entity.Organization;
import com.travelbillpro.entity.User;
import com.travelbillpro.exception.BusinessException;
import com.travelbillpro.repository.OrganizationRepository;
import com.travelbillpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * User management service.
 *
 * IMPORTANT: No @Transactional on methods here.
 * Users are always stored in the MASTER database (because login/auth queries master).
 * When an org admin calls these methods, TenantContext routes queries to the tenant DB.
 * We must clear TenantContext BEFORE any DB call, and Spring Data JPA's
 * SimpleJpaRepository provides its own @Transactional on each repository method,
 * so each call independently resolves the correct (master) DataSource.
 *
 * If we used @Transactional here, Spring would bind a connection at method entry
 * (before we can clear TenantContext), locking the entire transaction to the tenant DB.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Clears TenantContext and returns the saved values.
     * Call restoreTenantContext() in a finally block after DB operations.
     */
    private Long[] switchToMaster() {
        Long orgId = TenantContext.getOrgId();
        String dbUrl = TenantContext.getDbUrl();
        String orgSlug = TenantContext.getOrgSlug();
        TenantContext.clear();
        // Pack into array: [orgId] — dbUrl and slug stored as thread-local side data
        // Store them so we can restore later
        return new Long[]{orgId};
    }

    private void restoreTenantContext(Long orgId, String dbUrl, String orgSlug) {
        if (orgId != null) {
            TenantContext.setOrgId(orgId);
            TenantContext.setDbUrl(dbUrl);
            TenantContext.setOrgSlug(orgSlug);
        }
    }

    public Page<User> getAllUsers(Pageable pageable) {
        Long callerOrgId = TenantContext.getOrgId();
        String savedDbUrl = TenantContext.getDbUrl();
        String savedOrgSlug = TenantContext.getOrgSlug();
        log.info("getAllUsers called — callerOrgId={}, savedOrgSlug={}", callerOrgId, savedOrgSlug);
        try {
            TenantContext.clear();
            Page<User> result;
            if (callerOrgId != null) {
                // Org admin — only show users belonging to their org
                result = userRepository.findByOrganizationId(callerOrgId, pageable);
                log.info("Org-scoped query for orgId={} returned {} users", callerOrgId, result.getTotalElements());
            } else {
                // Super admin — show all users
                result = userRepository.findAll(pageable);
                log.info("Super admin query returned {} users", result.getTotalElements());
            }
            return result;
        } finally {
            restoreTenantContext(callerOrgId, savedDbUrl, savedOrgSlug);
        }
    }

    public User getUserById(Long id) {
        Long savedOrgId = TenantContext.getOrgId();
        String savedDbUrl = TenantContext.getDbUrl();
        String savedOrgSlug = TenantContext.getOrgSlug();
        try {
            TenantContext.clear();
            return userRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));
        } finally {
            restoreTenantContext(savedOrgId, savedDbUrl, savedOrgSlug);
        }
    }

    /**
     * Creates a user in the MASTER database so they can log in.
     * Links the user to the calling admin's organization.
     */
    public User createUser(UserDto.CreateUserRequest request) {
        // Capture org context BEFORE clearing
        Long callerOrgId = TenantContext.getOrgId();
        String savedDbUrl = TenantContext.getDbUrl();
        String savedOrgSlug = TenantContext.getOrgSlug();

        try {
            // Clear tenant context — ALL subsequent DB calls go to MASTER
            TenantContext.clear();

            log.info("Creating user '{}' — TenantContext cleared, routing to master DB", request.getUsername());

            if (userRepository.existsByUsername(request.getUsername())) {
                throw new BusinessException("Username already exists", "USERNAME_EXISTS", HttpStatus.CONFLICT);
            }
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException("Email already exists", "EMAIL_EXISTS", HttpStatus.CONFLICT);
            }

            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setRole(request.getRole());
            user.setIsActive(true);
            user.setFailedAttempts(0);

            // Link user to the calling admin's org so login produces correct JWT claims
            if (callerOrgId != null) {
                Organization org = organizationRepository.findById(callerOrgId).orElse(null);
                if (org != null) {
                    user.setOrganization(org);
                    log.info("Linking user '{}' to org '{}' (id={})",
                            request.getUsername(), org.getName(), org.getId());
                }
            }

            User saved = userRepository.save(user);
            log.info("User '{}' created successfully in master DB with id={}", saved.getUsername(), saved.getId());
            return saved;
        } finally {
            restoreTenantContext(callerOrgId, savedDbUrl, savedOrgSlug);
        }
    }

    public User updateUser(Long id, UserDto.UpdateUserRequest request) {
        Long savedOrgId = TenantContext.getOrgId();
        String savedDbUrl = TenantContext.getDbUrl();
        String savedOrgSlug = TenantContext.getOrgSlug();
        try {
            TenantContext.clear();

            User user = userRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

            if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
                if (userRepository.existsByEmail(request.getEmail())) {
                    throw new BusinessException("Email already exists", "EMAIL_EXISTS", HttpStatus.CONFLICT);
                }
                user.setEmail(request.getEmail());
            }

            if (request.getRole() != null) {
                user.setRole(request.getRole());
            }

            if (request.getIsActive() != null) {
                user.setIsActive(request.getIsActive());
            }

            if (Boolean.TRUE.equals(request.getUnlockAccount())) {
                user.setFailedAttempts(0);
                user.setLockedUntil(null);
            }

            return userRepository.save(user);
        } finally {
            restoreTenantContext(savedOrgId, savedDbUrl, savedOrgSlug);
        }
    }

    public void deleteUser(Long id) {
        Long savedOrgId = TenantContext.getOrgId();
        String savedDbUrl = TenantContext.getDbUrl();
        String savedOrgSlug = TenantContext.getOrgSlug();
        try {
            TenantContext.clear();

            User user = userRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));
            if (user.getUsername().equals("admin")) {
                throw new BusinessException("Cannot delete primary admin account", "CANNOT_DELETE_ADMIN", HttpStatus.FORBIDDEN);
            }
            userRepository.delete(user);
        } finally {
            restoreTenantContext(savedOrgId, savedDbUrl, savedOrgSlug);
        }
    }
}
