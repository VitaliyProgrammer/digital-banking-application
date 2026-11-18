import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { applyFieldErrors, useFieldErrorText, useRequestErrorText } from '@/shared/lib/forms';
import { Button } from '@/shared/ui/Button/Button';
import { FormAlert } from '@/shared/ui/FormAlert/FormAlert';
import { PasswordField } from '@/shared/ui/PasswordField/PasswordField';
import { TextField } from '@/shared/ui/TextField/TextField';
import { authApi } from '../api/authApi';
import { useAuth } from '../model/AuthContext';
import { registerSchema, type RegisterForm } from '../model/schemas';
import { AuthLayout } from '../ui/AuthLayout';
import styles from './AuthPages.module.css';

export function RegisterPage() {
  const { t } = useTranslation();
  const { login } = useAuth();
  const fieldErrorText = useFieldErrorText();
  const requestErrorText = useRequestErrorText();
  const [requestError, setRequestError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setValue,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
    defaultValues: { firstName: '', lastName: '', email: '', password: '' },
  });

  const onSubmit = async (form: RegisterForm) => {
    setRequestError(null);
    try {
      await authApi.register(form);
      await login({ email: form.email, password: form.password });
    } catch (error) {
      if (!applyFieldErrors(error, setError)) {
        setRequestError(requestErrorText(error));
      }
    }
  };

  return (
    <AuthLayout
      title={t('auth.register.title')}
      subtitle={t('auth.register.subtitle')}
      footer={
        <>
          {t('auth.register.haveAccount')} <Link to="/login">{t('auth.register.toLogin')}</Link>
        </>
      }
    >
      <form className={styles.form} onSubmit={handleSubmit(onSubmit)} noValidate>
        {requestError && <FormAlert tone="error">{requestError}</FormAlert>}

        <div className={styles.columns}>
          <TextField
            label={t('auth.fields.firstName')}
            autoComplete="given-name"
            autoFocus
            error={fieldErrorText(errors.firstName)}
            {...register('firstName')}
          />
          <TextField
            label={t('auth.fields.lastName')}
            autoComplete="family-name"
            error={fieldErrorText(errors.lastName)}
            {...register('lastName')}
          />
        </div>

        <TextField
          label={t('auth.fields.email')}
          type="email"
          autoComplete="email"
          error={fieldErrorText(errors.email)}
          {...register('email')}
        />

        <PasswordField
          label={t('auth.fields.password')}
          autoComplete="new-password"
          hint={t('password.hint')}
          error={fieldErrorText(errors.password)}
          onGenerate={(password) => setValue('password', password, { shouldValidate: true })}
          {...register('password')}
        />

        <Button type="submit" fullWidth loading={isSubmitting}>
          {t('auth.register.submit')}
        </Button>
      </form>
    </AuthLayout>
  );
}
