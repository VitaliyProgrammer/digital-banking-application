import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { useFieldErrorText, useRequestErrorText } from '@/shared/lib/forms';
import { Button } from '@/shared/ui/Button/Button';
import { FormAlert } from '@/shared/ui/FormAlert/FormAlert';
import { TextField } from '@/shared/ui/TextField/TextField';
import type { Card } from '../api/cardsApi';
import { useChangePin } from '../model/queries';
import { pinSchema, type PinForm as PinFormValues } from '../model/pinSchema';
import styles from './PinForm.module.css';

/** A PIN field: four digits, the phone's number pad, hidden like a password, never saved by the browser. */
const PIN_INPUT = {
  type: 'password',
  inputMode: 'numeric',
  maxLength: 4,
  autoComplete: 'off',
} as const;

interface PinFormProps {
  card: Card;
  onSaved: () => void;
  onCancel: () => void;
}

export function PinForm({ card, onSaved, onCancel }: PinFormProps) {
  const { t } = useTranslation();
  const fieldErrorText = useFieldErrorText();
  const requestErrorText = useRequestErrorText();
  const changePin = useChangePin(card.id);
  const [requestError, setRequestError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<PinFormValues>({
    resolver: zodResolver(pinSchema(card.pinSet)),
    defaultValues: { currentPin: '', newPin: '', repeatPin: '' },
  });

  const onSubmit = ({ currentPin, newPin }: PinFormValues) => {
    setRequestError(null);
    changePin.mutate(card.pinSet ? { currentPin, newPin } : { newPin }, {
      onSuccess: onSaved,
      onError: (error) => setRequestError(requestErrorText(error)),
    });
  };

  return (
    <form className={styles.form} onSubmit={handleSubmit(onSubmit)} noValidate>
      {requestError && <FormAlert tone="error">{requestError}</FormAlert>}

      <div className={styles.fields}>
        {card.pinSet && (
          <TextField
            label={t('cards.pin.current')}
            autoFocus
            error={fieldErrorText(errors.currentPin)}
            {...PIN_INPUT}
            {...register('currentPin')}
          />
        )}
        <TextField
          label={t('cards.pin.new')}
          autoFocus={!card.pinSet}
          hint={t('cards.pin.hint')}
          error={fieldErrorText(errors.newPin)}
          {...PIN_INPUT}
          {...register('newPin')}
        />
        <TextField
          label={t('cards.pin.repeat')}
          error={fieldErrorText(errors.repeatPin)}
          {...PIN_INPUT}
          {...register('repeatPin')}
        />
      </div>

      <div className={styles.buttons}>
        <Button type="submit" loading={changePin.isPending}>
          {t('cards.pin.save')}
        </Button>
        <Button variant="soft" onClick={onCancel}>
          {t('cards.pin.cancel')}
        </Button>
      </div>
    </form>
  );
}
