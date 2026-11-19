import { parseAmountInput } from './amountInput';

describe('parseAmountInput', () => {
  it.each([
    ['1500', 1500],
    ['1500,5', 1500.5],
    ['1500.50', 1500.5],
    ['1 500,50', 1500.5],
    ['0,01', 0.01],
  ])('reads "%s" as %d', (text, amount) => {
    expect(parseAmountInput(text)).toBe(amount);
  });

  it.each(['', 'abc', '10,555', '-5', '1,2,3', '1e3'])('refuses "%s"', (text) => {
    expect(parseAmountInput(text)).toBeNaN();
  });
});
