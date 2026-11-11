# Daily Scrum 4 · Sprint 2

| Fecha | Hora | Duración | Modalidad | Scrum Master |
|:--|:--|:--|:--|:--|
| Lunes 26 de octubre de 2026 | 13:02 | 15 min | Videollamada | Omar Salinas Salinas |

**Asistentes:** Noé Chavero Martínez · Omar Salinas Salinas · Juan Gustavo Ángel Cruz Méndez

<p align="center"><img src="img/daily-4-reunion.png" width="900" alt="Daily 4 del Sprint 2"></p>
<p align="center"><sub>Captura de la videollamada · presenta Juan Gustavo Ángel Cruz Méndez</sub></p>

## Juan Gustavo Ángel Cruz Méndez

- **¿Qué hice desde la última daily?** Encontré tres problemas de seguridad. El chat en vivo acepta mensajes sin sesión y con cualquier nombre. El login, el registro y el reconocimiento AR no tienen límite de peticiones. En la compra de boletos el precio lo manda el cliente.
- **¿Qué haré hoy?** Registrar los tres hallazgos como issues del Sprint 2 y empezar las correcciones en `feature/mejoras-seguridad`.
- **¿Tengo algún bloqueo?** Para probar sin tocar Neon hace falta una base desechable; se usa PostgreSQL en Docker.

## Omar Salinas Salinas

- **¿Qué hice desde la última daily?** Pantalla de demos del concierto (#30).
- **¿Qué haré hoy?** Reproductor en segundo plano con Media3 `MediaSessionService` (#31).
- **¿Tengo algún bloqueo?** Ninguno.

## Noé Chavero Martínez

- **¿Qué hice desde la última daily?** Diseño de la pantalla 8: una `PaneTemplate` con destino, distancia, tiempo estimado y el botón **Iniciar navegación**, que entrega la ruta al navegador del auto.
- **¿Qué haré hoy?** `NavegacionCalculator` con sus pruebas, y la lectura por voz del resumen de la ruta.
- **¿Tengo algún bloqueo?** La plantilla de navegación propia (`NavigationTemplate`) requiere que la app sea de categoría NAVIGATION, así que se usa `ACTION_NAVIGATE`, la alternativa que pide la #39.

## Avance mostrado

### Hallazgos registrados

![Hallazgos de seguridad](img/daily-4-hallazgos-seguridad.png)

### Código que lo hace funcionar

**`app/src/main/java/mx/utng/festivaltrack/app/auto/NavegacionCalculator.kt`**: URI que entiende `CarContext.ACTION_NAVIGATE` para mandar la ruta a Google Maps o Waze en el auto (#39) · **Responsable:** Noé

```kotlin
/** URI que entiende `CarContext.ACTION_NAVIGATE`: `geo:21.1566,-100.9341`. */
fun uriNavegacion(latitud: Double, longitud: Double): String = "geo:$latitud,$longitud"
```

## Acuerdos

- Las correcciones de seguridad van en su propia rama y se integran a `develop` con PR al cierre del sprint.

---

<p align="center"><sub><a href="daily-3.md">← Daily 3</a> · <a href="../README.md#sprint-2--19-al-30-de-octubre-de-2026">Índice del Sprint 2</a> · <a href="daily-5.md">Daily 5 →</a></sub></p>
