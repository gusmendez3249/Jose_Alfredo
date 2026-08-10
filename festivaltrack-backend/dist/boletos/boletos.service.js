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
exports.BoletosService = void 0;
const common_1 = require("@nestjs/common");
const prisma_service_1 = require("../prisma/prisma.service");
let BoletosService = class BoletosService {
    prisma;
    constructor(prisma) {
        this.prisma = prisma;
    }
    async comprar(usuarioId, body) {
        const { eventoId, categoria, cantidad, precioTotal } = body;
        const boleto = await this.prisma.boleto.create({
            data: {
                eventoId,
                usuarioId,
                categoria: categoria || 'GENERAL',
                precio: precioTotal,
            },
        });
        const qrFirma = `FESTIVAL-TICKET-2024::${boleto.id}`;
        const boletoFinal = await this.prisma.boleto.update({
            where: { id: boleto.id },
            data: { codigoQR: qrFirma },
        });
        return boletoFinal;
    }
    async misBoletos(usuarioId) {
        return this.prisma.boleto.findMany({
            where: { usuarioId },
            include: { evento: true },
            orderBy: { createdAt: 'desc' },
        });
    }
    async validarQr(qr) {
        const boleto = await this.prisma.boleto.findUnique({
            where: { codigoQR: qr },
            include: { evento: true, usuario: true }
        });
        if (!boleto) {
            throw new common_1.NotFoundException('Boleto no encontrado');
        }
        if (boleto.estado === 'USADO') {
            throw new common_1.BadRequestException('El boleto ya fue usado');
        }
        if (boleto.estado !== 'ACTIVO') {
            throw new common_1.BadRequestException('El boleto no es válido (estado: ' + boleto.estado + ')');
        }
        await this.prisma.boleto.update({
            where: { id: boleto.id },
            data: { estado: 'USADO' }
        });
        return { status: "APPROVED", message: "Acceso Concedido", evento: boleto.evento.nombre, usuario: boleto.usuario.nombre };
    }
};
exports.BoletosService = BoletosService;
exports.BoletosService = BoletosService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService])
], BoletosService);
//# sourceMappingURL=boletos.service.js.map