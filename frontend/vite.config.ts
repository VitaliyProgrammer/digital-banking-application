/// <reference types="vitest/config" />
import { fileURLToPath, URL } from 'node:url';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

const BACKEND_URL = 'http://localhost:8080';

const API_PREFIXES = [
  '/authentication',
  '/users',
  '/accounts',
  '/cards',
  '/payments',
  '/bill-payments',
  '/exchange',
  '/exchange-rates',
  '/savings',
  '/cashback',
  '/credit-line',
  '/credit-applications',
  '/business',
  '/tax',
  '/notifications',
  '/support',
  '/admin',
];

export default defineConfig({
  base: '/app/',
  plugins: [react()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: Object.fromEntries(API_PREFIXES.map((prefix) => [prefix, BACKEND_URL])),
  },
  build: {
    outDir: '../src/main/resources/static/app',
    emptyOutDir: true,
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    css: { modules: { classNameStrategy: 'non-scoped' } },
  },
});
