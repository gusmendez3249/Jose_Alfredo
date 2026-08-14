import { Injectable, UnauthorizedException, ConflictException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { PrismaService } from '../prisma/prisma.service';
import * as bcrypt from 'bcryptjs';
import { LoginDto } from './dto/login.dto';
import { RegisterDto } from './dto/register.dto';
import { Rol } from '@prisma/client';

/**
 * Servicio de autenticación del sistema FestivalTrack.
 *
 * Gestiona el registro de usuarios, el inicio de sesión y la firma de tokens JWT.
 * Las contraseñas nunca se almacenan en texto plano; se hashean con bcrypt (10 rounds).
 *
 * Inyecta:
 * - {@link PrismaService} para acceso a la base de datos PostgreSQL.
 * - {@link JwtService} para firmar y verificar tokens JWT.
 */
@Injectable()
export class AuthService {
  constructor(private prisma: PrismaService, private jwt: JwtService) {}

  /**
   * Registra un nuevo usuario en el sistema.
   *
   * Flujo:
   * 1. Verifica que el correo no esté ya registrado.
   * 2. Hashea la contraseña con bcrypt (10 rounds).
   * 3. Crea el usuario en la base de datos con rol "USER" por defecto.
   * 4. Firma y retorna un token JWT.
   *
   * @param dto Datos del nuevo usuario: nombre, correo, contraseña.
   * @returns {@link AuthResponseDto} con accessToken y datos del usuario.
   * @throws ConflictException si el correo ya está registrado (HTTP 409).
   */
  async register(dto: RegisterDto) {
    const existe = await this.prisma.usuario.findUnique({ where: { correo: dto.correo } });
    if (existe) throw new ConflictException('El correo ya está registrado');
    const hash = await bcrypt.hash(dto.contrasena, 10);
    const usuario = await this.prisma.usuario.create({
      data: { nombre: dto.nombre, correo: dto.correo, contrasena: hash },
    });
    return this.firmarToken(usuario.id, usuario.nombre, usuario.correo, usuario.rol);
  }

  /**
   * Inicia sesión con credenciales de usuario existente.
   *
   * Flujo:
   * 1. Busca al usuario por correo electrónico.
   * 2. Compara la contraseña ingresada con el hash almacenado usando bcrypt.
   * 3. Si es válida, firma y retorna un token JWT.
   *
   * @param dto Credenciales: correo y contraseña en texto plano.
   * @returns {@link AuthResponseDto} con accessToken y datos del usuario.
   * @throws UnauthorizedException si el correo no existe o la contraseña es incorrecta (HTTP 401).
   *
   * @remarks
   * El mensaje de error es genérico ("Credenciales inválidas") para no revelar
   * si el correo existe o no en el sistema (seguridad contra enumeración).
   */
  async login(dto: LoginDto) {
    const usuario = await this.prisma.usuario.findUnique({ where: { correo: dto.correo } });
    if (!usuario) throw new UnauthorizedException('Credenciales inválidas');
    const valida = await bcrypt.compare(dto.contrasena, usuario.contrasena);
    if (!valida) throw new UnauthorizedException('Credenciales inválidas');
    return this.firmarToken(usuario.id, usuario.nombre, usuario.correo, usuario.rol);
  }

  /**
   * Genera y firma un token JWT con el payload del usuario.
   *
   * El payload incluye:
   * - `sub`: ID del usuario (estándar JWT para "subject").
   * - `correo`: Correo del usuario.
   * - `rol`: Rol del usuario ("USER" o "ADMIN"), usado por los guards para control de acceso.
   *
   * @param id UUID del usuario.
   * @param nombre Nombre completo del usuario.
   * @param correo Correo electrónico.
   * @param rol Rol del sistema: "USER" o "ADMIN".
   * @returns Objeto con accessToken (JWT firmado) y datos del usuario.
   */
  private firmarToken(id: string, nombre: string, correo: string, rol: string) {
    const token = this.jwt.sign({ sub: id, correo, rol });

    return {
      accessToken: token,
      token,
      usuario: { id, nombre, correo, rol }
    };
  }

  /**
   * Vincula una Smart TV a la sesión de un usuario (funcionalidad experimental).
   *
   * En una implementación completa, esto guardaría la relación en una tabla
   * de sesiones TV o establecería un canal WebSocket bidireccional.
   * Actualmente simula la vinculación y retorna éxito.
   *
   * @param body Objeto con tvToken (ID único de la TV) y userId (ID del usuario).
   * @returns Objeto con status, mensaje de confirmación y el tvToken.
   */
  async tvSync(body: { tvToken: string, userId: string }) {
    // TODO: Implementar persistencia real de sesiones TV o WebSockets
    return { status: 'SUCCESS', message: 'Smart TV vinculada correctamente', tvToken: body.tvToken };
  }
  /**
   * Obtiene todos los usuarios registrados.
   */
  async getUsuarios() {
    return this.prisma.usuario.findMany({
      select: { id: true, nombre: true, correo: true, rol: true },
      orderBy: { rol: 'asc' }
    });
  }

  /**
   * Registra un nuevo administrador.
   */
  async registerAdmin(dto: RegisterDto) {
    const existe = await this.prisma.usuario.findUnique({ where: { correo: dto.correo } });
    if (existe) throw new ConflictException('El correo ya está registrado');
    const hash = await bcrypt.hash(dto.contrasena, 10);
    const usuario = await this.prisma.usuario.create({
      data: { nombre: dto.nombre, correo: dto.correo, contrasena: hash, rol: Rol.ADMINISTRADOR },
    });
    return this.firmarToken(usuario.id, usuario.nombre, usuario.correo, usuario.rol);
  }

  /**
   * Cambia el rol de un usuario existente.
   */
  async updateRole(id: string, rol: string) {
    if (rol !== 'USUARIO' && rol !== 'ADMINISTRADOR') {
      throw new ConflictException('Rol inválido');
    }
    return this.prisma.usuario.update({
      where: { id },
      data: { rol: Rol[rol as keyof typeof Rol] },
      select: { id: true, nombre: true, correo: true, rol: true }
    });
  }
}
