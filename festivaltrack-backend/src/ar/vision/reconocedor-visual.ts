/**
 * Contrato del motor de reconocimiento visual.
 *
 * La fachada AR depende de esta interfaz y no de un proveedor concreto, así se
 * puede cambiar Claude por ML Kit / Cloud Vision, o por un doble de prueba en
 * los tests de integración, sin tocar la lógica del flujo.
 */
export const RECONOCEDOR_VISUAL = Symbol('RECONOCEDOR_VISUAL');

export type MediaTypeImagen = 'image/jpeg' | 'image/png' | 'image/webp';

export interface SitioCandidato {
  id: string;
  nombre: string;
  pistasVisuales: string;
}

export interface ResultadoVisual {
  /** ID del sitio del catálogo que aparece en la foto, o null si ninguno coincide. */
  sitioId: string | null;
  /** 0..1 — qué tan seguro está el modelo de la coincidencia. */
  confianza: number;
  /** Explicación breve (qué rasgos vio). Útil para depurar. */
  razon: string;
}

export interface ReconocedorVisual {
  identificar(
    imagen: Buffer,
    mediaType: MediaTypeImagen,
    candidatos: SitioCandidato[],
  ): Promise<ResultadoVisual>;
}
