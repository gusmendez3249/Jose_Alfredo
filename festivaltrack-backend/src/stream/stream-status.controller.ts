import { Controller, Get, Post, Body } from '@nestjs/common';

/**
 * Controlador de Estado de Transmisión en Vivo ([StreamStatusController]).
 *
 * Mantiene en memoria la URL RTSP activa del stream del festival.
 * La app móvil del admin registra la URL al iniciar el live,
 * y la Smart TV la consulta cada pocos segundos para conectarse automáticamente.
 *
 * Endpoint: GET  /api/v1/stream/status  → { streamUrl, emulatorUrl, isLive, port }
 * Endpoint: POST /api/v1/stream/status  → registra { streamUrl, isLive }
 *
 * Nota de red:
 * El emulador usa 10.0.2.2 para acceder al host (PC). Para que el stream RTSP del
 * celular físico llegue al emulador, el PC necesita hacer port forwarding:
 *   netsh interface portproxy add v4tov4 listenport=1935 listenaddress=0.0.0.0
 *     connectport=1935 connectaddress=<IP_CELULAR>
 */
@Controller('stream/status')
export class StreamStatusController {
  private streamUrl: string = '';
  private isLive: boolean = false;

  /**
   * Retorna el estado actual de la transmisión.
   * emulatorUrl usa 10.0.2.2 para que el emulador acceda vía el host PC (con port forward).
   */
  @Get()
  getStatus() {
    let port = 1935;
    let emulatorUrl = '';
    if (this.streamUrl) {
      const match = this.streamUrl.match(/:(\d+)/);
      if (match) port = parseInt(match[1]);
      emulatorUrl = `rtsp://10.0.2.2:${port}`;
    }
    return {
      streamUrl: this.streamUrl,
      emulatorUrl,
      isLive: this.isLive,
      port,
    };
  }

  /**
   * El admin móvil registra la URL del stream al iniciar o detener la transmisión.
   */
  @Post()
  setStatus(@Body() body: { streamUrl: string; isLive: boolean }) {
    this.streamUrl = body.streamUrl || '';
    this.isLive = body.isLive !== false;
    return { ok: true, streamUrl: this.streamUrl, isLive: this.isLive };
  }
}
