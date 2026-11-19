import { zodResolver } from '@hookform/resolvers/zod';
import { useMemo, useState, type FocusEvent } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { ApiError } from '@/shared/api/ApiError';
import { formatCardNumber } from '@/shared/lib/cardNumber';
import { useFieldErrorText, useRequestErrorText } from '@/shared/lib/forms';
import { compactIban, formatIban, ibanHalves } from '@/shared/lib/iban';
import { useDebouncedValue } from '@/shared/lib/useDebouncedValue';
import { Button } from '@/shared/ui/Button/Button';
import { FormAlert } from '@/shared/ui/FormAlert/FormAlert';
import { TextField } from '@/shared/ui/TextField/TextField';
import type { Payment } from '../api/paymentsApi';
import { useCardTransfer, useFeeQuote, useTransfer, type TransferKind } from '../model/transfers';
import {
  amountSchema,
  DESCRIPTION_MAX,
  transferSchema,
  type TransferFormInput,
  type TransferFormOutput,
} from '../model/transferSchema';
import { FeeSummary } from './FeeSummary';
import { SourcePicker, type TransferSource } from './SourcePicker';
import styles from './TransferForm.module.css';

/** The fee is asked once the client pauses, not on every key. */
const FEE_QUOTE_DELAY_MS = 300;

/** The bank's refusals that are about one field: shown under it, where the client will fix them. */
const FIELD_OF_ERROR: Partial<Record<string, 'recipient' | 'amount'>> = {
  'recipient-not-found': 'recipient',
  'recipient-card-not-usable': 'recipient',
  'same-account-transfer': 'recipient',
  'currency-mismatch': 'recipient',
  'insufficient-funds': 'amount',
};

const PRINTED: Record<TransferKind, (value: string) => string> = {
  iban: (value) => formatIban(compactIban(value)),
  card: formatCardNumber,
};

export interface TransferResult {
  payment: Payment;
  source: TransferSource;
  /**
   * How the recipient is shown afterwards, in parts that never break inside: the IBAN in two halves of whole
   * groups, or only the end of a card number.
   */
  recipient: string[];
}

interface TransferFormProps {
  kind: TransferKind;
  /** Never empty: without a source the page shows why there is nothing to send from. */
  sources: TransferSource[];
  initialSourceId: string;
  onDone: (result: TransferResult) => void;
}

export function TransferForm({ kind, sources, initialSourceId, onDone }: TransferFormProps) {
  const { t } = useTranslation();
  const fieldErrorText = useFieldErrorText();
  const requestErrorText = useRequestErrorText();
  const transfer = useTransfer();
  const cardTransfer = useCardTransfer();
  const [sourceId, setSourceId] = useState(initialSourceId);
  const [failure, setFailure] = useState<string | null>(null);
  const schema = useMemo(() => transferSchema(kind), [kind]);

  const {
    register,
    handleSubmit,
    setValue,
    setError,
    control,
    formState: { errors },
  } = useForm<TransferFormInput, unknown, TransferFormOutput>({
    resolver: zodResolver(schema),
    defaultValues: { recipient: '', amount: '', description: '' },
  });

  const source = sources.find((item) => item.id === sourceId) ?? (sources[0] as TransferSource);
  const typedAmount = useWatch({ control, name: 'amount' });
  const settledAmount = useDebouncedValue(typedAmount, FEE_QUOTE_DELAY_MS);
  const amount = useMemo(() => amountSchema(kind).safeParse(settledAmount).data, [kind, settledAmount]);
  const feeQuote = useFeeQuote({ kind, sourceId: source.id, amount });

  const onError = (error: unknown) => {
    if (error instanceof ApiError) {
      const field = FIELD_OF_ERROR[error.code];
      if (field) {
        setError(field, { type: 'server', message: error.message });
        return;
      }
    }
    setFailure(requestErrorText(error));
  };

  const onSubmit = ({ recipient, amount: sum, description }: TransferFormOutput) => {
    setFailure(null);
    const onSuccess = (payment: Payment) =>
      onDone({
        payment,
        source,
        recipient: kind === 'iban' ? ibanHalves(recipient) : [`•• ${recipient.slice(-4)}`],
      });

    if (kind === 'iban') {
      transfer.mutate(
        { fromAccountId: source.id, toAccountNumber: recipient, amount: sum, description },
        { onSuccess, onError },
      );
    } else {
      cardTransfer.mutate(
        { fromCardId: source.id, toCardNumber: recipient, amount: sum, description },
        { onSuccess, onError },
      );
    }
  };

  return (
    <form className={styles.form} onSubmit={handleSubmit(onSubmit)} noValidate>
      <SourcePicker sources={sources} value={source.id} onChange={setSourceId} />

      <TextField
        className={styles.recipient}
        label={t(`transfers.recipient.${kind}`)}
        placeholder={t(`transfers.recipient.placeholder.${kind}`)}
        inputMode={kind === 'card' ? 'numeric' : 'text'}
        autoComplete="off"
        spellCheck={false}
        error={fieldErrorText(errors.recipient)}
        {...register('recipient', {
          // Printed in groups once typed: easy to compare with the number the recipient sent.
          onBlur: (event: FocusEvent<HTMLInputElement>) => {
            if (event.target.value.trim()) {
              setValue('recipient', PRINTED[kind](event.target.value));
            }
          },
        })}
      />

      <div className={styles.pair}>
        <TextField
          label={t('transfers.amount')}
          inputMode="decimal"
          autoComplete="off"
          placeholder="0,00"
          error={fieldErrorText(errors.amount)}
          endAdornment={<span className={styles.currency}>{source.currency}</span>}
          {...register('amount')}
        />
        <TextField
          label={t('transfers.description')}
          placeholder={t('transfers.descriptionPlaceholder')}
          maxLength={DESCRIPTION_MAX}
          autoComplete="off"
          hint={t('transfers.descriptionHint')}
          error={fieldErrorText(errors.description)}
          {...register('description')}
        />
      </div>

      <FeeSummary
        source={source}
        quote={amount === undefined ? undefined : feeQuote.data}
        stale={feeQuote.isPlaceholderData || typedAmount !== settledAmount}
        failed={feeQuote.isError}
      />

      {failure && <FormAlert tone="error">{failure}</FormAlert>}

      <Button type="submit" fullWidth loading={transfer.isPending || cardTransfer.isPending}>
        {t('transfers.submit')}
      </Button>
    </form>
  );
}
