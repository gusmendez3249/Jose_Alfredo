import { PrismaService } from '../prisma/prisma.service';
export declare class CancionesService {
    private prisma;
    constructor(prisma: PrismaService);
    findAll(): Promise<{
        artista: string;
        id: string;
        createdAt: Date;
        updatedAt: Date;
        administradorId: string;
        titulo: string;
        estado: import(".prisma/client").$Enums.EstadoCancion;
        duracion: number;
        archivoUrl: string;
        genero: string | null;
    }[]>;
    create(dto: {
        titulo: string;
        artista?: string;
        duracion?: number;
        archivoUrl?: string;
        genero?: string;
    }): Promise<{
        artista: string;
        id: string;
        createdAt: Date;
        updatedAt: Date;
        administradorId: string;
        titulo: string;
        estado: import(".prisma/client").$Enums.EstadoCancion;
        duracion: number;
        archivoUrl: string;
        genero: string | null;
    }>;
}
