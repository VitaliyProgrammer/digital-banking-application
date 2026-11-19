import { z } from 'zod';
import { parseAmountInput } from '@/shared/lib/amountInput';

/** The bank's limits of one top-up (TopUpRequestDto). */
export const TOP_UP_MIN = 0.01;
export const TOP_UP_MAX = 100_000;

export const topUpSchema = z.object({
  amount: z
    .string()
    .trim()
    .min(1, 'validation.required')
    .transform(parseAmountInput)
    .pipe(
      z
        .number({ error: 'validation.amount' })
        .min(TOP_UP_MIN, 'validation.topUpRange')
        .max(TOP_UP_MAX, 'validation.topUpRange'),
    ),
});

/** What the field holds: the text as typed. */
export type TopUpFormInput = z.input<typeof topUpSchema>;

/** What the form hands over after validation: the amount as a number. */
export type TopUpFormOutput = z.output<typeof topUpSchema>;
