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

  async create(dto: CreateEventoDto, adminId: string) {
    // Obtenemos el último evento cuyo ID empiece con EVT- (ordenado alfabéticamente descendente)
    const ultimoEvento = await this.prisma.evento.findFirst({
      where: { id: { startsWith: 'EVT-' } },
      orderBy: { id: 'desc' },
    });

    let nuevoId = 'EVT-001';
    if (ultimoEvento && ultimoEvento.id) {
      // Extraemos el número del último ID (ej. de "EVT-007" extraemos "7")
      const coincidencia = ultimoEvento.id.match(/^EVT-(\d+)$/);
      if (coincidencia) {
        const ultimoNumero = parseInt(coincidencia[1], 10);
        // Le sumamos 1 y rellenamos con ceros a la izquierda (ej. 8 -> "008")
        nuevoId = `EVT-${String(ultimoNumero + 1).padStart(3, '0')}`;
      }
    }

    // Insertamos forzando nuestro ID personalizado en vez de que Prisma genere uno aleatorio
    return this.prisma.evento.create({ 
      data: { 
        ...dto, 
        id: nuevoId,
        administradorId: adminId 
      } 
    });
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
