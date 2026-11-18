/** The bank of the approved mockup: Іван Петренко's accounts, card, credit line and November 2026. */

export const ACCOUNTS = [
  {
    id: 'acc-uah',
    number: 'UA213223130000026007233566001',
    currency: 'UAH',
    type: 'CLIENT',
    status: 'ACTIVE',
    balance: 48215.3,
    creditLimit: 20000,
    createdAt: '2026-10-01T09:00:00Z',
  },
  {
    id: 'acc-usd',
    number: 'UA213223130000026007233566002',
    currency: 'USD',
    type: 'CLIENT',
    status: 'ACTIVE',
    balance: 1240,
    creditLimit: null,
    createdAt: '2026-10-02T09:00:00Z',
  },
  {
    id: 'acc-eur',
    number: 'UA213223130000026007233566003',
    currency: 'EUR',
    type: 'CLIENT',
    status: 'ACTIVE',
    balance: 380.5,
    creditLimit: null,
    createdAt: '2026-10-02T09:00:00Z',
  },
];

export const CARDS = [
  {
    id: 'card-1',
    accountId: 'acc-uah',
    type: 'UNIVERSAL',
    status: 'ACTIVE',
    maskedNumber: '**** **** **** 2048',
    expiry: '11/29',
    pinSet: true,
    createdAt: '2026-10-01T09:00:00Z',
  },
];

export const CREDIT_LINE = {
  accountId: 'acc-uah',
  number: 'UA213223130000026007233566001',
  creditLimit: 20000,
  ownFunds: 48215.3,
  available: 68215.3,
  debt: 0,
  statementDebt: 0,
  leftToRepayInterestFree: 0,
  minimumPayment: 0,
  leftForMinimumPayment: 0,
  statementInterestFreeUntil: null,
  newSpendingInterestFreeUntil: '2026-12-31',
};

export const SPENDING = {
  from: '2026-11-01',
  to: '2026-11-30',
  currency: 'UAH',
  total: 10230,
  categories: [
    { category: 'TRANSFERS', amount: 4500 },
    { category: 'SAVINGS', amount: 3000 },
    { category: 'UTILITIES', amount: 2180 },
    { category: 'INTERNET', amount: 300 },
    { category: 'MOBILE', amount: 250 },
  ],
};

export const STATEMENT = {
  content: [
    {
      transactionId: 't1',
      type: 'HOLD',
      direction: 'DEBIT',
      amount: 250,
      balanceAfter: 48215.3,
      description: 'Мобільний Один, 380671234567',
      createdAt: '2026-11-17T11:42:00Z',
    },
    {
      transactionId: 't2',
      type: 'CARD_TRANSFER',
      direction: 'DEBIT',
      amount: 2000,
      balanceAfter: 48465.3,
      description: 'Переказ на картку *4417',
      createdAt: '2026-11-17T08:05:00Z',
    },
    {
      transactionId: 't3',
      type: 'EXCHANGE',
      direction: 'CREDIT',
      amount: 4135,
      balanceAfter: 50465.3,
      description: 'Обмін USD на UAH',
      createdAt: '2026-11-16T17:20:00Z',
    },
    {
      transactionId: 't4',
      type: 'HOLD',
      direction: 'DEBIT',
      amount: 1248.5,
      balanceAfter: 46330.3,
      description: 'Міське світло, 4410012345',
      createdAt: '2026-11-15T07:12:00Z',
    },
    {
      transactionId: 't5',
      type: 'DEPOSIT',
      direction: 'CREDIT',
      amount: 12000,
      balanceAfter: 47578.8,
      description: 'Поповнення рахунку',
      createdAt: '2026-11-14T06:11:00Z',
    },
    {
      transactionId: 't6',
      type: 'CASHBACK',
      direction: 'CREDIT',
      amount: 44.2,
      balanceAfter: 35578.8,
      description: 'Кешбек за жовтень',
      createdAt: '2026-10-31T23:30:00Z',
    },
  ],
  page: 0,
  size: 6,
  totalElements: 6,
  totalPages: 1,
};

export const SAVINGS = [
  {
    id: 'jar-1',
    number: 'UA213223130000026007233566010',
    currency: 'UAH',
    balance: 18400,
    annualRate: 12,
    accruedInterest: 96.79,
    createdAt: '2026-10-05T09:00:00Z',
  },
];

export const CASHBACK = {
  period: '2026-11',
  earned: 43.3,
  monthlyCap: 500,
  remaining: 456.7,
  paidOut: false,
  rates: { MOBILE: 5, INTERNET: 3, ELECTRICITY: 1, WATER: 1, GAS: 1, HEATING: 1 },
  items: [],
};

export const RATES = [
  {
    currency: 'USD',
    officialRate: 41.62,
    officialRateDate: '2026-11-17',
    stale: false,
    buy: 41.35,
    sell: 41.9,
    updatedAt: '2026-11-17T07:00:00Z',
  },
  {
    currency: 'EUR',
    officialRate: 48.17,
    officialRateDate: '2026-11-17',
    stale: false,
    buy: 47.8,
    sell: 48.55,
    updatedAt: '2026-11-17T07:00:00Z',
  },
];

/** The bank's answer for a path of the home screen, or undefined for anything else. */
export function homeAnswer(url: string): unknown {
  const path = url.split('?')[0];
  switch (path) {
    case '/accounts':
      return ACCOUNTS;
    case '/cards':
      return CARDS;
    case '/credit-line':
      return CREDIT_LINE;
    case '/payments/spending':
      return SPENDING;
    case '/accounts/acc-uah/transactions':
      return STATEMENT;
    case '/savings':
      return SAVINGS;
    case '/cashback':
      return CASHBACK;
    case '/exchange-rates':
      return RATES;
    case '/notifications/unread-count':
      return { unread: 2 };
    default:
      return undefined;
  }
}
