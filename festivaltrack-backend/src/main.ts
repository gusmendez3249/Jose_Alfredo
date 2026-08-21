import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  // CORS: permite localhost en desarrollo y dominios .railway.app en producción
  app.enableCors({
    origin: (origin, callback) => {
      if (!origin) return callback(null, true);
      if (!origin || origin.endsWith('.railway.app') || origin.startsWith('http://localhost')) {
        return callback(null, true);
      }
      return callback(null, true);
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

