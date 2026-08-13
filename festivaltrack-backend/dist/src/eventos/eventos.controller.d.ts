import { EventosService } from './eventos.service';
import { CreateEventoDto } from './dto/create-evento.dto';
export declare class EventosController {
    private eventosService;
    constructor(eventosService: EventosService);
    findAll(): import(".prisma/client").Prisma.PrismaPromise<({
        artista: {
            id: string;
            nombre: string;
            imagenUrl: string | null;
            activo: boolean;
            createdAt: Date;
            updatedAt: Date;
        } | null;
    } & {
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
    })[]>;
    findOne(id: string): Promise<{
        artista: {
            id: string;
            nombre: string;
            imagenUrl: string | null;
            activo: boolean;
            createdAt: Date;
            updatedAt: Date;
        } | null;
        transmision: {
            id: string;
            createdAt: Date;
            updatedAt: Date;
            titulo: string;
            estado: import(".prisma/client").$Enums.EstadoTransmision;
            eventoId: string;
            streamUrl: string | null;
            espectadores: number;
            chatActivo: boolean;
        } | null;
    } & {
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
    }>;
    create(dto: CreateEventoDto, req: any): Promise<{
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
    }>;
    update(id: string, dto: Partial<CreateEventoDto>): Promise<{
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
    }>;
    remove(id: string): Promise<{
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
    }>;
}
