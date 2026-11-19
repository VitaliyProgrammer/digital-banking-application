const CARD_NUMBER = /^\d{16}$/;

/** As typed: "4999 9912 3456 7894" and "4999-9912-3456-7894" are the same card. */
export function compactCardNumber(value: string): string {
  return value.replace(/[\s-]/g, '');
}

export function looksLikeCardNumber(value: string): boolean {
  return CARD_NUMBER.test(compactCardNumber(value));
}

/** "4999 9912 3456 7894": the way the number is printed on the card. */
export function formatCardNumber(value: string): string {
  return compactCardNumber(value).replace(/(\d{4})(?=\d)/g, '$1 ');
}

/**
 * Luhn (mod 10), the check digit of every Visa and Mastercard: from the right, every second digit is doubled
 * (minus 9 when it grows to two digits), and the sum of all digits must end in 0.
 */
export function passesLuhn(value: string): boolean {
  const digits = compactCardNumber(value);

  let sum = 0;
  for (let i = digits.length - 1, doubled = false; i >= 0; i--, doubled = !doubled) {
    let digit = Number(digits[i]);
    if (doubled) {
      digit *= 2;
      if (digit > 9) {
        digit -= 9;
      }
    }
    sum += digit;
  }
  return sum % 10 === 0;
}
