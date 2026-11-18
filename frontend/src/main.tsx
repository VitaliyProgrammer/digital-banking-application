import '@fontsource/manrope/500.css';
import '@fontsource/manrope/600.css';
import '@fontsource/manrope/700.css';
import '@fontsource/manrope/800.css';
import '@fontsource/ibm-plex-mono/500.css';
import '@/shared/styles/global.css';

import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from '@/app/App';

const root = document.getElementById('root');

if (!root) {
  throw new Error('The page has no #root element to mount the application into');
}

createRoot(root).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
