/** Joins CSS class names, skipping the empty ones: cn('row', isActive && 'active'). */
export function cn(...classNames: (string | false | null | undefined)[]): string {
  return classNames.filter(Boolean).join(' ');
}
