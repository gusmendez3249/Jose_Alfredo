import { Injectable, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

/**
 * Servicio de Gestión del Chat de Transmisión en Vivo (ChatService).
 *
 * Encargado de persistir y consultar los mensajes enviados por los espectadores
 * y administradores durante las transmisiones en directo del festival.
 */
@Injectable()
export class ChatService {
  constructor(private prisma: PrismaService) {}

  /**
   * Obtiene los últimos mensajes del chat asociados a un evento en vivo.
   *
   * @param eventoId Identificador único del evento (ej. "EVT-001").
   * @returns Lista de mensajes ordenados cronológicamente descendente (máximo 50).
   * @throws BadRequestException Si no se proporciona un ID de evento válido.
   */
  async getMensajes(eventoId: string) {
    if (!eventoId) throw new BadRequestException('El ID del evento es requerido');
    
    return this.prisma.chatStream.findMany({
      where: { eventoId },
      orderBy: { fechaEnvio: 'desc' },
      take: 50
    });
  }

  /**
   * Registra y guarda un nuevo mensaje en el chat de un evento.
   *
   * @param body Payload del mensaje que contiene eventoId, usuarioNombre, mensaje y opcionalmente esAdmin.
   * @returns Registro del mensaje recién guardado en PostgreSQL.
   * @throws BadRequestException Si faltan campos requeridos o el eventoId no existe en la BD.
   */
  async enviarMensaje(body: any) {
    const { eventoId, usuarioNombre, mensaje, esAdmin } = body;
    
    if (!eventoId || !usuarioNombre || !mensaje) {
      throw new BadRequestException('Faltan parámetros requeridos');
    }

    return this.prisma.chatStream.create({
      data: {
        eventoId,
        usuarioNombre,
        mensaje,
        esAdmin: esAdmin || false
      }
    });
  }
}
