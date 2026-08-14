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
exports.GaleriaService = void 0;
const common_1 = require("@nestjs/common");
const prisma_service_1 = require("../prisma/prisma.service");
let GaleriaService = class GaleriaService {
    prisma;
    constructor(prisma) {
        this.prisma = prisma;
    }
    async getGaleria() {
        return this.prisma.galeria.findMany({
            include: {
                imagenes: {
                    orderBy: { createdAt: 'desc' }
                }
            },
            orderBy: { createdAt: 'desc' }
        });
    }
    async addImagen(dto) {
        let galeria = await this.prisma.galeria.findFirst();
        if (!galeria) {
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
            galeria = await this.prisma.galeria.create({
                data: { nombre: 'Golden Era', categoria: 'GOLDEN_ERA', administradorId: admin.id }
            });
        }
        return this.prisma.imagen.create({
            data: {
                galeriaId: galeria.id,
                url: dto.url || 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=800&q=80',
                titulo: dto.titulo || 'Nueva Foto del Festival',
            }
        });
    }
    async removeImagen(id) {
        return this.prisma.imagen.delete({ where: { id } });
    }
};
exports.GaleriaService = GaleriaService;
exports.GaleriaService = GaleriaService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService])
], GaleriaService);
//# sourceMappingURL=galeria.service.js.map