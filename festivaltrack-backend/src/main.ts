import './load-env'; // carga .env antes de importar cualquier módulo
import { NestFactory } from '@nestjs/core';
import { NestExpressApplication } from '@nestjs/platform-express';
import { ValidationPipe } from '@nestjs/common';
import { join } from 'path';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create<NestExpressApplication>(AppModule);

  // CORS: permite localhost en desarrollo y el/los frontends de FRONTEND_URL (separados por coma).
  // Las apps Android no envían Origin, así que no se ven afectadas.
  const allowedOrigins = (process.env.FRONTEND_URL ?? 'https://festivaltrack-web.up.railway.app')
    .split(',')
    .map((url) => url.trim().replace(/\/$/, ''))
    .filter(Boolean);
  app.enableCors({
    origin: (origin, callback) => {
      if (!origin || origin.startsWith('http://localhost') || allowedOrigins.includes(origin)) {
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

