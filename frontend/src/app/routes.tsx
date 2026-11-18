import { Navigate, type RouteObject } from 'react-router';
import { ForgotPasswordPage } from '@/features/auth/pages/ForgotPasswordPage';
import { LoginPage } from '@/features/auth/pages/LoginPage';
import { RegisterPage } from '@/features/auth/pages/RegisterPage';
import { ResetPasswordPage } from '@/features/auth/pages/ResetPasswordPage';
import { GuestOnly } from '@/features/auth/routes/GuestOnly';
import { RequireAuth } from '@/features/auth/routes/RequireAuth';
import { HomePage } from '@/pages/HomePage';

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
    children: [{ path: '/', element: <HomePage /> }],
  },
  { path: '*', element: <Navigate to="/" replace /> },
];
