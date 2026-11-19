import { pinSchema } from './pinSchema';

describe('pinSchema', () => {
  it('asks only for the new PIN twice when the card has no PIN yet', () => {
    expect(pinSchema(false).safeParse({ currentPin: '', newPin: '4825', repeatPin: '4825' }).success).toBe(
      true,
    );
  });

  it('asks for the current PIN when the card already has one', () => {
    const result = pinSchema(true).safeParse({ currentPin: '', newPin: '4825', repeatPin: '4825' });

    expect(result.error?.issues.map((issue) => issue.path.join('.'))).toEqual(['currentPin']);
  });

  it('refuses a repeat that differs', () => {
    const result = pinSchema(false).safeParse({ currentPin: '', newPin: '4825', repeatPin: '4826' });

    expect(result.error?.issues.map((issue) => issue.message)).toEqual(['validation.pinMismatch']);
  });

  it('names every mistake at once: a PIN that is not four digits and a repeat that differs', () => {
    const result = pinSchema(false).safeParse({ currentPin: '', newPin: '48a', repeatPin: '4826' });

    expect(result.error?.issues.map((issue) => issue.message)).toEqual([
      'validation.pin',
      'validation.pinMismatch',
    ]);
  });
});
