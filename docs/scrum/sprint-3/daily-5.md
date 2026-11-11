# Daily Scrum 5 · Sprint 3

- **Fecha:** 2026-11-11 (miércoles)
- **Duración:** 15 min
- **Asistentes:** Gustavo · Omar · Noé
- **Facilitador (Scrum Master del sprint):** Gustavo

![Reunión](img/daily-5-reunion.png)

## Juan Gustavo Ángel Cruz Méndez

- **¿Qué hice desde la última daily?** El PR #56 integró en `develop` y `main` las correcciones de seguridad #50, #51 y #52, con Backend CI y Android CI en verde.
- **¿Qué haré hoy?** Probar el modo automóvil con mi celular en el Desktop Head Unit siguiendo `docs/pruebas/android-auto.md`, y tomar las capturas para la Review.
- **¿Tengo algún bloqueo?** Ninguno.

## Salinas Salinas Omar

- **¿Qué hice desde la última daily?** Integré el audio en `develop`. El PR #57 trae las demos del concierto como playlist, el repertorio por administrador y el reproductor en segundo plano con cola global (#24, #29, #30, #31, #25). El PR #58 corrige la cola, rediseña el reproductor y agrega la subida de canciones del administrador.
- **¿Qué haré hoy?** Revisar las demos con el backend en el celular y preparar la demostración del reproductor para la Review.
- **¿Tengo algún bloqueo?** Ninguno.

## Noé Chavero Martínez

- **¿Qué hice desde la última daily?** Integré los PRs #54 (documentación y reporte, #41) y #55 (`develop` → `main`), y publiqué la Release `v4.2-sprint3` con el APK. Cerré #41, #42 y #40.
- **¿Qué haré hoy?** Agregar a la documentación las capturas de las reuniones de los tres sprints.
- **¿Tengo algún bloqueo?** El audio llegó a `develop` después de publicar `v4.2-sprint3`, así que esa Release no lo incluye.

## Avance mostrado

### Evidencia técnica

| PR | Contenido | Responsable | Checks |
|---|---|---|---|
| #57 | Demos del concierto como playlist, repertorio por administrador y reproductor en segundo plano | Omar | Compilar APK ✅ · Backend ✅ |
| #58 | Cola robusta ante errores, rediseño del reproductor y subida de canciones | Omar | Compilar APK ✅ · Backend ✅ |
| #56 | Seguridad: chat con sesión, rate limiting y precio del boleto en el servidor | Gustavo | Compilar APK ✅ · Backend ✅ |

### Estado del tablero

| Estado | Tareas del Sprint 3 |
|---|---|
| Done | #25, #36, #39, #40, #41, #42 · épicas #21 y #24 |

## Acuerdos

- El viernes 13/11, después de la Review, se integra `develop` → `main` y se publica una Release con el audio.
- Gustavo sube las capturas del modo automóvil a `docs/scrum/sprint-3/img/` con los nombres de la guía.
