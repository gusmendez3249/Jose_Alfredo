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
        let { eventoId, categoria, cantidad, precioTotal, metodoPago } = body;
        const cantidadFinal = Number(cantidad || 1);
        const precioUnitario = Number(precioTotal || 4500);
        const montoTotal = precioUnitario * cantidadFinal;
        let evento = eventoId ? await this.prisma.evento.findUnique({ where: { id: eventoId } }) : null;
        if (!evento) {
            evento = await this.prisma.evento.findFirst();
        }
        if (!evento) {
            let admin = await this.prisma.administrador.findFirst();
            if (!admin) {
                let adminUser = await this.prisma.usuario.findFirst({ where: { rol: 'ADMINISTRADOR' } });
                if (!adminUser) {
                    adminUser = await this.prisma.usuario.create({
                        data: {
                            nombre: 'Admin Sistema',
                            correo: 'admin@festivaltrack.com',
                            contrasena: '$2a$10$abcdefghijklmnopqrstuv',
                            rol: 'ADMINISTRADOR'
                        }
                    });
                }
                admin = await this.prisma.administrador.create({
                    data: {
                        usuarioId: adminUser.id,
                        nivel: 1
                    }
                });
            }
            evento = await this.prisma.evento.create({
                data: {
                    nombre: 'Gala Inaugural José Alfredo Jiménez',
                    fechaHora: new Date('2026-11-23T20:00:00Z'),
                    ubicacion: 'Dolores Hidalgo, Gto.',
                    escenario: 'Escenario Principal',
                    capacidad: 5000,
                    estado: 'PUBLICADO',
                    administradorId: admin.id
                }
            });
        }
        const realEventoId = evento.id;
        const resultado = await this.prisma.$transaction(async (tx) => {
            const boleto = await tx.boleto.create({
                data: {
                    eventoId: realEventoId,
                    usuarioId,
                    categoria: categoria || 'GENERAL',
                    precio: montoTotal,
                },
            });
            await tx.pago.create({
                data: {
                    boletoId: boleto.id,
                    monto: montoTotal,
                    metodo: metodoPago || 'TARJETA_CREDITO',
                    estado: 'COMPLETADO',
                    referencia: `REF-${Date.now()}-${Math.floor(Math.random() * 1000)}`
                }
            });
            const qrFirma = `FESTIVAL-TICKET-2024::${boleto.id}`;
            const boletoFinal = await tx.boleto.update({
                where: { id: boleto.id },
                data: { codigoQR: qrFirma },
                include: { pago: true }
            });
            return boletoFinal;
        });
        return resultado;
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