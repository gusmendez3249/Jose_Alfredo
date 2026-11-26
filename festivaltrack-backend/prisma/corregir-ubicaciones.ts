import { PrismaClient } from '@prisma/client';
import { UBICACION_POR_ESCENARIO, UBICACION_POR_SITIO } from './ubicaciones';

/**
 * Corrige en la base las coordenadas de los sitios AR y de los eventos con las
 * ubicaciones oficiales de `ubicaciones.ts` (OpenStreetMap).
 * Se puede correr varias veces:
 *   npm run seed:ubicaciones
 */
const prisma = new PrismaClient();

async function main() {
  for (const [id, coords] of Object.entries(UBICACION_POR_SITIO)) {
    const r = await prisma.sitioAR.updateMany({ where: { id }, data: coords });
    console.log(`Sitio ${id}: ${r.count ? 'corregido' : 'no existe'}`);
  }
  for (const [escenario, coords] of Object.entries(UBICACION_POR_ESCENARIO)) {
    const r = await prisma.evento.updateMany({
      where: { escenario },
      data: coords,
    });
    console.log(`Eventos en "${escenario}": ${r.count} corregidos`);
  }
}

main()
  .catch((e) => {
    console.error(e);
    process.exitCode = 1;
  })
  .finally(() => prisma.$disconnect());
