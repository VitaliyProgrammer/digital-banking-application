import { Navigate, type RouteObject } from 'react-router';
import { ForgotPasswordPage } from '@/features/auth/pages/ForgotPasswordPage';
import { LoginPage } from '@/features/auth/pages/LoginPage';
import { RegisterPage } from '@/features/auth/pages/RegisterPage';
import { ResetPasswordPage } from '@/features/auth/pages/ResetPasswordPage';
import { GuestOnly } from '@/features/auth/routes/GuestOnly';
import { RequireAuth } from '@/features/auth/routes/RequireAuth';
import { HomePage } from '@/features/home/pages/HomePage';
import { SectionPlaceholder } from '@/pages/SectionPlaceholder';
import { AppLayout } from './layout/AppLayout';
import { NAVIGATION } from './layout/navigation';
import type { RouteHandle } from './layout/PageHeading';

const sections: RouteObject[] = NAVIGATION.map((item) => {
  const handle: RouteHandle = item.path === '/' ? {} : { title: item.label };
  const element = item.path === '/' ? <HomePage /> : <SectionPlaceholder />;
  return { path: item.path, element, handle };
});

export const routes: RouteObject[] = [
  {
    element: <GuestOnly />,
    children: [
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
      { path: '/forgot-password', element: <ForgotPasswordPage /> },
    ],
  },
  // Opens from the email, so it works whether the client is logged in or not.
  { path: '/reset-password', element: <ResetPasswordPage /> },
  {
    element: <RequireAuth />,
    children: [{ element: <AppLayout />, children: sections }],
  },
  { path: '*', element: <Navigate to="/" replace /> },
];
