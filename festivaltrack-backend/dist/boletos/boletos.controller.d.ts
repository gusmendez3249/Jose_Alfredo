import { BoletosService } from './boletos.service';
export declare class BoletosController {
    private readonly boletosService;
    constructor(boletosService: BoletosService);
    comprar(body: any, req: any): Promise<{
        id: string;
        createdAt: Date;
        updatedAt: Date;
        estado: import(".prisma/client").$Enums.EstadoTicket;
        eventoId: string;
        categoria: import(".prisma/client").$Enums.CategoriaTicket;
        precio: number;
        fechaCompra: Date;
        codigoQR: string;
        usuarioId: string;
    }>;
    misBoletos(req: any): Promise<({
        evento: {
            nombre: string;
            id: string;
            createdAt: Date;
            updatedAt: Date;
            fechaHora: Date;
            ubicacion: string;
            escenario: string | null;
            descripcion: string | null;
            capacidad: number;
            bannerUrl: string | null;
            estado: import(".prisma/client").$Enums.EstadoEvento;
            artistaId: string | null;
            administradorId: string;
            latitud: number | null;
            longitud: number | null;
        };
    } & {
        id: string;
        createdAt: Date;
        updatedAt: Date;
        estado: import(".prisma/client").$Enums.EstadoTicket;
        eventoId: string;
        categoria: import(".prisma/client").$Enums.CategoriaTicket;
        precio: number;
        fechaCompra: Date;
        codigoQR: string;
        usuarioId: string;
    })[]>;
    validarQr(qr: string): Promise<{
        status: string;
        message: string;
        evento: string;
        usuario: string;
    }>;
}
