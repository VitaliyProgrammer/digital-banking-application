import type { Currency } from '@/shared/lib/money';
import type { Account } from '../api/accountsApi';

/** The order of the accounts page: hryvnia first, as the mockup's currency panel does. */
export const ACCOUNT_CURRENCIES: readonly Currency[] = ['UAH', 'USD', 'EUR'];

export function isCurrency(value: string | null): value is Currency {
  return ACCOUNT_CURRENCIES.some((currency) => currency === value);
}

/**
 * The client's everyday account in each currency, or undefined where none is opened yet. Savings jars,
 * the business account and the tax jar have their own sections.
 */
export function clientAccountsByCurrency(accounts: Account[]): Record<Currency, Account | undefined> {
  const byCurrency = (currency: Currency) =>
    accounts.find((account) => account.type === 'CLIENT' && account.currency === currency);

  return { UAH: byCurrency('UAH'), USD: byCurrency('USD'), EUR: byCurrency('EUR') };
}
