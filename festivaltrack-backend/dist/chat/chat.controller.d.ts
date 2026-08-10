import { ChatService } from './chat.service';
export declare class ChatController {
    private readonly chatService;
    constructor(chatService: ChatService);
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
