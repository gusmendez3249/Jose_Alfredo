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
exports.StreamStatusController = void 0;
const common_1 = require("@nestjs/common");
let StreamStatusController = class StreamStatusController {
    streamUrl = '';
    isLive = false;
    getStatus() {
        let port = 1935;
        let emulatorUrl = '';
        if (this.streamUrl) {
            const match = this.streamUrl.match(/:(\d+)/);
            if (match)
                port = parseInt(match[1]);
            emulatorUrl = `rtsp://10.0.2.2:${port}`;
        }
        return {
            streamUrl: this.streamUrl,
            emulatorUrl,
            isLive: this.isLive,
            port,
        };
    }
    setStatus(body) {
        this.streamUrl = body.streamUrl || '';
        this.isLive = body.isLive !== false;
        return { ok: true, streamUrl: this.streamUrl, isLive: this.isLive };
    }
};
exports.StreamStatusController = StreamStatusController;
__decorate([
    (0, common_1.Get)(),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", void 0)
], StreamStatusController.prototype, "getStatus", null);
__decorate([
    (0, common_1.Post)(),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", void 0)
], StreamStatusController.prototype, "setStatus", null);
exports.StreamStatusController = StreamStatusController = __decorate([
    (0, common_1.Controller)('stream/status')
], StreamStatusController);
//# sourceMappingURL=stream-status.controller.js.map