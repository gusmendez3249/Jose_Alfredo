import './load-env'; // carga .env antes de importar cualquier módulo
import { NestFactory } from '@nestjs/core';
import { NestExpressApplication } from '@nestjs/platform-express';
import { join } from 'path';
import { AppModule } from './app.module';
import { configurarSeguridad } from './configurar-seguridad';

async function bootstrap() {
  const app = await NestFactory.create<NestExpressApplication>(AppModule);
  // Detrás del proxy de Railway: usar la IP real del cliente (X-Forwarded-For) para el rate limiting.
  app.set('trust proxy', 1);

  // helmet, CORS y validación de entradas (ver configurar-seguridad.ts).
  configurarSeguridad(app);

  // Fotos subidas (foro de la galería) accesibles en /uploads/<archivo>
  app.useStaticAssets(join(process.cwd(), 'uploads'), { prefix: '/uploads/' });
  app.setGlobalPrefix('api/v1');

  // Railway inyecta PORT automáticamente; en local se usa 3001
  const port = process.env.PORT ?? 3001;
  await app.listen(port, '0.0.0.0');
  console.log(`FestivalTrack API corriendo en puerto ${port}`);
}
bootstrap();

