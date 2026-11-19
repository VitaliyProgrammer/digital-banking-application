import { useTranslation } from 'react-i18next';
import { useCurrentUser } from '@/features/auth/model/AuthContext';
import { currentLanguage } from '@/shared/i18n/i18n';
import { ibanHalves } from '@/shared/lib/iban';
import { formatDayMonth, kyivDayOf } from '@/shared/lib/kyivTime';
import { CopyButton } from '@/shared/ui/CopyButton/CopyButton';
import { Panel, PanelTitle } from '@/shared/ui/Panel/Panel';
import type { Account } from '../api/accountsApi';
import styles from './AccountDetails.module.css';

/** What another bank needs to send money here, ready to be copied into a message. */
export function AccountDetails({ account }: { account: Account }) {
  const { t } = useTranslation();
  const language = currentLanguage();
  const user = useCurrentUser();

  const recipient = `${user.lastName} ${user.firstName}`;
  const [ibanStart, ibanEnd] = ibanHalves(account.number);
  const currency = t(`accounts.details.currencies.${account.currency}`);
  const opened = kyivDayOf(new Date(account.createdAt));

  const allDetails = [
    `${t('accounts.details.recipient')}: ${recipient}`,
    `${t('accounts.details.iban')}: ${account.number}`,
    `${t('accounts.details.currency')}: ${currency}`,
  ].join('\n');

  return (
    <Panel className={styles.panel}>
      <PanelTitle>{t('accounts.details.title')}</PanelTitle>

      <dl className={styles.list}>
        <div className={styles.row}>
          <dt>{t('accounts.details.recipient')}</dt>
          <dd>{recipient}</dd>
        </div>
        <div className={styles.row}>
          {/* The copy button sits by the label, so the whole IBAN fits on one line under it. */}
          <dt className={styles.ibanLabel}>
            {t('accounts.details.iban')}
            <CopyButton text={account.number} label={t('accounts.details.copyIban')} />
          </dt>
          <dd className={styles.iban}>
            <span className={styles.half}>{ibanStart}</span> <span className={styles.half}>{ibanEnd}</span>
          </dd>
        </div>
        <div className={styles.row}>
          <dt>{t('accounts.details.currency')}</dt>
          <dd>{currency}</dd>
        </div>
        <div className={styles.row}>
          <dt>{t('accounts.details.openedAt')}</dt>
          <dd>{`${formatDayMonth(opened, language)} ${opened.year}`}</dd>
        </div>
      </dl>

      <CopyButton
        text={allDetails}
        label={t('accounts.details.copyAll')}
        withText
        className={styles.copyAll}
      />
    </Panel>
  );
}
