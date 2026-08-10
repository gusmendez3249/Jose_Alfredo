import { GaleriaService } from './galeria.service';
export declare class GaleriaController {
    private readonly galeriaService;
    constructor(galeriaService: GaleriaService);
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
export declare class UploadController {
    uploadFile(file: Express.Multer.File): {
        url: string;
    };
}
