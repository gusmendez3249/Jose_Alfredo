-- CreateEnum
CREATE TYPE "Rol" AS ENUM ('USUARIO', 'ADMINISTRADOR');

-- CreateEnum
CREATE TYPE "CategoriaTicket" AS ENUM ('VIP', 'GENERAL', 'ESTUDIANTE');

-- CreateEnum
CREATE TYPE "EstadoTicket" AS ENUM ('ACTIVO', 'USADO', 'CANCELADO', 'EXPIRADO');

-- CreateEnum
CREATE TYPE "MetodoPago" AS ENUM ('TARJETA_CREDITO', 'TARJETA_DEBITO', 'TRANSFERENCIA', 'PAYPAL');

-- CreateEnum
CREATE TYPE "EstadoPago" AS ENUM ('PENDIENTE', 'COMPLETADO', 'FALLIDO', 'REEMBOLSADO');

-- CreateEnum
CREATE TYPE "EstadoEvento" AS ENUM ('BORRADOR', 'PUBLICADO', 'EN_CURSO', 'FINALIZADO', 'CANCELADO');

-- CreateEnum
CREATE TYPE "EstadoCancion" AS ENUM ('BORRADOR', 'PUBLICADA', 'ARCHIVADA');

-- CreateEnum
CREATE TYPE "EstadoTransmision" AS ENUM ('INACTIVA', 'EN_VIVO', 'PAUSADA', 'FINALIZADA');

-- CreateEnum
CREATE TYPE "TipoNotificacion" AS ENUM ('ALERTA_INICIO', 'RECORDATORIO', 'CAMBIO_ESCENARIO', 'TRANSMISION_INICIADA', 'BOLETO_CONFIRMADO', 'SISTEMA');

-- CreateEnum
CREATE TYPE "CategoriaGaleria" AS ENUM ('GOLDEN_ERA', 'LEGACY', 'PRESENTACIONES', 'BACKSTAGE', 'GENERAL');

-- CreateTable
CREATE TABLE "usuarios" (
    "id" TEXT NOT NULL,
    "nombre" TEXT NOT NULL,
    "correo" TEXT NOT NULL,
    "contrasena" TEXT NOT NULL,
    "rol" "Rol" NOT NULL DEFAULT 'USUARIO',
    "fechaRegistro" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "activo" BOOLEAN NOT NULL DEFAULT true,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "usuarios_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "administradores" (
    "id" TEXT NOT NULL,
    "usuarioId" TEXT NOT NULL,
    "nivel" INTEGER NOT NULL DEFAULT 1,
    "permisos" TEXT[],
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "administradores_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "artistas" (
    "id" TEXT NOT NULL,
    "nombre" TEXT NOT NULL,
    "imagenUrl" TEXT,
    "activo" BOOLEAN NOT NULL DEFAULT true,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "artistas_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "biografias" (
    "id" TEXT NOT NULL,
    "artistaId" TEXT NOT NULL,
    "descripcion" TEXT NOT NULL,
    "citaCelebre" TEXT,
    "hitos" JSONB NOT NULL DEFAULT '[]',
    "discografia" JSONB NOT NULL DEFAULT '[]',
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "biografias_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "canciones" (
    "id" TEXT NOT NULL,
    "titulo" TEXT NOT NULL,
    "artista" TEXT NOT NULL,
    "duracion" INTEGER NOT NULL,
    "archivoUrl" TEXT NOT NULL,
    "genero" TEXT,
    "estado" "EstadoCancion" NOT NULL DEFAULT 'BORRADOR',
    "administradorId" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "canciones_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "galerias" (
    "id" TEXT NOT NULL,
    "nombre" TEXT NOT NULL,
    "categoria" "CategoriaGaleria" NOT NULL DEFAULT 'GENERAL',
    "administradorId" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "galerias_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "imagenes" (
    "id" TEXT NOT NULL,
    "galeriaId" TEXT NOT NULL,
    "url" TEXT NOT NULL,
    "titulo" TEXT,
    "etiquetas" TEXT[],
    "orden" INTEGER NOT NULL DEFAULT 0,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "imagenes_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "eventos" (
    "id" TEXT NOT NULL,
    "nombre" TEXT NOT NULL,
    "fechaHora" TIMESTAMP(3) NOT NULL,
    "ubicacion" TEXT NOT NULL,
    "escenario" TEXT,
    "descripcion" TEXT,
    "capacidad" INTEGER NOT NULL,
    "bannerUrl" TEXT,
    "estado" "EstadoEvento" NOT NULL DEFAULT 'BORRADOR',
    "administradorId" TEXT NOT NULL,
    "artistaId" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "eventos_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "boletos" (
    "id" TEXT NOT NULL,
    "eventoId" TEXT NOT NULL,
    "usuarioId" TEXT NOT NULL,
    "categoria" "CategoriaTicket" NOT NULL DEFAULT 'GENERAL',
    "precio" DOUBLE PRECISION NOT NULL,
    "fechaCompra" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "codigoQR" TEXT NOT NULL,
    "estado" "EstadoTicket" NOT NULL DEFAULT 'ACTIVO',
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "boletos_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "pagos" (
    "id" TEXT NOT NULL,
    "boletoId" TEXT NOT NULL,
    "monto" DOUBLE PRECISION NOT NULL,
    "metodo" "MetodoPago" NOT NULL,
    "estado" "EstadoPago" NOT NULL DEFAULT 'PENDIENTE',
    "fecha" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "referencia" TEXT,
    "recibo" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "pagos_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "transmisiones" (
    "id" TEXT NOT NULL,
    "eventoId" TEXT NOT NULL,
    "titulo" TEXT NOT NULL,
    "streamUrl" TEXT,
    "espectadores" INTEGER NOT NULL DEFAULT 0,
    "estado" "EstadoTransmision" NOT NULL DEFAULT 'INACTIVA',
    "chatActivo" BOOLEAN NOT NULL DEFAULT false,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "transmisiones_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "canciones_transmisiones" (
    "cancionId" TEXT NOT NULL,
    "transmisionId" TEXT NOT NULL,
    "orden" INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT "canciones_transmisiones_pkey" PRIMARY KEY ("cancionId","transmisionId")
);

-- CreateTable
CREATE TABLE "usuarios_transmisiones" (
    "usuarioId" TEXT NOT NULL,
    "transmisionId" TEXT NOT NULL,
    "unidoEn" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "usuarios_transmisiones_pkey" PRIMARY KEY ("usuarioId","transmisionId")
);

-- CreateTable
CREATE TABLE "notificaciones" (
    "id" TEXT NOT NULL,
    "tipo" "TipoNotificacion" NOT NULL,
    "mensaje" TEXT NOT NULL,
    "fechaHora" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "leida" BOOLEAN NOT NULL DEFAULT false,
    "usuarioId" TEXT,
    "administradorId" TEXT,
    "eventoId" TEXT,
    "transmisionId" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "notificaciones_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "sync_meta" (
    "id" TEXT NOT NULL,
    "dispositivo" TEXT NOT NULL,
    "entidad" TEXT NOT NULL,
    "lastSyncAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "etag" TEXT,

    CONSTRAINT "sync_meta_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "usuarios_correo_key" ON "usuarios"("correo");

-- CreateIndex
CREATE UNIQUE INDEX "administradores_usuarioId_key" ON "administradores"("usuarioId");

-- CreateIndex
CREATE UNIQUE INDEX "biografias_artistaId_key" ON "biografias"("artistaId");

-- CreateIndex
CREATE UNIQUE INDEX "boletos_codigoQR_key" ON "boletos"("codigoQR");

-- CreateIndex
CREATE UNIQUE INDEX "pagos_boletoId_key" ON "pagos"("boletoId");

-- CreateIndex
CREATE UNIQUE INDEX "transmisiones_eventoId_key" ON "transmisiones"("eventoId");

-- CreateIndex
CREATE UNIQUE INDEX "sync_meta_dispositivo_entidad_key" ON "sync_meta"("dispositivo", "entidad");

-- AddForeignKey
ALTER TABLE "biografias" ADD CONSTRAINT "biografias_artistaId_fkey" FOREIGN KEY ("artistaId") REFERENCES "artistas"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "canciones" ADD CONSTRAINT "canciones_administradorId_fkey" FOREIGN KEY ("administradorId") REFERENCES "administradores"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "galerias" ADD CONSTRAINT "galerias_administradorId_fkey" FOREIGN KEY ("administradorId") REFERENCES "administradores"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "imagenes" ADD CONSTRAINT "imagenes_galeriaId_fkey" FOREIGN KEY ("galeriaId") REFERENCES "galerias"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "eventos" ADD CONSTRAINT "eventos_administradorId_fkey" FOREIGN KEY ("administradorId") REFERENCES "administradores"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "eventos" ADD CONSTRAINT "eventos_artistaId_fkey" FOREIGN KEY ("artistaId") REFERENCES "artistas"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "boletos" ADD CONSTRAINT "boletos_eventoId_fkey" FOREIGN KEY ("eventoId") REFERENCES "eventos"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "boletos" ADD CONSTRAINT "boletos_usuarioId_fkey" FOREIGN KEY ("usuarioId") REFERENCES "usuarios"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "pagos" ADD CONSTRAINT "pagos_boletoId_fkey" FOREIGN KEY ("boletoId") REFERENCES "boletos"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "transmisiones" ADD CONSTRAINT "transmisiones_eventoId_fkey" FOREIGN KEY ("eventoId") REFERENCES "eventos"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "canciones_transmisiones" ADD CONSTRAINT "canciones_transmisiones_cancionId_fkey" FOREIGN KEY ("cancionId") REFERENCES "canciones"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "canciones_transmisiones" ADD CONSTRAINT "canciones_transmisiones_transmisionId_fkey" FOREIGN KEY ("transmisionId") REFERENCES "transmisiones"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "usuarios_transmisiones" ADD CONSTRAINT "usuarios_transmisiones_usuarioId_fkey" FOREIGN KEY ("usuarioId") REFERENCES "usuarios"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "usuarios_transmisiones" ADD CONSTRAINT "usuarios_transmisiones_transmisionId_fkey" FOREIGN KEY ("transmisionId") REFERENCES "transmisiones"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "notificaciones" ADD CONSTRAINT "notificaciones_usuarioId_fkey" FOREIGN KEY ("usuarioId") REFERENCES "usuarios"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "notificaciones" ADD CONSTRAINT "notificaciones_administradorId_fkey" FOREIGN KEY ("administradorId") REFERENCES "administradores"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "notificaciones" ADD CONSTRAINT "notificaciones_eventoId_fkey" FOREIGN KEY ("eventoId") REFERENCES "eventos"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "notificaciones" ADD CONSTRAINT "notificaciones_transmisionId_fkey" FOREIGN KEY ("transmisionId") REFERENCES "transmisiones"("id") ON DELETE SET NULL ON UPDATE CASCADE;
