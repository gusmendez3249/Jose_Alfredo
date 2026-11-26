import { IconoAlerta } from './ui/Icons';
import { StatusMessage } from './ui/StatusMessage';

/** Compilación sin VITE_API_URL (o inválida): se explica en pantalla en vez de quedar en blanco. */
export default function ErrorDeConfiguracion({ mensaje }: { mensaje: string }) {
  return (
    <main className="pagina pagina--lectura pagina--centrada">
      <StatusMessage
        rol="alert"
        nivelTitulo={1}
        icono={IconoAlerta}
        titulo="FestivalTrack no está configurado"
        texto={mensaje}
      />
    </main>
  );
}

