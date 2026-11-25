import { PrismaClient } from '@prisma/client';

/**
 * Cartelera de demostración a partir de hoy.
 *
 * 1. Marca como FINALIZADO los eventos que ya terminaron (empezaron hace más de 4 horas).
 * 2. Crea los conciertos del festival para los próximos días, con hora de Ciudad de México.
 * 3. Asigna a cada concierto nuevo las canciones del catálogo como demos (repertorio).
 *
 * Se puede correr varias veces: si un concierto con el mismo nombre todavía no pasa, no se duplica.
 *   npm run seed:cartelera
 */
const prisma = new PrismaClient();

/** Ciudad de México está en UTC-6 todo el año (sin horario de verano desde 2022). */
const UTC_OFFSET_MX = 6;
const DURACION_HORAS = 4;

const PLANTILLAS = [
  {
    nombre: 'Noche de Bohemia en la Plaza',
    dia: 0,
    hora: 20,
    minuto: 0,
    ubicacion: 'Jardín Principal, Dolores Hidalgo',
    escenario: 'Plaza Principal',
    capacidad: 800,
    latitud: 21.1563,
    longitud: -100.9338,
    descripcion:
      'Tríos y bohemios interpretan los clásicos de José Alfredo bajo los faroles del Jardín Principal.',
  },
  {
    nombre: 'Serenata en el Mausoleo',
    dia: 1,
    hora: 21,
    minuto: 30,
    ubicacion: 'Panteón Municipal, Dolores Hidalgo',
    escenario: 'Mausoleo de José Alfredo Jiménez',
    capacidad: 400,
    latitud: 21.1571,
    longitud: -100.9406,
    descripcion:
      'Serenata de mariachi frente al mausoleo en forma de sombrero charro.',
  },
  {
    nombre: 'Recorrido Casa Museo José Alfredo',
    dia: 2,
    hora: 11,
    minuto: 0,
    ubicacion: 'Calle Guanajuato 13, Centro, Dolores Hidalgo',
    escenario: 'Casa Museo',
    capacidad: 150,
    latitud: 21.1574,
    longitud: -100.933,
    descripcion:
      'Visita guiada por la casa natal del compositor, con objetos personales y manuscritos.',
  },
  {
    nombre: 'Gran Gala Mariachi',
    dia: 2,
    hora: 21,
    minuto: 0,
    ubicacion: 'Jardín Principal, Dolores Hidalgo',
    escenario: 'Escenario Principal',
    capacidad: 2500,
    latitud: 21.1566,
    longitud: -100.9341,
    descripcion:
      'Los mejores mariachis del estado en el escenario principal del festival.',
  },
  {
    nombre: 'Homenaje: El Rey',
    dia: 3,
    hora: 21,
    minuto: 0,
    ubicacion: 'Jardín Principal, Dolores Hidalgo',
    escenario: 'Escenario Principal',
    capacidad: 3000,
    latitud: 21.1566,
    longitud: -100.9341,
    descripcion: 'Concierto de cierre en homenaje a José Alfredo Jiménez.',
  },
];

/** Fecha en UTC para "hoy + dia" a la hora indicada de Ciudad de México. */
function fechaMx(ahora: Date, dia: number, hora: number, minuto: number): Date {
  const hoyMx = new Date(ahora.getTime() - UTC_OFFSET_MX * 3600_000);
  return new Date(
    Date.UTC(
      hoyMx.getUTCFullYear(),
      hoyMx.getUTCMonth(),
      hoyMx.getUTCDate() + dia,
      hora + UTC_OFFSET_MX,
      minuto,
    ),
  );
}

async function main() {
  const ahora = new Date();
  const limite = new Date(ahora.getTime() - DURACION_HORAS * 3600_000);

  const finalizados = await prisma.evento.updateMany({
    where: {
      fechaHora: { lt: limite },
      estado: { in: ['PUBLICADO', 'EN_CURSO'] },
    },
    data: { estado: 'FINALIZADO' },
  });
  console.log(
    `Eventos que ya pasaron marcados como FINALIZADO: ${finalizados.count}`,
  );

  const admin = await prisma.administrador.findFirst();
  if (!admin)
    throw new Error(
      'No hay administrador en la base: corre primero el seed principal.',
    );
  const artista = await prisma.artista.findFirst();
  const canciones = await prisma.cancion.findMany({
    orderBy: { titulo: 'asc' },
  });

  for (const p of PLANTILLAS) {
    let fecha = fechaMx(ahora, p.dia, p.hora, p.minuto);
    // Si hoy ya pasó esa hora, el concierto se programa para mañana.
    if (fecha.getTime() < ahora.getTime() + 3600_000)
      fecha = new Date(fecha.getTime() + 24 * 3600_000);

    const existente = await prisma.evento.findFirst({
      where: {
        nombre: p.nombre,
        fechaHora: { gte: limite },
        estado: { in: ['PUBLICADO', 'EN_CURSO'] },
      },
    });
    const evento =
      existente ??
      (await prisma.evento.create({
        data: {
          nombre: p.nombre,
          fechaHora: fecha,
          ubicacion: p.ubicacion,
          escenario: p.escenario,
          descripcion: p.descripcion,
          capacidad: p.capacidad,
          latitud: p.latitud,
          longitud: p.longitud,
          estado: 'PUBLICADO',
          administradorId: admin.id,
          artistaId: artista?.id,
        },
      }));

    const transmision = await prisma.transmision.upsert({
      where: { eventoId: evento.id },
      create: { eventoId: evento.id, titulo: evento.nombre },
      update: {},
    });
    await prisma.cancionTransmision.createMany({
      data: canciones.map((c, orden) => ({
        cancionId: c.id,
        transmisionId: transmision.id,
        orden,
      })),
      skipDuplicates: true,
    });
    console.log(
      `${existente ? 'Ya existía' : 'Creado'}: ${evento.nombre} · ${evento.fechaHora.toISOString()} · ${canciones.length} demos`,
    );
  }
}

main()
  .catch((e) => {
    console.error(e);
    process.exitCode = 1;
  })
  .finally(() => prisma.$disconnect());
