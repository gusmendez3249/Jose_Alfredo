# Sprint Review · Sprint 3

| Fecha | Hora | Duración | Modalidad | Scrum Master | Product Owner |
|:--|:--|:--|:--|:--|:--|
| Viernes 13 de noviembre de 2026 | 11:03 | 60 min | Videollamada | Juan Gustavo Ángel Cruz Méndez | Armando Vega |

**Asistentes:** Noé Chavero Martínez · Omar Salinas Salinas · Juan Gustavo Ángel Cruz Méndez · Armando Vega (Product Owner)

**Periodo del sprint:** 2 al 13 de noviembre de 2026

> **Objetivo del sprint:** cerrar el MVP (reproductor con cola), integrar en `develop` todo lo terminado, probar los módulos nuevos, alinear el reporte técnico con el stack real y publicar la Release.

<p align="center"><img src="img/review-reunion.png" width="900" alt="Sprint Review del Sprint 3"></p>
<p align="center"><sub>Captura de la videollamada · presenta Noé Chavero Martínez</sub></p>

## Resultado del sprint

Milestone: [Sprint 3](https://github.com/gusmendez3249/Jose_Alfredo/milestone/3) · **7 de 7 issues cerradas (100 %)**

| Milestone | Cerradas | Abiertas | Avance |
|---|---|---|---|
| [Sprint 3](https://github.com/gusmendez3249/Jose_Alfredo/milestone/3) | 7 | 0 | 100 % |

### Burndown

![Burndown del Sprint 3](img/burndown-sprint-3.png)

Gráfica generada con `docs/scrum/herramientas/burndown.py` a partir del campo Sprint, el Estimate y la fecha de cierre de cada issue del Project.

## Tareas terminadas

| Issue | Tarea | Responsable | Evidencia |
|---|---|---|---|
| #39 | [Auto] Pantalla 8 · Navegación en Android Auto | Noé | PR #53 · [Daily 5 del Sprint 2](../sprint-2/daily-5.md) |
| #36 | [Auto] Pantalla 10 · Home del modo automóvil | Noé | PR #53 · [Daily 6 del Sprint 2](../sprint-2/daily-6.md) |
| #25 | Reproductor y cola de reproducción | Omar | PRs #57 y #58 · [Daily 5](daily-5.md) |
| #40 | [QA] Pruebas unitarias y e2e de los módulos nuevos | Equipo | Pruebas de `auto`, de la cola de audio y de seguridad · `docs/pruebas/android-auto.md` |
| #41 | [Docs] Reporte técnico alineado con el stack real | Noé | PR #54 · [Daily 2](daily-2.md), [Daily 3](daily-3.md) |
| #42 | [Release] `develop` → `main` y Release | Noé | PR #55 · [v4.2-sprint3](https://github.com/gusmendez3249/Jose_Alfredo/releases/tag/v4.2-sprint3) |

Épicas cerradas: **#21 Vista de navegación GPS para Android Auto** y **#24 Demos de audio del concierto**.

Integraciones del sprint: PR #56 (seguridad #50–#52), PRs #57 y #58 (audio), PR #54 (documentación) y PR #55 (`develop` → `main`).

## Tareas no terminadas

Ninguna: todas las issues del milestone quedaron cerradas e integradas en `develop`.

## Demostración

| Evidencia | Qué se muestra | Responsable |
|---|---|---|
| <img src="../sprint-2/img/review-codigo-home-auto.png" width="420"> | Home del modo automóvil: audio en curso, ruta activa, resumen AR y alerta de salida (#36) | Noé |
| <img src="../sprint-2/img/review-codigo-navegacion.png" width="420"> | Navegación en Android Auto con lectura por voz (#39) | Noé |
| <img src="img/review-codigo-cola.png" width="420"> | Cola de reproducción: qué hace cuando una demo falla (#25) | Omar |
| <img src="img/daily-5-integracion.png" width="420"> | PRs integrados en el sprint, con CI en verde | Equipo |

**Calidad:** los PRs #55, #56, #57 y #58 pasaron Backend CI y Android CI. Pruebas unitarias: 23 del módulo `auto`, las de la cola de audio (`PlaybackLogicTest`) y la suite del backend con las pruebas de seguridad.

## Tablero al cierre

![Tablero al cierre del Sprint 3](img/review-tablero.png)

## Retroalimentación del Product Owner

- El modo automóvil cubre las cuatro pantallas de la épica #21. Falta la prueba con usuarios en un celular físico, con la guía de `docs/pruebas/android-auto.md`.
- El audio quedó integrado después de la Release `v4.2-sprint3`, así que hay que publicar una Release complementaria con las demos y el reproductor.
- Se aprueba el incremento del sprint.

---

<p align="center"><sub><a href="daily-6.md">← Daily 6</a> · <a href="../README.md#sprint-3--2-al-13-de-noviembre-de-2026">Índice del Sprint 3</a> · <a href="retro.md">Retrospectiva →</a></sub></p>
