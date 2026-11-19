import { z } from 'zod';
import { parseAmountInput } from '@/shared/lib/amountInput';
import { compactCardNumber, looksLikeCardNumber, passesLuhn } from '@/shared/lib/cardNumber';
import { compactIban, hasValidIbanChecksum, looksLikeUaIban } from '@/shared/lib/iban';
import type { TransferKind } from './transfers';

/** The bank's limits (TransferRequestDto, CardTransferRequestDto). */
export const TRANSFER_MIN: Record<TransferKind, number> = { iban: 0.01, card: 1 };
export const DESCRIPTION_MAX = 140;

/**
 * The check digits catch a typo before the bank is asked: a wrong digit would otherwise come back
 * as "recipient not found", or worse, reach somebody else.
 */
const recipientRules: Record<TransferKind, z.ZodType<string, string>> = {
  iban: z
    .string()
    .trim()
    .min(1, 'validation.required')
    .refine(looksLikeUaIban, { error: 'validation.iban', abort: true })
    .refine(hasValidIbanChecksum, 'validation.ibanChecksum')
    .transform(compactIban),
  card: z
    .string()
    .trim()
    .min(1, 'validation.required')
    .refine(looksLikeCardNumber, { error: 'validation.cardNumber', abort: true })
    .refine(passesLuhn, 'validation.cardNumberChecksum')
    .transform(compactCardNumber),
};

const AMOUNT_TOO_SMALL: Record<TransferKind, string> = {
  iban: 'validation.amountPositive',
  card: 'validation.cardTransferMin',
};

/** The amount as typed ("1 500,50") becomes a number, or an error key. */
export function amountSchema(kind: TransferKind) {
  return z
    .string()
    .trim()
    .min(1, 'validation.required')
    .transform(parseAmountInput)
    .pipe(z.number({ error: 'validation.amount' }).min(TRANSFER_MIN[kind], AMOUNT_TOO_SMALL[kind]));
}

export function transferSchema(kind: TransferKind) {
  return z.object({
    recipient: recipientRules[kind],
    amount: amountSchema(kind),
    description: z
      .string()
      .trim()
      .max(DESCRIPTION_MAX, 'validation.tooLong')
      // Left empty, the bank writes its own: "Переказ на рахунок UA…" or "Переказ на картку *7894".
      .transform((text) => text || undefined),
  });
}

/** What the fields hold: the text as typed. */
export type TransferFormInput = z.input<ReturnType<typeof transferSchema>>;

/** What the form hands over: the recipient without spaces and the amount as a number. */
export type TransferFormOutput = z.output<ReturnType<typeof transferSchema>>;
