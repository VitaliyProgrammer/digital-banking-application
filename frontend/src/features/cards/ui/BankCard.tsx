import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { cn } from '@/shared/lib/cn';
import type { Currency } from '@/shared/lib/money';
import { Logo } from '@/shared/ui/Logo/Logo';
import type { Card } from '../api/cardsApi';
import { CARD_DETAILS_VISIBLE_MS, useCardDetails } from '../model/queries';
import styles from './BankCard.module.css';

interface BankCardProps {
  card: Card;
  currency: Currency;
  /** The name as embossed: Latin capitals. */
  holderName: string;
}

/** "**** **** **** 2048" → "•••• •••• •••• 2048"; "4999993177452048" → "4999 9931 7745 2048". */
function maskedNumber(masked: string): string {
  return masked.replace(/\*/g, '•');
}

function groupedNumber(number: string): string {
  return number.replace(/(\d{4})(?=\d)/g, '$1 ');
}

/**
 * The client's card, as approved in the mockup. A click turns it over: only then are the full number and CVV
 * asked from the bank, and a minute later the card turns back and forgets them.
 */
export function BankCard({ card, currency, holderName }: BankCardProps) {
  const { t } = useTranslation();
  const [flipped, setFlipped] = useState(false);
  const details = useCardDetails(card.id, flipped);

  useEffect(() => {
    if (!flipped) {
      return;
    }
    const timer = setTimeout(() => setFlipped(false), CARD_DETAILS_VISIBLE_MS);
    return () => clearTimeout(timer);
  }, [flipped]);

  return (
    <button
      type="button"
      className={styles.card}
      aria-label={flipped ? t('home.card.hideDetails') : t('home.card.showDetails')}
      aria-pressed={flipped}
      onClick={() => setFlipped((current) => !current)}
    >
      <span className={cn(styles.inner, flipped && styles.flipped)}>
        <span className={cn(styles.face, styles.front)} aria-hidden={flipped}>
          <span className={styles.glow} aria-hidden="true" />

          <span className={styles.topRow}>
            <Logo variant="card" />
            <span className={cn(styles.product, 'text-trim')}>
              {card.type} · {currency}
            </span>
          </span>

          <span className={styles.chip} aria-hidden="true" />

          <span className={styles.number}>{maskedNumber(card.maskedNumber)}</span>

          <span className={styles.bottomRow}>
            <span className={styles.holder}>{holderName}</span>
            <span className={styles.network}>VISA</span>
          </span>
        </span>

        <span className={cn(styles.face, styles.back)} aria-hidden={!flipped}>
          <span className={styles.stripe} />
          {flipped && details.data && (
            <span className={styles.details}>
              <span className={styles.signatureRow}>
                <span className={styles.signature} />
                <span className={styles.cvv}>CVV {details.data.cvv}</span>
              </span>
              <span className={styles.fullNumber}>{groupedNumber(details.data.number)}</span>
              <span className={styles.detailsFooter}>
                <span className={styles.muted}>{t('home.card.hideSoon')}</span>
                <span>
                  <span className={styles.muted}>{t('home.card.validThru')}</span>{' '}
                  <strong className={styles.expiry}>{details.data.expiry}</strong>
                </span>
              </span>
            </span>
          )}
          {flipped && details.isError && (
            <span className={styles.detailsError}>{t('home.card.detailsFailed')}</span>
          )}
        </span>
      </span>
    </button>
  );
}
