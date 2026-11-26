/** Backend local que se usa solo con `npm run dev` cuando no hay VITE_API_URL. */
const API_URL_DESARROLLO = 'http://localhost:3001/api/v1';

interface Configuracion {
  /** URL base de la API sin barra final; cadena vacía si la configuración es inválida. */
  apiUrl: string;
  /** Mensaje para mostrar en pantalla si la configuración no permite arrancar. */
  error: string | null;
}

function esLocal(host: string): boolean {
  return host === 'localhost' || host === '127.0.0.1' || host === '[::1]';
}

function resolverConfiguracion(): Configuracion {
  const valor = import.meta.env.VITE_API_URL?.trim();

  if (!valor) {
    // En producción no se cae en localhost: el sitio publicado llamaría a la máquina del visitante.
    if (import.meta.env.PROD) {
      return {
        apiUrl: '',
        error:
          'Falta la variable VITE_API_URL en la compilación. Configúrala con la URL del backend (por ejemplo https://<tu-backend>.onrender.com/api/v1) y vuelve a compilar.',
      };
    }
    return { apiUrl: API_URL_DESARROLLO, error: null };
  }

  let url: URL;
  try {
    url = new URL(valor);
  } catch {
    return { apiUrl: '', error: `VITE_API_URL no es una URL válida: "${valor}".` };
  }

  const protocoloValido = url.protocol === 'https:' || (url.protocol === 'http:' && esLocal(url.hostname));
  if (!protocoloValido) {
    return { apiUrl: '', error: 'VITE_API_URL debe usar https (http solo se admite para localhost).' };
  }

  return { apiUrl: valor.replace(/\/+$/, ''), error: null };
}

export const configuracion: Configuracion = resolverConfiguracion();
export const API_URL = configuracion.apiUrl;
