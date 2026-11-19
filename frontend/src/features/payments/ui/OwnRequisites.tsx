import { useTranslation } from 'react-i18next';
import type { Account } from '@/features/accounts/api/accountsApi';
import { ibanHalves } from '@/shared/lib/iban';
import { CopyButton } from '@/shared/ui/CopyButton/CopyButton';
import { Panel, PanelTitle } from '@/shared/ui/Panel/Panel';
import styles from './OwnRequisites.module.css';

/** The other way round: the client's own IBANs, ready to be sent to whoever is transferring money to them. */
export function OwnRequisites({ accounts }: { accounts: Account[] }) {
  const { t } = useTranslation();

  if (accounts.length === 0) {
    return null;
  }

  return (
    <Panel className={styles.panel}>
      <div>
        <PanelTitle>{t('transfers.requisites.title')}</PanelTitle>
        <p className={styles.subtitle}>{t('transfers.requisites.subtitle')}</p>
      </div>

      <ul className={styles.list}>
        {accounts.map((account) => {
          const [start, end] = ibanHalves(account.number);
          return (
            <li key={account.id} className={styles.item}>
              {/* The copy button sits by the name, so the whole IBAN keeps its line under it. */}
              <div className={styles.name}>
                {t(`accounts.names.${account.currency}`)}
                <CopyButton text={account.number} label={t('accounts.details.copyIban')} />
              </div>
              <div className={styles.iban}>
                <span className={styles.half}>{start}</span> <span className={styles.half}>{end}</span>
              </div>
            </li>
          );
        })}
      </ul>
    </Panel>
  );
}
