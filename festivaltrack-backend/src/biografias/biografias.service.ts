import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { BiografiaAltaDto, BiografiaFactoryProducer } from './factories/biografia.factory';

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

  /**
   * Da de alta una nueva biografía o hito interactivo implementando el FACTORY PATTERN.
   */
  async altaConFactory(dto: BiografiaAltaDto) {
    // 1. Invocar la Factory
    const generada = BiografiaFactoryProducer.crear(dto);

    // 2. Buscar o crear el Artista oficial en base de datos
    let artista = await this.prisma.artista.findFirst({
      where: { nombre: { contains: 'José Alfredo' } },
    });

    if (!artista) {
      artista = await this.prisma.artista.create({
        data: {
          nombre: dto.artistaNombre || 'José Alfredo Jiménez',
          imagenUrl: dto.imagenUrl || 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4',
          activo: true,
        },
      });
    }

    // 3. Crear o actualizar la Biografía asociada
    const biografiaExistente = await this.prisma.biografia.findUnique({
      where: { artistaId: artista.id },
    });

    if (biografiaExistente) {
      const hitosActuales = Array.isArray(biografiaExistente.hitos) ? (biografiaExistente.hitos as any[]) : [];
      const nuevosHitos = [...hitosActuales, ...generada.hitos];

      return this.prisma.biografia.update({
        where: { id: biografiaExistente.id },
        data: {
          descripcion: dto.descripcion || biografiaExistente.descripcion,
          citaCelebre: dto.citaCelebre || biografiaExistente.citaCelebre,
          hitos: nuevosHitos,
        },
        include: { artista: true },
      });
    } else {
      return this.prisma.biografia.create({
        data: {
          artistaId: artista.id,
          descripcion: generada.descripcion,
          citaCelebre: generada.citaCelebre,
          hitos: generada.hitos,
          discografia: generada.discografia,
        },
        include: { artista: true },
      });
    }
  }
}
