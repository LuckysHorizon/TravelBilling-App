package com.travelbillpro.security;

import com.travelbillpro.config.TenantContext;
import com.travelbillpro.entity.User;
import com.travelbillpro.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads user details for Spring Security authentication.
 * 
 * ALWAYS queries the MASTER database — users are stored there,
 * regardless of which tenant is active. No @Transactional here
 * so that TenantContext.clear() takes effect before any connection
 * is obtained from the DataSource.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Save and clear tenant context to ensure we query the MASTER DB
        Long savedOrgId = TenantContext.getOrgId();
        String savedDbUrl = TenantContext.getDbUrl();
        String savedOrgSlug = TenantContext.getOrgSlug();

        try {
            TenantContext.clear();

            User user = userRepository.findByUsernameWithOrg(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

            return new CustomUserDetails(user);
        } finally {
            // Restore context (important for JWT filter which sets context after this call)
            if (savedOrgId != null) {
                TenantContext.setOrgId(savedOrgId);
                TenantContext.setDbUrl(savedDbUrl);
                TenantContext.setOrgSlug(savedOrgSlug);
            }
        }
    }
}
