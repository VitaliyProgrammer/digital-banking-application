import { Link, type LinkProps } from 'react-router';
import { cn } from '@/shared/lib/cn';
import styles from './Button.module.css';

interface ButtonLinkProps extends LinkProps {
  variant?: 'primary' | 'soft' | 'ghost';
}

/** A link that looks like a button: for actions that open another screen rather than do something here. */
export function ButtonLink({ variant = 'primary', className, ...rest }: ButtonLinkProps) {
  return <Link className={cn(styles.button, styles[variant], className)} {...rest} />;
}
