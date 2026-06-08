/**
 * TravelBill Pro — Centralized Application Configuration
 * ═══════════════════════════════════════════════════════
 *
 * This is the SINGLE source of truth for all environment variables.
 * Import this module instead of accessing `import.meta.env` directly.
 *
 * Usage:
 *   import config from '@/config';       // or '../config'
 *   console.log(config.apiUrl);          // "http://localhost:8080/api"
 *   console.log(config.isDev);           // true (in development)
 */

// ── Helper ──────────────────────────────────────────────────────
function stripTrailingSlash(url: string): string {
  return url.replace(/\/+$/, '');
}

// ── Configuration Object ────────────────────────────────────────
const config = {
  // ── Environment ───────────────────────────────────────────────
  /** Current Vite mode: "development" | "production" */
  env: import.meta.env.MODE as 'development' | 'production',

  /** True when running `npm run dev` */
  isDev: import.meta.env.DEV,

  /** True when running `npm run build` / production serve */
  isProd: import.meta.env.PROD,

  // ── API URLs ──────────────────────────────────────────────────
  /** Backend base URL (no trailing slash, no /api suffix) */
  apiBaseUrl: stripTrailingSlash(
    import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  ),

  /** Full API prefix including /api path — use this for all API calls */
  get apiUrl(): string {
    return `${this.apiBaseUrl}/api`;
  },

  /** PDF Extractor service URL (no trailing slash) */
  pdfServiceUrl: stripTrailingSlash(
    import.meta.env.VITE_PDF_SERVICE_URL || 'http://localhost:8000'
  ),

  // ── App Metadata ──────────────────────────────────────────────
  appName: 'TravelBill Pro',
  appVersion: import.meta.env.VITE_APP_VERSION || '0.0.0',
} as const;

// ── Dev-mode logging ────────────────────────────────────────────
if (config.isDev) {
  console.log(
    '%c[Config]%c Environment loaded',
    'color: #6366f1; font-weight: bold',
    'color: inherit',
    {
      env: config.env,
      apiBaseUrl: config.apiBaseUrl,
      pdfServiceUrl: config.pdfServiceUrl,
    }
  );
}

export default config;
