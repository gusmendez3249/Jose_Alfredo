import { Body, Controller, Post, Get, Put, Param, UseGuards } from '@nestjs/common';
import { AuthService } from './auth.service';
import { LoginDto } from './dto/login.dto';
import { RegisterDto } from './dto/register.dto';
import { RoleUpdateDto } from './dto/role-update.dto';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { RolesGuard } from '../common/guards/roles.guard';
import { Roles } from '../common/decorators/roles.decorator';
import { Throttle } from '@nestjs/throttler';

@Controller('auth')
export class AuthController {
  constructor(private authService: AuthService) {}

  // Anti fuerza bruta / altas masivas: al superar el límite responde 429.
  @Throttle({ default: { limit: 3, ttl: 60_000 } })
  @Post('register') register(@Body() dto: RegisterDto) { return this.authService.register(dto); }
  @Throttle({ default: { limit: 5, ttl: 60_000 } })
  @Post('login')    login(@Body() dto: LoginDto)       { return this.authService.login(dto); }
  @Post('tv-sync')  tvSync(@Body() body: { tvToken: string, userId: string }) { return this.authService.tvSync(body); }

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
