import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { SitioArDto } from './dto/sitio-ar.dto';

/** Catálogo de sitios AR administrado por el equipo organizador. */
@Injectable()
export class ArCatalogoService {
  constructor(private prisma: PrismaService) {}

  activos() {
    return this.prisma.sitioAR.findMany({ where: { activo: true }, orderBy: { nombre: 'asc' } });
  }

  async porId(id: string) {
    const sitio = await this.prisma.sitioAR.findUnique({ where: { id } });
    if (!sitio) throw new NotFoundException(`Sitio ${id} no existe`);
    return sitio;
  }

  crear(dto: SitioArDto) {
    return this.prisma.sitioAR.create({ data: dto });
  }

  async actualizar(id: string, dto: Partial<SitioArDto>) {
    await this.porId(id);
    return this.prisma.sitioAR.update({ where: { id }, data: dto });
  }

  async eliminar(id: string) {
    await this.porId(id);
    return this.prisma.sitioAR.delete({ where: { id } });
  }
}
