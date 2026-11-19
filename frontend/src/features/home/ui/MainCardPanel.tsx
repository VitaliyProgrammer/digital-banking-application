import { useTranslation } from 'react-i18next';
import { useCurrentUser } from '@/features/auth/model/AuthContext';
import { BankCard } from '@/features/cards/ui/BankCard';
import { CreditLineNote } from '@/features/credit/ui/CreditLineNote';
import { currentLanguage } from '@/shared/i18n/i18n';
import { formatAmount, formatMoney } from '@/shared/lib/money';
import { cardholderName } from '@/shared/lib/transliterate';
import { ButtonLink } from '@/shared/ui/Button/ButtonLink';
import { Panel, PanelMessage } from '@/shared/ui/Panel/Panel';
import { Pill } from '@/shared/ui/Pill/Pill';
import { ExchangeIcon, LockIcon, PayIcon, PinIcon, PlusIcon, SendIcon } from '@/shared/ui/icons';
import { useMainAccount } from '../model/useMainAccount';
import styles from './MainCardPanel.module.css';

const CURRENCY_SIGNS = { UAH: '₴', USD: '$', EUR: '€' } as const;

/** The left panel of the home screen: the main card, its money, the credit line and the four everyday actions. */
export function MainCardPanel() {
  const { t } = useTranslation();
  const language = currentLanguage();
  const user = useCurrentUser();
  const main = useMainAccount();

  if (main.status === 'error') {
    return (
      <Panel className={styles.panel}>
        <PanelMessage>{t('home.loadFailed')}</PanelMessage>
      </Panel>
    );
  }

  if (main.status === 'loading') {
    return <Panel className={styles.panel} aria-busy="true" />;
  }

  const { account, card, creditLine: line } = main;
  const ownFunds = line ? line.ownFunds : (account?.balance ?? 0);
  const currency = account?.currency ?? 'UAH';

  return (
    <Panel className={styles.panel}>
      <div className={styles.summary}>
        <span className={styles.label}>
          {card ? t(`cards.types.${card.type}`) : t('home.card.ownFunds')} ·{' '}
          {t(`home.card.currencies.${currency}`)}
        </span>
        <p className={styles.balance}>
          {formatAmount(ownFunds, language)} <span className={styles.sign}>{CURRENCY_SIGNS[currency]}</span>
        </p>
        <span className={styles.caption}>
          {line ? (
            <>
              {t('home.card.withCredit')}{' '}
              <strong className={styles.available}>{formatMoney(line.available, 'UAH', language)}</strong>
            </>
          ) : (
            t('home.card.ownFunds')
          )}
        </span>
      </div>

      {card ? (
        <>
          <BankCard
            card={card}
            currency={currency}
            holderName={cardholderName(user.firstName, user.lastName)}
          />

          <div className={styles.cardActions}>
            <Pill
              className={styles.status}
              colors={{ background: 'var(--color-mint)', color: 'var(--color-brand-dark)' }}
            >
              {t(`cards.statuses.${card.status}`)}
            </Pill>
            <ButtonLink variant="ghost" to={`/cards?card=${card.id}`}>
              <LockIcon />
              {t('home.card.block')}
            </ButtonLink>
            <ButtonLink variant="ghost" to={`/cards?card=${card.id}`}>
              <PinIcon />
              {t('home.card.changePin')}
            </ButtonLink>
          </div>
        </>
      ) : (
        <div className={styles.noCard}>
          <PanelMessage>{t('home.card.noCard')}</PanelMessage>
          <ButtonLink variant="soft" to="/cards">
            {t('home.card.openCard')}
          </ButtonLink>
        </div>
      )}

      {line && <CreditLineNote creditLine={line} />}

      <div className={styles.actions}>
        <ButtonLink to="/accounts">
          <PlusIcon />
          {t('home.actions.topUp')}
        </ButtonLink>
        <ButtonLink to="/transfers">
          <SendIcon />
          {t('home.actions.transfer')}
        </ButtonLink>
        <ButtonLink variant="soft" to="/payments">
          <PayIcon />
          {t('home.actions.pay')}
        </ButtonLink>
        <ButtonLink variant="soft" to="/exchange">
          <ExchangeIcon />
          {t('home.actions.exchange')}
        </ButtonLink>
      </div>
    </Panel>
  );
}
