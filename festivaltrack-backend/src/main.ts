import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

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
  app.setGlobalPrefix('api/v1');

  // Railway inyecta PORT automáticamente; en local se usa 3001
  const port = process.env.PORT ?? 3001;
  await app.listen(port, '0.0.0.0');
  console.log(`FestivalTrack API corriendo en puerto ${port}`);
}
bootstrap();

