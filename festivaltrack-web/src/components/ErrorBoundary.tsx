import { Component, type ErrorInfo, type ReactNode } from 'react';
import { esErrorDeCargaDeModulo } from '../lib/recargaPorDespliegue';
import { Button } from './ui/Button';
import { claseBoton } from './ui/clases';
import { IconoAlerta } from './ui/Icons';
import { StatusMessage } from './ui/StatusMessage';

interface Props {
  children: ReactNode;
}

interface Estado {
  error: unknown;
}

/**
 * Atrapa errores de render (y de páginas lazy que no se pudieron descargar) para que la app
 * no quede en blanco. App.tsx lo monta con `key` = ruta, así que cambiar de página lo reinicia.
 */
export class ErrorBoundary extends Component<Props, Estado> {
  state: Estado = { error: null };

  static getDerivedStateFromError(error: unknown): Estado {
    return { error };
  }

  componentDidCatch(error: unknown, info: ErrorInfo): void {
    // Solo en desarrollo: en producción no se escribe nada en consola.
    if (import.meta.env.DEV) console.error(error, info.componentStack);
  }

  private reintentar = (): void => {
    // Un import() fallido queda cacheado por React.lazy: solo recargar trae el archivo nuevo.
    if (esErrorDeCargaDeModulo(this.state.error)) {
      window.location.reload();
      return;
    }
    this.setState({ error: null });
  };

  render() {
    if (this.state.error === null) return this.props.children;

    const deCarga = esErrorDeCargaDeModulo(this.state.error);
    return (
      <div className="pagina pagina--lectura">
        <StatusMessage
          rol="alert"
          nivelTitulo={1}
          icono={IconoAlerta}
          titulo="Algo salió mal"
          texto={
            deCarga
              ? 'Hay una versión nueva del sitio o se perdió la conexión. Vuelve a cargar la página.'
              : 'Ocurrió un error inesperado al mostrar esta página.'
          }
          acciones={
            <>
              <Button pildora onClick={this.reintentar}>
                Reintentar
              </Button>
              <a href="/" className={claseBoton({ variante: 'secundario', pildora: true })}>
                Ir al inicio
              </a>
            </>
          }
        />
      </div>
    );
  }
}
