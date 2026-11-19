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
