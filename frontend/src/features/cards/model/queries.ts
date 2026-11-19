import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  cardsApi,
  type Card,
  type CardAction,
  type ChangePinRequest,
  type IssueCardRequest,
} from '../api/cardsApi';

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

/** Every change of a card is answered with the card list refreshed: statuses and the PIN flag come from the bank. */
function useRefreshCards() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: cardKeys.all });
}

export function useIssueCard() {
  const refreshCards = useRefreshCards();
  return useMutation({
    mutationFn: (issue: IssueCardRequest) => cardsApi.issue(issue),
    onSuccess: refreshCards,
  });
}

export function useCardAction(cardId: string) {
  const refreshCards = useRefreshCards();
  return useMutation({
    mutationFn: (action: CardAction) => cardsApi.act(cardId, action),
    onSuccess: refreshCards,
  });
}

export function useChangePin(cardId: string) {
  const refreshCards = useRefreshCards();
  return useMutation({
    mutationFn: (pin: ChangePinRequest) => cardsApi.changePin(cardId, pin),
    // Also after a failure: a wrong current PIN may have locked the card, and the page must show it at once.
    onSettled: refreshCards,
  });
}
