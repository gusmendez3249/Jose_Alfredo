import { PrismaService } from '../prisma/prisma.service';
export declare class GaleriaService {
    private prisma;
    constructor(prisma: PrismaService);
    getGaleria(): Promise<({
        imagenes: {
            id: string;
            createdAt: Date;
            titulo: string | null;
            galeriaId: string;
            url: string;
            etiquetas: string[];
            orden: number;
        }[];
    } & {
        nombre: string;
        id: string;
        createdAt: Date;
        updatedAt: Date;
        administradorId: string;
        categoria: import(".prisma/client").$Enums.CategoriaGaleria;
    })[]>;
}
