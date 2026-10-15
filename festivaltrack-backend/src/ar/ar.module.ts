import { Module } from '@nestjs/common';
import { PrismaModule } from '../prisma/prisma.module';
import { ArController } from './ar.controller';
import { ArCatalogoService } from './ar-catalogo.service';
import { ArReconocimientoFacade } from './ar-reconocimiento.facade';
import { RECONOCEDOR_VISUAL } from './vision/reconocedor-visual';
import { ClaudeReconocedor } from './vision/claude-reconocedor';

@Module({
  imports: [PrismaModule],
  controllers: [ArController],
  providers: [
    ArCatalogoService,
    ArReconocimientoFacade,
    { provide: RECONOCEDOR_VISUAL, useClass: ClaudeReconocedor },
  ],
})
export class ArModule {}
