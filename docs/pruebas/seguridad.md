# Pruebas de seguridad e integración · FestivalTrack

Este documento explica cómo verificar autenticación, permisos, entradas inválidas, manejo de errores y la integración con los servicios en la nube. **Ninguna prueba usa datos personales ni credenciales reales**: los usuarios son ficticios (`*@ejemplo.test`) y el `JWT_SECRET` de las pruebas solo sirve en ellas.

> Resultados registrados el **2026-11-11** en la rama `feature/mejoras-seguridad`. Las mismas pruebas corren solas en GitHub Actions en cada PR a `develop`/`main` (ver [Automatización](#4-automatización-en-github-actions)).

## 1. Cómo ejecutarlas

### Backend (NestJS · Jest + Supertest) — no necesita base de datos

```bash
cd festivaltrack-backend
npm ci
npx prisma generate
npx jest --ci --verbose          # todas (51 pruebas)
npx jest src/seguridad.http.spec.ts --verbose   # solo las de seguridad HTTP (14)
```

### App Android (JUnit) — no necesita emulador

```bash
./gradlew :app:testDebugUnitTest
```

### Prueba de integración contra una base real (opcional)

`festivaltrack-backend/test/app.e2e-spec.ts` levanta la API completa contra Neon. Requiere un `.env` propio (ver [SECURITY.md](../../SECURITY.md)) y se recomienda usar una **rama de prueba de Neon**, no la de producción:

```bash
npx jest --config test/jest-e2e.json
```

## 2. Matriz de casos y resultados

### 2.1 Seguridad HTTP de la API — `src/seguridad.http.spec.ts`

Levanta los controladores reales con los guards reales (JWT + roles) y la misma configuración de `main.ts` (`configurar-seguridad.ts`: helmet, CORS y ValidationPipe). Solo los servicios y Prisma son dobles de prueba.

| # | Aspecto | Caso | Esperado | Resultado |
| :-: | :--- | :--- | :---: | :---: |
| S1 | Autenticación | `POST /boletos/comprar` sin token | 401 y el servicio no se ejecuta | ✅ |
| S2 | Autenticación | Token firmado con otra clave | 401 | ✅ |
| S3 | Autenticación | Token válido de un usuario dado de baja (`activo=false`) | 401 | ✅ |
| S4 | Autenticación | `POST /auth/tv-sync` sin sesión → 401; con sesión, el `userId` del cuerpo se ignora y se usa el del token | 401 / 201 | ✅ |
| S5 | Autorización | Un `USUARIO` intenta `POST /eventos` | 403 | ✅ |
| S6 | Autorización | `POST /biografias/alta`: sin sesión / `USUARIO` / `ADMINISTRADOR` | 401 / 403 / 201 | ✅ |
| S7 | Entradas inválidas | Biografía sin `titulo` ni `descripcion` | 400 | ✅ |
| S8 | Entradas inválidas | `PUT` y `PATCH /eventos/:id` con `capacidad: "muchos"` | 400 | ✅ |
| S9 | Integración app ↔ API | `PUT` (lo usa la app) y `PATCH` con datos válidos | 200 | ✅ |
| S10 | Entradas inválidas | Comprar 11 boletos o `-1` | 400 | ✅ |
| S11 | Datos del cliente | La compra manda `precioTotal: 1` y `usuarioId` ajeno | Se descartan; el servicio recibe el usuario del token | ✅ |
| S12 | Errores | Fallo interno en el servicio | 500 con mensaje genérico, sin detalles de la base | ✅ |
| S13 | Cabeceras | Respuesta normal | `X-Content-Type-Options: nosniff`, HSTS, sin `X-Powered-By` | ✅ |
| S14 | CORS | Petición desde `https://sitio-malicioso.example` | Sin `Access-Control-Allow-Origin` | ✅ |

> **Hallazgo corregido con S9:** `PUT /eventos/:id` respondía **404** porque el método tenía `@Patch` y `@Put` a la vez y Nest solo registra uno. La app Android edita eventos con PUT, así que esa función no funcionaba. Ahora son dos handlers separados.

### 2.2 Pruebas unitarias de seguridad (servicios y guards)

| Archivo | Qué verifica | Resultado |
| :--- | :--- | :---: |
| `auth/auth.service.spec.ts` | Registro con correo repetido (409); login con correo inexistente o contraseña incorrecta (401); validación de rol | 7/7 ✅ |
| `auth/auth.throttle.spec.ts` | Límite de intentos de login: el 6.º intento por minuto contra la misma cuenta responde 429 sin bloquear a otra cuenta en la misma IP | 2/2 ✅ |
| `common/guards/app-throttler.guard.spec.ts` | El límite usa la IP real detrás del proxy de Railway | 3/3 ✅ |
| `boletos/boletos.service.spec.ts` | El precio lo calcula el servidor; evento inexistente (404) o cancelado (400) | 4/4 ✅ |
| `chat/chat.service.spec.ts` | El chat usa el nombre del usuario autenticado; usuarios inactivos rechazados; no se puede suplantar al admin | 4/4 ✅ |
| `ar/ar-reconocimiento.facade.spec.ts` | Integración con la IA: rechaza archivos que no son imagen, confianza baja e IDs inexistentes | 5/5 ✅ |
| `canciones/canciones.service.spec.ts` | Repertorio: rechaza listas inválidas, eventos y canciones inexistentes | 8/8 ✅ |

**Total del backend: 12 suites, 51/51 pruebas aprobadas.**

### 2.3 App Android

| Suite | Pruebas | Resultado |
| :--- | :-: | :-: |
| `auto/*CalculatorTest` (modo automóvil: destinos, navegación, salida, resumen AR) | 23 | ✅ |
| `playback/PlaybackLogicTest`, `QueueTrackTest`, `ui/AudioUiLogicTest` | 30 | ✅ |

**Total de la app: 7 suites, 53/53 pruebas aprobadas.** La compilación de `app`, `tv` y `wear` también se verificó con la nueva configuración de red.

## 3. Verificación manual con la app (datos ficticios)

Pasos para revisar los controles en un emulador con el backend local. Usa una cuenta creada para la prueba, nunca una real.

| # | Paso | Resultado esperado |
| :-: | :--- | :--- |
| M1 | Iniciar sesión con contraseña incorrecta | Mensaje genérico de credenciales inválidas, sin decir si el correo existe |
| M2 | Intentar 6 inicios de sesión fallidos en un minuto | El 6.º responde "demasiados intentos" (429) |
| M3 | Entrar como `USUARIO` | No aparecen las pantallas de administración; si se llama al endpoint directamente responde 403 |
| M4 | Comprar boletos | El total lo muestra el servidor (VIP 4500, General 1200, Estudiante 600 MXN) |
| M5 | Cerrar sesión y volver a abrir la app | Pide iniciar sesión otra vez; el token se borró |
| M6 | Build de release apuntando a un `http://` público | La conexión se bloquea; en release solo se permite HTTPS (excepto el emulador y localhost) |

Las capturas de las mejoras #50, #51 y #52 están en [evidencias/seguridad](../../evidencias/seguridad/README.md).

## 4. Automatización en GitHub Actions

| Workflow | Qué corre | Cuándo |
| :--- | :--- | :--- |
| `.github/workflows/backend.yml` | `npm ci`, `npm audit --omit=dev --audit-level=high`, verificación de tipos y `jest` (incluye las pruebas de seguridad) | PR y push a `develop`/`main` con cambios en el backend |
| `.github/workflows/android.yml` | `:app:testDebugUnitTest` y compilación del APK | PR y push a `develop`/`main` con cambios en Android |
| `.github/dependabot.yml` | Revisa cada semana dependencias de npm, Gradle y Actions | Semanal |

El CI usa un `JWT_SECRET` de prueba en texto, sin valor real, y no se conecta a la base de datos.
