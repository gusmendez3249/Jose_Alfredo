import {
  BadRequestException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { Prisma, TipoPublicacion } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';

export interface UsuarioActual {
  id: string;
  rol: string;
}

const MAX_CONTENIDO = 1000;
const MAX_TITULO = 80;
const MAX_LUGAR = 60;
const MAX_COMENTARIO = 400;

/**
 * Foro de la Galería Interactiva: publicaciones (experiencias, recomendaciones
 * y fotos) con "me gusta" y comentarios compartidos entre todos los usuarios.
 */
@Injectable()
export class ForoService {
  constructor(private prisma: PrismaService) {}

  private includePublicacion(usuarioId?: string) {
    return {
      autor: { select: { id: true, nombre: true } },
      _count: { select: { likes: true, comentarios: true } },
      likes: usuarioId
        ? { where: { usuarioId }, select: { usuarioId: true } }
        : false,
    } satisfies Prisma.PublicacionInclude;
  }

  private mapPublicacion(p: any) {
    return {
      id: p.id,
      tipo: p.tipo,
      titulo: p.titulo,
      contenido: p.contenido,
      imagenUrl: p.imagenUrl,
      lugar: p.lugar,
      createdAt: p.createdAt,
      autorId: p.autor.id,
      autorNombre: p.autor.nombre,
      likes: p._count.likes,
      comentarios: p._count.comentarios,
      likedByMe: Array.isArray(p.likes) && p.likes.length > 0,
    };
  }

  /**
   * Lista paginada (más recientes primero). `before` es el `createdAt` del
   * último elemento recibido; `tipo` filtra por categoría.
   */
  async listar(
    usuario: UsuarioActual | null,
    opts: { tipo?: string; before?: string; limit?: number },
  ) {
    const limit = Math.min(Math.max(Number(opts.limit) || 20, 1), 50);
    const where: Prisma.PublicacionWhereInput = {};
    if (opts.tipo && opts.tipo in TipoPublicacion) {
      where.tipo = opts.tipo as TipoPublicacion;
    }
    if (opts.before) {
      const fecha = new Date(opts.before);
      if (isNaN(fecha.getTime())) throw new BadRequestException('Cursor inválido');
      where.createdAt = { lt: fecha };
    }
    const items = await this.prisma.publicacion.findMany({
      where,
      orderBy: { createdAt: 'desc' },
      take: limit,
      include: this.includePublicacion(usuario?.id),
    });
    return items.map((p) => this.mapPublicacion(p));
  }

  async crear(
    usuario: UsuarioActual,
    dto: {
      tipo?: string;
      titulo?: string;
      contenido?: string;
      imagenUrl?: string;
      lugar?: string;
    },
  ) {
    const contenido = (dto.contenido ?? '').trim();
    const imagenUrl = dto.imagenUrl?.trim() || null;
    const tipo =
      dto.tipo && dto.tipo in TipoPublicacion
        ? (dto.tipo as TipoPublicacion)
        : imagenUrl
          ? TipoPublicacion.FOTO
          : TipoPublicacion.EXPERIENCIA;

    if (!contenido && !imagenUrl) {
      throw new BadRequestException('Escribe algo o adjunta una foto');
    }
    if (tipo === TipoPublicacion.FOTO && !imagenUrl) {
      throw new BadRequestException('Una publicación de foto necesita imagen');
    }
    if (contenido.length > MAX_CONTENIDO) {
      throw new BadRequestException(`Máximo ${MAX_CONTENIDO} caracteres`);
    }
    if (imagenUrl && !/^(\/uploads\/[\w.-]+|https:\/\/.+)$/.test(imagenUrl)) {
      throw new BadRequestException('URL de imagen no válida');
    }

    const creada = await this.prisma.publicacion.create({
      data: {
        autorId: usuario.id,
        tipo,
        titulo: dto.titulo?.trim().slice(0, MAX_TITULO) || null,
        contenido,
        imagenUrl,
        lugar: dto.lugar?.trim().slice(0, MAX_LUGAR) || null,
      },
      include: this.includePublicacion(usuario.id),
    });
    return this.mapPublicacion(creada);
  }

  async eliminar(usuario: UsuarioActual, id: string) {
    const pub = await this.prisma.publicacion.findUnique({ where: { id } });
    if (!pub) throw new NotFoundException('Publicación no encontrada');
    if (pub.autorId !== usuario.id && usuario.rol !== 'ADMINISTRADOR') {
      throw new ForbiddenException('No puedes eliminar esta publicación');
    }
    await this.prisma.publicacion.delete({ where: { id } });
    return { ok: true };
  }

  /** Alterna el "me gusta" del usuario y devuelve el estado final. */
  async toggleLike(usuario: UsuarioActual, publicacionId: string) {
    const existe = await this.prisma.publicacion.findUnique({
      where: { id: publicacionId },
      select: { id: true },
    });
    if (!existe) throw new NotFoundException('Publicación no encontrada');

    const key = { publicacionId_usuarioId: { publicacionId, usuarioId: usuario.id } };
    const previo = await this.prisma.likePublicacion.findUnique({ where: key });
    if (previo) {
      await this.prisma.likePublicacion.deleteMany({
        where: { publicacionId, usuarioId: usuario.id },
      });
    } else {
      await this.prisma.likePublicacion.upsert({
        where: key,
        create: { publicacionId, usuarioId: usuario.id },
        update: {},
      });
    }
    const likes = await this.prisma.likePublicacion.count({ where: { publicacionId } });
    return { liked: !previo, likes };
  }

  async comentarios(publicacionId: string) {
    const lista = await this.prisma.comentarioPublicacion.findMany({
      where: { publicacionId },
      orderBy: { createdAt: 'asc' },
      take: 200,
      include: { autor: { select: { id: true, nombre: true } } },
    });
    return lista.map((c) => ({
      id: c.id,
      texto: c.texto,
      createdAt: c.createdAt,
      autorId: c.autor.id,
      autorNombre: c.autor.nombre,
    }));
  }

  async comentar(usuario: UsuarioActual, publicacionId: string, texto?: string) {
    const limpio = (texto ?? '').trim();
    if (!limpio) throw new BadRequestException('El comentario está vacío');
    if (limpio.length > MAX_COMENTARIO) {
      throw new BadRequestException(`Máximo ${MAX_COMENTARIO} caracteres`);
    }
    const existe = await this.prisma.publicacion.findUnique({
      where: { id: publicacionId },
      select: { id: true },
    });
    if (!existe) throw new NotFoundException('Publicación no encontrada');

    const c = await this.prisma.comentarioPublicacion.create({
      data: { publicacionId, autorId: usuario.id, texto: limpio },
      include: { autor: { select: { id: true, nombre: true } } },
    });
    return {
      id: c.id,
      texto: c.texto,
      createdAt: c.createdAt,
      autorId: c.autor.id,
      autorNombre: c.autor.nombre,
    };
  }

  async eliminarComentario(usuario: UsuarioActual, id: string) {
    const c = await this.prisma.comentarioPublicacion.findUnique({ where: { id } });
    if (!c) throw new NotFoundException('Comentario no encontrado');
    if (c.autorId !== usuario.id && usuario.rol !== 'ADMINISTRADOR') {
      throw new ForbiddenException('No puedes eliminar este comentario');
    }
    await this.prisma.comentarioPublicacion.delete({ where: { id } });
    return { ok: true };
  }
}
