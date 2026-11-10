import { Injectable, BadRequestException, NotFoundException, UnauthorizedException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { EnviarMensajeDto } from './dto/enviar-mensaje.dto';

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
   * El nombre visible y la marca `esAdmin` se obtienen del usuario autenticado,
   * nunca del cliente, para que nadie pueda hacerse pasar por otro usuario o por el admin.
   *
   * @param usuarioId ID del usuario autenticado (del JWT).
   * @param dto eventoId y texto del mensaje (validado por {@link EnviarMensajeDto}).
   * @returns Registro del mensaje recién guardado en PostgreSQL.
   * @throws UnauthorizedException Si el usuario ya no existe o está inactivo.
   * @throws NotFoundException Si el evento no existe.
   */
  async enviarMensaje(usuarioId: string, dto: EnviarMensajeDto) {
    // 1. El remitente se busca con el ID del token: así el nombre y el rol son los reales (#50)
    const usuario = await this.prisma.usuario.findUnique({ where: { id: usuarioId } });
    if (!usuario || !usuario.activo) throw new UnauthorizedException();

    // 2. El mensaje solo se guarda si el evento existe (antes fallaba la FK con un error 500)

    const evento = await this.prisma.evento.findUnique({ where: { id: dto.eventoId } });
    if (!evento) throw new NotFoundException('Evento no encontrado');

    // 3. Se guarda con datos del servidor: el cliente ya no puede mandar usuarioNombre ni esAdmin
    return this.prisma.chatStream.create({
      data: {
        eventoId: dto.eventoId,
        usuarioNombre: usuario.nombre,
        mensaje: dto.mensaje.trim(),
        esAdmin: usuario.rol === 'ADMINISTRADOR',
      }
    });
  }
}
