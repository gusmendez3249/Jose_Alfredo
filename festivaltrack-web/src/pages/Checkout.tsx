import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react';
import { useParams } from 'react-router-dom';
import { boletosService } from '../services/boletos.service';
import { eventosService, estaALaVenta, lugarDe } from '../services/eventos.service';
import type { Boleto, Evento } from '../services/tipos';
import {
  CATEGORIAS_A_LA_VENTA,
  MAX_BOLETOS_POR_CATEGORIA,
  PRECIOS_BOLETO,
  type CategoriaALaVenta,
} from '../config/precios';
import { ETIQUETA_CATEGORIA, folioCorto, formatearFechaEvento, formatearPrecio } from '../lib/formato';
import { mensajeDeError } from '../lib/errores';
import {
  formatearNumeroTarjeta,
  formatearVencimiento,
  soloDigitos,
  validarTarjeta,
  type DatosTarjeta,
  type ErroresTarjeta,
} from '../lib/tarjeta';
import { clases } from '../components/ui/clases';
import { Alert } from '../components/ui/Alert';
import { Button, ButtonLink } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { Field } from '../components/ui/Field';
import { IconoAlerta, IconoCheck, IconoEstrella, IconoTarjeta } from '../components/ui/Icons';
import { QuantityStepper } from '../components/ui/QuantityStepper';
import { Skeleton } from '../components/ui/Skeleton';
import { StatusMessage } from '../components/ui/StatusMessage';
import { useRecurso } from '../hooks/useRecurso';
import { useTituloPagina } from '../hooks/useTituloPagina';

type Cantidades = Record<CategoriaALaVenta, number>;

const TARJETA_VACIA: DatosTarjeta = { titular: '', numero: '', vencimiento: '', cvv: '' };
const CANTIDADES_INICIALES: Cantidades = { VIP: 0, GENERAL: 1 };

function totalDe(cantidades: Cantidades): { boletos: number; monto: number } {
  return CATEGORIAS_A_LA_VENTA.reduce(
    (acc, c) => ({ boletos: acc.boletos + cantidades[c], monto: acc.monto + cantidades[c] * PRECIOS_BOLETO[c] }),
    { boletos: 0, monto: 0 },
  );
}

function Contenedor({ children, ancho = 'ancho' }: { children: ReactNode; ancho?: 'ancho' | 'estrecho' }) {
  return <div className={clases('pagina', ancho === 'estrecho' && 'pagina--lectura')}>{children}</div>;
}

function ListaComprados({ boletos }: { boletos: Boleto[] }) {
  return (
    <ul className="lista-comprados">
      {boletos.map((b) => (
        <li key={b.id}>
          <span>
            {ETIQUETA_CATEGORIA[b.categoria]} · Folio {folioCorto(b.id)}
          </span>
          <span>{formatearPrecio(b.precio)}</span>
        </li>
      ))}
    </ul>
  );
}

/** Tarjeta de una categoría con su contador; la VIP lleva borde dorado y estrella, como en la app móvil. */
function TarjetaCategoria({
  categoria,
  cantidad,
  deshabilitado,
  onCambiar,
}: {
  categoria: CategoriaALaVenta;
  cantidad: number;
  deshabilitado: boolean;
  onCambiar: (nueva: number) => void;
}) {
  const etiqueta = ETIQUETA_CATEGORIA[categoria];
  const esVip = categoria === 'VIP';
  return (
    <Card dorada={esVip} className="categoria">
      <div className="categoria__info">
        <p className="categoria__nombre">
          {esVip && <IconoEstrella tamano={18} />}
          {etiqueta}
        </p>
        <p className="categoria__precio">
          {formatearPrecio(PRECIOS_BOLETO[categoria])} <span className="meta">c/u</span>
        </p>
      </div>
      <QuantityStepper
        nombre={etiqueta}
        valor={cantidad}
        max={MAX_BOLETOS_POR_CATEGORIA}
        deshabilitado={deshabilitado}
        onCambiar={onCambiar}
      />
    </Card>
  );
}

function FormularioCompra({ evento }: { evento: Evento }) {
  const [cantidades, setCantidades] = useState<Cantidades>(CANTIDADES_INICIALES);
  const [tarjeta, setTarjeta] = useState<DatosTarjeta>(TARJETA_VACIA);
  const [errores, setErrores] = useState<ErroresTarjeta>({});
  const [intentado, setIntentado] = useState(false);
  const [procesando, setProcesando] = useState(false);
  const [progreso, setProgreso] = useState<{ actual: number; total: number } | null>(null);
  const [comprados, setComprados] = useState<Boleto[]>([]);
  const [errorCompra, setErrorCompra] = useState('');
  const [exito, setExito] = useState(false);

  // Evita el doble envío aunque se pulse dos veces antes de que React vuelva a pintar.
  const enviandoRef = useRef(false);
  const montadoRef = useRef(true);
  useEffect(() => {
    montadoRef.current = true;
    return () => {
      montadoRef.current = false;
    };
  }, []);

  const { boletos: totalBoletos, monto: totalMonto } = totalDe(cantidades);
  const fecha = formatearFechaEvento(evento.fechaHora);
  const lugar = lugarDe(evento);

  const actualizarTarjeta = (campo: keyof DatosTarjeta, valor: string) => {
    const siguiente = { ...tarjeta, [campo]: valor };
    setTarjeta(siguiente);
    if (intentado) setErrores(validarTarjeta(siguiente));
  };

  const comprar = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (enviandoRef.current) return;

    setIntentado(true);
    const erroresTarjeta = validarTarjeta(tarjeta);
    setErrores(erroresTarjeta);
    if (Object.keys(erroresTarjeta).length > 0) return;

    // El backend, con cantidad > 1, crea UN solo boleto (un QR) por precio × cantidad.
    // Para que cada persona tenga su QR se hace una compra de 1 boleto por cada uno, en serie.
    const pedido = CATEGORIAS_A_LA_VENTA.flatMap((c) => Array.from({ length: cantidades[c] }, () => c));
    if (pedido.length === 0) {
      setErrorCompra('Elige al menos un boleto.');
      return;
    }

    enviandoRef.current = true;
    setProcesando(true);
    setErrorCompra('');

    let compradosAhora = 0;
    let fallo: unknown = null;
    for (const [indice, categoria] of pedido.entries()) {
      // Si el usuario salió de la página, no se siguen comprando boletos en segundo plano.
      if (!montadoRef.current) break;
      setProgreso({ actual: indice + 1, total: pedido.length });
      try {
        const boleto = await boletosService.comprar({
          eventoId: evento.id,
          categoria,
          cantidad: 1,
          metodoPago: 'TARJETA_CREDITO',
        });
        compradosAhora += 1;
        // Lo ya comprado se descuenta del pedido: un reintento solo compra lo que faltó.
        setCantidades((previas) => ({ ...previas, [categoria]: previas[categoria] - 1 }));
        setComprados((previos) => [...previos, boleto]);
      } catch (error) {
        fallo = error;
        break;
      }
    }

    enviandoRef.current = false;
    setProcesando(false);
    setProgreso(null);

    if (fallo === null) {
      setTarjeta(TARJETA_VACIA);
      setExito(true);
      return;
    }
    const motivo = mensajeDeError(fallo);
    setErrorCompra(
      compradosAhora > 0
        ? `Se ${compradosAhora === 1 ? 'compró' : 'compraron'} ${compradosAhora} de ${pedido.length} boletos. No se pudo completar el resto: ${motivo} Puedes reintentar la compra de los que faltan.`
        : `No se pudo completar la compra. ${motivo}`,
    );
  };

  if (exito) {
    return (
      <Contenedor ancho="estrecho">
        <Card className="exito" role="status">
          <div className="exito__icono">
            <IconoCheck tamano={44} />
          </div>
          <h1 className="titulo-pagina">¡Compra realizada!</h1>
          <p className="meta">
            {comprados.length === 1 ? 'Tu boleto' : `Tus ${comprados.length} boletos`} para {evento.nombre}{' '}
            {comprados.length === 1 ? 'ya está disponible' : 'ya están disponibles'} en Mis boletos.
          </p>
          <ListaComprados boletos={comprados} />
          <div className="exito__acciones">
            <ButtonLink to="/mis-boletos" tamano="grande" mayusculas>
              Ver mis boletos
            </ButtonLink>
            <ButtonLink to="/" variante="secundario" tamano="grande" mayusculas>
              Volver al inicio
            </ButtonLink>
          </div>
        </Card>
      </Contenedor>
    );
  }

  const errorDe = (campo: keyof DatosTarjeta) => (intentado ? errores[campo] : undefined);
  const restantes = comprados.length > 0;
  const textoBoton = procesando
    ? progreso
      ? `Comprando boleto ${progreso.actual} de ${progreso.total}…`
      : 'Procesando…'
    : restantes
      ? `Comprar ${totalBoletos} ${totalBoletos === 1 ? 'restante' : 'restantes'} · ${formatearPrecio(totalMonto)}`
      : `Confirmar pago · ${formatearPrecio(totalMonto)}`;

  return (
    <Contenedor>
      <header className="pagina__cabecera">
        <h1 className="titulo-pagina">Finalizar compra</h1>
        <p className="checkout__evento">{evento.nombre}</p>
        <p className="meta">{[fecha, lugar].filter(Boolean).join(' · ')}</p>
      </header>

      {restantes && (
        <Card className="comprados" aria-labelledby="titulo-comprados">
          <h2 id="titulo-comprados" className="titulo-seccion">
            Boletos ya comprados
          </h2>
          <ListaComprados boletos={comprados} />
          <ButtonLink to="/mis-boletos" variante="secundario" tamano="chico">
            Ver mis boletos
          </ButtonLink>
        </Card>
      )}

      <form onSubmit={comprar} noValidate className="checkout">
        <div className="checkout__columna">
          <section className="seccion" aria-labelledby="titulo-boletos">
            <h2 id="titulo-boletos" className="titulo-seccion">
              Boletos
            </h2>
            <div className="pila">
              {CATEGORIAS_A_LA_VENTA.map((categoria) => (
                <TarjetaCategoria
                  key={categoria}
                  categoria={categoria}
                  cantidad={cantidades[categoria]}
                  deshabilitado={procesando}
                  onCambiar={(nueva) => setCantidades((previas) => ({ ...previas, [categoria]: nueva }))}
                />
              ))}
            </div>
          </section>

          <section className="seccion" aria-labelledby="titulo-pago">
            <h2 id="titulo-pago" className="titulo-seccion">
              <IconoTarjeta tamano={20} /> Detalles de pago
            </h2>
            <Alert tono="info">Pago simulado para la demostración: no se hace ningún cargo a tu tarjeta.</Alert>

            <div className="pila">
              <Field
                etiqueta="Titular de la tarjeta"
                id="tarjeta-titular"
                type="text"
                autoComplete="cc-name"
                placeholder="Nombre como aparece en la tarjeta"
                value={tarjeta.titular}
                onChange={(e) => actualizarTarjeta('titular', e.target.value)}
                error={errorDe('titular')}
                disabled={procesando}
                required
              />
              <Field
                etiqueta="Número de tarjeta"
                id="tarjeta-numero"
                type="text"
                inputMode="numeric"
                autoComplete="cc-number"
                placeholder="0000 0000 0000 0000"
                maxLength={19}
                value={tarjeta.numero}
                onChange={(e) => actualizarTarjeta('numero', formatearNumeroTarjeta(e.target.value))}
                error={errorDe('numero')}
                disabled={procesando}
                required
              />
              <div className="fila-campos">
                <Field
                  etiqueta="Vencimiento"
                  id="tarjeta-vencimiento"
                  type="text"
                  inputMode="numeric"
                  autoComplete="cc-exp"
                  placeholder="MM/AA"
                  maxLength={5}
                  value={tarjeta.vencimiento}
                  onChange={(e) => actualizarTarjeta('vencimiento', formatearVencimiento(e.target.value))}
                  error={errorDe('vencimiento')}
                  disabled={procesando}
                  required
                />
                <Field
                  etiqueta="CVV"
                  id="tarjeta-cvv"
                  type="password"
                  inputMode="numeric"
                  autoComplete="cc-csc"
                  placeholder="123"
                  maxLength={4}
                  value={tarjeta.cvv}
                  onChange={(e) => actualizarTarjeta('cvv', soloDigitos(e.target.value).slice(0, 4))}
                  error={errorDe('cvv')}
                  disabled={procesando}
                  required
                />
              </div>
            </div>
          </section>
        </div>

        <aside className="checkout__resumen" aria-labelledby="titulo-resumen">
          <Card destacada className="pila">
            <h2 id="titulo-resumen" className="eyebrow">
              Total a pagar
            </h2>
            <div>
              <p className="resumen__importe">
                {formatearPrecio(totalMonto)}
              </p>
              <p className="meta">
                {totalBoletos} {totalBoletos === 1 ? 'boleto' : 'boletos'}
              </p>
            </div>

            {totalBoletos > 0 && (
              <ul className="resumen__desglose">
                {CATEGORIAS_A_LA_VENTA.filter((c) => cantidades[c] > 0).map((c) => (
                  <li key={c}>
                    <span>
                      {ETIQUETA_CATEGORIA[c]} × {cantidades[c]}
                    </span>
                    <span>{formatearPrecio(cantidades[c] * PRECIOS_BOLETO[c])}</span>
                  </li>
                ))}
              </ul>
            )}

            {errorCompra && <Alert tono="error">{errorCompra}</Alert>}

            <Button
              type="submit"
              tamano="grande"
              bloque
              mayusculas
              cargando={procesando}
              disabled={totalBoletos === 0}
            >
              {textoBoton}
            </Button>
            <ButtonLink to="/events" variante="fantasma" bloque>
              Cancelar
            </ButtonLink>
          </Card>
        </aside>
      </form>
    </Contenedor>
  );
}

export default function Checkout() {
  const { eventoId = '' } = useParams();
  const recurso = useRecurso((signal) => eventosService.porId(eventoId, signal), eventoId, {
    404: 'Este evento no existe o ya no está disponible.',
  });
  useTituloPagina(recurso.estado === 'listo' ? `Comprar · ${recurso.datos.nombre}` : 'Comprar boletos');

  if (recurso.estado === 'cargando') {
    return (
      <Contenedor>
        <div role="status" className="pila" aria-label="Cargando evento">
          <Skeleton alto={28} ancho="50%" />
          <Skeleton alto={16} ancho="35%" />
          <Skeleton alto={96} />
          <Skeleton alto={96} />
          <span className="solo-lectores">Cargando evento…</span>
        </div>
      </Contenedor>
    );
  }

  if (recurso.estado === 'listo' && estaALaVenta(recurso.datos)) {
    return <FormularioCompra evento={recurso.datos} />;
  }

  // El backend solo vende eventos PUBLICADO o EN_CURSO; sin evento no hay compra (nada de ids por defecto).
  const noDisponible = recurso.estado === 'error' ? recurso.mensaje : 'Este evento ya no está a la venta.';
  const puedeReintentar = recurso.estado === 'error' && recurso.status !== 404;
  return (
    <Contenedor ancho="estrecho">
      <StatusMessage
        rol="alert"
        nivelTitulo={1}
        icono={IconoAlerta}
        titulo="No se puede comprar"
        texto={noDisponible}
        acciones={
          <>
            {puedeReintentar && (
              <Button pildora onClick={recurso.reintentar}>
                Reintentar
              </Button>
            )}
            <ButtonLink to="/events" variante="secundario" pildora>
              Ver eventos
            </ButtonLink>
          </>
        }
      />
    </Contenedor>
  );
}
