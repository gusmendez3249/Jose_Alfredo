-- CreateTable
CREATE TABLE "chat_stream" (
    "id" TEXT NOT NULL,
    "eventoId" TEXT NOT NULL,
    "usuarioNombre" TEXT NOT NULL,
    "mensaje" TEXT NOT NULL,
    "esAdmin" BOOLEAN NOT NULL DEFAULT false,
    "fechaEnvio" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "chat_stream_pkey" PRIMARY KEY ("id")
);

-- AddForeignKey
ALTER TABLE "chat_stream" ADD CONSTRAINT "chat_stream_eventoId_fkey" FOREIGN KEY ("eventoId") REFERENCES "eventos"("id") ON DELETE CASCADE ON UPDATE CASCADE;
