-- CreateTable
CREATE TABLE "sitios_ar" (
    "id" TEXT NOT NULL,
    "nombre" TEXT NOT NULL,
    "ubicacion" TEXT NOT NULL,
    "descripcion" TEXT NOT NULL,
    "pistasVisuales" TEXT NOT NULL,
    "imagenUrl" TEXT,
    "audioUrl" TEXT,
    "galeriaId" TEXT,
    "latitud" DOUBLE PRECISION,
    "longitud" DOUBLE PRECISION,
    "activo" BOOLEAN NOT NULL DEFAULT true,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "sitios_ar_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "visitas_sitio" (
    "id" TEXT NOT NULL,
    "sitioId" TEXT NOT NULL,
    "usuarioId" TEXT NOT NULL,
    "confianza" DOUBLE PRECISION NOT NULL,
    "fechaVisita" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "visitas_sitio_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "visitas_sitio_usuarioId_fechaVisita_idx" ON "visitas_sitio"("usuarioId", "fechaVisita");

-- AddForeignKey
ALTER TABLE "visitas_sitio" ADD CONSTRAINT "visitas_sitio_sitioId_fkey" FOREIGN KEY ("sitioId") REFERENCES "sitios_ar"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "visitas_sitio" ADD CONSTRAINT "visitas_sitio_usuarioId_fkey" FOREIGN KEY ("usuarioId") REFERENCES "usuarios"("id") ON DELETE CASCADE ON UPDATE CASCADE;
