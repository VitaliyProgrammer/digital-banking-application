import { request } from '@/shared/api/http';

export type CardType = 'UNIVERSAL' | 'BUSINESS' | 'PAYROLL' | 'STUDENT' | 'VIRTUAL' | 'CREDIT';

export type CardStatus = 'ACTIVE' | 'BLOCKED' | 'PIN_LOCKED' | 'CLOSED';

export interface Card {
  id: string;
  accountId: string;
  type: CardType;
  status: CardStatus;
  /** "**** **** **** 2048": the bank never sends the full number in a list. */
  maskedNumber: string;
  /** "11/29" */
  expiry: string;
  pinSet: boolean;
  createdAt: string;
}

export interface CardDetails {
  number: string;
  expiry: string;
  cvv: string;
}

export const cardsApi = {
  list: () => request<Card[]>('/cards'),

  details: (cardId: string) => request<CardDetails>(`/cards/${cardId}/details`),
};
