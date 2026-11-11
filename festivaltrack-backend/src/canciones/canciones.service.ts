import { BadRequestException, Injectable, NotFoundException, InternalServerErrorException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class CancionesService {
  constructor(private prisma: PrismaService) {}

  async findAll() {
    return this.prisma.cancion.findMany({
      orderBy: { createdAt: 'desc' },
    });
  }

  /**
   * Canciones (demos) del concierto: las ligadas a la transmisión del evento,
   * en el orden definido por el administrador. Si no tiene repertorio, lista vacía.
   */
  async findByEvento(eventoId: string) {
    const ligadas = await this.prisma.cancionTransmision.findMany({
      where: { transmision: { eventoId }, cancion: { estado: 'PUBLICADA' } },
      orderBy: { orden: 'asc' },
      include: { cancion: true },
    });
    return ligadas.map((l) => l.cancion);
  }

  /**
   * Define el repertorio de un concierto (reemplaza el anterior). El orden del
   * arreglo es el orden de reproducción. Crea la transmisión del evento si aún no existe.
   */
  async setRepertorio(eventoId: string, cancionIds: string[]) {
    if (!Array.isArray(cancionIds)) throw new BadRequestException('cancionIds debe ser una lista');
    const ids = [...new Set(cancionIds)];

    const evento = await this.prisma.evento.findUnique({ where: { id: eventoId } });
    if (!evento) throw new NotFoundException('Evento no encontrado');

    const existentes = await this.prisma.cancion.count({ where: { id: { in: ids } } });
    if (existentes !== ids.length) throw new BadRequestException('Alguna canción no existe');

    await this.prisma.$transaction(async (tx) => {
      const transmision = await tx.transmision.upsert({
        where: { eventoId },
        create: { eventoId, titulo: evento.nombre },
        update: {},
      });
      await tx.cancionTransmision.deleteMany({ where: { transmisionId: transmision.id } });
      if (ids.length > 0) {
        await tx.cancionTransmision.createMany({
          data: ids.map((cancionId, orden) => ({ cancionId, transmisionId: transmision.id, orden })),
        });
      }
    });
    return this.findByEvento(eventoId);
  }

  async create(dto: { titulo: string; artista?: string; duracion?: number; archivoUrl?: string; portadaUrl?: string; genero?: string }) {
    let admin = await this.prisma.administrador.findFirst();
    if (!admin) {
      let user = await this.prisma.usuario.findFirst({ where: { rol: 'ADMINISTRADOR' } });
      // Nunca crear un admin con contraseña fija: el administrador se crea con el seed (SEED_ADMIN_PASSWORD).
      if (!user) throw new InternalServerErrorException('No hay administrador registrado. Ejecuta el seed.');
      admin = await this.prisma.administrador.create({ data: { usuarioId: user.id } });
    }

    return this.prisma.cancion.create({
      data: {
        titulo: dto.titulo,
        artista: dto.artista || 'José Alfredo Jiménez',
        duracion: dto.duracion || 180,
        archivoUrl: dto.archivoUrl || 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',
        portadaUrl: dto.portadaUrl || null,
        genero: dto.genero || 'Ranchera',
        estado: 'PUBLICADA',
        administradorId: admin.id,
      }
    });
  }

  async remove(id: string) {
    return this.prisma.cancion.delete({ where: { id } });
  }
}
