import { Injectable, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class ChatService {
  constructor(private prisma: PrismaService) {}

  async getMensajes(eventoId: string) {
    if (!eventoId) throw new BadRequestException('El ID del evento es requerido');
    
    return this.prisma.chatStream.findMany({
      where: { eventoId },
      orderBy: { fechaEnvio: 'desc' },
      take: 50 // Traemos los ultimos 50 mensajes por defecto
    });
  }

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
