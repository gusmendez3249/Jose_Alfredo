import { Module } from '@nestjs/common';
import { PrismaModule } from '../prisma/prisma.module';
import { ForoController } from './foro.controller';
import { ForoService } from './foro.service';

@Module({
  imports: [PrismaModule],
  controllers: [ForoController],
  providers: [ForoService],
})
export class ForoModule {}
