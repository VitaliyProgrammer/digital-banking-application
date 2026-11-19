import { useTranslation } from 'react-i18next';
import { currentLanguage } from '@/shared/i18n/i18n';
import { cn } from '@/shared/lib/cn';
import { useRequestErrorText } from '@/shared/lib/forms';
import { shortIban } from '@/shared/lib/iban';
import { formatMoney, type Currency } from '@/shared/lib/money';
import { Button } from '@/shared/ui/Button/Button';
import { FormAlert } from '@/shared/ui/FormAlert/FormAlert';
import { Pill } from '@/shared/ui/Pill/Pill';
import type { Account } from '../api/accountsApi';
import { ACCOUNT_CURRENCIES } from '../model/clientAccounts';
import { useOpenAccount } from '../model/queries';
import styles from './AccountTiles.module.css';

const SIGNS: Record<Currency, string> = { UAH: '₴', USD: '$', EUR: '€' };

interface AccountTilesProps {
  accounts: Record<Currency, Account | undefined>;
  selected: Currency;
  onSelect: (currency: Currency) => void;
}

/** One tile per currency: an opened account to choose, or an offer to open one. */
export function AccountTiles({ accounts, selected, onSelect }: AccountTilesProps) {
  const { t } = useTranslation();
  const language = currentLanguage();
  const requestErrorText = useRequestErrorText();
  const openAccount = useOpenAccount();

  const heading = (currency: Currency) => (
    <span className={styles.heading}>
      <span className={styles.sign} aria-hidden="true">
        {SIGNS[currency]}
      </span>
      <span className={styles.name}>{t(`accounts.names.${currency}`)}</span>
    </span>
  );

  return (
    <div className={styles.wrapper}>
      {openAccount.isError && <FormAlert tone="error">{requestErrorText(openAccount.error)}</FormAlert>}

      <ul className={styles.tiles} aria-label={t('accounts.list')}>
        {ACCOUNT_CURRENCIES.map((currency) => {
          const account = accounts[currency];

          if (!account) {
            return (
              <li key={currency} className={cn(styles.tile, styles.notOpened)}>
                {heading(currency)}
                <span className={styles.offer}>{t('accounts.notOpened')}</span>
                <Button
                  variant="soft"
                  className={styles.bottom}
                  loading={openAccount.isPending && openAccount.variables === currency}
                  disabled={openAccount.isPending}
                  onClick={() => openAccount.mutate(currency, { onSuccess: () => onSelect(currency) })}
                >
                  {t('accounts.open')}
                </Button>
              </li>
            );
          }

          const chosen = currency === selected;
          const creditLimit = account.creditLimit ?? 0;
          return (
            <li key={currency} className={styles.item}>
              <button
                type="button"
                className={cn(styles.tile, styles.choice, chosen && styles.chosen)}
                aria-pressed={chosen}
                onClick={() => onSelect(currency)}
              >
                {heading(currency)}
                <span className={styles.balance}>{formatMoney(account.balance, currency, language)}</span>
                {creditLimit > 0 && (
                  <span className={styles.credit}>
                    {t('accounts.creditLimit', {
                      amount: formatMoney(creditLimit, currency, language, { wholeIfRound: true }),
                    })}
                  </span>
                )}
                <span className={cn(styles.bottom, styles.footer)}>
                  <span className={styles.iban}>{shortIban(account.number)}</span>
                  {account.status !== 'ACTIVE' && (
                    <Pill colors={{ background: 'var(--color-danger-tint)', color: 'var(--color-danger)' }}>
                      {t(`accounts.statuses.${account.status}`)}
                    </Pill>
                  )}
                </span>
              </button>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
