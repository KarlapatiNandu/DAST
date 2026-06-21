import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

/**
 * =========================================================
 * MODULE 7: VITE CONFIGURATION
 * =========================================================
 *
 * Vite is the build tool and dev server for our React app.
 *
 * The `server.proxy` setting below solves a common problem in development:
 *
 * PROBLEM:
 *   React runs on http://localhost:5173
 *   Spring Boot runs on http://localhost:8080
 *   Browsers block cross-origin requests (CORS policy).
 *
 * SOLUTION:
 *   Tell Vite's dev server: "If the React app calls /api/...,
 *   silently forward that request to http://localhost:8080."
 *   The browser only ever talks to :5173, so no CORS issues.
 *
 * NOTE: In App.jsx we still use the full URL (http://localhost:8080/...)
 * so that ZAP can intercept it directly. If you want to use the proxy,
 * change the fetch URL to just /api/search?query=...
 */
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,  // React dev server port
    proxy: {
      // Any request starting with /api will be forwarded to Spring Boot
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,  // Changes the Host header to match the target
        // rewrite: (path) => path  // No path rewriting needed (keep /api prefix)
      }
    }
  }
})

