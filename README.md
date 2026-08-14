## Videos de Demostración

<p align="center">
  <!-- Video Presidencia -->
  <a href="https://drive.google.com/file/d/1dLFeqdP5TnHkWoP2MHVgiJjMQGENU93T/view?usp=sharing" target="_blank">
    <img width="45%" src="https://github.com/user-attachments/assets/c293ed36-3619-4b3c-909d-5f65cc7f6f1a" alt="Ver Video Presidencia" />
  </a>
  &nbsp;&nbsp;&nbsp;&nbsp;
  <!-- Video Exposición -->
  <a href="https://drive.google.com/file/d/12nPtrItlh9Xz9OyqE5wiD0VzrLD60jWl/view?usp=sharing" target="_blank">
    <img width="45%" src="https://github.com/user-attachments/assets/e2107fb9-f226-4423-8b4b-d3b74d1a1240" alt="Ver Video Exposición" />
  </a>
</p>
<p align="center">
  <i>▶️ Haz clic en las imágenes para reproducir los videos en Google Drive.</i>
</p>

# 🎺 FestivalTrack — Plataforma Multiplataforma del Festival José Alfredo Jiménez

> **FestivalTrack** es un ecosistema multiplataforma integral diseñado para la gestión, promoción, compra de boletos digitales, consulta cultural y transmisión en vivo en tiempo real del **Festival José Alfredo Jiménez** en Dolores Hidalgo, Cuna de la Independencia Nacional, Guanajuato.

---

## 📌 Resumen General del Sistema

La solución permite conectar a organizadores, visitantes presenciales, espectadores remotos y usuarios de Smart TV / Wearables mediante una arquitectura desacoplada y sincronizada en tiempo real.

```
                                  ┌─────────────────────────────┐
                                  │      PORTAL WEB ADMIN/USER  │
                                  │    (React + Vite + TS)      │
                                  └──────────────┬──────────────┘
                                                 │
                                                 │ HTTP / REST (3001)
                                                 ▼
┌─────────────────────────┐       ┌─────────────────────────────┐       ┌─────────────────────────┐
│     APLICACIÓN MÓVIL    │       │       BACKEND CENTRAL       │       │    APLICACIÓN SMART TV  │
│  (Android / Kotlin UI)  │◄─────►│    (NestJS + Prisma ORM)    │◄─────►│   (Android TV / Compose)│
│  - Usuarios & Admins    │ REST  │     PostgreSQL en Neon      │ REST  │  - Transmisión RTSP     │
└────────────┬────────────┘       └─────────────────────────────┘       └─────────────────────────┘
             │                                                                       ▲
             │                       RTSP Stream (Puerto 1935)                       │
             └───────────────────────────────────────────────────────────────────────┘
```

---

## 📱 Módulos del Sistema

| Módulo | Tipo | Descripción General | Documentación y Paso a Paso |
| :--- | :--- | :--- | :--- |
| **[`app/`](./app/README.md)** | Móvil (Android) | Aplicación nativa para usuarios (boletos, mapa, audio, live stream) y administradores (cámara RTSP, escáner QR, CRUD de eventos). | [📄 Guía Completa y Código `app`](./app/README.md) |
| **[`tv/`](./tv/README.md)** | Smart TV (Android TV) | Experiencia en pantalla grande con reproducción de stream RTSP, chat comunitario con control remoto (D-Pad) y galerías. | [📄 Guía Completa y Código `tv`](./tv/README.md) |
| **[`wear/`](./wear/README.md)** | Wearable (WearOS) | Aplicación para reloj inteligente optimizada para pantallas circulares con agenda y notificaciones del festival. | [📄 Guía Completa y Código `wear`](./wear/README.md) |
| **[`shared/`](./shared/README.md)** | Librería Kotlin | Capa de datos compartida entre Android (Retrofit API, DTOs, Room Database, Repositorio Offline-First). | [📄 Guía Completa y Código `shared`](./shared/README.md) |
| **[`festivaltrack-backend/`](./festivaltrack-backend/README.md)** | Backend REST | API REST desarrollada en NestJS con Prisma ORM, autenticación JWT con bcrypt y base de datos PostgreSQL en la nube (Neon). | [📄 Guía Completa y Código `backend`](./festivaltrack-backend/README.md) |
| **[`festivaltrack-web/`](./festivaltrack-web/README.md)** | Portal Web (React) | Aplicación web interactiva para exploración de catálogo, compra en línea de boletos, visualización del directo y administración. | [📄 Guía Completa y Código `web`](./festivaltrack-web/README.md) |

---

## 🖥️ Pantallas y Vistas Principales del Sistema

### 1. Aplicación Móvil (`app/`)
- **`LoginScreen` & `RegisterScreen`**: Autenticación segura basada en JWT. Distingue automáticamente entre roles `USER` y `ADMIN`.
- **`DashboardScreen`**: Portada principal con banner del monumento en Dolores Hidalgo, próximos eventos, reproductor de audio e información cultural.
- **`TicketsScreen` & `CheckoutScreen`**: Selección de categoría (*General, VIP*), cálculo de total en MXN, formulario con encriptación SSL simulada y emisión de boleto digital con código QR.
- **`TicketSuccessScreen`**: Confirmación visual de compra exitosa con QR almacenable.
- **`UserLiveStreamScreen`**: Reproducción de la señal en vivo en tiempo real con chat interactivo.
- **`AdminLiveStreamScreen`**: Panel donde el administrador usa la cámara física del smartphone para transmitir video en formato RTSP en el puerto `1935`.
- **`AdminScannerScreen`**: Escáner de cámara para validar códigos QR en el acceso del recinto y prevenir reutilización.
- **`AdminManageScreen` & `AdminCreateEventScreen`**: CRUD completo para publicar, actualizar o cancelar eventos en la agenda.

### 2. Aplicación Smart TV (`tv/`)
- **`TvLiveStreamScreen`**: Pantalla dividida (68% reproductor ExoPlayer RTSP + 32% panel de chat comunitario con refresco automático cada 3 segundos).
- **`TvChatInputDialog`**: Cuadro de diálogo modal adaptado para D-Pad que permite desplegar el teclado en pantalla sin requerir toque o ratón.
- **`TvHomeScreen` & `TvHistoricalGalleryScreen`**: Carrusel de imágenes y agenda de actividades para pantallas grandes de alta definición.
- **`TvSettingsScreen`**: Panel de información técnica del dispositivo y configuración del servidor API.

### 3. Portal Web (`festivaltrack-web/`)
- **`Home.tsx`**: Portada responsive con Hero animado, tarjetas de eventos en vivo y barra lateral de chat público en tiempo real.
- **`Events.tsx`**: Catálogo filtrable de todos los conciertos y serenatas.
- **`Checkout.tsx` & `MyTickets.tsx`**: Proceso de compra web e historial de boletos adquiridos guardados en la cuenta del usuario.
- **`MapRoute.tsx`**: Mapa geográfico e interactivo con la ubicación de escenarios en Dolores Hidalgo.

---

## 👥 Credenciales de Prueba por Defecto

| Rol | Correo Electrónico | Contraseña | Permisos y Acceso |
| :--- | :--- | :--- | :--- |
| **Administrador** | `admin@admin.com` | `admin123` | Emisión de streaming por cámara, escaneo QR, CRUD de eventos, gestión total. |
| **Usuario Espectador** | *(Cualquier nuevo registro)* | *(Elegida por el usuario)* | Compra de entradas, chat comunitario, reproductor musical y transmisión. |

---

## 🛠️ Tecnologías y Stack Técnico Global

- **Android Móvil / TV / Wear**: Kotlin, Jetpack Compose, Compose TV, ExoPlayer (Media3 RTSP), Retrofit 2, Room DB, Accompanist Permissions.
- **Backend API**: Node.js, NestJS, TypeScript, Prisma ORM, Passport JWT, Bcrypt.
- **Base de Datos**: PostgreSQL Serverless (Neon Cloud Database).
- **Web App**: React 18, Vite, TypeScript, Dark Gold Design System CSS3.
- **Protocolo de Video Streaming**: RTSP (Real-Time Streaming Protocol) en puerto `1935`.

---

## 🚀 Guías de Instalación y Ejecución Paso a Paso

Para desplegar y ejecutar cada módulo paso a paso con su código documentado, consulta su documento correspondiente:

1. [📄 Guía Paso a Paso Backend NestJS + PostgreSQL](./festivaltrack-backend/README.md)
2. [📄 Guía Paso a Paso App Móvil Android](./app/README.md)
3. [📄 Guía Paso a Paso App Smart TV Android](./tv/README.md)
4. [📄 Guía Paso a Paso Portal Web React](./festivaltrack-web/README.md)
5. [📄 Guía Paso a Paso Librería Shared](./shared/README.md)
6. [📄 Guía Paso a Paso Módulo WearOS](./wear/README.md)

## Evidencia Modulo Móvil

<p align="center">
  <img width="22%" src="https://github.com/user-attachments/assets/397fcb0e-1236-46b4-99c5-cb054c3302cd" />&nbsp;&nbsp;
  <img width="22%" src="https://github.com/user-attachments/assets/c1efb6e5-76c3-423b-bc55-bcac175be64a" />&nbsp;&nbsp;
  <img width="22%" src="https://github.com/user-attachments/assets/4ddf2be9-a215-4a2b-b85a-ccb3d9764d3e" />&nbsp;&nbsp;
  <img width="22%" src="https://github.com/user-attachments/assets/65468143-07b2-4893-bec6-efdcc07fdb3c" />
  <br><br>
  <img width="22%" src="https://github.com/user-attachments/assets/4ad0b104-938d-4d5b-bfee-b1c558b7c3f9" />&nbsp;&nbsp;
  <img width="22%" src="https://github.com/user-attachments/assets/220addd5-7eea-4a5d-bd68-cf92fe373c78" />&nbsp;&nbsp;
  <img width="22%" src="https://github.com/user-attachments/assets/ee424e38-0866-4da7-8cce-f2c7b02d67fc" />&nbsp;&nbsp;
  <img width="22%" src="https://github.com/user-attachments/assets/b5b351e4-6153-4e5b-8091-50f604320275" />
  <br><br>
  <img width="22%" src="https://github.com/user-attachments/assets/caa75b59-616b-478c-834e-18db20ed40b3" />&nbsp;&nbsp;
  <img width="22%" src="https://github.com/user-attachments/assets/9c16d7c7-fb4e-4d58-b331-db464f5a4675" />&nbsp;&nbsp;
  <img width="22%" src="https://github.com/user-attachments/assets/dd97d986-2aa6-4139-91dc-552ed7c58a08" />&nbsp;&nbsp;
  <img width="22%" src="https://github.com/user-attachments/assets/b6351086-fdaa-4a28-b5fb-1e302afa21f7" />
  <br><br>
  <img width="22%" src="https://github.com/user-attachments/assets/f208c15e-ec54-4b35-84a9-5b0646f57032" />
</p>

## Evidencia Modulo Wearable

<p align="center">
  <img width="28%" src="https://github.com/user-attachments/assets/a921311b-74f2-4bda-945d-7e5931467a42" />&nbsp;&nbsp;
  <img width="28%" src="https://github.com/user-attachments/assets/38bcefcf-7db3-4744-830d-bd1fad621563" />&nbsp;&nbsp;
  <img width="28%" src="https://github.com/user-attachments/assets/0d3a0533-6899-4ef7-946b-b0383d1ad678" />
  <br><br>
  <img width="28%" src="https://github.com/user-attachments/assets/6afab30e-2cfc-4773-9d5e-8e54d4d0f4a5" />&nbsp;&nbsp;
  <img width="28%" src="https://github.com/user-attachments/assets/26b47a6c-cdef-4886-bdcf-e707ebe97b87" />
</p>

## Evidencia Modulo Smart TV

<p align="center">
  <img width="45%" src="https://github.com/user-attachments/assets/1ecb50ae-1741-4c3d-945a-8302fef61e8a" />&nbsp;&nbsp;
  <img width="45%" src="https://github.com/user-attachments/assets/164daac9-406e-4b8a-ba22-cdd9af7297a6" />
  <br><br>
  <img width="45%" src="https://github.com/user-attachments/assets/1e471c0a-14b2-45cf-ab0f-e7a90d997436" />&nbsp;&nbsp;
  <img width="45%" src="https://github.com/user-attachments/assets/e7cbca18-8abb-4776-bee5-bc79cefbe1b0" />
  <br><br>
  <img width="45%" src="https://github.com/user-attachments/assets/2d9834c5-eb9f-4d50-a3de-848d1a55036d" />&nbsp;&nbsp;
  <img width="45%" src="https://github.com/user-attachments/assets/abf56b3c-7e0c-4974-9ec2-30d360b8ee7d" />
</p>




