import { EstadoEvento } from '@prisma/client';
export declare class CreateEventoDto {
    nombre: string;
    fechaHora: string;
    ubicacion: string;
    escenario?: string;
    descripcion?: string;
    capacidad: number;
    bannerUrl?: string;
    estado?: EstadoEvento;
    artistaId?: string;
}
