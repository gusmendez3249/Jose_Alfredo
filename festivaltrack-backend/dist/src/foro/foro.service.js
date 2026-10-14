"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.ForoService = void 0;
const common_1 = require("@nestjs/common");
const client_1 = require("@prisma/client");
const prisma_service_1 = require("../prisma/prisma.service");
const MAX_CONTENIDO = 1000;
const MAX_TITULO = 80;
const MAX_LUGAR = 60;
const MAX_COMENTARIO = 400;
let ForoService = class ForoService {
    prisma;
    constructor(prisma) {
        this.prisma = prisma;
    }
    includePublicacion(usuarioId) {
        return {
            autor: { select: { id: true, nombre: true } },
            _count: { select: { likes: true, comentarios: true } },
            likes: usuarioId
                ? { where: { usuarioId }, select: { usuarioId: true } }
                : false,
        };
    }
    mapPublicacion(p) {
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
    async listar(usuario, opts) {
        const limit = Math.min(Math.max(Number(opts.limit) || 20, 1), 50);
        const where = {};
        if (opts.tipo && opts.tipo in client_1.TipoPublicacion) {
            where.tipo = opts.tipo;
        }
        if (opts.before) {
            const fecha = new Date(opts.before);
            if (isNaN(fecha.getTime()))
                throw new common_1.BadRequestException('Cursor inválido');
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
    async crear(usuario, dto) {
        const contenido = (dto.contenido ?? '').trim();
        const imagenUrl = dto.imagenUrl?.trim() || null;
        const tipo = dto.tipo && dto.tipo in client_1.TipoPublicacion
            ? dto.tipo
            : imagenUrl
                ? client_1.TipoPublicacion.FOTO
                : client_1.TipoPublicacion.EXPERIENCIA;
        if (!contenido && !imagenUrl) {
            throw new common_1.BadRequestException('Escribe algo o adjunta una foto');
        }
        if (tipo === client_1.TipoPublicacion.FOTO && !imagenUrl) {
            throw new common_1.BadRequestException('Una publicación de foto necesita imagen');
        }
        if (contenido.length > MAX_CONTENIDO) {
            throw new common_1.BadRequestException(`Máximo ${MAX_CONTENIDO} caracteres`);
        }
        if (imagenUrl && !/^(\/uploads\/[\w.-]+|https:\/\/.+)$/.test(imagenUrl)) {
            throw new common_1.BadRequestException('URL de imagen no válida');
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
    async eliminar(usuario, id) {
        const pub = await this.prisma.publicacion.findUnique({ where: { id } });
        if (!pub)
            throw new common_1.NotFoundException('Publicación no encontrada');
        if (pub.autorId !== usuario.id && usuario.rol !== 'ADMINISTRADOR') {
            throw new common_1.ForbiddenException('No puedes eliminar esta publicación');
        }
        await this.prisma.publicacion.delete({ where: { id } });
        return { ok: true };
    }
    async toggleLike(usuario, publicacionId) {
        const existe = await this.prisma.publicacion.findUnique({
            where: { id: publicacionId },
            select: { id: true },
        });
        if (!existe)
            throw new common_1.NotFoundException('Publicación no encontrada');
        const key = { publicacionId_usuarioId: { publicacionId, usuarioId: usuario.id } };
        const previo = await this.prisma.likePublicacion.findUnique({ where: key });
        if (previo) {
            await this.prisma.likePublicacion.deleteMany({
                where: { publicacionId, usuarioId: usuario.id },
            });
        }
        else {
            await this.prisma.likePublicacion.upsert({
                where: key,
                create: { publicacionId, usuarioId: usuario.id },
                update: {},
            });
        }
        const likes = await this.prisma.likePublicacion.count({ where: { publicacionId } });
        return { liked: !previo, likes };
    }
    async comentarios(publicacionId) {
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
    async comentar(usuario, publicacionId, texto) {
        const limpio = (texto ?? '').trim();
        if (!limpio)
            throw new common_1.BadRequestException('El comentario está vacío');
        if (limpio.length > MAX_COMENTARIO) {
            throw new common_1.BadRequestException(`Máximo ${MAX_COMENTARIO} caracteres`);
        }
        const existe = await this.prisma.publicacion.findUnique({
            where: { id: publicacionId },
            select: { id: true },
        });
        if (!existe)
            throw new common_1.NotFoundException('Publicación no encontrada');
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
    async eliminarComentario(usuario, id) {
        const c = await this.prisma.comentarioPublicacion.findUnique({ where: { id } });
        if (!c)
            throw new common_1.NotFoundException('Comentario no encontrado');
        if (c.autorId !== usuario.id && usuario.rol !== 'ADMINISTRADOR') {
            throw new common_1.ForbiddenException('No puedes eliminar este comentario');
        }
        await this.prisma.comentarioPublicacion.delete({ where: { id } });
        return { ok: true };
    }
};
exports.ForoService = ForoService;
exports.ForoService = ForoService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService])
], ForoService);
//# sourceMappingURL=foro.service.js.map