import { formatIban, hasValidIbanChecksum, ibanHalves, looksLikeUaIban, shortIban } from './iban';

/** Built like the bank builds them: bank code 300999, check digits by ISO 13616. */
const VALID = 'UA273009991234567890123456789';

describe('looksLikeUaIban', () => {
  it('accepts UA and 27 digits, typed with spaces or in small letters', () => {
    expect(looksLikeUaIban(VALID)).toBe(true);
    expect(looksLikeUaIban('ua27 3009 9912 3456 7890 1234 5678 9')).toBe(true);
  });

  it('refuses another country, a short number and letters inside', () => {
    expect(looksLikeUaIban('PL273009991234567890123456789')).toBe(false);
    expect(looksLikeUaIban('UA2730099912345678901234567')).toBe(false);
    expect(looksLikeUaIban('UA27300999123456789012345678X')).toBe(false);
  });
});

describe('hasValidIbanChecksum', () => {
  it('passes an IBAN with correct check digits', () => {
    expect(hasValidIbanChecksum(VALID)).toBe(true);
    expect(hasValidIbanChecksum(formatIban(VALID))).toBe(true);
  });

  it('catches one wrong digit and two swapped neighbours', () => {
    expect(hasValidIbanChecksum('UA273009991234567890123456788')).toBe(false);
    expect(hasValidIbanChecksum('UA273009991234567890123456798')).toBe(false);
  });
});

describe('shortIban', () => {
  it('keeps the country with the check digits and the last four digits, joined by non-breaking spaces', () => {
    expect(shortIban('UA213223130000026007233566001')).toBe('UA21\u00A0…\u00A06001');
  });
});

describe('formatIban', () => {
  it('splits the number into groups of four, the last one shorter', () => {
    expect(formatIban('UA213223130000026007233566001')).toBe('UA21 3223 1300 0002 6007 2335 6600 1');
  });

  it('accepts a number that is already split', () => {
    expect(formatIban('UA21 3223 1300 0002 6007 2335 6600 1')).toBe('UA21 3223 1300 0002 6007 2335 6600 1');
  });
});

describe('ibanHalves', () => {
  it('splits a Ukrainian IBAN between whole groups, the longer half first', () => {
    expect(ibanHalves('UA213223130000026007233566001')).toEqual(['UA21 3223 1300 0002', '6007 2335 6600 1']);
  });
});
