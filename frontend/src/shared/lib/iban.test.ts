import { formatIban, ibanHalves, shortIban } from './iban';

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
