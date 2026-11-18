import '@testing-library/jest-dom/vitest';
import '@/shared/i18n/i18n';
import { cleanup } from '@testing-library/react';
import { afterEach, vi } from 'vitest';
import { clearTokens } from '@/shared/api/tokenStore';

afterEach(() => {
  cleanup();
  clearTokens();
  localStorage.clear();
  sessionStorage.clear();
  vi.unstubAllGlobals();
});
