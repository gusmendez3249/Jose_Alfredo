import { PrismaService } from '../prisma/prisma.service';
export declare class GaleriaService {
    private prisma;
    constructor(prisma: PrismaService);
    getGaleria(): Promise<({
        imagenes: {
            id: string;
            createdAt: Date;
            url: string;
            titulo: string | null;
            etiquetas: string[];
            orden: number;
            galeriaId: string;
        }[];
    } & {
        id: string;
        nombre: string;
        createdAt: Date;
        updatedAt: Date;
        categoria: import(".prisma/client").$Enums.CategoriaGaleria;
        administradorId: string;
    })[]>;
    addImagen(dto: {
        url?: string;
        titulo?: string;
    }): Promise<{
        id: string;
        createdAt: Date;
        url: string;
        titulo: string | null;
        etiquetas: string[];
        orden: number;
        galeriaId: string;
    }>;
    removeImagen(id: string): Promise<{
        id: string;
        createdAt: Date;
        url: string;
        titulo: string | null;
        etiquetas: string[];
        orden: number;
        galeriaId: string;
    }>;
}
