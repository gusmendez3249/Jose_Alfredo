import { IsNotEmpty, IsString, MaxLength } from 'class-validator';

/** Vinculación de una Smart TV: el usuario sale del JWT, nunca del cuerpo de la petición. */
export class TvSyncDto {
  @IsString() @IsNotEmpty() @MaxLength(128) tvToken: string;
}
