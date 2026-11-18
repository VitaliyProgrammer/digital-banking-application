import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { useFieldErrorText, useRequestErrorText } from '@/shared/lib/forms';
import { Button } from '@/shared/ui/Button/Button';
import { FormAlert } from '@/shared/ui/FormAlert/FormAlert';
import { TextField } from '@/shared/ui/TextField/TextField';
import { authApi } from '../api/authApi';
import { readRememberedEmail } from '../model/rememberedEmail';
import { forgotPasswordSchema, type ForgotPasswordForm } from '../model/schemas';
import { AuthLayout } from '../ui/AuthLayout';
import styles from './AuthPages.module.css';

export function ForgotPasswordPage() {
  const { t } = useTranslation();
  const fieldErrorText = useFieldErrorText();
  const requestErrorText = useRequestErrorText();
  const [requestError, setRequestError] = useState<string | null>(null);
  const [sent, setSent] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ForgotPasswordForm>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: readRememberedEmail() },
  });

  const onSubmit = async ({ email }: ForgotPasswordForm) => {
    setRequestError(null);
    try {
      await authApi.requestPasswordReset(email);
      setSent(true);
    } catch (error) {
      setRequestError(requestErrorText(error));
    }
  };

  return (
    <AuthLayout
      title={t('auth.forgot.title')}
      subtitle={t('auth.forgot.subtitle')}
      footer={<Link to="/login">{t('auth.forgot.back')}</Link>}
    >
      {sent ? (
        <FormAlert tone="success">{t('auth.forgot.sent')}</FormAlert>
      ) : (
        <form className={styles.form} onSubmit={handleSubmit(onSubmit)} noValidate>
          {requestError && <FormAlert tone="error">{requestError}</FormAlert>}

          <TextField
            label={t('auth.fields.email')}
            type="email"
            autoComplete="email"
            autoFocus
            error={fieldErrorText(errors.email)}
            {...register('email')}
          />

          <Button type="submit" fullWidth loading={isSubmitting}>
            {t('auth.forgot.submit')}
          </Button>
        </form>
      )}
    </AuthLayout>
  );
}
