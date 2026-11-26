import { QRCodeSVG } from 'qrcode.react';
import { boletosService } from '../services/boletos.service';
import type { BoletoConEvento, EstadoBoleto } from '../services/tipos';
import { useAuth } from '../auth/useAuth';
import {
  ETIQUETA_CATEGORIA,
  ETIQUETA_ESTADO_BOLETO,
  folioCorto,
  formatearFechaEvento,
  formatearPrecio,
} from '../lib/formato';
import { Badge, type TonoBadge } from '../components/ui/Badge';
import { Button, ButtonLink } from '../components/ui/Button';
import { IconoAlerta, IconoBoleto, IconoEstrella } from '../components/ui/Icons';
import { Skeleton } from '../components/ui/Skeleton';
import { StatusMessage } from '../components/ui/StatusMessage';
import { useRecurso } from '../hooks/useRecurso';
import { useTituloPagina } from '../hooks/useTituloPagina';

/** Por qué no se muestra el QR de un boleto que no está ACTIVO. */
const LEYENDA_SIN_QR: Record<Exclude<EstadoBoleto, 'ACTIVO'>, string> = {
  USADO: 'Este boleto ya se usó para entrar.',
  CANCELADO: 'Este boleto fue cancelado y no da acceso.',
  EXPIRADO: 'Este boleto expiró y ya no da acceso.',
};

const TONO_ESTADO: Record<EstadoBoleto, TonoBadge> = {
  ACTIVO: 'exito',
  USADO: 'neutra',
  CANCELADO: 'error',
  EXPIRADO: 'neutra',
};

function TarjetaBoleto({ boleto }: { boleto: BoletoConEvento }) {
  const fecha = boleto.evento ? formatearFechaEvento(boleto.evento.fechaHora) : '';
  const esVip = boleto.categoria === 'VIP';
  return (
    <article className="boleto" data-estado={boleto.estado} aria-label={`Boleto ${folioCorto(boleto.id)}`}>
      <div className="boleto__encabezado">
        <div className="boleto__badges">
          <Badge tono="dorada">
            {esVip && <IconoEstrella tamano={12} />}
            {ETIQUETA_CATEGORIA[boleto.categoria]}
          </Badge>
          <Badge tono={TONO_ESTADO[boleto.estado]}>{ETIQUETA_ESTADO_BOLETO[boleto.estado]}</Badge>
        </div>
        <h2 className="boleto__evento">{boleto.evento?.nombre ?? 'Evento no disponible'}</h2>
        {fecha && boleto.evento && (
          <p className="meta">
            <time dateTime={boleto.evento.fechaHora}>{fecha}</time>
          </p>
        )}
        <p className="boleto__precio">{formatearPrecio(boleto.precio)}</p>
      </div>

      <div className="boleto__corte" aria-hidden="true" />

      <div className="boleto__cuerpo">
        {/* Solo un boleto ACTIVO enseña su QR; los demás no sirven para entrar. */}
        {boleto.estado === 'ACTIVO' ? (
          <div className="boleto__qr">
            <QRCodeSVG value={boleto.codigoQR} size={160} title={`Código QR del boleto ${folioCorto(boleto.id)}`} />
          </div>
        ) : (
          <p className="boleto__sin-qr">{LEYENDA_SIN_QR[boleto.estado]}</p>
        )}
        <p className="boleto__folio">
          Folio <strong>{folioCorto(boleto.id)}</strong>
        </p>
      </div>
    </article>
  );
}

function BoletoSkeleton() {
  return (
    <div className="boleto" aria-hidden="true">
      <div className="boleto__encabezado">
        <Skeleton alto={22} ancho={120} />
        <Skeleton alto={20} ancho="70%" />
        <Skeleton alto={14} ancho="50%" />
      </div>
      <div className="boleto__corte" />
      <div className="boleto__cuerpo">
        <Skeleton alto={192} ancho={192} />
      </div>
    </div>
  );
}

const cargarBoletos = (signal: AbortSignal) => boletosService.misBoletos(signal);

export default function MyTickets() {
  useTituloPagina('Mis boletos');
  const { usuario } = useAuth();
  const recurso = useRecurso(cargarBoletos, 'mis-boletos');

  return (
    <div className="pagina">
      <header className="pagina__cabecera">
        <h1 className="titulo-pagina">Mis boletos</h1>
        {usuario && <p className="meta">Hola, {usuario.nombre}</p>}
      </header>

      {recurso.estado === 'cargando' && (
        <div role="status" aria-label="Cargando tus boletos" className="rejilla-boletos">
          <BoletoSkeleton />
          <BoletoSkeleton />
          <span className="solo-lectores">Cargando tus boletos…</span>
        </div>
      )}

      {recurso.estado === 'error' && (
        <StatusMessage
          rol="alert"
          icono={IconoAlerta}
          titulo="No se pudieron cargar tus boletos"
          texto={recurso.mensaje}
          acciones={
            <Button pildora onClick={recurso.reintentar}>
              Reintentar
            </Button>
          }
        />
      )}

      {recurso.estado === 'listo' && recurso.datos.length === 0 && (
        <StatusMessage
          icono={IconoBoleto}
          titulo="Aún no tienes boletos"
          texto="Explora los eventos y adquiere tu acceso para disfrutar del festival."
          acciones={
            <ButtonLink to="/events" pildora>
              Ver eventos
            </ButtonLink>
          }
        />
      )}

      {recurso.estado === 'listo' && recurso.datos.length > 0 && (
        <ul className="rejilla-boletos">
          {recurso.datos.map((boleto) => (
            <li key={boleto.id}>
              <TarjetaBoleto boleto={boleto} />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
