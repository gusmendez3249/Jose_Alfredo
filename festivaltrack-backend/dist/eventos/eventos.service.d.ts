import { PrismaService } from '../prisma/prisma.service';
import { CreateEventoDto } from './dto/create-evento.dto';
export declare class EventosService {
    private prisma;
    constructor(prisma: PrismaService);
    findAll(): import(".prisma/client").Prisma.PrismaPromise<({
        artista: {
            nombre: string;
            id: string;
            activo: boolean;
            createdAt: Date;
            updatedAt: Date;
            imagenUrl: string | null;
        } | null;
    } & {
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
    })[]>;
    findOne(id: string): Promise<{
        artista: {
            nombre: string;
            id: string;
            activo: boolean;
            createdAt: Date;
            updatedAt: Date;
            imagenUrl: string | null;
        } | null;
        transmision: {
            id: string;
            createdAt: Date;
            updatedAt: Date;
            estado: import(".prisma/client").$Enums.EstadoTransmision;
            eventoId: string;
            titulo: string;
            streamUrl: string | null;
            espectadores: number;
            chatActivo: boolean;
        } | null;
    } & {
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
    }>;
    create(dto: CreateEventoDto, adminId: string): import(".prisma/client").Prisma.Prisma__EventoClient<{
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
    }, never, import("@prisma/client/runtime/library").DefaultArgs>;
    update(id: string, dto: Partial<CreateEventoDto>): Promise<{
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
    }>;
    remove(id: string): Promise<{
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
    }>;
}
