import { z } from 'zod';

const pin = z.string().regex(/^\d{4}$/, 'validation.pin');

export interface PinForm {
  currentPin: string;
  newPin: string;
  repeatPin: string;
}

/**
 * The first PIN needs only the new one twice; a change needs the current PIN too. Whether a PIN is too easy
 * to guess (1111, 1234) is the bank's rule, so its answer is shown as it comes.
 */
export function pinSchema(hasPin: boolean) {
  return z
    .object({
      currentPin: hasPin ? pin : z.string(),
      newPin: pin,
      repeatPin: z.string(),
    })
    .refine((form) => form.newPin === form.repeatPin, {
      message: 'validation.pinMismatch',
      path: ['repeatPin'],
    });
}
