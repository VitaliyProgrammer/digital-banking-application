import { keepPreviousData, skipToken, useQuery } from '@tanstack/react-query';
import { paymentsApi } from '../api/paymentsApi';
import { usePaymentMutation } from './usePaymentMutation';

/** Where the money goes: an account by IBAN, or a card by its number. */
export type TransferKind = 'iban' | 'card';

export function useTransfer() {
  return usePaymentMutation(paymentsApi.transfer);
}

export function useCardTransfer() {
  return usePaymentMutation(paymentsApi.cardTransfer);
}

interface FeeQuoteRequest {
  kind: TransferKind;
  /** The account (by IBAN) or the card (card to card) the money leaves from. */
  sourceId: string;
  /** Undefined while the amount is not a valid one yet: nothing to price. */
  amount: number | undefined;
}

/** The fee the bank will charge, asked from the bank itself: the screen never guesses a tariff. */
export function useFeeQuote({ kind, sourceId, amount }: FeeQuoteRequest) {
  return useQuery({
    queryKey: ['fee', kind, sourceId, amount],
    queryFn:
      amount === undefined
        ? skipToken
        : () =>
            kind === 'iban'
              ? paymentsApi.transferFee(sourceId, amount)
              : paymentsApi.cardTransferFee(sourceId, amount),
    // While the next amount is priced, the line keeps the previous fee instead of blinking.
    placeholderData: keepPreviousData,
    staleTime: 60_000,
  });
}
