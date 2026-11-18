import { z } from 'zod';

/* The same limits as the bank's request DTOs, so the client hears about a mistake before sending the form. */

const email = z
  .string()
  .trim()
  .min(1, 'validation.required')
  .max(254, 'validation.tooLong')
  .pipe(z.email('validation.email'));

const newPassword = z.string().min(8, 'validation.passwordLength').max(72, 'validation.passwordLength');

const name = z.string().trim().min(1, 'validation.required').max(100, 'validation.tooLong');

export const loginSchema = z.object({
  email,
  password: z.string().min(1, 'validation.required'),
  rememberEmail: z.boolean(),
});

export const registerSchema = z.object({
  firstName: name,
  lastName: name,
  email,
  password: newPassword,
});

export const forgotPasswordSchema = z.object({ email });

export const resetPasswordSchema = z.object({ newPassword });

export type LoginForm = z.infer<typeof loginSchema>;
export type RegisterForm = z.infer<typeof registerSchema>;
export type ForgotPasswordForm = z.infer<typeof forgotPasswordSchema>;
export type ResetPasswordForm = z.infer<typeof resetPasswordSchema>;
