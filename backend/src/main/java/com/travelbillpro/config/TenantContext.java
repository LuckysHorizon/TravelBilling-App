package com.travelbillpro.config;

/**
 * ThreadLocal holder for the current tenant context.
 * Set by JwtAuthenticationFilter and cleared after each request.
 */
public class TenantContext {

    private static final ThreadLocal<Long> CURRENT_ORG_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_DB_URL = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_ORG_SLUG = new ThreadLocal<>();

    public static void setOrgId(Long orgId) { CURRENT_ORG_ID.set(orgId); }
    public static Long getOrgId() { return CURRENT_ORG_ID.get(); }

    public static void setDbUrl(String dbUrl) { CURRENT_DB_URL.set(dbUrl); }
    public static String getDbUrl() { return CURRENT_DB_URL.get(); }

    public static void setOrgSlug(String slug) { CURRENT_ORG_SLUG.set(slug); }
    public static String getOrgSlug() { return CURRENT_ORG_SLUG.get(); }

    public static boolean isSuperAdminContext() {
        return CURRENT_ORG_ID.get() == null;
    }

    public static void clear() {
        CURRENT_ORG_ID.remove();
        CURRENT_DB_URL.remove();
        CURRENT_ORG_SLUG.remove();
    }
}
