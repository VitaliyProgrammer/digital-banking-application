const STORAGE_KEY = 'taborovets.rememberedEmail';

/** Only the email is remembered, never the password: the browser's password manager is the place for that. */
export function readRememberedEmail(): string {
  try {
    return localStorage.getItem(STORAGE_KEY) ?? '';
  } catch {
    return '';
  }
}

export function rememberEmail(email: string | null): void {
  try {
    if (email) {
      localStorage.setItem(STORAGE_KEY, email);
    } else {
      localStorage.removeItem(STORAGE_KEY);
    }
  } catch {
    // Not critical: the client types the email next time.
  }
}
