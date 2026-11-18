import 'i18next';
import type { uk } from './locales/uk';

declare module 'i18next' {
  interface CustomTypeOptions {
    resources: { translation: typeof uk };
  }
}
