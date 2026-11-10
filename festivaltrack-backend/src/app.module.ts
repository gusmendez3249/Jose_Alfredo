import { Module } from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { ThrottlerModule } from '@nestjs/throttler';
import { AppThrottlerGuard } from './common/guards/app-throttler.guard';
import { PrismaModule } from './prisma/prisma.module';
import { AuthModule } from './auth/auth.module';
import { EventosModule } from './eventos/eventos.module';
import { SyncModule } from './sync/sync.module';
import { BoletosModule } from './boletos/boletos.module';
import { GaleriaModule } from './galeria/galeria.module';
import { ChatModule } from './chat/chat.module';
import { BiografiasModule } from './biografias/biografias.module';
import { CancionesModule } from './canciones/canciones.module';
import { StreamModule } from './stream/stream.module';
import { ArModule } from './ar/ar.module';
import { ForoModule } from './foro/foro.module';

@Module({
  imports: [
    // Límite global: 300 peticiones por minuto por IP (las apps hacen polling del chat y del stream).
    // Los endpoints sensibles bajan este límite con @Throttle.
    ThrottlerModule.forRoot([{ ttl: 60_000, limit: 300 }]),
    PrismaModule, AuthModule, EventosModule, SyncModule, BoletosModule, GaleriaModule, ChatModule, BiografiasModule, CancionesModule, StreamModule, ArModule, ForoModule],
  // Rate limiting (#51): el guard se registra como APP_GUARD para que aplique a TODAS las rutas
  // sin tener que decorar cada controlador; responde 429 Too Many Requests al pasarse del límite.
  providers: [{ provide: APP_GUARD, useClass: AppThrottlerGuard }],
})
export class AppModule {}
