import type { TFunction } from 'i18next';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router';
import type { Account } from '@/features/accounts/api/accountsApi';
import { ACCOUNT_CURRENCIES, clientAccountsByCurrency } from '@/features/accounts/model/clientAccounts';
import { useAccounts } from '@/features/accounts/model/queries';
import type { Card } from '@/features/cards/api/cardsApi';
import { useCards } from '@/features/cards/model/queries';
import { shortIban } from '@/shared/lib/iban';
import { Panel, PanelMessage, PanelTitle } from '@/shared/ui/Panel/Panel';
import { SegmentedControl } from '@/shared/ui/SegmentedControl/SegmentedControl';
import type { TransferKind } from '../model/transfers';
import { OwnRequisites } from '../ui/OwnRequisites';
import type { TransferSource } from '../ui/SourcePicker';
import { TransferDone } from '../ui/TransferDone';
import { TransferForm, type TransferResult } from '../ui/TransferForm';
import styles from './TransfersPage.module.css';

const KIND_PARAM = 'to';
const CARD_PARAM = 'card';

/** A transfer by IBAN leaves an everyday account: savings and the business account have their own sections. */
function accountSources(accounts: Account[], t: TFunction): TransferSource[] {
  return accounts.map((account) => ({
    id: account.id,
    name: t(`accounts.names.${account.currency}`),
    number: shortIban(account.number),
    currency: account.currency,
    balance: account.balance,
    spendable: account.balance,
  }));
}

/** Only a working card pays; a card payment may also spend the credit limit of its account. */
function cardSources(cards: Card[], accounts: Account[], t: TFunction): TransferSource[] {
  return cards.flatMap((card) => {
    const account = accounts.find((item) => item.id === card.accountId);
    if (card.status !== 'ACTIVE' || !account) {
      return [];
    }
    return [
      {
        id: card.id,
        name: t(`cards.types.${card.type}`),
        number: `•• ${card.maskedNumber.slice(-4)}`,
        currency: account.currency,
        balance: account.balance,
        spendable: account.balance + (account.creditLimit ?? 0),
      },
    ];
  });
}

/**
 * Money to another client: by IBAN or to a card. The kind of transfer is in the address
 * (/transfers?to=card&card=…), so a card's page can open a transfer from that very card.
 */
export function TransfersPage() {
  const { t } = useTranslation();
  const accounts = useAccounts();
  const cards = useCards();
  const [searchParams, setSearchParams] = useSearchParams();
  const [result, setResult] = useState<TransferResult | null>(null);

  if (accounts.isError || cards.isError) {
    return (
      <Panel className={styles.message}>
        <PanelMessage>{t('transfers.loadFailed')}</PanelMessage>
      </Panel>
    );
  }

  if (!accounts.data || !cards.data) {
    return null;
  }

  const kind: TransferKind = searchParams.get(KIND_PARAM) === 'card' ? 'card' : 'iban';
  const byCurrency = clientAccountsByCurrency(accounts.data);
  const everydayAccounts = ACCOUNT_CURRENCIES.flatMap((currency) => byCurrency[currency] ?? []);
  const sources =
    kind === 'iban'
      ? accountSources(
          everydayAccounts.filter((account) => account.status === 'ACTIVE'),
          t,
        )
      : cardSources(cards.data, accounts.data, t);
  const wanted = sources.find((source) => source.id === searchParams.get(CARD_PARAM));

  return (
    <div className={styles.page}>
      <Panel className={styles.main}>
        <div className={styles.head}>
          <PanelTitle>{t('transfers.title')}</PanelTitle>
          {!result && (
            <SegmentedControl
              name="kind"
              aria-label={t('transfers.kind')}
              className={styles.kinds}
              segments={[
                { value: 'iban', label: t('transfers.kinds.iban') },
                { value: 'card', label: t('transfers.kinds.card') },
              ]}
              value={kind}
              onChange={(next) => setSearchParams({ [KIND_PARAM]: next }, { replace: true })}
            />
          )}
        </div>

        {result && <TransferDone result={result} onAnother={() => setResult(null)} />}
        {!result && sources.length === 0 && <PanelMessage>{t(`transfers.noSources.${kind}`)}</PanelMessage>}
        {!result && sources.length > 0 && (
          <TransferForm
            key={kind}
            kind={kind}
            sources={sources}
            initialSourceId={(wanted ?? (sources[0] as TransferSource)).id}
            onDone={setResult}
          />
        )}
      </Panel>

      <div className={styles.column}>
        <OwnRequisites accounts={everydayAccounts} />
      </div>
    </div>
  );
}
