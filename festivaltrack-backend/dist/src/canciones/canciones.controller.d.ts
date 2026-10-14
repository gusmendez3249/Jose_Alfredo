import { CancionesService } from './canciones.service';
export declare class CancionesController {
    private readonly cancionesService;
    constructor(cancionesService: CancionesService);
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
        portadaUrl: string | null;
        genero: string | null;
    }[]>;
    findByEvento(eventoId: string): Promise<{
        artista: string;
        id: string;
        createdAt: Date;
        updatedAt: Date;
        administradorId: string;
        titulo: string;
        estado: import(".prisma/client").$Enums.EstadoCancion;
        duracion: number;
        archivoUrl: string;
        portadaUrl: string | null;
        genero: string | null;
    }[]>;
    setRepertorio(eventoId: string, cancionIds: string[]): Promise<{
        artista: string;
        id: string;
        createdAt: Date;
        updatedAt: Date;
        administradorId: string;
        titulo: string;
        estado: import(".prisma/client").$Enums.EstadoCancion;
        duracion: number;
        archivoUrl: string;
        portadaUrl: string | null;
        genero: string | null;
    }[]>;
    upload(file: Express.Multer.File): {
        url: string;
    };
    create(body: any): Promise<{
        artista: string;
        id: string;
        createdAt: Date;
        updatedAt: Date;
        administradorId: string;
        titulo: string;
        estado: import(".prisma/client").$Enums.EstadoCancion;
        duracion: number;
        archivoUrl: string;
        portadaUrl: string | null;
        genero: string | null;
    }>;
    remove(id: string): Promise<{
        artista: string;
        id: string;
        createdAt: Date;
        updatedAt: Date;
        administradorId: string;
        titulo: string;
        estado: import(".prisma/client").$Enums.EstadoCancion;
        duracion: number;
        archivoUrl: string;
        portadaUrl: string | null;
        genero: string | null;
    }>;
}
