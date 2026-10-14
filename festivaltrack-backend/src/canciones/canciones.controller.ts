import { BadRequestException, Controller, Get, Post, Put, Delete, Body, Param, UploadedFile, UseGuards, UseInterceptors } from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { diskStorage } from 'multer';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { RolesGuard } from '../common/guards/roles.guard';
import { Roles } from '../common/decorators/roles.decorator';
import { CancionesService } from './canciones.service';

/** Tipos permitidos: audio de la canción y su portada. La extensión sale del tipo MIME validado. */
const ARCHIVO_EXT: Record<string, string> = {
  'audio/mpeg': '.mp3',
  'audio/mp3': '.mp3',
  'audio/mp4': '.m4a',
  'audio/x-m4a': '.m4a',
  'audio/aac': '.aac',
  'audio/wav': '.wav',
  'audio/x-wav': '.wav',
  'audio/ogg': '.ogg',
  'image/jpeg': '.jpg',
  'image/png': '.png',
  'image/webp': '.webp',
};

@Controller('canciones')
export class CancionesController {
  constructor(private readonly cancionesService: CancionesService) {}

  @Get()
  findAll() {
    return this.cancionesService.findAll();
  }

  @Get('evento/:eventoId')
  findByEvento(@Param('eventoId') eventoId: string) {
    return this.cancionesService.findByEvento(eventoId);
  }

  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('ADMINISTRADOR')
  @Put('evento/:eventoId')
  setRepertorio(@Param('eventoId') eventoId: string, @Body('cancionIds') cancionIds: string[]) {
    return this.cancionesService.setRepertorio(eventoId, cancionIds);
  }

  /** Sube el audio o la portada de una canción (solo administrador, máx. 30 MB). */
  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('ADMINISTRADOR')
  @Post('upload')
  @UseInterceptors(
    FileInterceptor('file', {
      limits: { fileSize: 30 * 1024 * 1024 },
      fileFilter: (req, file, cb) => {
        if (!ARCHIVO_EXT[file.mimetype]) {
          return cb(new BadRequestException('Formato no permitido (usa MP3, M4A, WAV, OGG, JPG, PNG o WEBP)'), false);
        }
        cb(null, true);
      },
      storage: diskStorage({
        destination: './uploads',
        filename: (req, file, cb) => {
          const sufijo = Date.now() + '-' + Math.round(Math.random() * 1e9);
          cb(null, `cancion-${sufijo}${ARCHIVO_EXT[file.mimetype]}`);
        },
      }),
    }),
  )
  upload(@UploadedFile() file: Express.Multer.File) {
    if (!file) throw new BadRequestException('Archivo no proveído');
    return { url: `/uploads/${file.filename}` };
  }

  @Post()
  create(@Body() body: any) {
    return this.cancionesService.create(body);
  }

  @Delete(':id')
  remove(@Param('id') id: string) {
    return this.cancionesService.remove(id);
  }
}
