import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api/slack': {
        target: 'http://localhost:8080',
        rewrite: (path) => path.replace(/^\/api\/slack/, '/slack'),
      },
      '/api': {
        target: 'http://localhost:8080',
      },
    },
  },
});
