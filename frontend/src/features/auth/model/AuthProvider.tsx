import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { clearTokens, getRefreshToken, onSessionEnd, saveTokens } from '@/shared/api/tokenStore';
import { authApi, type Credentials } from '../api/authApi';
import { AuthContext, type AuthState } from './AuthContext';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>(() =>
    getRefreshToken() ? { status: 'restoring' } : { status: 'anonymous' },
  );

  useEffect(() => onSessionEnd(() => setState({ status: 'anonymous' })), []);

  useEffect(() => {
    if (state.status !== 'restoring') {
      return;
    }
    authApi
      .currentUser()
      .then((user) => setState({ status: 'authenticated', user }))
      .catch(() => {
        clearTokens();
        setState({ status: 'anonymous' });
      });
  }, [state.status]);

  const login = useCallback(async (credentials: Credentials) => {
    saveTokens(await authApi.login(credentials));
    const user = await authApi.currentUser();
    setState({ status: 'authenticated', user });
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = getRefreshToken();
    clearTokens();
    setState({ status: 'anonymous' });
    if (refreshToken) {
      // The client is logged out on this device at once; the bank revokes the token in the background.
      await authApi.logout(refreshToken).catch(() => undefined);
    }
  }, []);

  const value = useMemo(() => ({ state, login, logout }), [state, login, logout]);

  return <AuthContext value={value}>{children}</AuthContext>;
}
