/** A Ukrainian IBAN: UA, two check digits and 25 digits more (bank code and account number). */
const UA_IBAN = /^UA\d{27}$/;

/** As typed or pasted: without spaces, in capitals. */
export function compactIban(value: string): string {
  return value.replace(/\s/g, '').toUpperCase();
}

export function looksLikeUaIban(value: string): boolean {
  return UA_IBAN.test(compactIban(value));
}

/**
 * ISO 13616: the country and check digits move to the end, letters become numbers (A = 10 … Z = 35),
 * and a correct IBAN leaves 1 when divided by 97. The number is too long for a double, so the remainder
 * is carried digit by digit. A single wrong digit or two swapped neighbours never pass.
 */
export function hasValidIbanChecksum(value: string): boolean {
  const iban = compactIban(value);
  const rearranged = iban.slice(4) + iban.slice(0, 4);
  const digits = rearranged.replace(/[A-Z]/g, (letter) => String(letter.charCodeAt(0) - 55));

  let remainder = 0;
  for (const digit of digits) {
    remainder = (remainder * 10 + Number(digit)) % 97;
  }
  return remainder === 1;
}

/** "UA21 3223 1300 0002 6007 2335 6600 1": groups of four, the way IBANs are printed and dictated. */
export function formatIban(iban: string): string {
  return iban.replace(/\s/g, '').replace(/(.{4})(?=.)/g, '$1 ');
}

/** "UA21 … 6001": enough to tell one's accounts apart, never split between two lines. */
export function shortIban(iban: string): string {
  const compact = iban.replace(/\s/g, '');
  return `${compact.slice(0, 4)}\u00A0…\u00A0${compact.slice(-4)}`;
}

/** The printed IBAN in two halves of whole groups: where it may wrap, if the line is too narrow. */
export function ibanHalves(iban: string): [string, string] {
  const groups = formatIban(iban).split(' ');
  const middle = Math.ceil(groups.length / 2);
  return [groups.slice(0, middle).join(' '), groups.slice(middle).join(' ')];
}
