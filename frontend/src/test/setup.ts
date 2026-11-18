import '@testing-library/jest-dom/vitest';
import '@/shared/i18n/i18n';
import { cleanup, configure } from '@testing-library/react';
import { afterEach, vi } from 'vitest';
import { clearTokens } from '@/shared/api/tokenStore';

// Tests render the whole application with several requests per screen: one second is too tight on a busy machine.
configure({ asyncUtilTimeout: 3000 });

afterEach(() => {
  cleanup();
  clearTokens();
  localStorage.clear();
  sessionStorage.clear();
  vi.unstubAllGlobals();
});
