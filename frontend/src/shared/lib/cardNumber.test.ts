import { formatCardNumber, looksLikeCardNumber, passesLuhn } from './cardNumber';

/** Built like the bank builds them: BIN 499999, the last digit by Luhn. */
const VALID = '4999991234567894';

describe('looksLikeCardNumber', () => {
  it('accepts 16 digits, with spaces or dashes between the groups', () => {
    expect(looksLikeCardNumber(VALID)).toBe(true);
    expect(looksLikeCardNumber('4999 9912 3456 7894')).toBe(true);
    expect(looksLikeCardNumber('4999-9912-3456-7894')).toBe(true);
  });

  it('refuses fewer digits and letters', () => {
    expect(looksLikeCardNumber('499999123456789')).toBe(false);
    expect(looksLikeCardNumber('49999912345678a4')).toBe(false);
  });
});

describe('passesLuhn', () => {
  it('passes a number with the right check digit', () => {
    expect(passesLuhn(VALID)).toBe(true);
  });

  it('catches one wrong digit and two swapped neighbours', () => {
    expect(passesLuhn('4999991234567893')).toBe(false);
    expect(passesLuhn('4999991234567849')).toBe(false);
  });
});

describe('formatCardNumber', () => {
  it('splits the number into four groups', () => {
    expect(formatCardNumber('4999-991234567894')).toBe('4999 9912 3456 7894');
  });
});
