import {
  Body,
  Controller,
  HttpCode,
  Post,
  Get,
  Put,
  Param,
  UseGuards,
  Request,
} from '@nestjs/common';
import { AuthService } from './auth.service';
import { LoginDto } from './dto/login.dto';
import { RegisterDto } from './dto/register.dto';
import { RoleUpdateDto } from './dto/role-update.dto';
import { TvEstadoDto, TvSyncDto } from './dto/tv-sync.dto';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { RolesGuard } from '../common/guards/roles.guard';
import { Roles } from '../common/decorators/roles.decorator';
import { Throttle } from '@nestjs/throttler';
import type { RequestConUsuario } from '../common/request-con-usuario';

@Controller('auth')
export class AuthController {
  constructor(private authService: AuthService) {}

  // Rate limiting (#51): al superar el límite responde 429 Too Many Requests.
  // Registro: máx. 3 cuentas por minuto desde la misma IP (evita altas masivas).
  @Throttle({ default: { limit: 3, ttl: 60_000 } })
  @Post('register')
  register(@Body() dto: RegisterDto) {
    return this.authService.register(dto);
  }
  // Login: máx. 5 intentos por minuto por IP + correo (ver AppThrottlerGuard). Frena la
  // fuerza bruta contra una cuenta sin bloquear a otros usuarios que comparten la IP.
  @Throttle({ default: { limit: 5, ttl: 60_000 } })
  @Post('login')
  login(@Body() dto: LoginDto) {
    return this.authService.login(dto);
  }
  // ── Inicio de sesión en la TV con el celular (ver VinculosTv) ──────────────────────────
  // 1. La TV pide un código (sin sesión). Máx. 10 por minuto por IP: un código dura 5 min.
  @Throttle({ default: { limit: 10, ttl: 60_000 } })
  @Post('tv-sync/codigo')
  crearCodigoTv() {
    return this.authService.crearCodigoTv();
  }

  // 2. El usuario lo aprueba desde la web o la app. Exige sesión: el usuario sale del JWT y no
  //    del cuerpo (evita vincular cuentas ajenas). Máx. 10 intentos por minuto contra adivinar códigos.
  @Throttle({ default: { limit: 10, ttl: 60_000 } })
  @UseGuards(JwtAuthGuard)
  @Post('tv-sync')
  tvSync(@Body() body: TvSyncDto, @Request() req: RequestConUsuario) {
    return this.authService.tvSync({
      codigo: body.codigo,
      userId: req.user.id,
    });
  }

  // 3. La TV pregunta cada pocos segundos con su token secreto (límite global de 300/min).
  @HttpCode(200)
  @Post('tv-sync/estado')
  estadoTv(@Body() body: TvEstadoDto) {
    return this.authService.estadoTv(body.tvToken);
  }

  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('ADMINISTRADOR')
  @Get('usuarios')
  getUsuarios() {
    return this.authService.getUsuarios();
  }

  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('ADMINISTRADOR')
  @Post('register-admin')
  registerAdmin(@Body() dto: RegisterDto) {
    return this.authService.registerAdmin(dto);
  }

  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('ADMINISTRADOR')
  @Put('usuarios/:id/rol')
  updateRole(
    @Param('id') id: string,
    @Body() dto: RoleUpdateDto,
    @Request() req: RequestConUsuario,
  ) {
    return this.authService.updateRole(id, dto.rol, req.user.id);
  }
}
