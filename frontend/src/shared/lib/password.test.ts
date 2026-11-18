import { generatePassword } from './password';

describe('generatePassword', () => {
  it('makes a password of the asked length without look-alike characters', () => {
    const password = generatePassword(32);

    expect(password).toHaveLength(32);
    expect(password).not.toMatch(/[0O1lI]/);
  });

  it('never repeats itself', () => {
    const passwords = new Set(Array.from({ length: 50 }, () => generatePassword()));

    expect(passwords.size).toBe(50);
  });
});
