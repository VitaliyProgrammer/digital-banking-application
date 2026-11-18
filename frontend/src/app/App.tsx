import { createBrowserRouter, RouterProvider } from 'react-router';
import { AuthProvider } from '@/features/auth/model/AuthProvider';
import { routes } from './routes';

const router = createBrowserRouter(routes, { basename: '/app' });

export function App() {
  return (
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>
  );
}
