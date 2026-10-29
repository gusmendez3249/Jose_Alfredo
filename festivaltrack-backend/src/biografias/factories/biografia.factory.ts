/**
 * Patrón FACTORY (Creation Factory Pattern) para el alta de biografías
 * por el Administrador en el backend NestJS de FestivalTrack.
 */

export class BiografiaAltaDto {
  artistaId?: string;
  artistaNombre?: string;
  titulo: string;
  descripcion: string;
  citaCelebre?: string;
  anoOEpoca?: string;
  imagenUrl?: string;
  audioUrl?: string;
  tipo?: string; // "HISTORICO" | "STORAGE_FOTO" | "CURIOSIDAD"
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

/**
 * Fábrica concreta 1: Crea hitos históricos para la cronología
 */
export class HitoHistoricoFactory implements IBiografiaFactory {
  crear(dto: BiografiaAltaDto): BiografiaGenerada {
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

/**
 * Fábrica concreta 2: Crea fotografías de archivo para Firebase Storage
 */
export class FotoStorageFactory implements IBiografiaFactory {
  crear(dto: BiografiaAltaDto): BiografiaGenerada {
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

/**
 * Fábrica concreta 3: Crea anécdotas y curiosidades inéditas
 */
export class CuriosidadFactory implements IBiografiaFactory {
  crear(dto: BiografiaAltaDto): BiografiaGenerada {
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

/**
 * Producer / Selector central del Factory Pattern
 */
export class BiografiaFactoryProducer {
  static getFactory(tipo?: string): IBiografiaFactory {
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

  static crear(dto: BiografiaAltaDto): BiografiaGenerada {
    const factory = this.getFactory(dto.tipo);
    return factory.crear(dto);
  }
}
