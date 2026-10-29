-- CreateEnum
CREATE TYPE "TipoPublicacion" AS ENUM ('EXPERIENCIA', 'RECOMENDACION', 'FOTO');

-- CreateTable
CREATE TABLE "publicaciones" (
    "id" TEXT NOT NULL,
    "autorId" TEXT NOT NULL,
    "tipo" "TipoPublicacion" NOT NULL DEFAULT 'EXPERIENCIA',
    "titulo" TEXT,
    "contenido" TEXT NOT NULL,
    "imagenUrl" TEXT,
    "lugar" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "publicaciones_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "comentarios_publicacion" (
    "id" TEXT NOT NULL,
    "publicacionId" TEXT NOT NULL,
    "autorId" TEXT NOT NULL,
    "texto" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "comentarios_publicacion_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "likes_publicacion" (
    "publicacionId" TEXT NOT NULL,
    "usuarioId" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "likes_publicacion_pkey" PRIMARY KEY ("publicacionId","usuarioId")
);

-- CreateIndex
CREATE INDEX "publicaciones_createdAt_idx" ON "publicaciones"("createdAt");

-- CreateIndex
CREATE INDEX "publicaciones_tipo_createdAt_idx" ON "publicaciones"("tipo", "createdAt");

-- CreateIndex
CREATE INDEX "comentarios_publicacion_publicacionId_createdAt_idx" ON "comentarios_publicacion"("publicacionId", "createdAt");

-- AddForeignKey
ALTER TABLE "publicaciones" ADD CONSTRAINT "publicaciones_autorId_fkey" FOREIGN KEY ("autorId") REFERENCES "usuarios"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "comentarios_publicacion" ADD CONSTRAINT "comentarios_publicacion_publicacionId_fkey" FOREIGN KEY ("publicacionId") REFERENCES "publicaciones"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "comentarios_publicacion" ADD CONSTRAINT "comentarios_publicacion_autorId_fkey" FOREIGN KEY ("autorId") REFERENCES "usuarios"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "likes_publicacion" ADD CONSTRAINT "likes_publicacion_publicacionId_fkey" FOREIGN KEY ("publicacionId") REFERENCES "publicaciones"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "likes_publicacion" ADD CONSTRAINT "likes_publicacion_usuarioId_fkey" FOREIGN KEY ("usuarioId") REFERENCES "usuarios"("id") ON DELETE CASCADE ON UPDATE CASCADE;
