import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { useLatestOperations } from '@/features/accounts/model/queries';
import { OperationRow } from '@/features/accounts/ui/OperationRow';
import { useEventTime } from '@/shared/lib/useEventTime';
import { useNow } from '@/shared/lib/useNow';
import { Panel, PanelMessage, PanelTitle } from '@/shared/ui/Panel/Panel';
import { useMainAccount } from '../model/useMainAccount';
import styles from './RecentOperations.module.css';

const SHOWN = 6;

export function RecentOperations() {
  const { t } = useTranslation();
  const main = useMainAccount();
  const account = main.status === 'ready' ? main.account : undefined;
  const operations = useLatestOperations(account?.id, SHOWN);
  const eventTime = useEventTime(useNow());

  return (
    <Panel className={styles.panel}>
      <div className={styles.header}>
        <PanelTitle>{t('home.operations.title')}</PanelTitle>
        <Link className={styles.all} to="/accounts">
          {t('home.operations.all')}
        </Link>
      </div>

      {(main.status === 'error' || operations.isError) && (
        <div className={styles.message}>
          <PanelMessage>{t('home.loadFailed')}</PanelMessage>
        </div>
      )}

      {operations.data?.length === 0 && (
        <div className={styles.message}>
          <PanelMessage>{t('home.operations.empty')}</PanelMessage>
        </div>
      )}

      {account && operations.data && operations.data.length > 0 && (
        <ul className={styles.list}>
          {operations.data.map((entry) => (
            <OperationRow
              key={`${entry.transactionId}-${entry.direction}`}
              entry={entry}
              currency={account.currency}
              time={eventTime(entry.createdAt)}
            />
          ))}
        </ul>
      )}
    </Panel>
  );
}
