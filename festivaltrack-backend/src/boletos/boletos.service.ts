import { Injectable, NotFoundException, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class BoletosService {
  constructor(private prisma: PrismaService) {}

  async comprar(usuarioId: string, body: any) {
    const { eventoId, categoria, cantidad, precioTotal, metodoPago } = body;
    
    // Validar cantidad (por ahora procesaremos 1 a 1, o iterar si cantidad > 1)
    // Para simplificar, asumimos que crearán un boleto por la cantidad enviada
    // En este caso, para cumplir la regla "1 Pago por cada Boleto" crearemos 1 boleto con su respectivo pago
    
    const resultado = await this.prisma.$transaction(async (tx) => {
      // 1. Crear boleto base
      const boleto = await tx.boleto.create({
        data: {
          eventoId,
          usuarioId,
          categoria: categoria || 'GENERAL',
          precio: precioTotal,
        },
      });

      // 2. Crear Pago asociado al boleto
      await tx.pago.create({
        data: {
          boletoId: boleto.id,
          monto: precioTotal,
          metodo: metodoPago || 'TARJETA_CREDITO',
          estado: 'COMPLETADO',
          referencia: `REF-${Date.now()}-${Math.floor(Math.random() * 1000)}`
        }
      });

      // 3. Actualizar el QR con el formato FESTIVAL-TICKET-2024::ID
      const qrFirma = `FESTIVAL-TICKET-2024::${boleto.id}`;
      
      const boletoFinal = await tx.boleto.update({
        where: { id: boleto.id },
        data: { codigoQR: qrFirma },
        include: { pago: true } // Incluir el pago en la respuesta
      });
      
      return boletoFinal;
    });

    return resultado;
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
