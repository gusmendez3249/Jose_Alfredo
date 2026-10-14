import { BiografiasService } from './biografias.service';
import { BiografiaAltaDto } from './factories/biografia.factory';
export declare class BiografiasController {
    private readonly biografiasService;
    constructor(biografiasService: BiografiasService);
    findAll(): Promise<({
        artista: {
            id: string;
            nombre: string;
            imagenUrl: string | null;
            activo: boolean;
            createdAt: Date;
            updatedAt: Date;
        };
    } & {
        id: string;
        createdAt: Date;
        updatedAt: Date;
        descripcion: string;
        citaCelebre: string | null;
        hitos: import("@prisma/client/runtime/library").JsonValue;
        discografia: import("@prisma/client/runtime/library").JsonValue;
        artistaId: string;
    })[]>;
    altaBiografia(body: BiografiaAltaDto): Promise<{
        artista: {
            id: string;
            nombre: string;
            imagenUrl: string | null;
            activo: boolean;
            createdAt: Date;
            updatedAt: Date;
        };
    } & {
        id: string;
        createdAt: Date;
        updatedAt: Date;
        descripcion: string;
        citaCelebre: string | null;
        hitos: import("@prisma/client/runtime/library").JsonValue;
        discografia: import("@prisma/client/runtime/library").JsonValue;
        artistaId: string;
    }>;
    findOne(id: string): Promise<({
        artista: {
            id: string;
            nombre: string;
            imagenUrl: string | null;
            activo: boolean;
            createdAt: Date;
            updatedAt: Date;
        };
    } & {
        id: string;
        createdAt: Date;
        updatedAt: Date;
        descripcion: string;
        citaCelebre: string | null;
        hitos: import("@prisma/client/runtime/library").JsonValue;
        discografia: import("@prisma/client/runtime/library").JsonValue;
        artistaId: string;
    }) | null>;
}
