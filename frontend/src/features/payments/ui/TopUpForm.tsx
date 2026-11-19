import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import type { Account } from '@/features/accounts/api/accountsApi';
import { currentLanguage } from '@/shared/i18n/i18n';
import { applyFieldErrors, useFieldErrorText, useRequestErrorText } from '@/shared/lib/forms';
import { formatMoney, formatNumber } from '@/shared/lib/money';
import { Button } from '@/shared/ui/Button/Button';
import { FormAlert } from '@/shared/ui/FormAlert/FormAlert';
import { Panel, PanelMessage, PanelTitle } from '@/shared/ui/Panel/Panel';
import { TextField } from '@/shared/ui/TextField/TextField';
import { topUpSchema, type TopUpFormInput, type TopUpFormOutput } from '../model/topUpSchema';
import { useTopUp } from '../model/useTopUp';
import styles from './TopUpForm.module.css';

const QUICK_AMOUNTS = [500, 1000, 5000];

type Outcome = { tone: 'success' | 'error'; text: string } | null;

export function TopUpForm({ account }: { account: Account }) {
  const { t } = useTranslation();
  const language = currentLanguage();
  const fieldErrorText = useFieldErrorText();
  const requestErrorText = useRequestErrorText();
  const topUp = useTopUp();
  const [outcome, setOutcome] = useState<Outcome>(null);

  const {
    register,
    handleSubmit,
    setValue,
    setError,
    reset,
    formState: { errors },
  } = useForm<TopUpFormInput, unknown, TopUpFormOutput>({
    resolver: zodResolver(topUpSchema),
    defaultValues: { amount: '' },
  });

  if (account.status !== 'ACTIVE') {
    return (
      <Panel className={styles.panel}>
        <PanelTitle>{t('accounts.topUp.title')}</PanelTitle>
        <PanelMessage>{t('accounts.topUp.unavailable')}</PanelMessage>
      </Panel>
    );
  }

  const onSubmit = ({ amount }: TopUpFormOutput) => {
    setOutcome(null);
    topUp.mutate(
      { accountId: account.id, amount },
      {
        onSuccess: (payment) => {
          const credited = formatMoney(payment.amount, payment.currency, language);
          setOutcome({ tone: 'success', text: t('accounts.topUp.done', { amount: credited }) });
          reset();
        },
        onError: (error) => {
          if (!applyFieldErrors(error, setError)) {
            setOutcome({ tone: 'error', text: requestErrorText(error) });
          }
        },
      },
    );
  };

  return (
    <Panel className={styles.panel}>
      <div>
        <PanelTitle>{t('accounts.topUp.title')}</PanelTitle>
        <p className={styles.subtitle}>{t('accounts.topUp.subtitle')}</p>
      </div>

      <form className={styles.form} onSubmit={handleSubmit(onSubmit)} noValidate>
        {outcome && <FormAlert tone={outcome.tone}>{outcome.text}</FormAlert>}

        <TextField
          label={t('accounts.topUp.amount')}
          inputMode="decimal"
          autoComplete="off"
          placeholder="0,00"
          error={fieldErrorText(errors.amount)}
          endAdornment={<span className={styles.currency}>{account.currency}</span>}
          {...register('amount', { onChange: () => setOutcome(null) })}
        />

        <div className={styles.quick} role="group" aria-label={t('accounts.topUp.quick')}>
          {QUICK_AMOUNTS.map((amount) => (
            <button
              key={amount}
              type="button"
              className={styles.chip}
              onClick={() => {
                setOutcome(null);
                setValue('amount', String(amount), { shouldValidate: true });
              }}
            >
              {formatNumber(amount, language)}
            </button>
          ))}
        </div>

        <Button type="submit" fullWidth loading={topUp.isPending}>
          {t('accounts.topUp.submit')}
        </Button>
      </form>
    </Panel>
  );
}
