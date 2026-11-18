/** No look-alikes (0/O, 1/l/I): a generated password may still have to be typed in by hand once. */
const ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%&*?';

/**
 * A strong password from the browser's cryptographic generator (Math.random is predictable).
 * Bytes above the largest multiple of the alphabet size are skipped, so every character is equally likely.
 */
export function generatePassword(length = 16): string {
  const limit = 256 - (256 % ALPHABET.length);
  let password = '';

  while (password.length < length) {
    for (const byte of crypto.getRandomValues(new Uint8Array(length * 2))) {
      if (byte < limit && password.length < length) {
        password += ALPHABET[byte % ALPHABET.length];
      }
    }
  }

  return password;
}
