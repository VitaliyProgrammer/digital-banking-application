import { cardholderName } from './transliterate';

describe('cardholderName', () => {
  it.each([
    ['Іван', 'Петренко', 'IVAN PETRENKO'],
    ['Юлія', 'Ярошенко', 'YULIIA YAROSHENKO'],
    ['Олексій', 'Згурський', 'OLEKSII ZGHURSKYI'],
    ['Євген', 'Щербань', 'YEVHEN SHCHERBAN'],
    ['John', 'Smith', 'JOHN SMITH'],
  ])('%s %s → %s', (firstName, lastName, expected) => {
    expect(cardholderName(firstName, lastName)).toBe(expected);
  });
});
