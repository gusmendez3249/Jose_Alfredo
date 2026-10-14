import { Injectable, Logger, ServiceUnavailableException } from '@nestjs/common';
import Anthropic from '@anthropic-ai/sdk';
import { zodOutputFormat } from '@anthropic-ai/sdk/helpers/zod';
import { z } from 'zod';
import { MediaTypeImagen, ReconocedorVisual, ResultadoVisual, SitioCandidato } from './reconocedor-visual';

const RespuestaSchema = z.object({
  sitioId: z.string().nullable(),
  confianza: z.number(),
  razon: z.string(),
});

const SYSTEM = `Eres el motor de reconocimiento visual del festival FestivalTrack en Dolores Hidalgo, Guanajuato.
Recibes una foto tomada por un visitante y un catálogo de sitios con su descripción visual.
Decide cuál sitio del catálogo aparece en la foto y responde con su "id" exacto.
Si la foto no muestra claramente ninguno de los sitios, responde sitioId = null.
"confianza" va de 0 a 1. "razon" es una frase corta en español con los rasgos que viste.`;

/**
 * Implementación de ReconocedorVisual con Claude (visión + salida estructurada).
 *
 * La API key se lee de la variable de entorno ANTHROPIC_API_KEY del backend;
 * nunca viaja a la app móvil.
 */
@Injectable()
export class ClaudeReconocedor implements ReconocedorVisual {
  private readonly logger = new Logger(ClaudeReconocedor.name);
  // Las keys que no están ligadas a un workspace exigen el header anthropic-workspace-id.
  private readonly client = new Anthropic(
    process.env.ANTHROPIC_WORKSPACE_ID
      ? { defaultHeaders: { 'anthropic-workspace-id': process.env.ANTHROPIC_WORKSPACE_ID } }
      : {},
  );
  private readonly model = process.env.ANTHROPIC_MODEL || 'claude-opus-5-5';

  async identificar(
    imagen: Buffer,
    mediaType: MediaTypeImagen,
    candidatos: SitioCandidato[],
  ): Promise<ResultadoVisual> {
    const catalogo = candidatos
      .map((c) => `- id: ${c.id}\n  nombre: ${c.nombre}\n  cómo se ve: ${c.pistasVisuales}`)
      .join('\n');

    let response;
    try {
      response = await this.client.beta.messages.parse({
        model: this.model,
        max_tokens: 1024,
        // Si el modelo declina por política, la API reintenta con otro modelo en la misma llamada.
        betas: ['server-side-fallback-2026-07-01'],
        fallbacks: 'default',
        output_config: { effort: 'low', format: zodOutputFormat(RespuestaSchema) },
        system: SYSTEM,
        messages: [
          {
            role: 'user',
            content: [
              { type: 'image', source: { type: 'base64', media_type: mediaType, data: imagen.toString('base64') } },
              { type: 'text', text: `Catálogo de sitios:\n${catalogo}\n\n¿Qué sitio aparece en la foto?` },
            ],
          },
        ],
      });
    } catch (e) {
      this.logger.error(`Fallo al llamar a Claude: ${(e as Error).message}`);
      throw new ServiceUnavailableException('El servicio de reconocimiento no está disponible');
    }

    if (response.stop_reason === 'refusal' || !response.parsed_output) {
      return { sitioId: null, confianza: 0, razon: 'El modelo no devolvió una respuesta válida' };
    }
    const r = response.parsed_output;
    return { sitioId: r.sitioId, confianza: Math.max(0, Math.min(1, r.confianza)), razon: r.razon };
  }
}
