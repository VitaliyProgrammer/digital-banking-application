import { createContext, useContext } from 'react';
import type { Credentials, User } from '../api/authApi';

export type AuthState =
  { status: 'restoring' } | { status: 'anonymous' } | { status: 'authenticated'; user: User };

export interface AuthContextValue {
  state: AuthState;
  login: (credentials: Credentials) => Promise<void>;
  logout: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }
  return context;
}

/** The signed-in client. Only for screens behind <RequireAuth>, where a client is always present. */
export function useCurrentUser(): User {
  const { state } = useAuth();
  if (state.status !== 'authenticated') {
    throw new Error('useCurrentUser is used outside <RequireAuth>');
  }
  return state.user;
}
