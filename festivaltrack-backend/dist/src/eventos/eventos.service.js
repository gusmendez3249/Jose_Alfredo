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
exports.EventosService = void 0;
const common_1 = require("@nestjs/common");
const prisma_service_1 = require("../prisma/prisma.service");
let EventosService = class EventosService {
    prisma;
    constructor(prisma) {
        this.prisma = prisma;
    }
    findAll() {
        return this.prisma.evento.findMany({
            where: { estado: { not: 'CANCELADO' } },
            include: { artista: true },
            orderBy: { fechaHora: 'asc' },
        });
    }
    async findOne(id) {
        const evento = await this.prisma.evento.findUnique({
            where: { id },
            include: { artista: true, transmision: true },
        });
        if (!evento)
            throw new common_1.NotFoundException('Evento no encontrado');
        return evento;
    }
    async create(dto, usuarioId) {
        let administrador = await this.prisma.administrador.findUnique({
            where: { usuarioId },
        });
        if (!administrador) {
            administrador = await this.prisma.administrador.create({
                data: { usuarioId, nivel: 1, permisos: ['CREAR_EVENTOS', 'EDITAR_EVENTOS', 'ELIMINAR_EVENTOS'] },
            });
        }
        const adminId = administrador.id;
        const ultimoEvento = await this.prisma.evento.findFirst({
            where: { id: { startsWith: 'EVT-' } },
            orderBy: { id: 'desc' },
        });
        let nuevoId = 'EVT-001';
        if (ultimoEvento && ultimoEvento.id) {
            const coincidencia = ultimoEvento.id.match(/^EVT-(\d+)$/);
            if (coincidencia) {
                const ultimoNumero = parseInt(coincidencia[1], 10);
                nuevoId = `EVT-${String(ultimoNumero + 1).padStart(3, '0')}`;
            }
        }
        const fechaHoraParsed = dto.fechaHora ? new Date(dto.fechaHora) : new Date('2026-11-23T20:00:00Z');
        const estadoParsed = dto.estado === 'ACTIVO' ? 'PUBLICADO' : (dto.estado || 'PUBLICADO');
        return this.prisma.evento.create({
            data: {
                nombre: dto.nombre,
                fechaHora: fechaHoraParsed,
                ubicacion: dto.ubicacion,
                escenario: dto.escenario,
                descripcion: dto.descripcion,
                capacidad: dto.capacidad,
                bannerUrl: dto.bannerUrl,
                artistaId: dto.artistaId,
                estado: estadoParsed,
                id: nuevoId,
                administradorId: adminId,
            }
        });
    }
    async update(id, dto) {
        await this.findOne(id);
        const dataToUpdate = { ...dto };
        if (dto.fechaHora) {
            dataToUpdate.fechaHora = new Date(dto.fechaHora);
        }
        if (dto.estado === 'ACTIVO') {
            dataToUpdate.estado = 'PUBLICADO';
        }
        return this.prisma.evento.update({ where: { id }, data: dataToUpdate });
    }
    async remove(id) {
        await this.findOne(id);
        return this.prisma.evento.update({ where: { id }, data: { estado: 'CANCELADO' } });
    }
};
exports.EventosService = EventosService;
exports.EventosService = EventosService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService])
], EventosService);
//# sourceMappingURL=eventos.service.js.map