import { Navigate, Outlet, useLocation } from 'react-router';
import { PageLoader } from '@/shared/ui/PageLoader/PageLoader';
import { useAuth } from '../model/AuthContext';
import type { LoginRedirectState } from './RequireAuth';

/**
 * Login, registration and password reset. Once the client is logged in, they go to the page they wanted
 * before the login, or to the home page.
 */
export function GuestOnly() {
  const { state } = useAuth();
  const location = useLocation();

  if (state.status === 'restoring') {
    return <PageLoader />;
  }

  if (state.status === 'authenticated') {
    const from = (location.state as LoginRedirectState | null)?.from;
    return <Navigate to={from ?? '/'} replace />;
  }

  return <Outlet />;
}
