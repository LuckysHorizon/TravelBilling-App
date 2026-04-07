import axios from 'axios';

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const apiPrefix = `${apiBaseUrl}/api`;

// Create an Axios instance
const api = axios.create({
  baseURL: apiPrefix,
  withCredentials: true, // Important for cookies (JWT)
  headers: {
    'Content-Type': 'application/json',
  },
});

// Response interceptor to handle token refresh
api.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error) => {
    const originalRequest = error.config;

    // If error is 401 (Unauthorized) and we haven't already retried
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      // Don't try to refresh if the failed request was a login or refresh itself
      if (originalRequest.url.includes('/auth/login') || originalRequest.url.includes('/auth/refresh')) {
        return Promise.reject(error);
      }

      try {
        // Attempt to refresh the token using the HttpOnly refresh cookie
        await axios.post(`${apiPrefix}/auth/refresh`, {}, { withCredentials: true });
        
        // If successful, retry the original request
        return api(originalRequest);
      } catch (refreshError) {
        // If refresh fails, we need to log out the user
        // We can dispatch a logout action, or simply redirect to login
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default api;
