import { StreamStatusBodyDto } from './dto/stream-status.dto';
import { Controller, Get, Post, Body, UseGuards } from '@nestjs/common';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { RolesGuard } from '../common/guards/roles.guard';
import { Roles } from '../common/decorators/roles.decorator';

/**
 * Controlador de Estado de Transmisión en Vivo ([StreamStatusController]).
 *
 * Mantiene en memoria la URL RTSP activa del stream del festival.
 * La app móvil del admin registra la URL al iniciar el live,
 * y la Smart TV la consulta cada pocos segundos para conectarse automáticamente.
 *
 * Endpoint: GET  /api/v1/stream/status  → { streamUrl, emulatorUrl, isLive, port, eventoId, eventoNombre }
 * Endpoint: POST /api/v1/stream/status  → registra { streamUrl, isLive, eventoId, eventoNombre }
 *
 * Nota de red:
 * El emulador usa 10.0.2.2 para acceder al host (PC). Para que el stream RTSP del
 * celular físico llegue al emulador, el PC necesita hacer port forwarding:
 *   netsh interface portproxy add v4tov4 listenport=1935 listenaddress=0.0.0.0
 *     connectport=1935 connectaddress=<IP_CELULAR>
 */
@Controller('stream/status')
export class StreamStatusController {
  /**
   * Si el admin deja de reportar durante este tiempo (cerró la app, perdió señal),
   * la transmisión se da por terminada. La app del admin reporta cada 15 s mientras transmite.
   */
  static readonly SIN_SENAL_MS = 45_000;

  private streamUrl = '';
  private isLive = false;
  private eventoId: string | null = null;
  private eventoNombre: string | null = null;
  private ultimoReporte = 0;

  /** Transmisión activa: marcada en vivo y con reporte reciente del admin. */
  private enVivo(ahora = Date.now()): boolean {
    return (
      this.isLive &&
      ahora - this.ultimoReporte < StreamStatusController.SIN_SENAL_MS
    );
  }

  /**
   * Retorna el estado actual de la transmisión.
   * emulatorUrl usa 10.0.2.2 para que el emulador acceda vía el host PC (con port forward).
   */
  @Get()
  getStatus() {
    const vivo = this.enVivo();
    let port = 1935;
    let emulatorUrl = '';
    if (vivo && this.streamUrl) {
      const match = this.streamUrl.match(/:(\d+)/);
      if (match) port = parseInt(match[1]);
      emulatorUrl = `rtsp://10.0.2.2:${port}`;
    }
    return {
      streamUrl: vivo ? this.streamUrl : '',
      emulatorUrl,
      isLive: vivo,
      port,
      eventoId: vivo ? this.eventoId : null,
      eventoNombre: vivo ? this.eventoNombre : null,
    };
  }

  /**
   * El admin móvil registra la URL del stream al iniciar, la reporta periódicamente
   * mientras transmite y la limpia al detener.
   */
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('ADMINISTRADOR')
  @Post()
  setStatus(@Body() body: StreamStatusBodyDto) {
    this.streamUrl = body.streamUrl || '';
    this.isLive = body.isLive !== false && this.streamUrl !== '';
    this.eventoId = this.isLive ? (body.eventoId ?? null) : null;
    this.eventoNombre = this.isLive ? (body.eventoNombre ?? null) : null;
    this.ultimoReporte = Date.now();
    return { ok: true, ...this.getStatus() };
  }
}
