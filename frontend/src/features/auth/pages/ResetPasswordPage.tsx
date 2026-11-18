import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { Link, useSearchParams } from 'react-router';
import { applyFieldErrors, useFieldErrorText, useRequestErrorText } from '@/shared/lib/forms';
import { Button } from '@/shared/ui/Button/Button';
import { FormAlert } from '@/shared/ui/FormAlert/FormAlert';
import { PasswordField } from '@/shared/ui/PasswordField/PasswordField';
import { authApi } from '../api/authApi';
import { resetPasswordSchema, type ResetPasswordForm } from '../model/schemas';
import { AuthLayout } from '../ui/AuthLayout';
import styles from './AuthPages.module.css';

/** Opens from the link in the password reset email: /app/reset-password?token=… */
export function ResetPasswordPage() {
  const { t } = useTranslation();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const fieldErrorText = useFieldErrorText();
  const requestErrorText = useRequestErrorText();
  const [requestError, setRequestError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  const {
    register,
    handleSubmit,
    setValue,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ResetPasswordForm>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { newPassword: '' },
  });

  const onSubmit = async ({ newPassword }: ResetPasswordForm) => {
    if (!token) {
      return;
    }
    setRequestError(null);
    try {
      await authApi.confirmPasswordReset(token, newPassword);
      setDone(true);
    } catch (error) {
      if (!applyFieldErrors(error, setError)) {
        setRequestError(requestErrorText(error));
      }
    }
  };

  if (!token) {
    return (
      <AuthLayout title={t('auth.reset.title')}>
        <FormAlert tone="error">{t('auth.reset.noToken')}</FormAlert>
        <Link className={styles.link} to="/forgot-password">
          {t('auth.reset.requestLink')}
        </Link>
      </AuthLayout>
    );
  }

  if (done) {
    return (
      <AuthLayout title={t('auth.reset.title')}>
        <FormAlert tone="success">{t('auth.reset.done')}</FormAlert>
        <Link className={styles.link} to="/login">
          {t('auth.reset.toLogin')}
        </Link>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout title={t('auth.reset.title')} subtitle={t('auth.reset.subtitle')}>
      <form className={styles.form} onSubmit={handleSubmit(onSubmit)} noValidate>
        {requestError && <FormAlert tone="error">{requestError}</FormAlert>}

        <PasswordField
          label={t('auth.fields.newPassword')}
          autoComplete="new-password"
          autoFocus
          hint={t('password.hint')}
          error={fieldErrorText(errors.newPassword)}
          onGenerate={(password) => setValue('newPassword', password, { shouldValidate: true })}
          {...register('newPassword')}
        />

        <Button type="submit" fullWidth loading={isSubmitting}>
          {t('auth.reset.submit')}
        </Button>
      </form>
    </AuthLayout>
  );
}
