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

  /**
   * Canciones (demos) del concierto: las ligadas a la transmisión del evento,
   * en su orden. Si el evento aún no tiene repertorio asignado se devuelve el
   * catálogo publicado del tributo, para que siempre haya demos que escuchar.
   */
  async findByEvento(eventoId: string) {
    const ligadas = await this.prisma.cancionTransmision.findMany({
      where: { transmision: { eventoId }, cancion: { estado: 'PUBLICADA' } },
      orderBy: { orden: 'asc' },
      include: { cancion: true },
    });
    if (ligadas.length > 0) return ligadas.map((l) => l.cancion);

    const catalogo = await this.prisma.cancion.findMany({
      where: { estado: 'PUBLICADA' },
      orderBy: { createdAt: 'asc' },
    });
    // El catálogo puede traer la misma canción dada de alta más de una vez.
    const vistas = new Set<string>();
    return catalogo.filter((c) => {
      const clave = `${c.titulo.trim().toLowerCase()}|${c.artista.trim().toLowerCase()}`;
      if (vistas.has(clave)) return false;
      vistas.add(clave);
      return true;
    });
  }

  async create(dto: { titulo: string; artista?: string; duracion?: number; archivoUrl?: string; genero?: string }) {
    let admin = await this.prisma.administrador.findFirst();
    if (!admin) {
      let user = await this.prisma.usuario.findFirst({ where: { rol: 'ADMINISTRADOR' } });
      if (!user) {
        user = await this.prisma.usuario.create({
          data: { nombre: 'Admin', correo: 'admin@admin.com', contrasena: 'admin123', rol: 'ADMINISTRADOR' }
        });
      }
      admin = await this.prisma.administrador.create({ data: { usuarioId: user.id } });
    }

    return this.prisma.cancion.create({
      data: {
        titulo: dto.titulo,
        artista: dto.artista || 'José Alfredo Jiménez',
        duracion: dto.duracion || 180,
        archivoUrl: dto.archivoUrl || 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',
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
