# 🔐 Seguridad y manejo de secretos — FestivalTrack

Este documento explica dónde vive cada secreto del proyecto, cómo configurarlo sin publicarlo y qué hacer si alguno se expone.

## 1. Regla general

- **Ningún secreto se guarda en el repositorio**: ni en código, ni en README, ni en capturas, ni en el historial.
- Los valores reales viven solo en:
  - el archivo `.env` local de cada integrante (ignorado por `.gitignore`), y
  - las variables del servicio en Railway o los *GitHub Secrets* cuando se despliega o se automatiza.
- Los archivos `*.env.example` contienen **solo valores ficticios** (`TU_CONTRASENA_LOCAL_AQUI`, `user:password@host`) y sirven para saber qué variables hacen falta.

## 2. Inventario de secretos

| Variable | Módulo | Para qué sirve | ¿Secreta? | Dónde se configura |
| :--- | :--- | :--- | :---: | :--- |
| `DATABASE_URL` / `DIRECT_URL` | Backend | Conexión a PostgreSQL en Neon (incluye usuario y contraseña) | ✅ Sí | `.env` local / variables de Railway |
| `JWT_SECRET` | Backend | Firma y verificación de los tokens de sesión. El servidor **no arranca** sin ella (`src/auth/jwt-secret.ts`) | ✅ Sí | `.env` local / variables de Railway |
| `ANTHROPIC_API_KEY` / `ANTHROPIC_WORKSPACE_ID` | Backend (AR) | Reconocimiento visual de sitios con IA | ✅ Sí | `.env` local / variables de Railway |
| `SEED_ADMIN_PASSWORD` | Backend (seed) | Contraseña del administrador que crea `npm run seed` (mínimo 12 caracteres) | ✅ Sí | Solo `.env` local |
| `SEED_ADMIN_EMAIL` | Backend (seed) | Correo del administrador inicial | ❌ No | `.env` local |
| `JWT_EXPIRES_IN`, `PORT`, `NODE_ENV`, `FRONTEND_URL` | Backend | Configuración de ejecución y CORS | ❌ No | `.env` local / Railway |
| `VITE_API_URL` | Web | URL pública del backend | ❌ No | `festivaltrack-web/.env` |
| `festivaltrackApiUrl` | Android (`shared`) | URL del backend que se compila en `BuildConfig.API_BASE_URL` | ❌ No | `-PfestivaltrackApiUrl=...` al compilar, o `~/.gradle/gradle.properties`; vacía = detección automática |

> Las apps Android **no contienen claves**: solo conocen la URL del backend y guardan el JWT que el servidor entrega al iniciar sesión.

## 3. Cómo configurar el entorno local

```bash
cd festivaltrack-backend
cp .env.example .env
# Edita .env con los valores que te comparta el equipo por un canal privado
# (nunca por issues, PRs, commits ni capturas).
npm ci
npx prisma generate
npm run seed
npm run start:dev
```

Para generar un `JWT_SECRET` aleatorio:

```bash
node -e "console.log(require('crypto').randomBytes(48).toString('base64url'))"
```

## 4. Controles en el repositorio

- `.gitignore` excluye `.env`, `.env.*` (excepto `.env.example`), `local.properties`, `build/` y `dist/`.
- El backend falla al arrancar si falta `JWT_SECRET`; ya no existe un valor por defecto en el código.
- El seed exige `SEED_ADMIN_PASSWORD`; ya no hay contraseñas de administrador fijas en el código.
- Las contraseñas de usuario se guardan con hash `bcrypt`.
- El CI (`.github/workflows/backend.yml`) usa un `JWT_SECRET` de prueba que no sirve fuera de las pruebas unitarias y no se conecta a la base real.
- Controles de la API, la app y sus pruebas: ver [docs/pruebas/seguridad.md](docs/pruebas/seguridad.md).

## 4.1 Dependencias y vulnerabilidades

| Módulo | Dónde se declaran | Cómo se instalan |
| :--- | :--- | :--- |
| Backend | `festivaltrack-backend/package.json` + `package-lock.json` | `npm ci` |
| Portal web | `festivaltrack-web/package.json` + `package-lock.json` | `npm ci` |
| Android (app, tv, wear, shared) | `gradle/libs.versions.toml` y `*/build.gradle.kts` | Android Studio o `./gradlew assembleDebug` (las descarga Gradle) |

**Revisión del 2026-11-11 (`npm audit`):**

| Módulo | Antes | Acción | Después (producción, `--omit=dev`) |
| :--- | :--- | :--- | :--- |
| Backend | 31 alertas (2 críticas: `handlebars`, `proxy-addr`; 8 altas, entre ellas `multer`, `body-parser`, `@nestjs/platform-express`) | `npm audit fix` sin cambios mayores; build y 51 pruebas en verde | **0** |
| Portal web | 8 altas (`axios`, `vite`, `react-router`, `postcss`…) | `npm audit fix`; compila sin errores | **0** |

**Alertas aceptadas (justificación):** el backend conserva 29 alertas bajas, moderadas y una alta (`brace-expansion`), todas **solo en herramientas de desarrollo** (`jest`, `ts-jest`, `@nestjs/cli`, `typescript-eslint`). Solo se usan para compilar y probar: el servidor en producción arranca con `node dist/main` y nunca carga esas herramientas ni les pasa datos de usuarios. Corregirlas exige subir versiones mayores (`jest` 30→29, `@nestjs/cli` 11→12), y eso se hará en un PR aparte con pruebas.

**Automatización:**
- `backend.yml` y `web.yml` corren `npm audit --omit=dev --audit-level=high`: el PR falla si una dependencia de producción tiene una vulnerabilidad alta o crítica.
- `.github/dependabot.yml` revisa cada semana npm (backend y web), Gradle y GitHub Actions, y abre PRs hacia `develop`.
- Las **alertas de Dependabot** se activan en *Settings → Code security* del repositorio (requiere permisos de administrador).

## 5. Registro de incidentes

| Fecha de detección | Hallazgo (sin valores) | Acción |
| :--- | :--- | :--- |
| 2026-10-14 | Cadena de conexión real de Neon en un `.env` y en un README del backend, en commits de julio y agosto de 2026. | Se dejó de versionar el `.env` (commit `53181ce`) y se quitó del README (commit `dcd727c`). |
| 2026-10-14 | Valor de respaldo de `JWT_SECRET` escrito en el código desde junio de 2026. | Se reemplazó por `getJwtSecret()`, que exige la variable (commit `dcd727c`). |
| 2026-11-11 | Contraseña fija del administrador en el seed, en dos servicios (`canciones`, `galeria`) y en los README. | Se movió a `SEED_ADMIN_PASSWORD`; los servicios ya no crean usuarios con contraseña fija. |
| _pendiente_ | Rotación de la contraseña de Neon y de `JWT_SECRET` en Railway. | _Fecha y responsable al completarse._ |
| _pendiente_ | Limpieza del historial de Git con `git filter-repo`. | _Fecha y responsable al completarse._ |

## 6. Qué hacer si se expone un secreto

1. **Rotarlo de inmediato** en el servicio correspondiente (Neon, Railway, consola de Anthropic). Borrar el archivo no basta: el valor sigue en el historial.
2. Avisar en privado al equipo y al docente, indicando solo el archivo y el commit, **sin copiar el valor**.
3. Quitarlo del historial con `git filter-repo` y hacer push forzado, previo acuerdo del equipo.
4. Registrar el incidente en la tabla anterior.

## 7. Reportar una vulnerabilidad

No abras un issue público. Escribe en privado a los integrantes del equipo (ver `README.md`) o usa **Security → Report a vulnerability** en GitHub.
