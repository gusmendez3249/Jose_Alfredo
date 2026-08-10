import { PrismaService } from '../prisma/prisma.service';
export declare class ChatService {
    private prisma;
    constructor(prisma: PrismaService);
    getMensajes(eventoId: string): Promise<{
        id: string;
        createdAt: Date;
        eventoId: string;
        usuarioNombre: string;
        mensaje: string;
        esAdmin: boolean;
        fechaEnvio: Date;
    }[]>;
    enviarMensaje(body: any): Promise<{
        id: string;
        createdAt: Date;
        eventoId: string;
        usuarioNombre: string;
        mensaje: string;
        esAdmin: boolean;
        fechaEnvio: Date;
    }>;
}
