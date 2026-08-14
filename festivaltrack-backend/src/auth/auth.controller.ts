import { Body, Controller, Post, Get, Put, Param } from '@nestjs/common';
import { AuthService } from './auth.service';
import { LoginDto } from './dto/login.dto';
import { RegisterDto } from './dto/register.dto';

@Controller('auth')
export class AuthController {
  constructor(private authService: AuthService) {}

  @Post('register') register(@Body() dto: RegisterDto) { return this.authService.register(dto); }
  @Post('login')    login(@Body() dto: LoginDto)       { return this.authService.login(dto); }
  @Post('tv-sync')  tvSync(@Body() body: { tvToken: string, userId: string }) { return this.authService.tvSync(body); }

  @Get('usuarios')
  getUsuarios() {
    return this.authService.getUsuarios();
  }

  @Post('register-admin')
  registerAdmin(@Body() dto: RegisterDto) {
    return this.authService.registerAdmin(dto);
  }

  @Put('usuarios/:id/rol')
  updateRole(@Param('id') id: string, @Body('rol') rol: string) {
    return this.authService.updateRole(id, rol);
  }
}
