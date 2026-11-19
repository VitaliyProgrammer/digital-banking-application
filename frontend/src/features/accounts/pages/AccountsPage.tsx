import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router';
import { TopUpForm } from '@/features/payments/ui/TopUpForm';
import type { Currency } from '@/shared/lib/money';
import { Panel, PanelMessage } from '@/shared/ui/Panel/Panel';
import { ACCOUNT_CURRENCIES, clientAccountsByCurrency, isCurrency } from '../model/clientAccounts';
import { useAccounts } from '../model/queries';
import { AccountDetails } from '../ui/AccountDetails';
import { AccountTiles } from '../ui/AccountTiles';
import { Statement } from '../ui/Statement';
import styles from './AccountsPage.module.css';

const CURRENCY_PARAM = 'currency';

/**
 * The client's accounts in hryvnia, dollars and euros. The chosen one is in the address
 * (/accounts?currency=USD), so a reload or a link from the home screen opens the same account.
 */
export function AccountsPage() {
  const { t } = useTranslation();
  const accounts = useAccounts();
  const [searchParams, setSearchParams] = useSearchParams();

  if (accounts.isError) {
    return (
      <Panel className={styles.message}>
        <PanelMessage>{t('accounts.loadFailed')}</PanelMessage>
      </Panel>
    );
  }

  if (!accounts.data) {
    return null;
  }

  const byCurrency = clientAccountsByCurrency(accounts.data);
  const asked = searchParams.get(CURRENCY_PARAM);
  const selected: Currency =
    isCurrency(asked) && byCurrency[asked]
      ? asked
      : (ACCOUNT_CURRENCIES.find((currency) => byCurrency[currency]) ?? 'UAH');
  const account = byCurrency[selected];

  const select = (currency: Currency) => {
    setSearchParams({ [CURRENCY_PARAM]: currency }, { replace: true });
  };

  return (
    <div className={styles.page}>
      <AccountTiles accounts={byCurrency} selected={selected} onSelect={select} />

      {account && (
        <div className={styles.bottom}>
          <div className={styles.statement}>
            <Statement key={account.id} account={account} />
          </div>
          <div className={styles.column}>
            <TopUpForm key={account.id} account={account} />
            <AccountDetails account={account} />
          </div>
        </div>
      )}
    </div>
  );
}
