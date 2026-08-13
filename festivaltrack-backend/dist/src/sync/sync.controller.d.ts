import { SyncService } from './sync.service';
export declare class SyncController {
    private syncService;
    constructor(syncService: SyncService);
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
