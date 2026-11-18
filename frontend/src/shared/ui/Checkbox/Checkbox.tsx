import type { InputHTMLAttributes, Ref } from 'react';
import styles from './Checkbox.module.css';

interface CheckboxProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label: string;
  ref?: Ref<HTMLInputElement>;
}

export function Checkbox({ label, ref, ...inputProps }: CheckboxProps) {
  return (
    <label className={styles.checkbox}>
      <input ref={ref} type="checkbox" className={styles.input} {...inputProps} />
      <span>{label}</span>
    </label>
  );
}
