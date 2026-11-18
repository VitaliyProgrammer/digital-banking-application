import { formatMoney, formatSignedMoney } from './money';

const NBSP = '\u00A0';

describe('money', () => {
  it('writes hryvnias the way the approved mockup does, without line breaks inside', () => {
    expect(formatMoney('48215.3', 'UAH', 'uk')).toBe(`48${NBSP}215,30${NBSP}₴`);
  });

  it('puts the dollar and euro signs before the number', () => {
    expect(formatMoney(1240, 'USD', 'uk')).toBe(`$${NBSP}1${NBSP}240,00`);
    expect(formatMoney('380.5', 'EUR', 'en')).toBe(`€${NBSP}380.50`);
  });

  it('signs operations with a real minus, not a hyphen', () => {
    expect(formatSignedMoney(-250, 'UAH', 'uk')).toBe(`−250,00${NBSP}₴`);
    expect(formatSignedMoney(2000, 'UAH', 'uk')).toBe(`+2${NBSP}000,00${NBSP}₴`);
  });
});
