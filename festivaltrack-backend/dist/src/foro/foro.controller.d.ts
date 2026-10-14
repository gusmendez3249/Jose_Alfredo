import { ForoService } from './foro.service';
export declare class ForoController {
    private readonly foro;
    constructor(foro: ForoService);
    listar(req: any, tipo?: string, before?: string, limit?: string): Promise<{
        id: any;
        tipo: any;
        titulo: any;
        contenido: any;
        imagenUrl: any;
        lugar: any;
        createdAt: any;
        autorId: any;
        autorNombre: any;
        likes: any;
        comentarios: any;
        likedByMe: boolean;
    }[]>;
    crear(req: any, body: any): Promise<{
        id: any;
        tipo: any;
        titulo: any;
        contenido: any;
        imagenUrl: any;
        lugar: any;
        createdAt: any;
        autorId: any;
        autorNombre: any;
        likes: any;
        comentarios: any;
        likedByMe: boolean;
    }>;
    eliminarComentario(req: any, id: string): Promise<{
        ok: boolean;
    }>;
    eliminar(req: any, id: string): Promise<{
        ok: boolean;
    }>;
    like(req: any, id: string): Promise<{
        liked: boolean;
        likes: number;
    }>;
    comentarios(id: string): Promise<{
        id: string;
        texto: string;
        createdAt: Date;
        autorId: string;
        autorNombre: string;
    }[]>;
    comentar(req: any, id: string, texto?: string): Promise<{
        id: string;
        texto: string;
        createdAt: Date;
        autorId: string;
        autorNombre: string;
    }>;
}
