# Daily Scrum 6 · Sprint 2

- **Fecha:** 2026-10-29 (jueves)
- **Duración:** 15 min
- **Asistentes:** Gustavo · Omar · Noé
- **Facilitador (Scrum Master del sprint):** Omar

![Reunión](img/daily-6-reunion.png)

## Juan Gustavo Ángel Cruz Méndez

- **¿Qué hice desde la última daily?** Comenté el código de las correcciones #50, #51 y #52 y subí la evidencia de funcionamiento.
- **¿Qué haré hoy?** Abrir el PR `feature/mejoras-seguridad` → `develop`.
- **¿Tengo algún bloqueo?** Ninguno.

## Salinas Salinas Omar

- **¿Qué hice desde la última daily?** Reporta terminadas las tareas de audio #29, #30 y #31.
- **¿Qué haré hoy?** Subir la rama de audio y abrir su PR hacia `develop`.
- **¿Tengo algún bloqueo?** Ninguno reportado.

## Noé Chavero Martínez

- **¿Qué hice desde la última daily?** Integré el PR #53 (pantallas 8 y 10) en `develop` con el CI en verde.
- **¿Qué haré hoy?** Actualizar el tablero para el cierre del sprint y preparar la Sprint Review.
- **¿Tengo algún bloqueo?** Ninguno.

## Avance mostrado

### Código que lo hace funcionar

**`app/src/main/java/mx/utng/festivaltrack/app/auto/ResumenArCalculator.kt`**: sitios descubiertos hoy con la cámara AR, sin repetir, del más reciente al más antiguo. Alimenta la tarjeta "Resumen AR" del Home del auto (#36) · **Responsable:** Noé

```kotlin
val hoy = ahora.atZone(zona).toLocalDate()
val nombres = visitas
    .mapNotNull { visita -> parsearFecha(visita.fechaVisita)?.let { it to visita } }
    .filter { (fecha, _) -> fecha.atZone(zona).toLocalDate() == hoy }
    .sortedByDescending { (fecha, _) -> fecha }
    .distinctBy { (_, visita) -> visita.sitio.id }
    .map { (_, visita) -> visita.sitio.nombre }
```

**Integración en `develop`**

| PR | Rama | Contenido |
|---|---|---|
| #53 | `feat/NaveagionGPSCarPlay` | Pantalla 8 · Navegación (#39) y pantalla 10 · Home del auto (#36), con pruebas de `NavegacionCalculator` (6) y `ResumenArCalculator` (4) |

## Acuerdos

- Sprint Review y Retrospectiva el viernes 30 de octubre.
- Antes de cerrar el sprint, el código de seguridad y el de audio deben estar en un PR hacia `develop`.
