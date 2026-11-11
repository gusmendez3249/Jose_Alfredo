# Daily Scrum 2 · Sprint 3

- **Fecha:** 2026-11-04 (miércoles)
- **Duración:** 15 min
- **Asistentes:** Gustavo · Omar · Noé
- **Facilitador (Scrum Master del sprint):** Gustavo

![Reunión](img/daily-2-reunion.png)

## Juan Gustavo Ángel Cruz Méndez

- **¿Qué hice desde la última daily?** Confirmé que `feature/mejoras-seguridad` no tiene conflictos con `develop`.
- **¿Qué haré hoy?** Preparar la descripción del PR con la evidencia de `evidencias/seguridad/`.
- **¿Tengo algún bloqueo?** Ninguno.

## Salinas Salinas Omar

- **¿Qué hice desde la última daily?** Avance en la cola de reproducción (#25).
- **¿Qué haré hoy?** Continuar con la #25.
- **¿Tengo algún bloqueo?** Ninguno reportado.

## Noé Chavero Martínez

- **¿Qué hice desde la última daily?** Comparé el reporte técnico con el código: las secciones 12, 14 y 15 mencionan Firebase, Cloud Functions, HLS y Provider/Riverpod.
- **¿Qué haré hoy?** Redactar las secciones nuevas con el stack real: NestJS + Prisma + Neon, JWT, Media3 con RTSP, MVVM con ViewModel de Jetpack, CameraX con reconocimiento visual en el backend y Car App Library.
- **¿Tengo algún bloqueo?** Ninguno.

## Avance mostrado

### Diferencias encontradas en el reporte (#41)

| Sección | Dice el reporte | Lo que usa el proyecto |
|---|---|---|
| 12 · Arquitectura | ViewModel con Provider / Riverpod; Firestore | ViewModel de Jetpack con `StateFlow`; API REST propia con Retrofit |
| 14 · Tecnologías | Firebase (Firestore, Auth, Storage), Cloud Functions, HLS | NestJS + Prisma + PostgreSQL en Neon; JWT; Media3 (ExoPlayer) con RTSP |
| 15 · Servicios | Firebase Authentication, Cloud Vision, FCM | Login propio con JWT y bcrypt; reconocimiento AR en el backend; recordatorios locales |

## Acuerdos

- El documento con las secciones corregidas se guarda en `docs/reporte/` para pasarlo al PDF final.
