import { Controller, Get, Post, Delete, Body, Param, UseInterceptors, UploadedFile, BadRequestException } from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { diskStorage } from 'multer';
import { extname } from 'path';
import { GaleriaService } from './galeria.service';

@Controller('galeria')
export class GaleriaController {
  constructor(private readonly galeriaService: GaleriaService) {}

  @Get()
  getGaleria() {
    return this.galeriaService.getGaleria();
  }

  @Post('imagen')
  addImagen(@Body() body: any) {
    return this.galeriaService.addImagen(body);
  }

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
  
  @Post()
  @UseInterceptors(FileInterceptor('file', {
    storage: diskStorage({
      destination: './uploads',
      filename: (req, file, cb) => {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1e9);
        const ext = extname(file.originalname);
        cb(null, `${file.fieldname}-${uniqueSuffix}${ext}`);
      }
    })
  }))
  uploadFile(@UploadedFile() file: Express.Multer.File) {
    if (!file) throw new BadRequestException('Archivo no proveído');
    
    // Retornamos una URL relativa que luego el front puede consumir (sirviendo estáticos)
    return { url: `/uploads/${file.filename}` };
  }
}
