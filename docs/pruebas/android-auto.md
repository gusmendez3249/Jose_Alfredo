# Prueba del modo automóvil (Android Auto) · Sprint 4

Guía para probar las pantallas 8, 9 y 10 del modo automóvil (#39, #37, #36) en el **Desktop Head Unit (DHU)**
y dejar las capturas que faltan en la Sprint Review y el Incremento.

## ⚠️ Se necesita un celular Android físico

Los emuladores no sirven para esta prueba. Se intentó el 10/11/2026 en dos emuladores con Google Play:

| Emulador | Resultado |
|---|---|
| Pixel 9 Pro XL · Android 16 (API 36) | Android Auto viene como *stub* y la Play Store dice *"This app isn't compatible with your device anymore"* |
| Pixel 7 · Android 14 (API 34) | Mismo mensaje en la Play Store |
| Celular físico del equipo (12/11/2026) | Android Auto tampoco permitió usar la aplicación |

> **Sprint 4 (#84):** la validación se hará con el emulador de **Android Automotive OS**, que corre las pantallas
> del auto directamente, sin teléfono, sin Android Auto y sin DHU.

Sin la app real de Android Auto no existe la opción **Iniciar servidor de unidad principal** y el DHU no tiene a qué
conectarse. No instalar apps parecidas de otros desarrolladores (por ejemplo "Car Play Connect"): no funcionan con el
DHU. Tampoco instalar el APK de Android Auto desde sitios espejo.

## Requisitos

- Celular Android 10 o superior con **Android Auto** actualizado desde la Play Store (en Android 10+ viene dentro de
  *Ajustes → Dispositivos conectados → Preferencias de conexión → Android Auto*).
- Cable USB y **depuración USB** activada (*Ajustes → Acerca del teléfono →* tocar 7 veces *Número de compilación →
  Opciones de desarrollador → Depuración USB*).
- En la PC: Android Studio con el **Android Auto Desktop Head Unit Emulator**
  (*Tools → SDK Manager → SDK Tools*). Queda en `%LOCALAPPDATA%\Android\Sdk\extras\google\auto`.
- Backend corriendo con el `.env` del equipo: `cd festivaltrack-backend && npm run start:dev`.

## Pasos

1. **Instalar la app** en el celular desde `develop`:
   ```bash
   ./gradlew :app:installDebug
   ```
   Si el celular no está en la misma red que la PC, redirigir el backend: `adb reverse tcp:3001 tcp:3001`.
2. **Iniciar sesión** en FestivalTrack con una cuenta de rol **USUARIO** (el AR y el resumen AR solo se ven con ese rol).
3. **Modo desarrollador de Android Auto:** abrir Android Auto, ir hasta *Versión* y tocarla 10 veces. En el menú ⋮
   elegir **Iniciar servidor de unidad principal**.
4. **Redirigir el puerto** desde la PC:
   ```bash
   adb forward tcp:5277 tcp:5277
   ```
5. **Abrir el DHU:**
   ```bash
   cd %LOCALAPPDATA%\Android\Sdk\extras\google\auto
   desktop-head-unit.exe
   ```
6. En la pantalla del auto, abrir el lanzador de apps y elegir **FestivalTrack Phone**.

## Qué revisar y qué capturar

Guardar cada captura del DHU en `docs/scrum/sprint-3/img/` con el nombre indicado.

| # | Pantalla | Qué hacer | Resultado esperado | Captura |
|---|---|---|---|---|
| 1 | 10 · Home del auto (#36) | Abrir FestivalTrack en el DHU | Lista con *Audio en curso*, *Ruta activa*, *Resumen AR* y *Alerta de salida y próximos eventos* | `auto-home.png` |
| 2 | 7 · Elige tu destino (#33) | En el celular: *Mapa → ícono del auto →* elegir un destino → **Navegar** | Destinos con distancia y tiempo; el elegido queda como ruta activa | `auto-destinos.png` (captura del celular) |
| 3 | 10 · Ruta activa | Volver al Home del DHU | La fila dice *Ruta activa: (destino)* | `auto-home-ruta.png` |
| 4 | 8 · Navegación (#39) | Tocar la ruta activa | Destino, distancia, tiempo estimado y botón **Iniciar navegación**; se lee en voz alta | `auto-navegacion.png` |
| 5 | 8 · Iniciar navegación | Tocar **Iniciar navegación** | Se abre Google Maps / Waze en el DHU con la ruta (prueba de integración "el GPS envía la ruta al auto") | `auto-navegacion-maps.png` |
| 6 | 9 · Alerta de salida (#37) | Home → *Alerta de salida y próximos eventos* | Eventos con "Sal ya" / "Sal en X min", leídos por voz | `auto-alerta-salida.png` |
| 7 | 10 · Resumen AR | Reconocer un sitio con la cámara AR en el celular y volver al Home | *Resumen AR* muestra el sitio descubierto hoy | `auto-resumen-ar.png` |
| 8 | Prueba con usuarios (#40) | Pedir a alguien ajeno al equipo que llegue a la navegación sin instrucciones | Lo logra sin ayuda; anotar el tiempo y los comentarios | — |

La fila *Audio en curso* va a decir "Sin audio en curso" hasta que se integre el reproductor en segundo plano (#31).

## Registro del resultado

Al terminar, llenar esta tabla y agregar las capturas a la Review del Sprint 3 (`docs/scrum/sprint-3/review.md`).

| Fecha | Probó | Celular / Android | Pasos 1–7 | Prueba con usuarios | Observaciones |
|---|---|---|---|---|---|
| | | | | | |
