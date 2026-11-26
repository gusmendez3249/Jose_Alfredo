import type { ReactNode } from 'react';
import { clases } from './clases';
import { IconoAlerta, IconoCheck, IconoInfo } from './Icons';

type TonoAlerta = 'error' | 'info' | 'exito';

/**
 * Aviso en línea. `error` usa role="alert" (se anuncia de inmediato); `info` y `exito`,
 * role="status" (se anuncian sin interrumpir).
 */
export function Alert({ tono = 'info', children, className }: { tono?: TonoAlerta; children: ReactNode; className?: string }) {
  const Icono = tono === 'error' ? IconoAlerta : tono === 'exito' ? IconoCheck : IconoInfo;
  return (
    <div className={clases('alerta', `alerta--${tono}`, className)} role={tono === 'error' ? 'alert' : 'status'}>
      <Icono tamano={20} className="alerta__icono" />
      <div>{children}</div>
    </div>
  );
}
