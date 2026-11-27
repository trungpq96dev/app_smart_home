import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './index.css';
import { App } from './App.js';
import { WebAccessGate } from './components/WebAccessGate.js';
import { SmartHomeProvider } from './store/SmartHomeContext.js';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <WebAccessGate>
      <SmartHomeProvider>
        <App />
      </SmartHomeProvider>
    </WebAccessGate>
  </StrictMode>,
);
