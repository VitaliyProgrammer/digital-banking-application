import { useId, type InputHTMLAttributes, type ReactNode, type Ref } from 'react';
import { cn } from '@/shared/lib/cn';
import styles from './TextField.module.css';

export interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  hint?: ReactNode;
  /** A control inside the field on the right, e.g. "show password". */
  endAdornment?: ReactNode;
  ref?: Ref<HTMLInputElement>;
}

export function TextField({
  label,
  error,
  hint,
  endAdornment,
  id,
  className,
  ref,
  ...inputProps
}: TextFieldProps) {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const messageId = `${inputId}-message`;
  const message = error ?? hint;

  return (
    <div className={cn(styles.field, className)}>
      <label className={styles.label} htmlFor={inputId}>
        {label}
      </label>

      <div className={styles.control}>
        <input
          ref={ref}
          id={inputId}
          className={cn(
            styles.input,
            Boolean(error) && styles.invalid,
            Boolean(endAdornment) && styles.withAdornment,
          )}
          aria-invalid={Boolean(error)}
          aria-describedby={message ? messageId : undefined}
          {...inputProps}
        />
        {endAdornment && <div className={styles.adornment}>{endAdornment}</div>}
      </div>

      {message && (
        <div id={messageId} className={cn(styles.message, Boolean(error) && styles.error)}>
          {message}
        </div>
      )}
    </div>
  );
}
