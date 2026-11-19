import type { ReactNode } from 'react';

/* The stroke icons of the approved mockup. */

interface IconProps {
  size?: number;
}

function Icon({
  size = 18,
  strokeWidth,
  children,
}: IconProps & { strokeWidth: number; children: ReactNode }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {children}
    </svg>
  );
}

export const PlusIcon = () => (
  <Icon strokeWidth={2.4}>
    <path d="M12 5v14M5 12h14" />
  </Icon>
);

export const SendIcon = () => (
  <Icon strokeWidth={2.4}>
    <path d="M5 12h14M13 6l6 6-6 6" />
  </Icon>
);

export const PayIcon = () => (
  <Icon strokeWidth={2}>
    <path d="M6 3h12v18l-3-2-3 2-3-2-3 2z" />
    <path d="M9 8h6M9 12h6" />
  </Icon>
);

export const ExchangeIcon = () => (
  <Icon strokeWidth={2}>
    <path d="M7 7h12l-3-3M17 17H5l3 3" />
  </Icon>
);

export const LockIcon = () => (
  <Icon size={15} strokeWidth={2.2}>
    <rect x="5" y="11" width="14" height="10" rx="2" />
    <path d="M8 11V7a4 4 0 0 1 8 0v4" />
  </Icon>
);

export const PinIcon = () => (
  <Icon size={15} strokeWidth={2.2}>
    <circle cx="6" cy="12" r="1.6" />
    <circle cx="12" cy="12" r="1.6" />
    <circle cx="18" cy="12" r="1.6" />
  </Icon>
);

export const ChevronIcon = ({ className }: { className?: string }) => (
  <svg
    className={className}
    width="16"
    height="16"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2.4"
    strokeLinecap="round"
    strokeLinejoin="round"
    aria-hidden="true"
  >
    <path d="M6 9l6 6 6-6" />
  </svg>
);

export const CopyIcon = () => (
  <Icon size={16} strokeWidth={2}>
    <rect x="9" y="9" width="11" height="11" rx="2.5" />
    <path d="M5 15V6a2 2 0 0 1 2-2h9" />
  </Icon>
);

export const CheckIcon = ({ size = 16 }: IconProps) => (
  <Icon size={size} strokeWidth={2.6}>
    <path d="M5 12.5l4.5 4.5L19 7.5" />
  </Icon>
);
