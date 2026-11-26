import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import '@fontsource/roboto/latin-400.css';
import '@fontsource/roboto/latin-500.css';
import '@fontsource/roboto/latin-700.css';
import './styles/tokens.css';
import './styles/base.css';
import './styles/components.css';
import './styles/layout.css';
import './styles/pages.css';
import App from './App.tsx';
import ErrorDeConfiguracion from './components/ErrorDeConfiguracion';
import { configuracion } from './config/env';
import { instalarRecargaPorDespliegue } from './lib/recargaPorDespliegue';

instalarRecargaPorDespliegue();

const raiz = document.getElementById('root');
if (raiz) {
  createRoot(raiz).render(
    <StrictMode>{configuracion.error ? <ErrorDeConfiguracion mensaje={configuracion.error} /> : <App />}</StrictMode>,
  );
}
