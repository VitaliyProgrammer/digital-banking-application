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

/** The cards a client may ask for; UNIVERSAL comes with the account, BUSINESS with the business account. */
export type IssuableCardType = Extract<CardType, 'PAYROLL' | 'STUDENT' | 'VIRTUAL'>;

export interface IssueCardRequest {
  accountId: string;
  type: IssuableCardType;
}

export interface ChangePinRequest {
  /** Only when the card already has a PIN. */
  currentPin?: string;
  newPin: string;
}

/** What the client can do with a card in one click. */
export type CardAction = 'block' | 'unblock' | 'close';

export const cardsApi = {
  list: () => request<Card[]>('/cards'),

  details: (cardId: string) => request<CardDetails>(`/cards/${cardId}/details`),

  issue: (issue: IssueCardRequest) => request<Card>('/cards', { method: 'POST', body: issue }),

  act: (cardId: string, action: CardAction) =>
    request<Card>(`/cards/${cardId}/${action}`, { method: 'POST' }),

  changePin: (cardId: string, pin: ChangePinRequest) =>
    request<undefined>(`/cards/${cardId}/pin`, { method: 'PUT', body: pin }),
};
