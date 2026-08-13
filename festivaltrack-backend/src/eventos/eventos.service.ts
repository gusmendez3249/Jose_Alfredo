import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { CreateEventoDto } from './dto/create-evento.dto';

/**
 * Servicio de Gestión de Eventos del Festival ([EventosService]).
 *
 * Ofrece operaciones CRUD para la agenda oficial de actuaciones, galas y conferencias.
 * Implementa una estrategia de IDs legibles personalizados (`EVT-001`, `EVT-002`)
 * y borrado suave (*soft-delete*) cambiando el estado a `CANCELADO`.
 */
@Injectable()
export class EventosService {
  constructor(private prisma: PrismaService) {}

  /**
   * Obtiene la lista completa de eventos activos (no cancelados).
   *
   * @returns Lista de eventos ordenados por fecha y hora ascendente, incluyendo la relación con Artista.
   */
  findAll() {
    return this.prisma.evento.findMany({
      where: { estado: { not: 'CANCELADO' } },
      include: { artista: true },
      orderBy: { fechaHora: 'asc' },
    });
  }

  /**
   * Busca un evento específico por su ID único.
   *
   * @param id Identificador del evento (ej. "EVT-001").
   * @returns El evento con la información del Artista y Transmisión en vivo incluida.
   * @throws NotFoundException Si el evento no existe en la base de datos (HTTP 404).
   */
  async findOne(id: string) {
    const evento = await this.prisma.evento.findUnique({
      where: { id },
      include: { artista: true, transmision: true },
    });
    if (!evento) throw new NotFoundException('Evento no encontrado');
    return evento;
  }

  /**
   * Crea un nuevo evento del festival generando un ID secuencial legible (ej. EVT-003).
   *
   * Algoritmo de ID:
   * 1. Consulta el último evento cuyo ID comience con "EVT-".
   * 2. Extrae la parte numérica mediante expresión regular.
   * 3. Incrementa en +1 el valor numérico y aplica padStart de 3 dígitos (ej. 4 -> "004").
   *
   * @param dto Datos del evento a crear (nombre, fechaHora, ubicación, capacidad, etc.).
   * @param adminId ID del administrador autenticado que crea el evento.
   * @returns El registro del evento creado en PostgreSQL.
   */
  async create(dto: CreateEventoDto, usuarioId: string) {
    // Evento.administradorId → Administrador.id (NO Usuario.id).
    // Buscamos el registro Administrador cuyo usuarioId coincida.
    let administrador = await this.prisma.administrador.findUnique({
      where: { usuarioId },
    });

    // Si el usuario tiene rol ADMINISTRADOR pero aún no tiene fila en la tabla,
    // la creamos automáticamente (primera vez que crea un evento).
    if (!administrador) {
      administrador = await this.prisma.administrador.create({
        data: { usuarioId, nivel: 1, permisos: ['CREAR_EVENTOS', 'EDITAR_EVENTOS', 'ELIMINAR_EVENTOS'] },
      });
    }

    const adminId = administrador.id;

    const ultimoEvento = await this.prisma.evento.findFirst({
      where: { id: { startsWith: 'EVT-' } },
      orderBy: { id: 'desc' },
    });

    let nuevoId = 'EVT-001';
    if (ultimoEvento && ultimoEvento.id) {
      const coincidencia = ultimoEvento.id.match(/^EVT-(\d+)$/);
      if (coincidencia) {
        const ultimoNumero = parseInt(coincidencia[1], 10);
        nuevoId = `EVT-${String(ultimoNumero + 1).padStart(3, '0')}`;
      }
    }

    const fechaHoraParsed = dto.fechaHora ? new Date(dto.fechaHora) : new Date('2026-11-23T20:00:00Z');
    const estadoParsed = (dto.estado as any) === 'ACTIVO' ? 'PUBLICADO' : (dto.estado || 'PUBLICADO');

    return this.prisma.evento.create({ 
      data: { 
        nombre: dto.nombre,
        fechaHora: fechaHoraParsed,
        ubicacion: dto.ubicacion,
        escenario: dto.escenario,
        descripcion: dto.descripcion,
        capacidad: dto.capacidad,
        bannerUrl: dto.bannerUrl,
        artistaId: dto.artistaId,
        estado: estadoParsed as any,
        id: nuevoId,
        administradorId: adminId,
      } 
    });
  }

  /**
   * Actualiza la información de un evento existente.
   *
   * @param id ID del evento a actualizar.
   * @param dto Nuevos datos parciales o completos del evento.
   * @returns El evento actualizado.
   */
  async update(id: string, dto: Partial<CreateEventoDto>) {
    await this.findOne(id);
    const dataToUpdate: any = { ...dto };
    if (dto.fechaHora) {
      dataToUpdate.fechaHora = new Date(dto.fechaHora);
    }
    if ((dto.estado as any) === 'ACTIVO') {
      dataToUpdate.estado = 'PUBLICADO';
    }
    return this.prisma.evento.update({ where: { id }, data: dataToUpdate });
  }

  /**
   * Marca un evento como cancelado (*soft-delete*).
   * No elimina físicamente la fila para preservar la integridad referencial con los boletos vendidos.
   *
   * @param id ID del evento a cancelar.
   * @returns El registro del evento actualizado con estado 'CANCELADO'.
   */
  async remove(id: string) {
    await this.findOne(id);
    return this.prisma.evento.update({ where: { id }, data: { estado: 'CANCELADO' } });
  }
}
