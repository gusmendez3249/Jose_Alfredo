import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class GaleriaService {
  constructor(private prisma: PrismaService) {}

  async getGaleria() {
    // Retornamos todas las galerías junto con sus imágenes
    return this.prisma.galeria.findMany({
      include: { imagenes: true },
      orderBy: { createdAt: 'desc' }
    });
  }
}
