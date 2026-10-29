import { Controller, Get, Post, Delete, Body, Param, UseInterceptors, UseGuards, UploadedFile, BadRequestException } from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { diskStorage } from 'multer';

const IMAGE_EXT: Record<string, string> = {
  'image/jpeg': '.jpg',
  'image/png': '.png',
  'image/webp': '.webp',
};
import { GaleriaService } from './galeria.service';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { RolesGuard } from '../common/guards/roles.guard';
import { Roles } from '../common/decorators/roles.decorator';

@Controller('galeria')
export class GaleriaController {
  constructor(private readonly galeriaService: GaleriaService) {}

  @Get()
  getGaleria() {
    return this.galeriaService.getGaleria();
  }

  @UseGuards(JwtAuthGuard, RolesGuard) @Roles('ADMINISTRADOR')
  @Post('imagen')
  addImagen(@Body() body: any) {
    return this.galeriaService.addImagen(body);
  }

  @UseGuards(JwtAuthGuard, RolesGuard) @Roles('ADMINISTRADOR')
  @Delete('imagen/:id')
  removeImagen(@Param('id') id: string) {
    return this.galeriaService.removeImagen(id);
  }
}

// El endpoint /upload debe estar a nivel /api/upload según el prompt, 
// pero en Nest se maneja usualmente como /upload o dentro de galeria.
// Para cumplir con /upload, crearemos otro controlador global o lo mapearemos aquí.
@Controller('upload')
export class UploadController {
  
  // Solo usuarios autenticados, solo imágenes (jpg/png/webp) y máx. 8 MB.
  @Post()
  @UseGuards(JwtAuthGuard)
  @UseInterceptors(FileInterceptor('file', {
    limits: { fileSize: 8 * 1024 * 1024 },
    fileFilter: (req, file, cb) => {
      if (!IMAGE_EXT[file.mimetype]) {
        return cb(new BadRequestException('Solo se permiten imágenes JPG, PNG o WEBP'), false);
      }
      cb(null, true);
    },
    storage: diskStorage({
      destination: './uploads',
      filename: (req, file, cb) => {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1e9);
        // La extensión sale del tipo MIME validado, no del nombre que manda el cliente.
        cb(null, `${file.fieldname}-${uniqueSuffix}${IMAGE_EXT[file.mimetype]}`);
      }
    })
  }))
  uploadFile(@UploadedFile() file: Express.Multer.File) {
    if (!file) throw new BadRequestException('Archivo no proveído');
    
    // Retornamos una URL relativa que luego el front puede consumir (sirviendo estáticos)
    return { url: `/uploads/${file.filename}` };
  }
}
