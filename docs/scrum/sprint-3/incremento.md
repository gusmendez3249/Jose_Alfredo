# Incremento · Sprint 3

- **Fecha de entrega:** 2026-11-10. La Release se publicó antes del cierre del sprint (13/11) para tener el APK listo en la Sprint Review.
- **Versión:** `v4.2-sprint3`
- **Release:** [v4.2-sprint3](https://github.com/gusmendez3249/Jose_Alfredo/releases/tag/v4.2-sprint3), con el APK adjunto
- **Pull Request develop → main:** #55

Esta Release es el incremento acumulado de la Unidad II: incluye lo de los Sprints 1 y 2, que no tuvieron Release propia.

## Funcionalidades incluidas

| Sprint | Issue | Funcionalidad | Responsable |
|---|---|---|---|
| 1 | #22 | Galería interactiva como foro de la comunidad | Omar |
| 1 | #32, #35, #23 | Realidad aumentada: cámara de reconocimiento, sitio identificado e historial | Gustavo |
| 1 | #34, #37 | Módulo Android Auto y pantalla 9 · Alerta de salida | Noé |
| 1 | #26, #27 | Rama `develop`, flujo con PR y CI con GitHub Actions | Noé |
| 2 | #33, #28 | Pantalla 7 · Elige tu destino con el catálogo de sitios | Noé · Gustavo |
| 2 | #50, #51, #52 | Seguridad: chat con sesión, rate limiting y precio del boleto en el servidor | Gustavo |
| 3 | #39 | Pantalla 8 · Navegación en Android Auto | Noé |
| 3 | #36 | Pantalla 10 · Home del modo automóvil | Noé |
| 3 | #41 | Reporte técnico y READMEs alineados con el stack real | Noé |
| 3 | #42 | Integración `develop` → `main` y Release | Noé |

**No incluido:** las demos de audio y el reproductor (#29, #30, #31 y #25) no están en el repositorio a la fecha de la Release.
La tarjeta *Audio en curso* del Home del auto muestra "Sin audio en curso" hasta que se integren.

## Cómo probarlo

1. Descargar `FestivalTrack-v4.2-sprint3.apk` de la Release e instalarlo en un celular Android.
2. Levantar el backend: `cd festivaltrack-backend && npm install && npm run start:dev` con el `.env` del equipo.
3. **Galería y AR:** iniciar sesión con una cuenta de rol USUARIO → **Galería** y **Explorar AR** desde Inicio.
4. **Pantalla 7:** Mapa → ícono del auto → elegir un destino → **Navegar**.
5. **Pantallas 8, 9 y 10:** conectar el celular al Desktop Head Unit siguiendo [`docs/pruebas/android-auto.md`](../../pruebas/android-auto.md).
6. **Seguridad:** el chat en vivo pide sesión, y el checkout muestra el precio calculado por el servidor.

## Evidencia

| Captura | Qué se muestra | Responsable |
|---|---|---|
| <img src="../sprint-2/img/seguridad-52-app-checkout-desglose.png" width="220"> | Compra con el precio calculado por el servidor (#52) | Gustavo |
| 📷 *Captura pendiente*: `img/auto-home.png` | Home del modo automóvil en el DHU (#36), tomada con celular físico | Equipo |
| 📷 *Captura pendiente*: `img/auto-navegacion.png` | Pantalla 8 · Navegación (#39) | Equipo |
| 📷 *Captura pendiente*: `img/incremento-release.png` | Página de la Release `v4.2-sprint3` con el APK | Noé |

**Calidad:** Android CI en verde en el PR #55. Pruebas unitarias: 23 de la app (módulo `auto`) y la suite del backend con las pruebas de seguridad.
