# Reporte técnico · Secciones actualizadas (#41)

El reporte *Propuesta y Planeación Técnica* (septiembre 2026) se escribió antes de construir los módulos de la
Unidad II y menciona tecnologías que el proyecto no usa: Firebase (Firestore, Auth, Storage, Cloud Functions y
Cloud Messaging), streaming HLS y Provider/Riverpod. Este documento trae el texto de reemplazo de las secciones
**12, 14 y 15**, alineado con el código de la rama `develop` al 10/11/2026. Los cambios se pasan al PDF final del reporte.

## Resumen de cambios

| Sección | Decía | Dice ahora | Dónde se comprueba |
|---|---|---|---|
| 12 | ViewModel con Provider / Riverpod | ViewModel de Jetpack con `StateFlow` | `app/.../ui/viewmodels/` |
| 12 | Repository hacia Firestore | Repository hacia la API REST propia y caché local Room | `shared/.../data/` |
| 14 | Firebase (Firestore, Auth, Storage) | NestJS + Prisma + PostgreSQL en Neon | `festivaltrack-backend/` |
| 14 | ARCore / ARKit + ML Kit o Cloud Vision | CameraX + reconocimiento visual con IA en el backend | `ArScannerScreen.kt`, `festivaltrack-backend/src/ar/` |
| 14 | HLS | Media3 (ExoPlayer) con RTSP | `UserLiveStreamScreen.kt`, `TvLiveStreamScreen.kt` |
| 14 | Firebase Cloud Functions | Módulos de NestJS (boletos, QR, chat, AR) | `festivaltrack-backend/src/` |
| 14 | Apple CarPlay | Solo Android Auto (Car App Library); CarPlay queda fuera de alcance | `app/.../auto/` |
| 15 | Firebase Authentication | Registro e inicio de sesión propios con JWT y bcrypt, roles USUARIO / ADMINISTRADOR | `festivaltrack-backend/src/auth/` |
| 15 | Firebase Cloud Messaging | Recordatorios locales en el reloj y avisos por voz en el auto | `wear/.../ScheduleAlertasUseCase.kt`, `AlertaSalidaScreen.kt` |

---

## 12. Arquitectura propuesta

Se utiliza una arquitectura **MVVM (Model-View-ViewModel)** sobre Android nativo con Kotlin y Jetpack Compose,
conectada a un **backend REST propio** desarrollado en NestJS.

**¿Por qué es adecuada para el proyecto?**

- Jetpack Compose dibuja la interfaz a partir del estado. El ViewModel expone ese estado con `StateFlow` y la
  pantalla se actualiza sola cuando cambia.
- Separa la lógica de negocio del renderizado. Eso permite que módulos como la realidad aumentada y el modo
  automóvil se desarrollen y prueben por separado.
- La lógica de cálculo vive en clases puras, sin dependencias de Android, y se cubre con pruebas unitarias.
  Ejemplos: `SalidaCalculator`, `DestinosCalculator`, `NavegacionCalculator` y `ResumenArCalculator`.

**Organización de los componentes**

Cada módulo de la aplicación (Boletos, Transmisión, Realidad Aumentada, Galería, Demos de audio y Modo automóvil)
tiene su propia pantalla (View) y su ViewModel. Accede a los datos por medio de un Repository que combina la
**API REST** del backend (Retrofit) con una **caché local** en Room. Así la app sigue mostrando información aunque
se pierda la señal dentro del recinto.

El modo automóvil no usa Compose. Android Auto solo permite **plantillas de la Car App Library** (`ListTemplate`,
`PaneTemplate`), que el auto dibuja con su propio estilo para que la interfaz sea segura al conducir.

**Diagrama de arquitectura**

| Capa | Responsabilidad |
|---|---|
| View | Pantallas en Jetpack Compose (Boletos, Biografía, Cámara AR, Galería, Elige tu destino) y plantillas de Android Auto (Home, Navegación, Alerta de salida). |
| ViewModel | Estado observable con `StateFlow` y lógica de presentación (ViewModel de Jetpack). |
| Repository | Abstrae el acceso a datos: API REST (Retrofit), caché local (Room) y sesión (`TokenManager`). |
| Backend / API | NestJS + Prisma sobre PostgreSQL en Neon: autenticación JWT, eventos, boletos con QR, chat, galería-foro y reconocimiento AR. |
| Servicios externos | Reconocimiento visual con IA (detrás de la interfaz `ReconocedorVisual`), mapas OSMDroid / OSRM y navegador del auto (Google Maps / Waze). |

```
 App Android (Compose / Car App Library)
        │  View ⇄ ViewModel (StateFlow)
        ▼
   Repository ── Room (caché local)
        │ Retrofit + JWT
        ▼
 Backend NestJS ── Prisma ── PostgreSQL (Neon)
        │
        └── Reconocimiento visual (IA) para el módulo AR
```

---

## 14. Framework y tecnologías

| Categoría | Tecnología | Uso en el proyecto |
|---|---|---|
| Desarrollo móvil | Android Studio · Kotlin · Jetpack Compose | App nativa para Android (teléfono), Wear OS y Android TV. |
| Arquitectura | MVVM con ViewModel de Jetpack y `StateFlow` | Estado de cada pantalla y lógica de presentación. |
| Entorno de desarrollo | Android Studio | Depuración, emuladores (teléfono, reloj y TV), Desktop Head Unit para Android Auto y Logcat. |
| Diseño | Figma | Prototipado de las pantallas. |
| Backend | NestJS (Node.js + TypeScript) | API REST: autenticación, eventos, boletos con QR, chat en vivo, galería-foro, sitios AR e historial. |
| Base de datos | PostgreSQL en Neon + Prisma ORM | Datos de usuarios, eventos, boletos, canciones, sitios culturales y publicaciones; migraciones con Prisma. |
| Persistencia local | Room | Caché de eventos y contenido para uso sin conexión. |
| Consumo de la API | Retrofit 2 + Gson | Cliente HTTP compartido en el módulo `shared`. |
| Realidad aumentada | CameraX + reconocimiento visual con IA en el backend | La app captura la foto del monumento y el backend la compara con el catálogo de sitios (`SitioAR`). |
| Integración vehicular | Android Auto · Car App Library | Pantallas de Home, Navegación y Alerta de salida con plantillas aprobadas para manejo seguro. |
| Mapas y rutas | OSMDroid · OSRM | Mapa del recinto, ubicación de escenarios y cálculo de rutas. |
| Streaming y audio | Media3 (ExoPlayer) con RTSP | Transmisión en vivo desde la cámara del administrador y reproducción de audio. |
| Seguridad | JWT (Passport) · bcrypt · CORS restringido | Sesión por token, contraseñas cifradas y endpoints de administrador protegidos por rol. |
| Integración continua | GitHub Actions | Pruebas del backend y compilación de la app en cada Pull Request; burndown diario del sprint. |

> **Fuera de alcance:** Apple CarPlay y la versión iOS. El equipo desarrolla en Android nativo; la integración
> vehicular se entrega con Android Auto.

---

## 15. Posibles servicios y APIs

| Pregunta | Respuesta | Detalle |
|---|---|---|
| ¿La aplicación necesitará autenticación? | Sí | Registro e inicio de sesión propios en el backend con JWT y contraseñas cifradas con bcrypt. Roles USUARIO y ADMINISTRADOR. |
| ¿Almacenará información en la nube? | Sí | PostgreSQL en Neon para los datos estructurados; imágenes y audio servidos por URL desde el backend. |
| ¿Consumirá una API externa? | Sí | Servicio de reconocimiento visual con IA para el módulo AR, OSRM para rutas y el navegador del auto (Google Maps / Waze) mediante `CarContext.ACTION_NAVIGATE`. |
| ¿Podría utilizar geolocalización? | Sí | Ubicación del teléfono para calcular distancia y tiempo a cada escenario, y la hora de salida recomendada. |
| ¿Requerirá notificaciones? | Sí | Recordatorios programados en el reloj (Wear OS) y avisos leídos por voz en el auto (Text-to-Speech). |
| ¿Utilizará cámara, GPS u otro sensor? | Sí | Cámara (CameraX) para la realidad aumentada y el escáner de boletos QR; GPS para la navegación hacia el recinto. |

---

## Cómo aplicarlo al PDF

1. Reemplazar el texto de las secciones 12, 14 y 15 por el de este documento.
2. En el **Pitch del proyecto**, cambiar *"Repository hacia Firebase"* por *"Repository hacia una API REST propia
   (NestJS + PostgreSQL)"* y *"Firebase, ARCore/ARKit … streaming HLS"* por *"NestJS, PostgreSQL (Neon), CameraX,
   Android Auto Car App Library y Media3 con RTSP"*.
3. En la sección 13 (Patrones), el renglón **Repository** debe decir *"Evento, Boleto y Pago (API REST)"* en lugar
   de *"Firebase / API REST"*.
4. Aplicar los mismos cambios en la diapositiva de tecnologías de la presentación.
