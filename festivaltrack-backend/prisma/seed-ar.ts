import { PrismaClient } from '@prisma/client';

/**
 * Catálogo inicial de sitios AR (administrado por el equipo organizador).
 * Usa upsert, así que se puede ejecutar varias veces sin duplicar:
 *   npx ts-node prisma/seed-ar.ts
 */
export async function seedSitiosAR(prisma: PrismaClient, galeriaId?: string) {
  const sitiosAR = [
    {
      id: 'SITIO-MAUSOLEO',
      nombre: 'Mausoleo de José Alfredo Jiménez',
      ubicacion: 'Panteón Municipal, Dolores Hidalgo, Gto.',
      descripcion: 'Tumba monumental del compositor inaugurada en 1998. Su techo reproduce un sarape de Saltillo en mosaico de colores y está rodeado por un sombrero charro gigante; en sus muros se leen títulos de sus canciones.',
      pistasVisuales: 'Estructura con techo ondulado en forma de sombrero charro con franjas de colores tipo sarape (rojo, verde, amarillo, azul) en mosaico; cruz en la parte superior; jardín del panteón alrededor.',
      latitud: 21.1571, longitud: -100.9406,
    },
    {
      id: 'SITIO-PARROQUIA',
      nombre: 'Parroquia de Nuestra Señora de los Dolores',
      ubicacion: 'Jardín Principal, Centro, Dolores Hidalgo, Gto.',
      descripcion: 'Templo barroco churrigueresco del siglo XVIII. Desde su atrio Miguel Hidalgo llamó a la población a levantarse en armas la madrugada del 16 de septiembre de 1810, el Grito de Independencia.',
      pistasVisuales: 'Fachada barroca de cantera rosa muy labrada, dos torres campanario altas a los lados, gran portada central con esculturas, a veces se aprecia una cúpula detrás de la fachada; atrio con escalinata frente a la plaza (Jardín Principal) con árboles y faroles.',
      latitud: 21.1563, longitud: -100.9338,
    },
    {
      id: 'SITIO-HIDALGO',
      nombre: 'Monumento a Miguel Hidalgo',
      ubicacion: 'Jardín Principal, Dolores Hidalgo, Gto.',
      descripcion: 'Estatua en bronce del Padre de la Patria sobre un pedestal de cantera, al centro del jardín donde inició el movimiento de Independencia. Es punto de reunión de la ceremonia del Grito cada 15 de septiembre.',
      pistasVisuales: 'Estatua de bronce de un sacerdote de pie con el brazo extendido sosteniendo un estandarte, sobre pedestal alto de piedra, rodeada de laureles de la India podados.',
      latitud: 21.1566, longitud: -100.9341,
    },
    {
      id: 'SITIO-CASA-MUSEO',
      nombre: 'Casa Museo José Alfredo Jiménez',
      ubicacion: 'Calle Guanajuato 13, Centro, Dolores Hidalgo, Gto.',
      descripcion: 'Casa natal del compositor, nacido el 19 de enero de 1926. Hoy es museo con objetos personales, trajes de charro, discos y manuscritos de canciones como "Camino de Guanajuato".',
      pistasVisuales: 'Casona colonial de fachada amarilla/ocre con balcones de herrería, puerta de madera y placa conmemorativa; interior con patio y vitrinas.',
      latitud: 21.1574, longitud: -100.9330,
    },
  ];
  for (const sitio of sitiosAR) {
    await prisma.sitioAR.upsert({
      where: { id: sitio.id },
      update: sitio,
      create: { ...sitio, galeriaId },
    });
  }
  console.log(`Catálogo AR: ${sitiosAR.length} sitios.`);
}

if (require.main === module) {
  try { process.loadEnvFile(); } catch { /* sin .env: variables del sistema */ }
  const prisma = new PrismaClient();
  prisma.galeria
    .findFirst({ orderBy: { createdAt: 'asc' } })
    .then((galeria) => seedSitiosAR(prisma, galeria?.id))
    .catch((e) => {
      console.error(e);
      process.exit(1);
    })
    .finally(() => prisma.$disconnect());
}
