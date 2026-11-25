# Retrospectiva · Sprint 3

| Fecha | Hora | Duración | Modalidad | Scrum Master | Formato |
|:--|:--|:--|:--|:--|:--|
| Viernes 13 de noviembre de 2026 | 12:16 | 45 min | Videollamada | Juan Gustavo Ángel Cruz Méndez | Continuar / Dejar de hacer / Empezar |

**Asistentes:** Noé Chavero Martínez · Omar Salinas Salinas · Juan Gustavo Ángel Cruz Méndez

<p align="center"><img src="img/retro-reunion.png" width="900" alt="Retrospectiva del Sprint 3"></p>
<p align="center"><sub>Captura de la videollamada · conversación del equipo</sub></p>

## Continuar (lo que funcionó)

- Integrar con PR y CI en verde: los PRs #54 a #58 entraron a `develop` sin conflictos.
- Acompañar cada cambio de seguridad con pruebas HTTP y evidencia, como en #50–#52 y el PR #59.
- Documentar cada ceremonia el mismo día, con su evidencia (captura de la app o código).

## Dejar de hacer (lo que no funcionó)

- Publicar la Release antes de integrar todo lo del sprint: el audio llegó a `develop` un día después de `v4.2-sprint3`.
- Trabajar una rama mucho tiempo solo en local: la rama de audio se subió al final del sprint.

## Empezar (lo que vamos a intentar)

- Congelar `develop` el jueves de cierre y publicar la Release solo después de integrar todos los PRs del sprint.
- Subir la rama al remoto al menos una vez al día, aunque el trabajo no esté terminado.
- Rotar las credenciales en cuanto se detecte que quedaron expuestas en el repositorio.

## Acciones para el siguiente sprint

| Acción | Responsable | Cómo sabremos que se cumplió |
|---|---|---|
| Publicar una Release con el audio (`develop` → `main`) | Noé | Release nueva con el APK que incluye las demos |
| Rotar la contraseña de la base de datos y el `JWT_SECRET` | Gustavo | Las credenciales anteriores dejan de funcionar |
| Regla de protección en `develop` | Gustavo | Settings → Branches muestra la regla activa |

## Seguimiento de las acciones del sprint anterior

| Acción | ¿Se cumplió? | Comentario |
|---|---|---|
| Abrir e integrar el PR `feature/mejoras-seguridad` → `develop` | Sí | PR #56, integrado el 10/11 |
| Subir la rama de audio y abrir su PR hacia `develop` | Sí | PRs #57 y #58, integrados el 10 y 11/11 |
| Regla de protección en `develop` | No | Sigue pendiente; pasa al siguiente sprint |

---

<p align="center"><sub><a href="review.md">← Sprint Review</a> · <a href="../README.md#sprint-3--2-al-13-de-noviembre-de-2026">Índice del Sprint 3</a> · <a href="incremento.md">Incremento →</a></sub></p>
