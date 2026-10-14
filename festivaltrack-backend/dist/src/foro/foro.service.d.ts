import { PrismaService } from '../prisma/prisma.service';
export interface UsuarioActual {
    id: string;
    rol: string;
}
export declare class ForoService {
    private prisma;
    constructor(prisma: PrismaService);
    private includePublicacion;
    private mapPublicacion;
    listar(usuario: UsuarioActual | null, opts: {
        tipo?: string;
        before?: string;
        limit?: number;
    }): Promise<{
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
    crear(usuario: UsuarioActual, dto: {
        tipo?: string;
        titulo?: string;
        contenido?: string;
        imagenUrl?: string;
        lugar?: string;
    }): Promise<{
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
    eliminar(usuario: UsuarioActual, id: string): Promise<{
        ok: boolean;
    }>;
    toggleLike(usuario: UsuarioActual, publicacionId: string): Promise<{
        liked: boolean;
        likes: number;
    }>;
    comentarios(publicacionId: string): Promise<{
        id: string;
        texto: string;
        createdAt: Date;
        autorId: string;
        autorNombre: string;
    }[]>;
    comentar(usuario: UsuarioActual, publicacionId: string, texto?: string): Promise<{
        id: string;
        texto: string;
        createdAt: Date;
        autorId: string;
        autorNombre: string;
    }>;
    eliminarComentario(usuario: UsuarioActual, id: string): Promise<{
        ok: boolean;
    }>;
}
