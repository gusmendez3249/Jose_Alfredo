import { Body, Controller, Post, Get, Put, Param, UseGuards, Request } from '@nestjs/common';
import { AuthService } from './auth.service';
import { LoginDto } from './dto/login.dto';
import { RegisterDto } from './dto/register.dto';
import { RoleUpdateDto } from './dto/role-update.dto';
import { TvSyncDto } from './dto/tv-sync.dto';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { RolesGuard } from '../common/guards/roles.guard';
import { Roles } from '../common/decorators/roles.decorator';
import { Throttle } from '@nestjs/throttler';

@Controller('auth')
export class AuthController {
  constructor(private authService: AuthService) {}

  // Rate limiting (#51): al superar el límite responde 429 Too Many Requests.
  // Registro: máx. 3 cuentas por minuto desde la misma IP (evita altas masivas).
  @Throttle({ default: { limit: 3, ttl: 60_000 } })
  @Post('register') register(@Body() dto: RegisterDto) { return this.authService.register(dto); }
  // Login: máx. 5 intentos por minuto por IP + correo (ver AppThrottlerGuard). Frena la
  // fuerza bruta contra una cuenta sin bloquear a otros usuarios que comparten la IP.
  @Throttle({ default: { limit: 5, ttl: 60_000 } })
  @Post('login')    login(@Body() dto: LoginDto)       { return this.authService.login(dto); }
  // Vincular una TV exige sesión: el usuario sale del JWT y no del cuerpo (evita vincular cuentas ajenas).
  @UseGuards(JwtAuthGuard)
  @Post('tv-sync')  tvSync(@Body() body: TvSyncDto, @Request() req: any) {
    return this.authService.tvSync({ tvToken: body.tvToken, userId: req.user.id });
  }

  @UseGuards(JwtAuthGuard, RolesGuard) @Roles('ADMINISTRADOR')
  @Get('usuarios')
  getUsuarios() {
    return this.authService.getUsuarios();
  }

  @UseGuards(JwtAuthGuard, RolesGuard) @Roles('ADMINISTRADOR')
  @Post('register-admin')
  registerAdmin(@Body() dto: RegisterDto) {
    return this.authService.registerAdmin(dto);
  }

  @UseGuards(JwtAuthGuard, RolesGuard) @Roles('ADMINISTRADOR')
  @Put('usuarios/:id/rol')
  updateRole(@Param('id') id: string, @Body() dto: RoleUpdateDto) {
    return this.authService.updateRole(id, dto.rol);
  }
}
