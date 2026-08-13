# 🌐 Módulo `festivaltrack-web` — Guía Paso a Paso y Código Documentado

> Portal web oficial del **Festival José Alfredo Jiménez**, desarrollado con **React 18**, **Vite**, **TypeScript** y **Vanilla CSS3 (Dark Gold Design System)**.

---

## 📋 Índice
1. [Requisitos Previos](#1-requisitos-previos)
2. [Estructura Completa del Proyecto](#2-estructura-completa-del-proyecto)
3. [Paso 1: Instalación de Dependencias](#paso-1-instalación-de-dependencias)
4. [Paso 2: Configuración del Cliente HTTP](#paso-2-configuración-del-cliente-http)
5. [Paso 3: Ejecución del Servidor de Desarrollo (Vite)](#paso-3-ejecución-del-servidor-de-desarrollo-vite)
6. [Paso 4: Guía de Navegación y Uso Paso a Paso](#paso-4-guía-de-navegación-y-uso-paso-a-paso)
7. [Paso 5: Compilación y Despliegue para Producción](#paso-5-compilación-y-despliegue-para-producción)
8. [Paso 6: Código Fuente Explicado y Documentado](#paso-6-código-fuente-explicado-y-documentado)
9. [Paso 7: Solución de Problemas Frecuentes](#paso-7-solución-de-problemas-frecuentes)

---

## 1. Requisitos Previos

- **Node.js**: v18.0.0 o superior (`node -v`).
- **npm**: v9.0.0 o superior (`npm -v`).
- **Backend NestJS**: Ejecutándose en `http://localhost:3001` (ver guía de `festivaltrack-backend`).

---

## 2. Estructura Completa del Proyecto

```
festivaltrack-web/
│
├── index.html              # HTML con contenedores e inclusión de meta etiquetas SEO.
├── vite.config.ts          # Configuración del servidor de desarrollo Vite (puerto 5173).
│
└── src/
    ├── main.tsx            # Punto de montaje de la aplicación React en el DOM.
    ├── App.tsx             # Enrutador principal con React Router v6.
    ├── index.css           # Sistema de diseño CSS global (Dark Gold Design System).
    │
    ├── pages/
    │   ├── Home.tsx        # ⭐ Portada interactiva: Hero banner, agenda y chat en vivo.
    │   ├── Login.tsx       # Formulario de acceso.
    │   ├── Register.tsx    # Formulario de nuevo registro.
    │   ├── Events.tsx      # Catálogo completo de eventos.
    │   ├── Checkout.tsx    # Proceso de compra de entradas en línea.
    │   ├── MyTickets.tsx   # Consulta de boletos digitales adquiridos.
    │   └── MapRoute.tsx    # Mapa con escenarios en Dolores Hidalgo.
    │
    └── services/
        └── api.ts          # ⭐ Cliente HTTP centralizado para peticiones REST.
```

---

## Paso 1: Instalación de Dependencias

1. Abre una terminal de comandos.
2. Navega al directorio del portal web:
   ```bash
   cd festivaltrack-web
   ```
3. Instala las dependencias necesarias:
   ```bash
   npm install
   ```

---

## Paso 2: Configuración del Cliente HTTP

El cliente (`src/services/api.ts`) apunta por defecto al backend local:
`http://localhost:3001/api/v1`

Los tokens de sesión se guardan automáticamente en `localStorage` tras iniciar sesión.

---

## Paso 3: Ejecución del Servidor de Desarrollo (Vite)

Ejecuta el servidor de desarrollo:

```bash
npm run dev
```

Abre tu navegador en `http://localhost:5173`.

---

## Paso 4: Guía de Navegación y Uso Paso a Paso

1. **Home**: Disfruta del Hero Banner de Dolores Hidalgo, consulta los conciertos destacados y participa en el chat comunitario.
2. **Iniciar Sesión**: Haz clic en el botón de acceso. El token devuelto por el backend se guardará en `localStorage`.
3. **Comprar Boletos**: Navega a *Events*, selecciona la cantidad de entradas y completa el pago en la vista *Checkout*.
4. **Consultar Boletos**: Revisa tus entradas con código QR digital guardadas en *My Tickets*.

---

## Paso 5: Compilación y Despliegue para Producción

Para compilar el proyecto optimizado y minificado:

```bash
npm run build
```

La salida final se generará en la carpeta `dist/` lista para ser desplegada en Vercel, Netlify o Nginx.

---

## Paso 6: Código Fuente Explicado y Documentado

### 1. Polling de Chat en Tiempo Real (`Home.tsx`)
```typescript
// Polling automático cada 3 segundos para sincronizar comentarios del chat
useEffect(() => {
  const fetchMessages = async () => {
    try {
      const res = await fetch('http://localhost:3001/api/v1/stream/chat/EVT-001');
      if (res.ok) {
        const data = await res.json();
        setChatMessages(data);
      }
    } catch (e) {
      console.error('Error al actualizar chat:', e);
    }
  };

  fetchMessages();
  const interval = setInterval(fetchMessages, 3000);
  return () => clearInterval(interval);
}, []);
```

### 2. Gestión de Sesión en Cliente HTTP (`src/services/api.ts`)
```typescript
// Envío del token JWT en peticiones autenticadas
export const getHeaders = () => {
  const token = localStorage.getItem('token');
  return {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {})
  };
};
```

---

## Paso 7: Solución de Problemas Frecuentes

### Los eventos o el chat no cargan en la web
- Confirma que el servidor backend NestJS esté corriendo en `http://localhost:3001` y la base PostgreSQL esté conectada.
