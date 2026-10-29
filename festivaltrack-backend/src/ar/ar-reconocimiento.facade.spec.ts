import { Test } from '@nestjs/testing';
import { BadRequestException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { ArCatalogoService } from './ar-catalogo.service';
import { ArReconocimientoFacade } from './ar-reconocimiento.facade';
import { RECONOCEDOR_VISUAL, ReconocedorVisual, ResultadoVisual } from './vision/reconocedor-visual';

/**
 * Prueba de integración del flujo AR: Facade + ArCatalogoService reales,
 * con una BD en memoria y un reconocedor visual simulado (no gasta API).
 * Criterio de aceptación: el reconocimiento regresa la descripción correcta.
 */

const CATALOGO = [
  {
    id: 'SITIO-MAUSOLEO', nombre: 'Mausoleo de José Alfredo Jiménez',
    ubicacion: 'Panteón Municipal, Dolores Hidalgo, Gto.',
    descripcion: 'Tumba monumental del compositor inaugurada en 1998.',
    pistasVisuales: 'Techo en forma de sombrero charro con sarape de colores',
    imagenUrl: null, audioUrl: null, galeriaId: 'GAL-1', latitud: null, longitud: null,
    activo: true, createdAt: new Date(), updatedAt: new Date(),
  },
  {
    id: 'SITIO-PARROQUIA', nombre: 'Parroquia de Nuestra Señora de los Dolores',
    ubicacion: 'Jardín Principal, Dolores Hidalgo, Gto.',
    descripcion: 'Templo barroco desde donde se dio el Grito de Independencia.',
    pistasVisuales: 'Fachada barroca de cantera rosa con dos torres',
    imagenUrl: null, audioUrl: null, galeriaId: null, latitud: null, longitud: null,
    activo: true, createdAt: new Date(), updatedAt: new Date(),
  },
];

function prismaEnMemoria() {
  const visitas: any[] = [];
  return {
    visitas,
    sitioAR: {
      findMany: jest.fn(async () => CATALOGO.filter((s) => s.activo)),
      findUnique: jest.fn(async ({ where }) => CATALOGO.find((s) => s.id === where.id) ?? null),
    },
    visitaSitio: {
      create: jest.fn(async ({ data }) => {
        const v = { id: `V${visitas.length + 1}`, fechaVisita: new Date(), ...data };
        visitas.push(v);
        return v;
      }),
      findMany: jest.fn(async ({ where }) =>
        visitas
          .filter((v) => v.usuarioId === where.usuarioId)
          .map((v) => ({ ...v, sitio: CATALOGO.find((s) => s.id === v.sitioId) })),
      ),
    },
  };
}

describe('ArReconocimientoFacade (integración)', () => {
  let facade: ArReconocimientoFacade;
  let prisma: ReturnType<typeof prismaEnMemoria>;
  let respuestaVisual: ResultadoVisual;
  const reconocedor: ReconocedorVisual = { identificar: jest.fn(async () => respuestaVisual) };
  const foto = Buffer.from([0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10]); // cabecera JPEG

  beforeEach(async () => {
    prisma = prismaEnMemoria();
    const modulo = await Test.createTestingModule({
      providers: [
        ArReconocimientoFacade,
        ArCatalogoService,
        { provide: PrismaService, useValue: prisma },
        { provide: RECONOCEDOR_VISUAL, useValue: reconocedor },
      ],
    }).compile();
    facade = modulo.get(ArReconocimientoFacade);
  });

  it('regresa la descripción correcta del sitio reconocido y registra la visita', async () => {
    respuestaVisual = { sitioId: 'SITIO-MAUSOLEO', confianza: 0.93, razon: 'sombrero charro' };

    const r = await facade.reconocer(foto, 'U1');

    expect(r.reconocido).toBe(true);
    if (!r.reconocido) return;
    expect(r.sitio.nombre).toBe('Mausoleo de José Alfredo Jiménez');
    expect(r.sitio.descripcion).toBe('Tumba monumental del compositor inaugurada en 1998.');
    expect(r.sitio.ubicacion).toBe('Panteón Municipal, Dolores Hidalgo, Gto.');
    expect(r.sitio).not.toHaveProperty('pistasVisuales');
    expect(prisma.visitas).toHaveLength(1);

    // El reconocedor recibió todo el catálogo activo como candidatos.
    expect((reconocedor.identificar as jest.Mock).mock.calls[0][2]).toHaveLength(CATALOGO.length);
  });

  it('el historial muestra los sitios visitados por el usuario', async () => {
    respuestaVisual = { sitioId: 'SITIO-PARROQUIA', confianza: 0.8, razon: 'dos torres' };
    await facade.reconocer(foto, 'U1');
    await facade.reconocer(foto, 'OTRO');

    const historial = await facade.historial('U1');

    expect(historial).toHaveLength(1);
    expect(historial[0].sitio.nombre).toBe('Parroquia de Nuestra Señora de los Dolores');
  });

  it('no reconoce si la confianza es baja', async () => {
    respuestaVisual = { sitioId: 'SITIO-MAUSOLEO', confianza: 0.3, razon: 'borroso' };
    const r = await facade.reconocer(foto, 'U1');
    expect(r.reconocido).toBe(false);
    expect(prisma.visitas).toHaveLength(0);
  });

  it('ignora IDs que no existen en el catálogo', async () => {
    respuestaVisual = { sitioId: 'SITIO-INVENTADO', confianza: 0.99, razon: '' };
    const r = await facade.reconocer(foto, 'U1');
    expect(r.reconocido).toBe(false);
  });

  it('rechaza archivos que no son imagen', async () => {
    await expect(facade.reconocer(Buffer.from('%PDF-1.7'), 'U1')).rejects.toThrow(BadRequestException);
    await expect(facade.reconocer(undefined, 'U1')).rejects.toThrow(BadRequestException);
  });
});
