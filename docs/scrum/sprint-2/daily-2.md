# Daily Scrum 2 · Sprint 2

| Fecha | Hora | Duración | Modalidad | Scrum Master |
|:--|:--|:--|:--|:--|
| Miércoles 21 de octubre de 2026 | 13:03 | 15 min | Videollamada | Omar Salinas Salinas |

**Asistentes:** Noé Chavero Martínez · Omar Salinas Salinas · Juan Gustavo Ángel Cruz Méndez

<p align="center"><img src="img/daily-2-reunion.png" width="900" alt="Daily 2 del Sprint 2"></p>
<p align="center"><sub>Captura de la videollamada · presenta Noé Chavero Martínez</sub></p>

## Juan Gustavo Ángel Cruz Méndez

- **¿Qué hice desde la última daily?** Revisé el modelo `SitioAR` contra lo que pide el GPS: ya tiene coordenadas, y el seed incluye Mausoleo, Parroquia, Monumento a Hidalgo y Casa Museo.
- **¿Qué haré hoy?** Revisar el PR #48 (documentación Scrum) y dejar lista la integración `develop` → `main`.
- **¿Tengo algún bloqueo?** Ninguno.

## Omar Salinas Salinas

- **¿Qué hice desde la última daily?** Diseño de la relación canción–evento para las demos (#29).
- **¿Qué haré hoy?** Seguir con la #29 y empezar la pantalla de demos en el detalle del evento (#30).
- **¿Tengo algún bloqueo?** Ninguno.

## Noé Chavero Martínez

- **¿Qué hice desde la última daily?** Actualicé la rama con `develop` y terminé la #33: lista de destinos con distancia y tiempo estimado. "Navegar" guarda la ruta activa y la envía al auto. PR #47 integrado.
- **¿Qué haré hoy?** Generar el burndown de cada sprint con los datos del Project (`burndown.py`) y un workflow que lo actualice cada día.
- **¿Tengo algún bloqueo?** GitHub Projects solo ofrece burn up en Insights; por eso la gráfica se genera con un script propio.

## Avance mostrado

### Código que lo hace funcionar

**`app/src/main/java/mx/utng/festivaltrack/app/auto/DestinosCalculator.kt`**: arma los destinos con un escenario por evento y agrega los sitios del catálogo que no se repiten. El destacado es el siguiente evento (#33) · **Responsable:** Noé

```kotlin
proximos.groupBy { it.evento.escenario ?: it.evento.ubicacion }.forEach { (lugar, salidas) ->
    val siguiente = salidas.first()
    destinos += destino(
        nombre = lugar,
        subtitulo = "${siguiente.evento.nombre} • ${formatoHora.format(siguiente.inicio)} hrs",
        lat = siguiente.evento.latitud!!,
        lon = siguiente.evento.longitud!!,
        origenLat = origenLat,
        origenLon = origenLon,
        esDestacado = siguiente === proximos.first()
    )
}
```

**`docs/scrum/herramientas/burndown.py`**: lee el campo Sprint, el Estimate y la fecha de cierre de cada issue del Project y dibuja la línea ideal contra la real · **Responsable:** Noé

**`festivaltrack-backend/src/canciones/canciones.service.ts`**: el administrador define el repertorio de un concierto; el orden del arreglo es el orden de reproducción (#29) · **Responsable:** Omar

```ts
async setRepertorio(eventoId: string, cancionIds: string[]) {
  const ids = [...new Set(cancionIds)];
  const evento = await this.prisma.evento.findUnique({ where: { id: eventoId } });
  if (!evento) throw new NotFoundException('Evento no encontrado');

  await this.prisma.$transaction(async (tx) => {
    const transmision = await tx.transmision.upsert({
      where: { eventoId }, create: { eventoId, titulo: evento.nombre }, update: {},
    });
    await tx.cancionTransmision.deleteMany({ where: { transmisionId: transmision.id } });
    await tx.cancionTransmision.createMany({
      data: ids.map((cancionId, orden) => ({ cancionId, transmisionId: transmision.id, orden })),
    });
  });
  return this.findByEvento(eventoId);
}
```

**Integración en `develop`**

| PR | Rama | Contenido |
|---|---|---|
| #47 | `feat/NaveagionGPSCarPlay` | Pantalla 7 · Elige tu destino (#33) con 5 pruebas de `DestinosCalculator` |

## Acuerdos

- El PR #48 (documentación Scrum y burndown) lo revisa Gustavo antes de integrarlo.

---

<p align="center"><sub><a href="daily-1.md">← Daily 1</a> · <a href="../README.md#sprint-2--19-al-30-de-octubre-de-2026">Índice del Sprint 2</a> · <a href="daily-3.md">Daily 3 →</a></sub></p>
