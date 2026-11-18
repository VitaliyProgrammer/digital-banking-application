import { SpendingPanel } from '@/features/spending/ui/SpendingPanel';
import { MainCardPanel } from '../ui/MainCardPanel';
import { RecentOperations } from '../ui/RecentOperations';
import { BankRatesPanel, CashbackPanel, CurrencyAccountsPanel, SavingsPanel } from '../ui/SidePanels';
import styles from './HomePage.module.css';

/** The home screen of the approved mockup: the card and the spending on top, operations and the rest below. */
export function HomePage() {
  return (
    <div className={styles.page}>
      <div className={styles.top}>
        <MainCardPanel />
        <SpendingPanel />
      </div>

      <div className={styles.bottom}>
        <RecentOperations />
        <div className={styles.column}>
          <CurrencyAccountsPanel />
          <SavingsPanel />
          <CashbackPanel />
          <BankRatesPanel />
        </div>
      </div>
    </div>
  );
}
