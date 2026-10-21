import { BadRequestException, Inject, Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { ArCatalogoService } from './ar-catalogo.service';
import { RECONOCEDOR_VISUAL } from './vision/reconocedor-visual';
import type { MediaTypeImagen, ReconocedorVisual } from './vision/reconocedor-visual';

/** Debajo de este umbral se considera que no se reconoció ningún sitio. */
export const CONFIANZA_MINIMA = 0.6;

/**
 * Detecta el formato real por los primeros bytes del archivo: el mimetype que
 * manda el cliente (o la extensión) puede no coincidir con el contenido.
 */
export function detectarMediaType(foto: Buffer): MediaTypeImagen | null {
  if (foto.length > 3 && foto[0] === 0xff && foto[1] === 0xd8 && foto[2] === 0xff) return 'image/jpeg';
  if (foto.length > 8 && foto.subarray(0, 8).equals(Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]))) return 'image/png';
  if (foto.length > 12 && foto.toString('ascii', 0, 4) === 'RIFF' && foto.toString('ascii', 8, 12) === 'WEBP') return 'image/webp';
  return null;
}

/**
 * Facade del módulo AR: en una sola llamada combina
 *   foto de la cámara → reconocimiento visual → catálogo → historial.
 *
 * El controlador (y la app) solo conocen `reconocer()`; no saben qué motor de
 * visión se usa ni cómo se arma el catálogo.
 */
@Injectable()
export class ArReconocimientoFacade {
  constructor(
    private prisma: PrismaService,
    private catalogo: ArCatalogoService,
    @Inject(RECONOCEDOR_VISUAL) private reconocedor: ReconocedorVisual,
  ) {}

  async reconocer(foto: Buffer | undefined, usuarioId: string) {
    if (!foto?.length) throw new BadRequestException('Foto no proporcionada');
    const mediaType = detectarMediaType(foto);
    if (!mediaType) throw new BadRequestException('Formato no soportado: usa JPEG, PNG o WebP');

    const sitios = await this.catalogo.activos();
    if (sitios.length === 0) {
      return { reconocido: false as const, mensaje: 'El catálogo de sitios está vacío' };
    }

    const visual = await this.reconocedor.identificar(
      foto,
      mediaType,
      sitios.map((s) => ({ id: s.id, nombre: s.nombre, pistasVisuales: s.pistasVisuales })),
    );

    // Solo se aceptan IDs que existan en el catálogo: el modelo no puede inventar sitios.
    const sitio = sitios.find((s) => s.id === visual.sitioId);
    if (!sitio || visual.confianza < CONFIANZA_MINIMA) {
      return {
        reconocido: false as const,
        mensaje: 'No reconocimos ningún sitio del festival. Acércate o intenta con otro ángulo.',
      };
    }

    const visita = await this.prisma.visitaSitio.create({
      data: { sitioId: sitio.id, usuarioId, confianza: visual.confianza },
    });

    const { pistasVisuales, ...publico } = sitio;
    return {
      reconocido: true as const,
      confianza: visual.confianza,
      fechaVisita: visita.fechaVisita,
      sitio: publico,
    };
  }

  /** Historial de sitios visitados por el usuario, del más reciente al más antiguo. */
  historial(usuarioId: string) {
    return this.prisma.visitaSitio.findMany({
      where: { usuarioId },
      orderBy: { fechaVisita: 'desc' },
      select: {
        id: true,
        fechaVisita: true,
        confianza: true,
        sitio: { select: { id: true, nombre: true, ubicacion: true, imagenUrl: true } },
      },
    });
  }
}
