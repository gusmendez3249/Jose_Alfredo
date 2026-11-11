/**
 * Patrón FACTORY (Creation Factory Pattern) para el alta de biografías
 * por el Administrador en el backend NestJS de FestivalTrack.
 */
import { IsIn, IsNotEmpty, IsOptional, IsString, MaxLength } from 'class-validator';

/** Datos del alta. Los decoradores validan en el servidor (sin ellos, ValidationPipe descartaba todos los campos). */
export class BiografiaAltaDto {
  @IsOptional() @IsString() artistaId?: string;
  @IsOptional() @IsString() @MaxLength(150) artistaNombre?: string;
  @IsString() @IsNotEmpty() @MaxLength(200) titulo: string;
  @IsString() @IsNotEmpty() @MaxLength(5000) descripcion: string;
  @IsOptional() @IsString() @MaxLength(500) citaCelebre?: string;
  @IsOptional() @IsString() @MaxLength(50) anoOEpoca?: string;
  @IsOptional() @IsString() imagenUrl?: string;
  @IsOptional() @IsString() audioUrl?: string;
  @IsOptional() @IsIn(['HISTORICO', 'STORAGE_FOTO', 'CURIOSIDAD']) tipo?: string;
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
