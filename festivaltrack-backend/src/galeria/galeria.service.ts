import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class GaleriaService {
  constructor(private prisma: PrismaService) {}

  async getGaleria() {
    // Retornamos todas las galerías junto con sus imágenes
    return this.prisma.galeria.findMany({
      include: {
        imagenes: {
          orderBy: { createdAt: 'desc' }
        }
      },
      orderBy: { createdAt: 'desc' }
    });
  }

  async addImagen(dto: { url?: string; titulo?: string }) {
    let galeria = await this.prisma.galeria.findFirst();
    if (!galeria) {
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
      galeria = await this.prisma.galeria.create({
        data: { nombre: 'Golden Era', categoria: 'GOLDEN_ERA', administradorId: admin.id }
      });
    }

    return this.prisma.imagen.create({
      data: {
        galeriaId: galeria.id,
        url: dto.url || 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=800&q=80',
        titulo: dto.titulo || 'Nueva Foto del Festival',
      }
    });
  }

  async removeImagen(id: string) {
    return this.prisma.imagen.delete({ where: { id } });
  }
}
