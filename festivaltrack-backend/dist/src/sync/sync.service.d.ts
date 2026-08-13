import { PrismaService } from '../prisma/prisma.service';
export declare class SyncService {
    private prisma;
    constructor(prisma: PrismaService);
    getWearPayload(since?: string): Promise<{
        eventos: {
            artista: {
                id: string;
                nombre: string;
                imagenUrl: string | null;
            } | null;
            id: string;
            nombre: string;
            updatedAt: Date;
            fechaHora: Date;
            ubicacion: string;
            escenario: string | null;
            bannerUrl: string | null;
            estado: import(".prisma/client").$Enums.EstadoEvento;
        }[];
        artistas: {
            id: string;
            nombre: string;
            imagenUrl: string | null;
            updatedAt: Date;
        }[];
        generadoEn: string;
        total: {
            eventos: number;
            artistas: number;
        };
    }>;
}
