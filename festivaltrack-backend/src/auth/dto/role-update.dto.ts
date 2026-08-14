import { IsString, IsIn } from 'class-validator';

export class RoleUpdateDto {
  @IsString()
  @IsIn(['USUARIO', 'ADMINISTRADOR'])
  rol: string;
}
