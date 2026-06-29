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
exports.SyncService = void 0;
const common_1 = require("@nestjs/common");
const prisma_service_1 = require("../prisma/prisma.service");
let SyncService = class SyncService {
    prisma;
    constructor(prisma) {
        this.prisma = prisma;
    }
    async getWearPayload(since) {
        const fechaDesde = since ? new Date(since) : new Date(0);
        const [eventos, artistas] = await Promise.all([
            this.prisma.evento.findMany({
                where: { estado: { not: 'CANCELADO' }, updatedAt: { gte: fechaDesde } },
                select: {
                    id: true, nombre: true, fechaHora: true, ubicacion: true,
                    escenario: true, bannerUrl: true, estado: true, updatedAt: true,
                    artista: { select: { id: true, nombre: true, imagenUrl: true } },
                },
                orderBy: { fechaHora: 'asc' },
            }),
            this.prisma.artista.findMany({
                where: { activo: true, updatedAt: { gte: fechaDesde } },
                select: { id: true, nombre: true, imagenUrl: true, updatedAt: true },
            }),
        ]);
        await this.prisma.syncMeta.upsert({
            where: { dispositivo_entidad: { dispositivo: 'wear', entidad: 'all' } },
            create: { dispositivo: 'wear', entidad: 'all', lastSyncAt: new Date() },
            update: { lastSyncAt: new Date() },
        });
        return {
            eventos,
            artistas,
            generadoEn: new Date().toISOString(),
            total: { eventos: eventos.length, artistas: artistas.length },
        };
    }
};
exports.SyncService = SyncService;
exports.SyncService = SyncService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService])
], SyncService);
//# sourceMappingURL=sync.service.js.map