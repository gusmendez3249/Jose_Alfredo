import { JwtService } from '@nestjs/jwt';
import { PrismaService } from '../prisma/prisma.service';
import { LoginDto } from './dto/login.dto';
import { RegisterDto } from './dto/register.dto';
export declare class AuthService {
    private prisma;
    private jwt;
    constructor(prisma: PrismaService, jwt: JwtService);
    register(dto: RegisterDto): Promise<{
        accessToken: string;
        usuario: {
            id: string;
            nombre: string;
            correo: string;
            rol: string;
        };
    }>;
    login(dto: LoginDto): Promise<{
        accessToken: string;
        usuario: {
            id: string;
            nombre: string;
            correo: string;
            rol: string;
        };
    }>;
    private firmarToken;
    tvSync(body: {
        tvToken: string;
        userId: string;
    }): Promise<{
        status: string;
        message: string;
        tvToken: string;
    }>;
}
