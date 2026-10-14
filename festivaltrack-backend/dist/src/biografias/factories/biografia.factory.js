"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.BiografiaFactoryProducer = exports.CuriosidadFactory = exports.FotoStorageFactory = exports.HitoHistoricoFactory = exports.BiografiaAltaDto = void 0;
class BiografiaAltaDto {
    artistaId;
    artistaNombre;
    titulo;
    descripcion;
    citaCelebre;
    anoOEpoca;
    imagenUrl;
    audioUrl;
    tipo;
}
exports.BiografiaAltaDto = BiografiaAltaDto;
class HitoHistoricoFactory {
    crear(dto) {
        const ano = dto.anoOEpoca || '1954';
        return {
            artistaId: dto.artistaId || 'art_jose_alfredo',
            descripcion: dto.descripcion,
            citaCelebre: dto.citaCelebre || '"Sigo siendo el Rey"',
            hitos: [
                {
                    ano: ano,
                    titulo: dto.titulo,
                    descripcion: dto.descripcion,
                    icono: 'flag',
                },
            ],
            discografia: [],
            metadata: {
                tipo: 'HISTORICO',
                titulo: dto.titulo,
                anoOEpoca: ano,
                imagenStorageUrl: dto.imagenUrl,
                audioUrl: dto.audioUrl,
                firestoreColeccion: 'biografias_galeria',
                registradoPorAdmin: true,
                creadoEn: new Date().toISOString(),
            },
        };
    }
}
exports.HitoHistoricoFactory = HitoHistoricoFactory;
class FotoStorageFactory {
    crear(dto) {
        const url = dto.imagenUrl || 'gs://festival-josealfredo.appspot.com/biografias/archivo_historico.jpg';
        return {
            artistaId: dto.artistaId || 'art_jose_alfredo',
            descripcion: dto.descripcion,
            citaCelebre: dto.citaCelebre,
            hitos: [],
            discografia: [],
            metadata: {
                tipo: 'STORAGE_FOTO',
                titulo: dto.titulo,
                anoOEpoca: dto.anoOEpoca || 'Archivo Histórico',
                imagenStorageUrl: url,
                audioUrl: dto.audioUrl,
                firestoreColeccion: 'biografias_galeria',
                registradoPorAdmin: true,
                creadoEn: new Date().toISOString(),
            },
        };
    }
}
exports.FotoStorageFactory = FotoStorageFactory;
class CuriosidadFactory {
    crear(dto) {
        return {
            artistaId: dto.artistaId || 'art_jose_alfredo',
            descripcion: dto.descripcion,
            citaCelebre: dto.citaCelebre || '"¡Y no me rajo!"',
            hitos: [],
            discografia: [],
            metadata: {
                tipo: 'CURIOSIDAD',
                titulo: dto.titulo,
                anoOEpoca: dto.anoOEpoca || 'Anécdota Legendaria',
                imagenStorageUrl: dto.imagenUrl,
                audioUrl: dto.audioUrl,
                firestoreColeccion: 'biografias_galeria',
                registradoPorAdmin: true,
                creadoEn: new Date().toISOString(),
            },
        };
    }
}
exports.CuriosidadFactory = CuriosidadFactory;
class BiografiaFactoryProducer {
    static getFactory(tipo) {
        switch (tipo?.toUpperCase()) {
            case 'STORAGE_FOTO':
            case 'ARCHIVE_PHOTO':
                return new FotoStorageFactory();
            case 'CURIOSIDAD':
            case 'ANECDOTE_CURIOSITY':
                return new CuriosidadFactory();
            case 'HISTORICO':
            case 'HISTORICAL_MILESTONE':
            default:
                return new HitoHistoricoFactory();
        }
    }
    static crear(dto) {
        const factory = this.getFactory(dto.tipo);
        return factory.crear(dto);
    }
}
exports.BiografiaFactoryProducer = BiografiaFactoryProducer;
//# sourceMappingURL=biografia.factory.js.map