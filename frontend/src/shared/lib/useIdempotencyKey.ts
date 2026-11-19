import { useCallback, useRef } from 'react';

interface Attempt {
  request: string;
  key: string;
}

/**
 * One Idempotency-Key per payment the client means. A retry of the same request after a lost answer
 * sends the same key, so the bank pays once; a changed request gets a new key, because the bank
 * refuses a key reused for another payment.
 */
export function useIdempotencyKey() {
  const attempt = useRef<Attempt | null>(null);

  const keyFor = useCallback((request: unknown): string => {
    const fingerprint = JSON.stringify(request);
    if (attempt.current?.request !== fingerprint) {
      attempt.current = { request: fingerprint, key: crypto.randomUUID() };
    }
    return attempt.current.key;
  }, []);

  /** The bank has answered: the next payment, even an identical one, is a new payment. */
  const settle = useCallback(() => {
    attempt.current = null;
  }, []);

  return { keyFor, settle };
}
