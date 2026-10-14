export declare class BiografiaAltaDto {
    artistaId?: string;
    artistaNombre?: string;
    titulo: string;
    descripcion: string;
    citaCelebre?: string;
    anoOEpoca?: string;
    imagenUrl?: string;
    audioUrl?: string;
    tipo?: string;
}
export interface BiografiaGenerada {
    artistaId: string;
    descripcion: string;
    citaCelebre?: string;
    hitos: any[];
    discografia: any[];
    metadata: {
        tipo: string;
        titulo: string;
        anoOEpoca: string;
        imagenStorageUrl?: string;
        audioUrl?: string;
        firestoreColeccion: string;
        registradoPorAdmin: boolean;
        creadoEn: string;
    };
}
export interface IBiografiaFactory {
    crear(dto: BiografiaAltaDto): BiografiaGenerada;
}
export declare class HitoHistoricoFactory implements IBiografiaFactory {
    crear(dto: BiografiaAltaDto): BiografiaGenerada;
}
export declare class FotoStorageFactory implements IBiografiaFactory {
    crear(dto: BiografiaAltaDto): BiografiaGenerada;
}
export declare class CuriosidadFactory implements IBiografiaFactory {
    crear(dto: BiografiaAltaDto): BiografiaGenerada;
}
export declare class BiografiaFactoryProducer {
    static getFactory(tipo?: string): IBiografiaFactory;
    static crear(dto: BiografiaAltaDto): BiografiaGenerada;
}
