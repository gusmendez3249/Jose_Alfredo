import { IsNotEmpty, IsString, Matches, MaxLength } from 'class-validator';

/**
 * Aprobar desde el celular el código que muestra la TV. El usuario sale del JWT, nunca del
 * cuerpo de la petición (con `whitelist` cualquier otro campo, como un `userId`, se descarta).
 */
export class TvSyncDto {
  @IsString()
  @IsNotEmpty()
  @MaxLength(20)
  @Matches(/^[A-Za-z0-9\s-]+$/, {
    message: 'El código solo lleva letras y números',
  })
  codigo: string;
}

/** La TV pregunta si ya aprobaron su código con el token secreto que recibió al pedirlo. */
export class TvEstadoDto {
  @IsString()
  @Matches(/^[a-f0-9]{64}$/, { message: 'tvToken inválido' })
  tvToken: string;
}
