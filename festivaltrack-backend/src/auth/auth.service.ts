import { Injectable, UnauthorizedException, ConflictException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { PrismaService } from '../prisma/prisma.service';
import * as bcrypt from 'bcryptjs';
import { LoginDto } from './dto/login.dto';
import { RegisterDto } from './dto/register.dto';

@Injectable()
export class AuthService {
  constructor(private prisma: PrismaService, private jwt: JwtService) {}

  async register(dto: RegisterDto) {
    const existe = await this.prisma.usuario.findUnique({ where: { correo: dto.correo } });
    if (existe) throw new ConflictException('El correo ya está registrado');
    const hash = await bcrypt.hash(dto.contrasena, 10);
    const usuario = await this.prisma.usuario.create({
      data: { nombre: dto.nombre, correo: dto.correo, contrasena: hash },
    });
    return this.firmarToken(usuario.id, usuario.correo, usuario.rol);
  }

  async login(dto: LoginDto) {
    const usuario = await this.prisma.usuario.findUnique({ where: { correo: dto.correo } });
    if (!usuario) throw new UnauthorizedException('Credenciales inválidas');
    const valida = await bcrypt.compare(dto.contrasena, usuario.contrasena);
    if (!valida) throw new UnauthorizedException('Credenciales inválidas');
    return this.firmarToken(usuario.id, usuario.correo, usuario.rol);
  }

  private firmarToken(id: string, correo: string, rol: string) {
    return { access_token: this.jwt.sign({ sub: id, correo, rol }), rol };
  }

  async tvSync(body: { tvToken: string, userId: string }) {
    // Aquí idealmente guardaríamos la relación en una tabla de sesiones TV o usaríamos WebSockets
    // Por ahora, simulamos la vinculación exitosa.
    return { status: 'SUCCESS', message: 'Smart TV vinculada correctamente', tvToken: body.tvToken };
  }
}
