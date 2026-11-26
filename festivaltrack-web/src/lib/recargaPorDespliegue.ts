/**
 * Tras un redeploy, los chunks con hash viejo ya no existen y una página lazy falla al
 * cargarse. Vite emite `vite:preloadError`: se recarga UNA vez para traer el index.html
 * nuevo. Si vuelve a fallar enseguida, no se recarga otra vez (evita el bucle) y el
 * ErrorBoundary muestra el error con su botón "Reintentar".
 */
const CLAVE = 'festivaltrack.recargaPorDespliegue';
const VENTANA_MS = 30_000;

function recargaReciente(): boolean {
  try {
    const marca = Number(sessionStorage.getItem(CLAVE));
    return Number.isFinite(marca) && Date.now() - marca < VENTANA_MS;
  } catch {
    // Sin sessionStorage no se puede saber si ya se recargó: no se arriesga un bucle.
    return true;
  }
}

export function instalarRecargaPorDespliegue(): void {
  window.addEventListener('vite:preloadError', (evento) => {
    if (recargaReciente()) return;
    try {
      sessionStorage.setItem(CLAVE, String(Date.now()));
    } catch {
      return;
    }
    evento.preventDefault();
    window.location.reload();
  });
}

/** Errores típicos de un import() dinámico que no encontró su archivo. */
export function esErrorDeCargaDeModulo(error: unknown): boolean {
  if (!(error instanceof Error)) return false;
  return /dynamically imported module|Importing a module script failed|error loading dynamically imported module|Unable to preload CSS/i.test(
    error.message,
  );
}
