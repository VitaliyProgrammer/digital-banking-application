import type { Messages } from './messages';

export const en: Messages = {
  common: {
    language: 'Language',
    loading: 'Loading…',
    logout: 'Log out',
  },
  validation: {
    required: 'Fill in this field',
    email: 'Enter an email like name@example.com',
    passwordLength: 'The password must be 8 to 72 characters long',
    tooLong: 'Too many characters',
  },
  errors: {
    network: 'No connection to the bank. Check the internet and try again.',
    unexpected: 'Something went wrong. Try again in a minute.',
  },
  password: {
    show: 'Show password',
    hide: 'Hide password',
    generate: 'Generate a strong password',
    generated: 'Generated in your browser. Save it in your password manager.',
    hint: '8 characters or more',
  },
  auth: {
    brand: {
      title: 'Your bank in one place',
      features: {
        accounts: 'Accounts in hryvnia, dollars and euros, cards and transfers',
        payments: 'Utility bills with PDF receipts and cashback',
        savings: 'Savings jars, currency exchange and a credit line',
      },
    },
    fields: {
      email: 'Email',
      password: 'Password',
      newPassword: 'New password',
      firstName: 'First name',
      lastName: 'Last name',
    },
    login: {
      title: 'Log in',
      subtitle: 'Good to see you again',
      remember: 'Remember my email',
      forgot: 'Forgot password?',
      submit: 'Log in',
      noAccount: 'Not a client yet?',
      toRegister: 'Open an account',
    },
    register: {
      title: 'Open an account',
      subtitle: 'A few fields, and a hryvnia account is yours',
      submit: 'Open an account',
      haveAccount: 'Already have an account?',
      toLogin: 'Log in',
    },
    forgot: {
      title: 'Reset your password',
      subtitle: 'We will send a link that works once, for 30 minutes',
      submit: 'Send the link',
      sent: 'If this email has an account, the link is on its way. Check your inbox.',
      back: 'Back to log in',
    },
    reset: {
      title: 'New password',
      subtitle: 'Once saved, every session of the account ends on every device',
      submit: 'Save the password',
      noToken: 'This page opens from the link in the email. Request a new link.',
      requestLink: 'Request a link',
      done: 'Password changed. Log in with the new one now.',
      toLogin: 'Log in',
    },
  },
  home: {
    signedInAs: 'Signed in as {{name}}',
  },
};
