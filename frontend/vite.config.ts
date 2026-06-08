import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  // Load env files from monorepo root (.env.development / .env.production)
  // as well as local frontend-level overrides
  const rootEnv = loadEnv(mode, path.resolve(__dirname, '..'), '');
  const localEnv = loadEnv(mode, __dirname, '');

  return {
    plugins: [react()],

    // ── Path Aliases ──────────────────────────────────────────
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src'),
      },
    },

    // ── Dev Server ────────────────────────────────────────────
    server: {
      port: 3000,
      strictPort: false,
      // Proxy API calls in dev to avoid CORS issues
      proxy: mode === 'development' ? {
        '/api': {
          target: localEnv.VITE_API_BASE_URL || rootEnv.VITE_API_BASE_URL || 'http://localhost:8080',
          changeOrigin: true,
        },
      } : undefined,
    },

    // ── Build Optimization ────────────────────────────────────
    build: {
      // Sourcemaps only in development for debugging
      sourcemap: mode !== 'production',
      rollupOptions: {
        output: {
          // Vendor chunking for better cache performance
          manualChunks: {
            vendor: ['react', 'react-dom', 'react-router-dom'],
            ui: ['antd', '@ant-design/icons'],
          },
        },
      },
    },
  };
});
