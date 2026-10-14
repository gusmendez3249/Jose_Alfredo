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
exports.CancionesService = void 0;
const common_1 = require("@nestjs/common");
const prisma_service_1 = require("../prisma/prisma.service");
let CancionesService = class CancionesService {
    prisma;
    constructor(prisma) {
        this.prisma = prisma;
    }
    async findAll() {
        return this.prisma.cancion.findMany({
            orderBy: { createdAt: 'desc' },
        });
    }
    async findByEvento(eventoId) {
        const ligadas = await this.prisma.cancionTransmision.findMany({
            where: { transmision: { eventoId }, cancion: { estado: 'PUBLICADA' } },
            orderBy: { orden: 'asc' },
            include: { cancion: true },
        });
        return ligadas.map((l) => l.cancion);
    }
    async setRepertorio(eventoId, cancionIds) {
        if (!Array.isArray(cancionIds))
            throw new common_1.BadRequestException('cancionIds debe ser una lista');
        const ids = [...new Set(cancionIds)];
        const evento = await this.prisma.evento.findUnique({ where: { id: eventoId } });
        if (!evento)
            throw new common_1.NotFoundException('Evento no encontrado');
        const existentes = await this.prisma.cancion.count({ where: { id: { in: ids } } });
        if (existentes !== ids.length)
            throw new common_1.BadRequestException('Alguna canción no existe');
        await this.prisma.$transaction(async (tx) => {
            const transmision = await tx.transmision.upsert({
                where: { eventoId },
                create: { eventoId, titulo: evento.nombre },
                update: {},
            });
            await tx.cancionTransmision.deleteMany({ where: { transmisionId: transmision.id } });
            if (ids.length > 0) {
                await tx.cancionTransmision.createMany({
                    data: ids.map((cancionId, orden) => ({ cancionId, transmisionId: transmision.id, orden })),
                });
            }
        });
        return this.findByEvento(eventoId);
    }
    async create(dto) {
        let admin = await this.prisma.administrador.findFirst();
        if (!admin) {
            let user = await this.prisma.usuario.findFirst({ where: { rol: 'ADMINISTRADOR' } });
            if (!user) {
                user = await this.prisma.usuario.create({
                    data: { nombre: 'Admin', correo: 'admin@admin.com', contrasena: 'admin123', rol: 'ADMINISTRADOR' }
                });
            }
            admin = await this.prisma.administrador.create({ data: { usuarioId: user.id } });
        }
        return this.prisma.cancion.create({
            data: {
                titulo: dto.titulo,
                artista: dto.artista || 'José Alfredo Jiménez',
                duracion: dto.duracion || 180,
                archivoUrl: dto.archivoUrl || 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',
                portadaUrl: dto.portadaUrl || null,
                genero: dto.genero || 'Ranchera',
                estado: 'PUBLICADA',
                administradorId: admin.id,
            }
        });
    }
    async remove(id) {
        return this.prisma.cancion.delete({ where: { id } });
    }
};
exports.CancionesService = CancionesService;
exports.CancionesService = CancionesService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService])
], CancionesService);
//# sourceMappingURL=canciones.service.js.map