# 🎸 FestivalTrack - Proyecto Integral

¡Bienvenidos al repositorio oficial de FestivalTrack!
Este proyecto es un sistema completo compuesto por un servidor backend robusto (NestJS) y una aplicación para relojes inteligentes (Wear OS) diseñada para guiar a los usuarios en los eventos del festival.

A continuación, encontrarás todas las instrucciones necesarias para que cualquier miembro del equipo pueda levantar el proyecto localmente sin problemas.

---

## 🏗️ Arquitectura del Proyecto

El repositorio está dividido en carpetas principales:
- `/festivaltrack-backend`: Contiene todo el código del servidor backend, base de datos y la API REST.
- `/wear`: Contiene el código fuente de la aplicación para relojes Wear OS (Kotlin/Jetpack Compose).
- `/shared`: Código compartido entre módulos.

---

## 🚀 1. Levantar el Backend (NestJS + Prisma + Neon)

El backend es el cerebro que alimenta los eventos del reloj. Usa Node.js y se conecta a una base de datos PostgreSQL en Neon.

### A. Instalación
Asegúrate de tener instalado [Node.js](https://nodejs.org/) (versión 18 o superior).
Abre una terminal, entra a la carpeta del backend y descarga las dependencias:

```bash
cd festivaltrack-backend
npm install
```

### B. Configuración de Base de Datos (.env)
Por seguridad, la contraseña de la base de datos nunca se sube a GitHub. Cada desarrollador debe configurar su archivo `.env` localmente.

1. Dentro de la carpeta `festivaltrack-backend`, crea un archivo llamado `.env`.
2. Pega la variable `DATABASE_URL` que te proporcionó el administrador. Debería verse así:
   ```env
   DATABASE_URL="postgresql://usuario:password@tu-host-neon.tech/tu-db?sslmode=require"
   ```

### C. Inicializar Prisma (Base de Datos)
Con tu `.env` listo, necesitas sincronizar Prisma para que lea la estructura de la base de datos y genere el código cliente.

```bash
# Estando dentro de la carpeta festivaltrack-backend
npx prisma generate
```

> **⚠️ REGLA DE ORO PARA MIGRACIONES:**
> **Nunca** uses `npx prisma db push`. Si alguien hace un cambio en el esquema (añadir una tabla o campo en `schema.prisma`), siempre debes usar el sistema de migraciones para no borrar los datos de producción:
> ```bash
> npx prisma migrate dev --name descripcion_de_tu_cambio
> ```

### D. Ejecutar el Servidor
Para encender el servidor y empezar a programar:

```bash
npm run start:dev
```
Si todo sale bien, verás que el servidor arranca (usualmente en el puerto `3000` o `3001`).

---

## ⌚ 2. Levantar la Aplicación de Reloj (Wear OS)

La app del reloj se encarga de mostrar la ruta GPS y sincronizar los eventos guardados por el backend.

### Pasos para probar:
1. Abre este proyecto (`Jose_Alfredo`) directamente desde **Android Studio**.
2. Sincroniza Gradle (`Sync Project with Gradle Files`).
3. En la barra superior, asegúrate de seleccionar el módulo **`wear`**.
4. Conecta tu reloj físico por Wi-Fi/Bluetooth o crea un emulador de Wear OS en el *Device Manager*.
5. Dale al botón de **Play (Run)**.

### Características Actuales (Wear OS):
- **Ubicación GPS Real:** La aplicación solicita permisos y utiliza los servicios de Google Play (FusedLocationProviderClient) para darte tu ubicación exacta en el mapa.
- **Ruta Peatonal Inteligente:** El mapa traza la ruta hacia el evento y previene pantallazos grises limitando el zoom de OSMDroid a `19.0`.
- **Filtro de Horario Universal:** Utiliza UTC (`Instant.now()`) para garantizar que la alerta de "Próximos Eventos" solo muestre eventos futuros basándose en la fecha real.

---

## 🤝 Flujo de Trabajo (Git)
Cuando vayas a trabajar en una nueva característica:
1. Asegúrate de estar en tu rama (`git checkout ramaNoe` o crea una nueva).
2. Sube tus cambios con commits descriptivos.
3. Haz push a GitHub y crea un **Pull Request** para fusionar con `main`.
