import { request } from '@/shared/api/http';
import type { Tokens } from '@/shared/api/tokenStore';

export type Role = 'CLIENT' | 'SUPPORT' | 'ADMIN';

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  createdAt: string;
}

export interface Credentials {
  email: string;
  password: string;
}

export interface Registration extends Credentials {
  firstName: string;
  lastName: string;
}

export const authApi = {
  login: (credentials: Credentials) =>
    request<Tokens>('/authentication/login', { method: 'POST', body: credentials, authenticated: false }),

  register: (registration: Registration) =>
    request<User>('/authentication/register', { method: 'POST', body: registration, authenticated: false }),

  logout: (refreshToken: string) =>
    request<undefined>('/authentication/logout', {
      method: 'POST',
      body: { refreshToken },
      authenticated: false,
    }),

  requestPasswordReset: (email: string) =>
    request<undefined>('/authentication/password-reset', {
      method: 'POST',
      body: { email },
      authenticated: false,
    }),

  confirmPasswordReset: (token: string, newPassword: string) =>
    request<undefined>('/authentication/password-reset/confirm', {
      method: 'POST',
      body: { token, newPassword },
      authenticated: false,
    }),

  currentUser: () => request<User>('/users/me'),
};
