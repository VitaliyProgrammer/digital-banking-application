import { QueryClientProvider } from '@tanstack/react-query';
import { createBrowserRouter, RouterProvider } from 'react-router';
import { AuthProvider } from '@/features/auth/model/AuthProvider';
import { createQueryClient } from '@/shared/api/queryClient';
import { routes } from './routes';

const router = createBrowserRouter(routes, { basename: '/app' });
const queryClient = createQueryClient();

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </QueryClientProvider>
  );
}
