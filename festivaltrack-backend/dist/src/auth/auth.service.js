"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.AuthService = void 0;
const common_1 = require("@nestjs/common");
const jwt_1 = require("@nestjs/jwt");
const prisma_service_1 = require("../prisma/prisma.service");
const bcrypt = __importStar(require("bcryptjs"));
const client_1 = require("@prisma/client");
let AuthService = class AuthService {
    prisma;
    jwt;
    constructor(prisma, jwt) {
        this.prisma = prisma;
        this.jwt = jwt;
    }
    async register(dto) {
        const existe = await this.prisma.usuario.findUnique({ where: { correo: dto.correo } });
        if (existe)
            throw new common_1.ConflictException('El correo ya está registrado');
        const hash = await bcrypt.hash(dto.contrasena, 10);
        const usuario = await this.prisma.usuario.create({
            data: { nombre: dto.nombre, correo: dto.correo, contrasena: hash },
        });
        return this.firmarToken(usuario.id, usuario.nombre, usuario.correo, usuario.rol);
    }
    async login(dto) {
        const usuario = await this.prisma.usuario.findUnique({ where: { correo: dto.correo } });
        if (!usuario)
            throw new common_1.UnauthorizedException('Credenciales inválidas');
        const valida = await bcrypt.compare(dto.contrasena, usuario.contrasena);
        if (!valida)
            throw new common_1.UnauthorizedException('Credenciales inválidas');
        return this.firmarToken(usuario.id, usuario.nombre, usuario.correo, usuario.rol);
    }
    firmarToken(id, nombre, correo, rol) {
        const token = this.jwt.sign({ sub: id, correo, rol });
        return {
            accessToken: token,
            token,
            usuario: { id, nombre, correo, rol }
        };
    }
    async tvSync(body) {
        return { status: 'SUCCESS', message: 'Smart TV vinculada correctamente', tvToken: body.tvToken };
    }
    async getUsuarios() {
        return this.prisma.usuario.findMany({
            select: { id: true, nombre: true, correo: true, rol: true },
            orderBy: { rol: 'asc' }
        });
    }
    async registerAdmin(dto) {
        const existe = await this.prisma.usuario.findUnique({ where: { correo: dto.correo } });
        if (existe)
            throw new common_1.ConflictException('El correo ya está registrado');
        const hash = await bcrypt.hash(dto.contrasena, 10);
        const usuario = await this.prisma.usuario.create({
            data: { nombre: dto.nombre, correo: dto.correo, contrasena: hash, rol: client_1.Rol.ADMINISTRADOR },
        });
        return this.firmarToken(usuario.id, usuario.nombre, usuario.correo, usuario.rol);
    }
    async updateRole(id, rol) {
        console.log('Update role called with:', { id, rol });
        if (rol !== 'USUARIO' && rol !== 'ADMINISTRADOR') {
            throw new common_1.ConflictException(`Rol inválido recibido: ${rol}`);
        }
        return this.prisma.usuario.update({
            where: { id },
            data: { rol: client_1.Rol[rol] },
            select: { id: true, nombre: true, correo: true, rol: true }
        });
    }
};
exports.AuthService = AuthService;
exports.AuthService = AuthService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService, jwt_1.JwtService])
], AuthService);
//# sourceMappingURL=auth.service.js.map