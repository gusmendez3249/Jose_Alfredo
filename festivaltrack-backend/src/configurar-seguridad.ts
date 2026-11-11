import { INestApplication, ValidationPipe } from '@nestjs/common';
import helmet from 'helmet';

/**
 * Controles HTTP comunes a toda la API. Se usa en main.ts y en las pruebas de seguridad,
 * así las pruebas verifican exactamente la misma configuración que corre en producción.
 */
export function configurarSeguridad(app: INestApplication) {
  // Cabeceras HTTP de seguridad (X-Content-Type-Options, HSTS, X-Frame-Options, etc.).
  // /uploads se sirve también a otros orígenes porque el portal web muestra esas imágenes.
  app.use(helmet({ crossOriginResourcePolicy: { policy: 'cross-origin' } }));

  // CORS: localhost solo fuera de producción; en producción, únicamente los frontends de FRONTEND_URL.
  // Las apps Android no envían Origin, así que no se ven afectadas.
  const allowedOrigins = (process.env.FRONTEND_URL ?? 'https://festivaltrack-web.up.railway.app')
    .split(',')
    .map((url) => url.trim().replace(/\/$/, ''))
    .filter(Boolean);
  app.enableCors({
    origin: (origin, callback) => {
      const esLocal =
        process.env.NODE_ENV !== 'production' && /^http:\/\/localhost(:\d+)?$/.test(origin ?? '');
      if (!origin || esLocal || allowedOrigins.includes(origin)) {
        return callback(null, true);
      }
      return callback(null, false);
    },
    credentials: true,
  });

  // Validación en el servidor: descarta campos que no estén en el DTO y rechaza tipos inválidos (400).
  app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }));
}
