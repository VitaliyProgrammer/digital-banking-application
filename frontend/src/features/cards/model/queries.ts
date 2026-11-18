import { useQuery } from '@tanstack/react-query';
import { cardsApi, type Card } from '../api/cardsApi';

export const cardKeys = {
  all: ['cards'] as const,
  details: (cardId: string) => ['cards', cardId, 'details'] as const,
};

/** How long the full card number and CVV stay on the screen. */
export const CARD_DETAILS_VISIBLE_MS = 60_000;

export function useCards() {
  return useQuery({ queryKey: cardKeys.all, queryFn: cardsApi.list });
}

/**
 * The full number and CVV, asked only when the client turns the card over. They are never cached:
 * once hidden, they are gone from memory until the next turn.
 */
export function useCardDetails(cardId: string, enabled: boolean) {
  return useQuery({
    queryKey: cardKeys.details(cardId),
    queryFn: () => cardsApi.details(cardId),
    enabled,
    gcTime: 0,
    staleTime: 0,
  });
}

/** The card the home screen shows: the universal card if the client has an active one, otherwise the first card. */
export function primaryCard(cards: Card[]): Card | undefined {
  const usable = cards.filter((card) => card.status !== 'CLOSED');
  return (
    usable.find((card) => card.type === 'UNIVERSAL' && card.status === 'ACTIVE') ??
    usable.find((card) => card.status === 'ACTIVE') ??
    usable[0]
  );
}
