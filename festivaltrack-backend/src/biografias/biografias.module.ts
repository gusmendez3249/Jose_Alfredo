import { Module } from '@nestjs/common';
import { BiografiasController } from './biografias.controller';
import { BiografiasService } from './biografias.service';
import { PrismaModule } from '../prisma/prisma.module';

@Module({
  imports: [PrismaModule],
  controllers: [BiografiasController],
  providers: [BiografiasService]
})
export class BiografiasModule {}
