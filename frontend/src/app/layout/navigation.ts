import type { uk } from '@/shared/i18n/locales/uk';

/** A key of the nav texts: 'home', 'cards', … */
export type NavKey = keyof typeof uk.nav;

export interface NavigationItem {
  path: string;
  label: NavKey;
  tag?: NavKey;
}

/** The sidebar of the approved mockup, in its order. */
export const NAVIGATION: NavigationItem[] = [
  { path: '/', label: 'home' },
  { path: '/accounts', label: 'accounts' },
  { path: '/cards', label: 'cards' },
  { path: '/transfers', label: 'transfers' },
  { path: '/payments', label: 'payments' },
  { path: '/exchange', label: 'exchange' },
  { path: '/savings', label: 'savings' },
  { path: '/cashback', label: 'cashback' },
  { path: '/credit-line', label: 'creditLine' },
  { path: '/business', label: 'business', tag: 'businessTag' },
  { path: '/support', label: 'support' },
];
