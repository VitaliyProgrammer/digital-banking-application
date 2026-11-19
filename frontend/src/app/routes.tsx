import type { ReactNode } from 'react';
import { Navigate, type RouteObject } from 'react-router';
import { AccountsPage } from '@/features/accounts/pages/AccountsPage';
import { CardsPage } from '@/features/cards/pages/CardsPage';
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

/** The screens built so far; every other section of the menu shows a placeholder until its branch. */
const SCREENS: Partial<Record<string, ReactNode>> = {
  '/': <HomePage />,
  '/accounts': <AccountsPage />,
  '/cards': <CardsPage />,
};

const sections: RouteObject[] = NAVIGATION.map((item) => {
  const handle: RouteHandle = item.path === '/' ? {} : { title: item.label };
  return { path: item.path, element: SCREENS[item.path] ?? <SectionPlaceholder />, handle };
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
