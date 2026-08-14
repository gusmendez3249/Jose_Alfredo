import { AuthService } from './auth.service';
import { LoginDto } from './dto/login.dto';
import { RegisterDto } from './dto/register.dto';
import { RoleUpdateDto } from './dto/role-update.dto';
export declare class AuthController {
    private authService;
    constructor(authService: AuthService);
    register(dto: RegisterDto): Promise<{
        accessToken: string;
        token: string;
        usuario: {
            id: string;
            nombre: string;
            correo: string;
            rol: string;
        };
    }>;
    login(dto: LoginDto): Promise<{
        accessToken: string;
        token: string;
        usuario: {
            id: string;
            nombre: string;
            correo: string;
            rol: string;
        };
    }>;
    tvSync(body: {
        tvToken: string;
        userId: string;
    }): Promise<{
        status: string;
        message: string;
        tvToken: string;
    }>;
    getUsuarios(): Promise<{
        id: string;
        nombre: string;
        correo: string;
        rol: import(".prisma/client").$Enums.Rol;
    }[]>;
    registerAdmin(dto: RegisterDto): Promise<{
        accessToken: string;
        token: string;
        usuario: {
            id: string;
            nombre: string;
            correo: string;
            rol: string;
        };
    }>;
    updateRole(id: string, dto: RoleUpdateDto): Promise<{
        id: string;
        nombre: string;
        correo: string;
        rol: import(".prisma/client").$Enums.Rol;
    }>;
}
