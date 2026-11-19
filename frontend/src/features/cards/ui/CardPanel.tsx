import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { Account } from '@/features/accounts/api/accountsApi';
import { useCurrentUser } from '@/features/auth/model/AuthContext';
import { currentLanguage } from '@/shared/i18n/i18n';
import { useRequestErrorText } from '@/shared/lib/forms';
import { shortIban } from '@/shared/lib/iban';
import { formatDayMonth, kyivDayOf } from '@/shared/lib/kyivTime';
import { cardholderName } from '@/shared/lib/transliterate';
import { Button } from '@/shared/ui/Button/Button';
import { FormAlert } from '@/shared/ui/FormAlert/FormAlert';
import { LockIcon, PinIcon } from '@/shared/ui/icons';
import { Panel } from '@/shared/ui/Panel/Panel';
import { Pill } from '@/shared/ui/Pill/Pill';
import type { Card } from '../api/cardsApi';
import { useCardAction } from '../model/queries';
import { BankCard } from './BankCard';
import styles from './CardPanel.module.css';
import { PinForm } from './PinForm';

/** What the panel shows under the card: the actions, the PIN form, or the question before closing. */
type Mode = 'actions' | 'pin' | 'close';

interface CardPanelProps {
  card: Card;
  account: Account | undefined;
}

/** One card in full: the card itself, its facts and everything the client can do with it. */
export function CardPanel({ card, account }: CardPanelProps) {
  const { t } = useTranslation();
  const language = currentLanguage();
  const user = useCurrentUser();
  const requestErrorText = useRequestErrorText();
  const cardAction = useCardAction(card.id);
  const [mode, setMode] = useState<Mode>('actions');
  const [pinSaved, setPinSaved] = useState(false);

  const issued = kyivDayOf(new Date(card.createdAt));
  const active = card.status === 'ACTIVE';

  const run = (action: 'block' | 'unblock' | 'close') => {
    setPinSaved(false);
    cardAction.mutate(action, { onSuccess: () => setMode('actions') });
  };

  return (
    <Panel className={styles.panel}>
      <div className={styles.top}>
        <div className={styles.cardColumn}>
          <BankCard
            card={card}
            currency={account?.currency ?? 'UAH'}
            holderName={cardholderName(user.firstName, user.lastName)}
          />
          {active && <p className={styles.flipHint}>{t('cards.flipHint')}</p>}
        </div>

        <dl className={styles.facts}>
          <div className={styles.fact}>
            <dt>{t(`cards.types.${card.type}`)}</dt>
            <dd>
              <Pill
                colors={
                  active
                    ? { background: 'var(--color-mint)', color: 'var(--color-brand-dark)' }
                    : { background: 'var(--color-danger-tint)', color: 'var(--color-danger)' }
                }
              >
                {t(`cards.statuses.${card.status}`)}
              </Pill>
            </dd>
          </div>
          {account && (
            <div className={styles.fact}>
              <dt>{t('cards.info.account')}</dt>
              <dd className={styles.account}>
                {t(`accounts.names.${account.currency}`)}
                <span className={styles.mono}>{shortIban(account.number)}</span>
              </dd>
            </div>
          )}
          <div className={styles.fact}>
            <dt>{t('cards.info.validThru')}</dt>
            <dd className={styles.mono}>{card.expiry}</dd>
          </div>
          <div className={styles.fact}>
            <dt>{t('cards.info.pin')}</dt>
            <dd>{card.pinSet ? t('cards.info.pinSet') : t('cards.info.pinNotSet')}</dd>
          </div>
          <div className={styles.fact}>
            <dt>{t('cards.info.issuedAt')}</dt>
            <dd>{`${formatDayMonth(issued, language)} ${issued.year}`}</dd>
          </div>
        </dl>
      </div>

      {card.status !== 'ACTIVE' && <p className={styles.note}>{t(`cards.notes.${card.status}`)}</p>}
      {cardAction.isError && <FormAlert tone="error">{requestErrorText(cardAction.error)}</FormAlert>}
      {pinSaved && mode === 'actions' && <FormAlert tone="success">{t('cards.pin.saved')}</FormAlert>}

      {mode === 'pin' && (
        <PinForm
          card={card}
          onSaved={() => {
            setPinSaved(true);
            setMode('actions');
          }}
          onCancel={() => setMode('actions')}
        />
      )}

      {mode === 'close' && (
        <div className={styles.confirm} role="alertdialog" aria-labelledby={`close-${card.id}`}>
          <p id={`close-${card.id}`} className={styles.warning}>
            {t('cards.close.warning')}
          </p>
          <div className={styles.buttons}>
            <Button
              variant="danger"
              className={styles.confirmClose}
              loading={cardAction.isPending}
              onClick={() => run('close')}
            >
              {t('cards.close.confirm')}
            </Button>
            <Button variant="soft" onClick={() => setMode('actions')}>
              {t('cards.close.cancel')}
            </Button>
          </div>
        </div>
      )}

      {mode === 'actions' && card.status !== 'CLOSED' && (
        <div className={styles.buttons}>
          {card.status === 'ACTIVE' && (
            <Button variant="soft" loading={cardAction.isPending} onClick={() => run('block')}>
              <LockIcon />
              {t('cards.actions.block')}
            </Button>
          )}
          {card.status === 'BLOCKED' && (
            <Button loading={cardAction.isPending} onClick={() => run('unblock')}>
              {t('cards.actions.unblock')}
            </Button>
          )}
          {active && (
            <Button
              variant="soft"
              onClick={() => {
                setPinSaved(false);
                setMode('pin');
              }}
            >
              <PinIcon />
              {card.pinSet ? t('cards.actions.changePin') : t('cards.actions.setPin')}
            </Button>
          )}
          <Button variant="danger" className={styles.close} onClick={() => setMode('close')}>
            {t('cards.actions.close')}
          </Button>
        </div>
      )}
    </Panel>
  );
}
