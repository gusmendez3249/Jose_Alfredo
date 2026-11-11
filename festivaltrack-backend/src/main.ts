import './load-env'; // carga .env antes de importar cualquier módulo
import { NestFactory } from '@nestjs/core';
import { NestExpressApplication } from '@nestjs/platform-express';
import { ValidationPipe } from '@nestjs/common';
import { join } from 'path';
import helmet from 'helmet';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create<NestExpressApplication>(AppModule);
  // Detrás del proxy de Railway: usar la IP real del cliente (X-Forwarded-For) para el rate limiting.
  app.set('trust proxy', 1);

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

  app.useGlobalPipes(new ValidationPipe({ whitelist: true, transform: true }));

  // Fotos subidas (foro de la galería) accesibles en /uploads/<archivo>
  app.useStaticAssets(join(process.cwd(), 'uploads'), { prefix: '/uploads/' });
  app.setGlobalPrefix('api/v1');

  // Railway inyecta PORT automáticamente; en local se usa 3001
  const port = process.env.PORT ?? 3001;
  await app.listen(port, '0.0.0.0');
  console.log(`FestivalTrack API corriendo en puerto ${port}`);
}
bootstrap();

