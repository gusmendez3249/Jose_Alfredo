import { PrismaService } from '../prisma/prisma.service';
export declare class BoletosService {
    private prisma;
    constructor(prisma: PrismaService);
    comprar(usuarioId: string, body: any): Promise<{
        pago: {
            id: string;
            createdAt: Date;
            updatedAt: Date;
            estado: import(".prisma/client").$Enums.EstadoPago;
            monto: number;
            metodo: import(".prisma/client").$Enums.MetodoPago;
            fecha: Date;
            referencia: string | null;
            recibo: string | null;
            boletoId: string;
        } | null;
    } & {
        id: string;
        createdAt: Date;
        updatedAt: Date;
        usuarioId: string;
        categoria: import(".prisma/client").$Enums.CategoriaTicket;
        estado: import(".prisma/client").$Enums.EstadoTicket;
        eventoId: string;
        codigoQR: string;
        precio: number;
        fechaCompra: Date;
    }>;
    misBoletos(usuarioId: string): Promise<({
        evento: {
            id: string;
            nombre: string;
            createdAt: Date;
            updatedAt: Date;
            descripcion: string | null;
            administradorId: string;
            fechaHora: Date;
            ubicacion: string;
            escenario: string | null;
            capacidad: number;
            bannerUrl: string | null;
            estado: import(".prisma/client").$Enums.EstadoEvento;
            artistaId: string | null;
            latitud: number | null;
            longitud: number | null;
        };
    } & {
        id: string;
        createdAt: Date;
        updatedAt: Date;
        usuarioId: string;
        categoria: import(".prisma/client").$Enums.CategoriaTicket;
        estado: import(".prisma/client").$Enums.EstadoTicket;
        eventoId: string;
        codigoQR: string;
        precio: number;
        fechaCompra: Date;
    })[]>;
    validarQr(qr: string): Promise<{
        status: string;
        message: string;
        evento: string;
        usuario: string;
    }>;
}
