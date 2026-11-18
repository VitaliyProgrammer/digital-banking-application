import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { useFieldErrorText, useRequestErrorText } from '@/shared/lib/forms';
import { Button } from '@/shared/ui/Button/Button';
import { Checkbox } from '@/shared/ui/Checkbox/Checkbox';
import { FormAlert } from '@/shared/ui/FormAlert/FormAlert';
import { PasswordField } from '@/shared/ui/PasswordField/PasswordField';
import { TextField } from '@/shared/ui/TextField/TextField';
import { useAuth } from '../model/AuthContext';
import { readRememberedEmail, rememberEmail } from '../model/rememberedEmail';
import { loginSchema, type LoginForm } from '../model/schemas';
import { AuthLayout } from '../ui/AuthLayout';
import styles from './AuthPages.module.css';

export function LoginPage() {
  const { t } = useTranslation();
  const { login } = useAuth();
  const fieldErrorText = useFieldErrorText();
  const requestErrorText = useRequestErrorText();
  const [requestError, setRequestError] = useState<string | null>(null);

  const rememberedEmail = readRememberedEmail();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: rememberedEmail, password: '', rememberEmail: Boolean(rememberedEmail) },
  });

  const onSubmit = async ({ email, password, rememberEmail: remember }: LoginForm) => {
    setRequestError(null);
    try {
      await login({ email, password });
      rememberEmail(remember ? email : null);
    } catch (error) {
      setRequestError(requestErrorText(error));
    }
  };

  return (
    <AuthLayout
      title={t('auth.login.title')}
      subtitle={t('auth.login.subtitle')}
      footer={
        <>
          {t('auth.login.noAccount')} <Link to="/register">{t('auth.login.toRegister')}</Link>
        </>
      }
    >
      <form className={styles.form} onSubmit={handleSubmit(onSubmit)} noValidate>
        {requestError && <FormAlert tone="error">{requestError}</FormAlert>}

        <TextField
          label={t('auth.fields.email')}
          type="email"
          autoComplete="username"
          autoFocus={!rememberedEmail}
          error={fieldErrorText(errors.email)}
          {...register('email')}
        />

        <PasswordField
          label={t('auth.fields.password')}
          autoComplete="current-password"
          autoFocus={Boolean(rememberedEmail)}
          error={fieldErrorText(errors.password)}
          {...register('password')}
        />

        <div className={styles.row}>
          <Checkbox label={t('auth.login.remember')} {...register('rememberEmail')} />
          <Link className={styles.link} to="/forgot-password">
            {t('auth.login.forgot')}
          </Link>
        </div>

        <Button type="submit" fullWidth loading={isSubmitting}>
          {t('auth.login.submit')}
        </Button>
      </form>
    </AuthLayout>
  );
}
