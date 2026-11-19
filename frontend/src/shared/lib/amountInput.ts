/** Digits with up to two after a comma or a dot: "1500", "1500,5", "1500.50". */
const AMOUNT = /^\d+([.,]\d{1,2})?$/;

/**
 * What the client typed in an amount field, as a number, or NaN when it is not an amount.
 * Spaces are allowed, so "1 500,50" copied from a statement works too.
 */
export function parseAmountInput(text: string): number {
  const compact = text.replace(/\s/g, '');
  return AMOUNT.test(compact) ? Number(compact.replace(',', '.')) : Number.NaN;
}
