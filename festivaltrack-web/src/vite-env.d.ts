/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** URL base de la API, p. ej. https://<tu-backend>.onrender.com/api/v1 */
  readonly VITE_API_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
