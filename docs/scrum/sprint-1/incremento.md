# Incremento · Sprint 1

- **Fecha de entrega:** 2026-10-16
- **Versión:** `v4.0-sprint1`
- **Release:** [v4.0-sprint1](https://github.com/gusmendez3249/Jose_Alfredo/releases) *(se publica al cierre del sprint)*
- **Pull Request develop → main:** *(pendiente)*

## Funcionalidades incluidas

| Issue | Funcionalidad | Responsable |
|---|---|---|
| #22 | Galería interactiva como foro: publicar experiencias, recomendaciones y fotos; me gusta, comentarios, filtros, mosaico y zoom | Omar |
| #32 | Escáner AR con la cámara y reconocimiento del sitio en el backend | Gustavo |
| #35 | Tarjeta del sitio identificado con galería y audio | Gustavo |
| #23 | Historial de sitios visitados | Gustavo |
| #34 | Modo automóvil en Android Auto | Noé |
| #37 | Alerta de salida y próximos eventos en el auto | Noé |
| #26, #27 | Rama `develop`, flujo con Pull Request y CI con GitHub Actions | Noé |
| — | Seguridad del backend: endpoints de administrador protegidos, `JWT_SECRET` obligatorio, CORS restringido | Noé |

## Cómo probarlo

1. Descargar el APK de la Release (o el artefacto `festivaltrack-app-debug` del último Android CI).
2. Levantar el backend: `cd festivaltrack-backend && npm run start:dev` con el `.env` del equipo.
3. Galería: barra inferior → **Galería**. Iniciar sesión para publicar, dar me gusta o comentar.
4. AR: Inicio → **Explorar AR**. Requiere sesión y `ANTHROPIC_API_KEY` en el `.env` del backend.
5. Android Auto: seguir la guía de `app/README.md` (Desktop Head Unit) → **Alerta de salida y próximos eventos**.

## Evidencia

| Captura | Qué se muestra |
|---|---|
| <img src="img/daily-3-foro.png" width="220"> | Galería como foro |
| <img src="img/daily-4-ar-camara.png" width="220"> | Escáner AR |
| <img src="img/daily-6-inicio.png" width="220"> | Inicio con los módulos nuevos |
