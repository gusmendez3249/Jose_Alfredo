import { Injectable, NotFoundException, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { ComprarBoletoDto, PRECIOS_BOLETO } from './dto/comprar-boleto.dto';

/**
 * Servicio de Gestión y Venta de Boletos ([BoletosService]).
 *
 * Administra el ciclo de vida completo de la boletaje del festival:
 * 1. Compras transaccionales de entradas con generación de pagos (precio calculado en el servidor).
 * 2. Asignación de firma de código QR único (`FESTIVAL-TICKET-2024::<ID>`).
 * 3. Consulta de historial de accesos comprados por usuario.
 * 4. Escaneo y validación de QR en el acceso presencial con prevención de reutilización.
 */
@Injectable()
export class BoletosService {
  constructor(private prisma: PrismaService) {}

  /**
   * Procesa la compra de un boleto dentro de una transacción atómica ($transaction).
   *
   * Flujo:
   * 1. Verifica que el evento exista y esté a la venta (PUBLICADO o EN_CURSO).
   * 2. Calcula el monto en el servidor: precio oficial de la categoría × cantidad
   *    (cualquier precio que mande el cliente se ignora).
   * 3. Crea el registro de `Boleto` y su `Pago` con estado COMPLETADO.
   * 4. Genera la firma digital del código QR asociada al boleto.
   *
   * @param usuarioId UUID del usuario autenticado que realiza la compra.
   * @param dto eventoId, categoría, cantidad y método de pago (validados por {@link ComprarBoletoDto}).
   * @returns El registro final del boleto con el pago incluido.
   * @throws NotFoundException Si el evento no existe (HTTP 404).
   * @throws BadRequestException Si el evento no está a la venta (HTTP 400).
   */
  async comprar(usuarioId: string, dto: ComprarBoletoDto) {
    const categoria = dto.categoria ?? 'GENERAL';
    const cantidad = dto.cantidad ?? 1;
    const metodoPago = dto.metodoPago ?? 'TARJETA_CREDITO';
    const montoTotal = PRECIOS_BOLETO[categoria] * cantidad;

    const evento = await this.prisma.evento.findUnique({ where: { id: dto.eventoId } });
    if (!evento) throw new NotFoundException('Evento no encontrado');
    if (evento.estado !== 'PUBLICADO' && evento.estado !== 'EN_CURSO') {
      throw new BadRequestException(`El evento no está a la venta (estado: ${evento.estado})`);
    }
    const realEventoId = evento.id;

    // Ejecuta las operaciones en una sola transacción SQL
    const resultado = await this.prisma.$transaction(async (tx) => {
      // 1. Crear boleto base
      const boleto = await tx.boleto.create({
        data: {
          eventoId: realEventoId,
          usuarioId,
          categoria,
          precio: montoTotal,
        },
      });

      // 2. Crear Registro de Pago asociado al boleto
      await tx.pago.create({
        data: {
          boletoId: boleto.id,
          monto: montoTotal,
          metodo: metodoPago,
          estado: 'COMPLETADO',
          referencia: `REF-${Date.now()}-${Math.floor(Math.random() * 1000)}`
        }
      });

      // 3. Generar la firma única del código QR
      const qrFirma = `FESTIVAL-TICKET-2024::${boleto.id}`;
      
      const boletoFinal = await tx.boleto.update({
        where: { id: boleto.id },
        data: { codigoQR: qrFirma },
        include: { pago: true }
      });
      
      return boletoFinal;
    });

    return resultado;
  }

  /**
   * Obtiene la lista de boletos comprados por un usuario específico.
   *
   * @param usuarioId UUID del usuario autenticado.
   * @returns Lista de entradas ordenadas de más reciente a más antigua con información del evento.
   */
  async misBoletos(usuarioId: string) {
    return this.prisma.boleto.findMany({
      where: { usuarioId },
      include: { evento: true },
      orderBy: { createdAt: 'desc' },
    });
  }

  /**
   * Valida un código QR presentado en los accesos del festival (usado por la app Admin Scanner).
   *
   * Reglas de validación:
   * 1. El código QR debe existir en la base de datos.
   * 2. El boleto no debe haber sido marcado previamente como 'USADO'.
   * 3. El estado del boleto debe ser 'ACTIVO'.
   *
   * Al pasar la validación, el boleto cambia automáticamente a estado 'USADO'.
   *
   * @param qr Firma de código QR leída por la cámara del escáner.
   * @returns Objeto con resultado de aprobación, nombre del evento y nombre del usuario.
   * @throws NotFoundException Si el boleto no existe (HTTP 404).
   * @throws BadRequestException Si el boleto ya fue usado o está cancelado (HTTP 400).
   */
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

    // Actualiza a estado USADO para evitar doble ingreso
    await this.prisma.boleto.update({
      where: { id: boleto.id },
      data: { estado: 'USADO' }
    });

    return { status: "APPROVED", message: "Acceso Concedido", evento: boleto.evento.nombre, usuario: boleto.usuario.nombre };
  }
}
