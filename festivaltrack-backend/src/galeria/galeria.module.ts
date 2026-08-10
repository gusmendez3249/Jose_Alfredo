import { Module } from '@nestjs/common';
import { GaleriaController, UploadController } from './galeria.controller';
import { GaleriaService } from './galeria.service';
import { PrismaModule } from '../prisma/prisma.module';

@Module({
  imports: [PrismaModule],
  controllers: [GaleriaController, UploadController],
  providers: [GaleriaService]
})
export class GaleriaModule {}
