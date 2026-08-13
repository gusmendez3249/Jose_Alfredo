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
}
