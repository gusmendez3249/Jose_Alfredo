import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class BiografiasService {
  constructor(private prisma: PrismaService) {}

  async findAll() {
    return this.prisma.biografia.findMany({
      include: {
        artista: true,
      },
    });
  }

  async findOne(id: string) {
    return this.prisma.biografia.findUnique({
      where: { id },
      include: {
        artista: true,
      },
    });
  }
}
