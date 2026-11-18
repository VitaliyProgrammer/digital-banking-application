import { Navigate, Outlet, useLocation } from 'react-router';
import { PageLoader } from '@/shared/ui/PageLoader/PageLoader';
import { useAuth } from '../model/AuthContext';

export interface LoginRedirectState {
  from?: string;
}

/** Screens of the internet bank: a client who is not logged in goes to the login page and comes back after it. */
export function RequireAuth() {
  const { state } = useAuth();
  const location = useLocation();

  if (state.status === 'restoring') {
    return <PageLoader />;
  }

  if (state.status === 'anonymous') {
    const redirectState: LoginRedirectState = { from: location.pathname + location.search };
    return <Navigate to="/login" replace state={redirectState} />;
  }

  return <Outlet />;
}
