import { SyncService } from './sync.service';
export declare class SyncController {
    private syncService;
    constructor(syncService: SyncService);
    getWearPayload(since?: string): Promise<{
        eventos: {
            artista: {
                nombre: string;
                id: string;
                imagenUrl: string | null;
            } | null;
            nombre: string;
            id: string;
            updatedAt: Date;
            fechaHora: Date;
            ubicacion: string;
            escenario: string | null;
            bannerUrl: string | null;
            estado: import(".prisma/client").$Enums.EstadoEvento;
        }[];
        artistas: {
            nombre: string;
            id: string;
            updatedAt: Date;
            imagenUrl: string | null;
        }[];
        generadoEn: string;
        total: {
            eventos: number;
            artistas: number;
        };
    }>;
}
