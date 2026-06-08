import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import config from '../config';

// ── Axios instance with centralized config ──────────────────────────────────
const api = axios.create({
  baseURL: config.apiUrl,
  withCredentials: true, // Important for cookies (JWT)
  timeout: 30000,        // 30s default timeout
  headers: {
    'Content-Type': 'application/json',
  },
});

// ── Retry config ────────────────────────────────────────────────────────────
const MAX_RETRIES = 2;
const RETRY_DELAY_MS = 1000;

interface RetryConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
  _retryCount?: number;
}

// ── Response interceptor ────────────────────────────────────────────────────
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryConfig;
    if (!originalRequest) return Promise.reject(error);

    const status = error.response?.status;
    const isAuthEndpoint =
      originalRequest.url?.includes('/auth/login') ||
      originalRequest.url?.includes('/auth/refresh');

    // ── 401: Token expired → attempt refresh ──────────────────────────────
    if (status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      if (isAuthEndpoint) {
        return Promise.reject(error);
      }

      try {
        await axios.post(`${config.apiUrl}/auth/refresh`, {}, { withCredentials: true });
        return api(originalRequest);
      } catch {
        // Refresh failed → redirect to login
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    }

    // ── 429: Rate limited ─────────────────────────────────────────────────
    if (status === 429) {
      const retryAfter = error.response?.headers?.['retry-after'] || '60';
      console.warn(`[API] Rate limited. Retry after ${retryAfter}s`);
      return Promise.reject(error);
    }

    // ── 5xx: Server error → retry with backoff ────────────────────────────
    if (status && status >= 500 && !isAuthEndpoint) {
      const retryCount = originalRequest._retryCount || 0;
      if (retryCount < MAX_RETRIES) {
        originalRequest._retryCount = retryCount + 1;
        console.warn(`[API] Server error ${status}, retry ${retryCount + 1}/${MAX_RETRIES}`);
        await new Promise((r) => setTimeout(r, RETRY_DELAY_MS * (retryCount + 1)));
        return api(originalRequest);
      }
      console.error(`[API] Server error ${status} after ${MAX_RETRIES} retries`);
    }

    // ── Network error (no response at all) ────────────────────────────────
    if (!error.response && error.code !== 'ERR_CANCELED') {
      console.error('[API] Network error — server may be unreachable');
    }

    return Promise.reject(error);
  }
);

export default api;
