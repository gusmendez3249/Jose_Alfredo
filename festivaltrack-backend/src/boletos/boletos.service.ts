import { Injectable, NotFoundException, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class BoletosService {
  constructor(private prisma: PrismaService) {}

  async comprar(usuarioId: string, body: any) {
    const { eventoId, categoria, cantidad, precioTotal } = body;
    
    // Crear boleto base
    const boleto = await this.prisma.boleto.create({
      data: {
        eventoId,
        usuarioId,
        categoria: categoria || 'GENERAL',
        precio: precioTotal,
        // El codigoQR por default es un CUID, lo generamos manualmente si es necesario o usamos el autogenerado
      },
    });

    // Actualizar el QR con el formato FESTIVAL-TICKET-2024::ID
    const qrFirma = `FESTIVAL-TICKET-2024::${boleto.id}`;
    
    const boletoFinal = await this.prisma.boleto.update({
      where: { id: boleto.id },
      data: { codigoQR: qrFirma },
    });

    return boletoFinal;
  }

  async misBoletos(usuarioId: string) {
    return this.prisma.boleto.findMany({
      where: { usuarioId },
      include: { evento: true },
      orderBy: { createdAt: 'desc' },
    });
  }

  async validarQr(qr: string) {
    const boleto = await this.prisma.boleto.findUnique({
      where: { codigoQR: qr },
      include: { evento: true, usuario: true }
    });

    if (!boleto) {
      throw new NotFoundException('Boleto no encontrado');
    }

    if (boleto.estado === 'USADO') {
      throw new BadRequestException('El boleto ya fue usado');
    }

    if (boleto.estado !== 'ACTIVO') {
      throw new BadRequestException('El boleto no es válido (estado: ' + boleto.estado + ')');
    }

    await this.prisma.boleto.update({
      where: { id: boleto.id },
      data: { estado: 'USADO' }
    });

    return { status: "APPROVED", message: "Acceso Concedido", evento: boleto.evento.nombre, usuario: boleto.usuario.nombre };
  }
}
