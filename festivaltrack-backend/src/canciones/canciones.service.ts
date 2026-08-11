import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class CancionesService {
  constructor(private prisma: PrismaService) {}

  async findAll() {
    return this.prisma.cancion.findMany({
      orderBy: { createdAt: 'desc' },
    });
  }
}
