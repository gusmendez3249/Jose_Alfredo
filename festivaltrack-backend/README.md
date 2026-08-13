# 🖥️ Módulo `festivaltrack-backend` — Guía Paso a Paso y Código Documentado

> Servidor de API REST desarrollado con **NestJS**, **TypeScript**, **Prisma ORM** y **PostgreSQL (Neon Cloud Database)**.

---

## 📋 Índice
1. [Requisitos Previos](#1-requisitos-previos)
2. [Estructura Completa del Proyecto](#2-estructura-completa-del-proyecto)
3. [Paso 1: Instalación de Dependencias](#paso-1-instalación-de-dependencias)
4. [Paso 2: Configuración de Variables de Entorno (.env)](#paso-2-configuración-de-variables-de-entorno-env)
5. [Paso 3: Sincronización y Migraciones de Base de Datos (Prisma)](#paso-3-sincronización-y-migraciones-de-base-de-datos-prisma)
6. [Paso 4: Sembrado de Datos de Prueba (Seed)](#paso-4-sembrado-de-datos-de-prueba-seed)
7. [Paso 5: Ejecución del Servidor Backend](#paso-5-ejecución-del-servidor-backend)
8. [Paso 6: Guía de Pruebas de Endpoints (cURL / Postman)](#paso-6-guía-de-pruebas-de-endpoints-curl--postman)
9. [Paso 7: Código Fuente Explicado y Documentado](#paso-7-código-fuente-explicado-y-documentado)
10. [Paso 8: Solución de Problemas Frecuentes](#paso-8-solución-de-problemas-frecuentes)

---

## 1. Requisitos Previos

- **Node.js**: v18.0.0 o superior (`node -v`).
- **npm**: v9.0.0 o superior (`npm -v`).
- Conexión a internet para conectar con la base de datos PostgreSQL alojada en **Neon Cloud**.

---

## 2. Estructura Completa del Proyecto

```
festivaltrack-backend/
│
├── src/
│   ├── main.ts                  # Configura prefijo /api/v1 y puerto 3001.
│   ├── app.module.ts            # Módulo raíz que importa auth, eventos, boletos, chat, etc.
│   │
│   ├── prisma/                  # Conexión inyectable de Prisma Client a PostgreSQL.
│   │
│   ├── auth/                    # ⭐ Autenticación JWT con Bcrypt.
│   │   ├── auth.controller.ts   # Endpoints POST /auth/login y POST /auth/register.
│   │   └── auth.service.ts      # Hash de contraseñas y firma de tokens JWT.
│   │
│   ├── boletos/                 # ⭐ Compra transaccional y validación QR.
│   │   ├── boletos.controller.ts # POST /boletos/comprar, GET /boletos/mis-boletos.
│   │   └── boletos.service.ts   # Transacciones SQL atómicas ($transaction) y validación.
│   │
│   ├── eventos/                 # ⭐ CRUD de la agenda oficial.
│   │   ├── eventos.controller.ts # GET, POST, PUT, DELETE /eventos.
│   │   └── eventos.service.ts   # Generación de IDs secuenciales (EVT-001) y soft delete.
│   │
│   ├── chat/                    # ⭐ Servidor de chat para transmisión en vivo.
│   │   ├── chat.controller.ts   # GET /stream/chat/:id, POST /stream/chat.
│   │   └── chat.service.ts      # Historial y recepción de comentarios.
│   │
│   ├── galeria/                 # Servicio de imágenes oficiales.
│   ├── canciones/               # Catálogo musical del festival.
│   └── biografias/              # Información cultural del artista.
│
└── prisma/
    ├── schema.prisma            # Esquema completo de la base de datos PostgreSQL.
    └── seed.ts                  # Script de sembrado de datos iniciales.
```

---

## Paso 1: Instalación de Dependencias

1. Navega al directorio del backend:
   ```bash
   cd festivaltrack-backend
   ```
2. Instala los módulos requeridos:
   ```bash
   npm install
   ```

---

## Paso 2: Configuración de Variables de Entorno (.env)

Crea o verifica el archivo `.env` en `festivaltrack-backend/`:

```env
# Conexión a PostgreSQL en Neon Cloud (SSL requerido)
DATABASE_URL="postgresql://neondb_owner:npg_vU8Nylw8mWkQ@ep-royal-darkness-at4ww4dx.us-east-2.aws.neon.tech/neondb?sslmode=require"

# Clave secreta para la firma de tokens JWT
JWT_SECRET="festivaltrack_super_secret_jwt_key_2024"

# Puerto en el que correrá el servidor HTTP
PORT=3001
```

---

## Paso 3: Sincronización y Migraciones de Base de Datos (Prisma)

1. Genera los clientes de Prisma:
   ```bash
   npx prisma generate
   ```
2. Aplica las migraciones a PostgreSQL:
   ```bash
   npx prisma migrate dev --name init
   ```

---

## Paso 4: Sembrado de Datos de Prueba (Seed)

Pobla la base de datos con el usuario administrador por defecto (`admin@admin.com`) y el evento `EVT-001`:

```bash
npx prisma db seed
```

---

## Paso 5: Ejecución del Servidor Backend

### Modo Desarrollo (con recarga automática):
```bash
npm run start:dev
```

**Verificación:** Consola mostrará: `Escuchando en http://localhost:3001/api/v1`

---

## Paso 6: Guía de Pruebas de Endpoints (cURL / Postman)

### 1. Iniciar Sesión (Login)
- **POST** `http://localhost:3001/api/v1/auth/login`
- **Body JSON**:
  ```json
  { "correo": "admin@admin.com", "contrasena": "admin123" }
  ```

### 2. Comprar Boleto
- **POST** `http://localhost:3001/api/v1/boletos/comprar`
- **Headers**: `Authorization: Bearer <TOKEN_JWT>`
- **Body JSON**:
  ```json
  { "eventoId": "EVT-001", "categoria": "VIP", "cantidad": 2, "precioTotal": 9000, "metodoPago": "TARJETA" }
  ```

### 3. Enviar Comentario al Chat en Vivo
- **POST** `http://localhost:3001/api/v1/stream/chat`
- **Body JSON**:
  ```json
  { "eventoId": "EVT-001", "usuarioNombre": "Juan", "mensaje": "¡Viva el Mariachi!" }
  ```

---

## Paso 7: Código Fuente Explicado y Documentado

### 1. `auth.service.ts` — Hash de Contraseñas y Firma JWT
```typescript
@Injectable()
export class AuthService {
  constructor(private prisma: PrismaService, private jwt: JwtService) {}

  async login(dto: LoginDto) {
    const usuario = await this.prisma.usuario.findUnique({ where: { correo: dto.correo } });
    if (!usuario) throw new UnauthorizedException('Credenciales inválidas');
    
    // Comparación segura de contraseña con bcrypt
    const valida = await bcrypt.compare(dto.contrasena, usuario.contrasena);
    if (!valida) throw new UnauthorizedException('Credenciales inválidas');
    
    return {
      accessToken: this.jwt.sign({ sub: usuario.id, correo: usuario.correo, rol: usuario.rol }),
      usuario: { id: usuario.id, nombre: usuario.nombre, correo: usuario.correo, rol: usuario.rol }
    };
  }
}
```

### 2. `boletos.service.ts` — Transacciones SQL Atómicas y Firma QR
```typescript
async comprar(usuarioId: string, body: any) {
  // Garantiza que la creación del Boleto y del Pago ocurran juntas o se cancelen ambas
  return this.prisma.$transaction(async (tx) => {
    const boleto = await tx.boleto.create({
      data: { eventoId, usuarioId, categoria, precio: precioTotal }
    });

    await tx.pago.create({
      data: { boletoId: boleto.id, monto: precioTotal, estado: 'COMPLETADO' }
    });

    // Asignación de firma de código QR único
    const qrFirma = `FESTIVAL-TICKET-2024::${boleto.id}`;
    return tx.boleto.update({
      where: { id: boleto.id },
      data: { codigoQR: qrFirma }
    });
  });
}
```

---

## Paso 8: Solución de Problemas Frecuentes

### Error: `Foreign key constraint violated on chat_stream_eventoId_fkey`
- Asegúrate de asociar los mensajes a un `eventoId` existente como `"EVT-001"`. Si fue borrado, ejecuta `npx prisma db seed`.
