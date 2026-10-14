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
exports.CancionesController = void 0;
const common_1 = require("@nestjs/common");
const platform_express_1 = require("@nestjs/platform-express");
const multer_1 = require("multer");
const jwt_auth_guard_1 = require("../common/guards/jwt-auth.guard");
const roles_guard_1 = require("../common/guards/roles.guard");
const roles_decorator_1 = require("../common/decorators/roles.decorator");
const canciones_service_1 = require("./canciones.service");
const ARCHIVO_EXT = {
    'audio/mpeg': '.mp3',
    'audio/mp3': '.mp3',
    'audio/mp4': '.m4a',
    'audio/x-m4a': '.m4a',
    'audio/aac': '.aac',
    'audio/wav': '.wav',
    'audio/x-wav': '.wav',
    'audio/ogg': '.ogg',
    'image/jpeg': '.jpg',
    'image/png': '.png',
    'image/webp': '.webp',
};
let CancionesController = class CancionesController {
    cancionesService;
    constructor(cancionesService) {
        this.cancionesService = cancionesService;
    }
    findAll() {
        return this.cancionesService.findAll();
    }
    findByEvento(eventoId) {
        return this.cancionesService.findByEvento(eventoId);
    }
    setRepertorio(eventoId, cancionIds) {
        return this.cancionesService.setRepertorio(eventoId, cancionIds);
    }
    upload(file) {
        if (!file)
            throw new common_1.BadRequestException('Archivo no proveído');
        return { url: `/uploads/${file.filename}` };
    }
    create(body) {
        return this.cancionesService.create(body);
    }
    remove(id) {
        return this.cancionesService.remove(id);
    }
};
exports.CancionesController = CancionesController;
__decorate([
    (0, common_1.Get)(),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", void 0)
], CancionesController.prototype, "findAll", null);
__decorate([
    (0, common_1.Get)('evento/:eventoId'),
    __param(0, (0, common_1.Param)('eventoId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", void 0)
], CancionesController.prototype, "findByEvento", null);
__decorate([
    (0, common_1.UseGuards)(jwt_auth_guard_1.JwtAuthGuard, roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('ADMINISTRADOR'),
    (0, common_1.Put)('evento/:eventoId'),
    __param(0, (0, common_1.Param)('eventoId')),
    __param(1, (0, common_1.Body)('cancionIds')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Array]),
    __metadata("design:returntype", void 0)
], CancionesController.prototype, "setRepertorio", null);
__decorate([
    (0, common_1.UseGuards)(jwt_auth_guard_1.JwtAuthGuard, roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)('ADMINISTRADOR'),
    (0, common_1.Post)('upload'),
    (0, common_1.UseInterceptors)((0, platform_express_1.FileInterceptor)('file', {
        limits: { fileSize: 30 * 1024 * 1024 },
        fileFilter: (req, file, cb) => {
            if (!ARCHIVO_EXT[file.mimetype]) {
                return cb(new common_1.BadRequestException('Formato no permitido (usa MP3, M4A, WAV, OGG, JPG, PNG o WEBP)'), false);
            }
            cb(null, true);
        },
        storage: (0, multer_1.diskStorage)({
            destination: './uploads',
            filename: (req, file, cb) => {
                const sufijo = Date.now() + '-' + Math.round(Math.random() * 1e9);
                cb(null, `cancion-${sufijo}${ARCHIVO_EXT[file.mimetype]}`);
            },
        }),
    })),
    __param(0, (0, common_1.UploadedFile)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", void 0)
], CancionesController.prototype, "upload", null);
__decorate([
    (0, common_1.Post)(),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", void 0)
], CancionesController.prototype, "create", null);
__decorate([
    (0, common_1.Delete)(':id'),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", void 0)
], CancionesController.prototype, "remove", null);
exports.CancionesController = CancionesController = __decorate([
    (0, common_1.Controller)('canciones'),
    __metadata("design:paramtypes", [canciones_service_1.CancionesService])
], CancionesController);
//# sourceMappingURL=canciones.controller.js.map