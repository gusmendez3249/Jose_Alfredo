import { Module } from '@nestjs/common';
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

@Module({
  imports: [PrismaModule, AuthModule, EventosModule, SyncModule, BoletosModule, GaleriaModule, ChatModule, BiografiasModule, CancionesModule, StreamModule],
})
export class AppModule {}
