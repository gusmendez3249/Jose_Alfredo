import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { CreateEventoDto } from './dto/create-evento.dto';

@Injectable()
export class EventosService {
  constructor(private prisma: PrismaService) {}

  findAll() {
    return this.prisma.evento.findMany({
      where: { estado: { not: 'CANCELADO' } },
      include: { artista: true },
      orderBy: { fechaHora: 'asc' },
    });
  }

  async findOne(id: string) {
    const evento = await this.prisma.evento.findUnique({
      where: { id },
      include: { artista: true, transmision: true },
    });
    if (!evento) throw new NotFoundException('Evento no encontrado');
    return evento;
  }

  create(dto: CreateEventoDto, adminId: string) {
    return this.prisma.evento.create({ data: { ...dto, administradorId: adminId } });
  }

  async update(id: string, dto: Partial<CreateEventoDto>) {
    await this.findOne(id);
    return this.prisma.evento.update({ where: { id }, data: dto });
  }

  async remove(id: string) {
    await this.findOne(id);
    return this.prisma.evento.update({ where: { id }, data: { estado: 'CANCELADO' } });
  }
}
