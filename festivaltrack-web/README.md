# festivaltrack-web

Portal web del **Festival José Alfredo Jiménez** (FestivalTrack): cartelera de eventos, compra de boletos (pago simulado), boletos con QR y mapa con la ruta a pie hasta el escenario.

**Stack:** React 19 · Vite 8 · TypeScript 6 · React Router 7 · axios · Leaflet + react-leaflet · Swiper · qrcode.react · Roboto autoalojada (`@fontsource/roboto`) · Vitest + Testing Library. Se sirve en producción con [`serve`](https://github.com/vercel/serve) (versión fijada en `package.json`).

## Requisitos

- Node.js 20 o superior.
- El backend `festivaltrack-backend` en marcha (local en `http://localhost:3001` o desplegado en Render).

## Variables de entorno

Copia `.env.example` a `.env.local` y ajusta:

| Variable | Obligatoria | Descripción |
| --- | --- | --- |
| `VITE_API_URL` | Sí en producción | URL base de la API, p. ej. `https://<tu-backend>.onrender.com/api/v1`. Debe ser `https` (se admite `http` solo para `localhost`). |

- Con `npm run dev` y sin `VITE_API_URL` se usa `http://localhost:3001/api/v1`.
- En un build de producción sin `VITE_API_URL` (o con un valor inválido) la web **no** cae en localhost: muestra una pantalla "FestivalTrack no está configurado".
- La variable se incrusta en el bundle al compilar: si cambia, hay que volver a compilar.
- El backend solo acepta peticiones CORS de `localhost` y del origen configurado en su `FRONTEND_URL`.

## Scripts

| Script | Qué hace |
| --- | --- |
| `npm run dev` | Servidor de desarrollo en `http://localhost:5173`. |
| `npm run build` | `tsc -b` + `vite build` a `dist/` (incluye `dist/serve.json` con la CSP ajustada, ver abajo). |
| `npm run lint` | oxlint. |
| `npm test` | Pruebas con Vitest (`vitest run`, jsdom; la red se simula, nunca se llama al backend). |
| `npm run test:watch` | Vitest en modo interactivo. |
| `npm run preview` | Vista previa de Vite en `:4173` (sin las cabeceras de `serve.json`). |
| `npm start` | Sirve `dist/` con `serve` en `0.0.0.0:$PORT` (3000 si no hay `PORT`). Pensado para Linux (Nixpacks/Railway/Render). |

## Estructura

```
src/
  main.tsx                 Montaje; importa fuentes y estilos; pantalla de error si falta configuración
  App.tsx                  Rutas (lazy), ErrorBoundary por ruta, RequireAuth, 404; `Aplicacion` = layout + rutas
  styles/tokens.css        Variables de diseño (colores, tipografía, radios, espacios)
  styles/base.css          Reset, tipografía, :focus-visible, "Saltar al contenido", prefers-reduced-motion
  styles/components.css    Estilos de src/components/ui
  styles/layout.css        Cabecera, navegación (inferior en móvil), pie
  styles/pages.css         Estilos de las páginas
  components/ui/           Presentación: AppLayout, Button/ButtonLink, Field, Card, Badge, Alert, Skeleton,
                           Spinner, StatusMessage, QuantityStepper, ConfirmDialog (<dialog>), Icons
  components/              EventoCard, MapaRuta, ErrorBoundary, NotFound, ErrorDeConfiguracion
  config/env.ts            VITE_API_URL validada
  config/precios.ts        Categorías a la venta (VIP, GENERAL) y precios (deben coincidir con el backend)
  lib/http.ts              Cliente axios: baseURL, timeout 20 s, token y manejo de 401
  lib/errores.ts           mensajeDeError(): convierte errores de red/HTTP en mensajes en español
  lib/formato.ts           Fechas (es-MX, hora del centro de México), precios "$4,500 MXN", folios
  lib/fechaEvento.ts       Día/mes y hora para las tarjetas de evento
  lib/proximos.ts          Filtro y orden de "próximos eventos" (lo usan Inicio y Eventos)
  lib/tarjeta.ts           Validación de formato de tarjeta (Luhn, MM/AA, CVV) para el pago simulado
  lib/recargaPorDespliegue.ts  Manejo de `vite:preloadError` (una recarga, sin bucles)
  auth/session.ts          Sesión en localStorage (clave única), caducidad del JWT, sincronía entre pestañas
  auth/useAuth.ts          Hook: usuario, autenticado, iniciarSesion, registrarse, cerrarSesion
  auth/RequireAuth.tsx     Protege rutas; manda a /login recordando la ruta de origen
  auth/rutas.ts            Validación de la ruta de retorno (solo rutas internas)
  services/                auth, eventos, boletos y rutas (OSRM); tipos y validación de respuestas
  hooks/                   useRecurso (carga con cancelación y reintento), useTituloPagina, useMovimientoReducido
  pages/                   Home, Login, Register, Events, MapRoute, Checkout, MyTickets
  flujos/*.test.tsx        Pruebas de integración de los flujos críticos
  test/                    Utilidades de prueba: setup, red simulada, datos, render
public/
  serve.json               Rewrites de SPA y cabeceras de seguridad/caché para `serve`
  images/                  Imágenes del carrusel (WebP)
```

## Diseño

El diseño replica el de la app móvil (`app/.../ui/theme/Color.kt` y sus pantallas): tema oscuro, dorado de marca, tarjetas verdes oscuras y casi sin sombras (la jerarquía va por color de fondo y borde de 1 px).

- **Tokens:** todo color, radio, espacio y tamaño de letra sale de `src/styles/tokens.css`. No hay colores escritos en componentes ni estilos en línea (solo el alto/ancho dinámico de `Skeleton`). Para cambiar la marca basta editar ese archivo.
- **Paleta:** fondo `#121212`, tarjeta `#1E2720`, destacado `#162219`, borde `#2A3A2E`, primario dorado `#E6C27A` (hover `#C4A059`), texto secundario `#AAAAAA`, error `#FF8A80`, peligro `#C51111`, éxito `#4CAF50`.
- **Tipografía:** Roboto 400/500/700, autoalojada con `@fontsource/roboto` (subconjunto latino). Escala: eyebrow 12 px en mayúsculas, cuerpo 15–16, sección 18, página 22, precio 24–28, hero fluido con `clamp()`.
- **Componentes:** viven en `src/components/ui`. Los llamados a la acción principales (`INICIAR SESIÓN`, `COMPRAR BOLETOS`, `CONFIRMAR PAGO · $X MXN`…) usan botón dorado de 56 px en mayúsculas.
- **Responsivo:** móvil primero. Por debajo de 640 px aparece una barra de navegación inferior; desde 640 px la navegación va en la cabecera; el checkout pasa a dos columnas desde 960 px.
- **Accesibilidad:** foco dorado de 2 px, enlace "Saltar al contenido", `aria-current` en la navegación, errores con `role="alert"` y estados con `role="status"`, objetivos táctiles de al menos 44 px, un `h1` por página, diálogo de confirmación con `<dialog>` nativo y `prefers-reduced-motion` respetado (también el autoplay del carrusel).
- **CSP:** solo CSS en archivos, fuentes e imágenes propias y teselas de OSM; no hay CDN, `<style>` en línea ni librerías CSS-in-JS.

## Pruebas

`npm test` ejecuta Vitest con jsdom y Testing Library. La red se simula sustituyendo el adaptador del cliente axios real (`src/test/red.ts`), de modo que también se prueban el token y el manejo de 401, y nunca se toca el backend.

- **Unitarias:** `lib/tarjeta`, `lib/formato`, `lib/errores`, `lib/proximos`, `auth/rutas`, `auth/session`.
- **Integración (`src/flujos`):** ruta protegida y regreso a `from`, login (errores, 429), registro, cerrar sesión con confirmación, 404, cartelera (lista, vacío, error con reintento), inicio, checkout (total, validación de tarjeta, una petición por boleto con `cantidad: 1`, fallo parcial, éxito) y mis boletos (QR solo en boletos ACTIVOS, sin id completo).

## Rutas

| Ruta | Página | Sesión |
| --- | --- | --- |
| `/` | Inicio | — |
| `/login`, `/register` | Acceso y registro (el registro entra directamente con la sesión iniciada) | — |
| `/events` | Cartelera de próximos eventos | — |
| `/events/:id/map` | Mapa y ruta a pie hasta el evento | — |
| `/checkout/:eventoId` | Compra de boletos VIP/GENERAL (pago simulado) | Sí |
| `/mis-boletos` | Boletos con QR (solo los activos muestran QR) | Sí |
| `/events/map` | Redirige a `/events` (ruta antigua) | — |
| cualquier otra | Página no encontrada | — |

## Notas de funcionamiento

- **Compras:** con `cantidad > 1` el backend crea un único boleto (un QR) por precio × cantidad, así que la web hace una compra de 1 boleto por cada uno, en serie. Si una falla, se informa cuántos se compraron y se puede reintentar solo lo que faltó.
- **Pago simulado:** los datos de tarjeta solo se validan en el navegador; nunca se envían al backend ni se guardan.
- **Mapa:** teselas de `tile.openstreetmap.org` y ruta a pie con el OSRM de FOSSGIS (`routing.openstreetmap.de`, perfil *foot*). La ubicación del usuario solo se envía a ese servicio. Si se niega la ubicación, el mapa se muestra igual centrado en el evento.
- **Sesión:** si el backend responde 401 a una petición con token, la sesión se cierra y se pide iniciar sesión de nuevo.

## Despliegue y cabeceras

`npm start` ejecuta `serve dist`, que lee `dist/serve.json` (copiado de `public/serve.json`):

- **SPA:** cualquier ruta sin archivo devuelve `index.html`, excepto `/assets/**`: un chunk inexistente responde 404 en vez de HTML (así un deploy nuevo no rompe con "MIME type text/html").
- **Caché:** `assets/**` → `public, max-age=31536000, immutable` (nombres con hash); `index.html` → `no-cache`.
- **Seguridad:** `Content-Security-Policy`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy` (geolocalización solo para el propio sitio), `Strict-Transport-Security` y `Cross-Origin-Opener-Policy`.

### CSP y el origen del backend

`connect-src` necesita el origen del backend, que es configurable. `public/serve.json` trae el comodín `https://*.onrender.com`; durante `npm run build` el plugin `festivaltrack:csp-backend` de `vite.config.ts` lo reemplaza en `dist/serve.json` por el origen exacto de `VITE_API_URL` (p. ej. `https://festivaltrack-backend.onrender.com`). Si se compila sin `VITE_API_URL`, se queda el comodín y Vite avisa.

Si se cambia de servicio de rutas o de teselas, hay que actualizar `connect-src` / `img-src` en `public/serve.json`.

> Si el frontend se publica como *Static Site* de Render (sin `serve`), `serve.json` no se aplica: las mismas cabeceras y la regla de rewrite hay que configurarlas en el panel de Render.

### Nixpacks (Railway u otros)

`nixpacks.toml` instala con `npm ci`, compila con `npm run build` y arranca con `npm start`. Define `VITE_API_URL` como variable del servicio **antes** de compilar.
