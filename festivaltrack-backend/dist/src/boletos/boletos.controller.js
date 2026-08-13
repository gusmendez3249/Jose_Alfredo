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
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.BoletosController = void 0;
const common_1 = require("@nestjs/common");
const boletos_service_1 = require("./boletos.service");
const jwt_auth_guard_1 = require("../common/guards/jwt-auth.guard");
const roles_guard_1 = require("../common/guards/roles.guard");
const roles_decorator_1 = require("../common/decorators/roles.decorator");
let BoletosController = class BoletosController {
    boletosService;
    constructor(boletosService) {
        this.boletosService = boletosService;
    }
    comprar(body, req) {
        return this.boletosService.comprar(req.user.id, body);
    }
    misBoletos(req) {
        return this.boletosService.misBoletos(req.user.id);
    }
    validarQr(qr) {
        return this.boletosService.validarQr(qr);
    }
};
exports.BoletosController = BoletosController;
__decorate([
    (0, common_1.UseGuards)(jwt_auth_guard_1.JwtAuthGuard),
    (0, common_1.Post)('comprar'),
    __param(0, (0, common_1.Body)()),
    __param(1, (0, common_1.Request)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object, Object]),
    __metadata("design:returntype", void 0)
], BoletosController.prototype, "comprar", null);
__decorate([
    (0, common_1.UseGuards)(jwt_auth_guard_1.JwtAuthGuard),
    (0, common_1.Get)('mis-boletos'),
    __param(0, (0, common_1.Request)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", void 0)
], BoletosController.prototype, "misBoletos", null);
__decorate([
    (0, common_1.UseGuards)(jwt_auth_guard_1.JwtAuthGuard, roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('ADMINISTRADOR'),
    (0, common_1.Post)('validar-qr'),
    __param(0, (0, common_1.Body)('qr')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", void 0)
], BoletosController.prototype, "validarQr", null);
exports.BoletosController = BoletosController = __decorate([
    (0, common_1.Controller)('boletos'),
    __metadata("design:paramtypes", [boletos_service_1.BoletosService])
], BoletosController);
//# sourceMappingURL=boletos.controller.js.map