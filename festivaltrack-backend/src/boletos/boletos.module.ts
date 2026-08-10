import { Module } from '@nestjs/common';
import { BoletosController } from './boletos.controller';
import { BoletosService } from './boletos.service';
import { PrismaModule } from '../prisma/prisma.module';

@Module({
  imports: [PrismaModule],
  controllers: [BoletosController],
  providers: [BoletosService]
})
export class BoletosModule {}
