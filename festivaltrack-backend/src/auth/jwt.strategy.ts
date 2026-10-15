import { Injectable, UnauthorizedException } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy } from 'passport-jwt';
import { PrismaService } from '../prisma/prisma.service';
import { getJwtSecret } from './jwt-secret';

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy) {
  constructor(private prisma: PrismaService) {
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      secretOrKey: getJwtSecret(),
    });
  }

  async validate(payload: { sub: string; correo: string; rol: string }) {
    const usuario = await this.prisma.usuario.findUnique({ where: { id: payload.sub } });
    if (!usuario || !usuario.activo) throw new UnauthorizedException();
    return { id: usuario.id, correo: usuario.correo, rol: usuario.rol };
  }
}
