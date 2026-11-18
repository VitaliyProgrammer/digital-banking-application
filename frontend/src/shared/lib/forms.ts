import { useCallback } from 'react';
import type { FieldError, FieldValues, Path, UseFormSetError } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { ApiError, NETWORK_ERROR_CODE } from '@/shared/api/ApiError';

const VALIDATION_KEY_PREFIX = 'validation.';

/**
 * Text for a field error. Rules in the forms carry i18n keys ("validation.required"),
 * the bank's own errors arrive already translated, so they are shown as they are.
 */
export function useFieldErrorText() {
  const { t } = useTranslation();

  return useCallback(
    (error: FieldError | undefined): string | undefined => {
      const message = error?.message;
      if (!message) {
        return undefined;
      }
      return message.startsWith(VALIDATION_KEY_PREFIX) ? t(message as 'validation.required') : message;
    },
    [t],
  );
}

/** Text for a failed request: the bank's own explanation, or a general one when the bank was not reached. */
export function useRequestErrorText() {
  const { t } = useTranslation();

  return useCallback(
    (error: unknown): string => {
      if (error instanceof ApiError) {
        if (error.code === NETWORK_ERROR_CODE) {
          return t('errors.network');
        }
        if (error.status < 500 && error.message) {
          return error.message;
        }
      }
      return t('errors.unexpected');
    },
    [t],
  );
}

/** Puts the bank's field errors (e.g. "this email is taken") under the fields they belong to. */
export function applyFieldErrors<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
): boolean {
  if (!(error instanceof ApiError)) {
    return false;
  }

  const fields = Object.entries(error.fieldErrors);
  fields.forEach(([field, message]) => setError(field as Path<T>, { type: 'server', message }));
  return fields.length > 0;
}
