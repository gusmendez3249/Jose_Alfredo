import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class SyncService {
  constructor(private prisma: PrismaService) {}

  async getWearPayload(since?: string) {
    const fechaDesde = since ? new Date(since) : new Date(0);

    const [eventos, artistas] = await Promise.all([
      this.prisma.evento.findMany({
        where: { estado: { not: 'CANCELADO' }, updatedAt: { gte: fechaDesde } },
        select: {
          id: true, nombre: true, fechaHora: true, ubicacion: true,
          escenario: true, bannerUrl: true, estado: true, updatedAt: true,
          artista: { select: { id: true, nombre: true, imagenUrl: true } },
        },
        orderBy: { fechaHora: 'asc' },
      }),
      this.prisma.artista.findMany({
        where: { activo: true, updatedAt: { gte: fechaDesde } },
        select: { id: true, nombre: true, imagenUrl: true, updatedAt: true },
      }),
    ]);

    await this.prisma.syncMeta.upsert({
      where: { dispositivo_entidad: { dispositivo: 'wear', entidad: 'all' } },
      create: { dispositivo: 'wear', entidad: 'all', lastSyncAt: new Date() },
      update: { lastSyncAt: new Date() },
    });

    return {
      eventos,
      artistas,
      generadoEn: new Date().toISOString(),
      total: { eventos: eventos.length, artistas: artistas.length },
    };
  }
}
