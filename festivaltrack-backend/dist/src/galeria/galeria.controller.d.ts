import { GaleriaService } from './galeria.service';
export declare class GaleriaController {
    private readonly galeriaService;
    constructor(galeriaService: GaleriaService);
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
    addImagen(body: any): Promise<{
        id: string;
        createdAt: Date;
        url: string;
        titulo: string | null;
        etiquetas: string[];
        orden: number;
        galeriaId: string;
    }>;
}
export declare class UploadController {
    uploadFile(file: Express.Multer.File): {
        url: string;
    };
}
