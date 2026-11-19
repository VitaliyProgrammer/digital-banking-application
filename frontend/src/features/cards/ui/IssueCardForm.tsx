import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { Account } from '@/features/accounts/api/accountsApi';
import { cn } from '@/shared/lib/cn';
import { useRequestErrorText } from '@/shared/lib/forms';
import { Button } from '@/shared/ui/Button/Button';
import { FormAlert } from '@/shared/ui/FormAlert/FormAlert';
import { Panel, PanelTitle } from '@/shared/ui/Panel/Panel';
import type { Card, IssuableCardType } from '../api/cardsApi';
import { useIssueCard } from '../model/queries';
import styles from './IssueCardForm.module.css';

const ISSUABLE: readonly IssuableCardType[] = ['PAYROLL', 'STUDENT', 'VIRTUAL'];

interface IssueCardFormProps {
  /** The client's everyday accounts, hryvnia first: a card is issued on one of them. */
  accounts: Account[];
  onIssued: (card: Card) => void;
}

export function IssueCardForm({ accounts, onIssued }: IssueCardFormProps) {
  const { t } = useTranslation();
  const requestErrorText = useRequestErrorText();
  const issueCard = useIssueCard();
  const [type, setType] = useState<IssuableCardType>('VIRTUAL');
  const [accountId, setAccountId] = useState(accounts[0]?.id);

  if (!accountId) {
    return null;
  }

  return (
    <Panel className={styles.panel}>
      <PanelTitle>{t('cards.issue.title')}</PanelTitle>

      <form
        className={styles.form}
        onSubmit={(event) => {
          event.preventDefault();
          issueCard.mutate({ accountId, type }, { onSuccess: onIssued });
        }}
      >
        {issueCard.isError && <FormAlert tone="error">{requestErrorText(issueCard.error)}</FormAlert>}

        <fieldset className={styles.group}>
          <legend className={styles.legend}>{t('cards.issue.type')}</legend>
          {ISSUABLE.map((option) => (
            <label key={option} className={cn(styles.option, option === type && styles.chosen)}>
              <input
                type="radio"
                name="type"
                className={styles.radio}
                checked={option === type}
                onChange={() => setType(option)}
              />
              <span className={styles.optionText}>
                <span className={styles.optionName}>{t(`cards.types.${option}`)}</span>
                <span className={styles.optionNote}>{t(`cards.issue.descriptions.${option}`)}</span>
              </span>
            </label>
          ))}
        </fieldset>

        {accounts.length > 1 && (
          <fieldset className={styles.group}>
            <legend className={styles.legend}>{t('cards.issue.account')}</legend>
            <div className={styles.segments}>
              {accounts.map((account) => (
                <label
                  key={account.id}
                  className={cn(styles.segment, account.id === accountId && styles.segmentChosen)}
                >
                  <input
                    type="radio"
                    name="account"
                    className={styles.radio}
                    checked={account.id === accountId}
                    onChange={() => setAccountId(account.id)}
                  />
                  {account.currency}
                </label>
              ))}
            </div>
          </fieldset>
        )}

        <Button type="submit" fullWidth loading={issueCard.isPending}>
          {t('cards.issue.submit')}
        </Button>
      </form>
    </Panel>
  );
}
