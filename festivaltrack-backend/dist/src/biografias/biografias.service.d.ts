import { PrismaService } from '../prisma/prisma.service';
export declare class BiografiasService {
    private prisma;
    constructor(prisma: PrismaService);
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
