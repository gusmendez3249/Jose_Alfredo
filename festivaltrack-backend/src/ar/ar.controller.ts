import {
  Body, Controller, Delete, Get, Param, Post, Put, Request, UploadedFile, UseGuards, UseInterceptors,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { RolesGuard } from '../common/guards/roles.guard';
import { Roles } from '../common/decorators/roles.decorator';
import { ArReconocimientoFacade } from './ar-reconocimiento.facade';
import { ArCatalogoService } from './ar-catalogo.service';
import { SitioArDto } from './dto/sitio-ar.dto';

@Controller('ar')
export class ArController {
  constructor(
    private readonly facade: ArReconocimientoFacade,
    private readonly catalogo: ArCatalogoService,
  ) {}

  /** Pantalla 1 → 2: recibe la foto (multipart, campo "foto") y devuelve el sitio identificado. */
  @UseGuards(JwtAuthGuard)
  @Post('reconocer')
  @UseInterceptors(FileInterceptor('foto', { limits: { fileSize: 5 * 1024 * 1024 } }))
  reconocer(@UploadedFile() foto: Express.Multer.File, @Request() req: any) {
    return this.facade.reconocer(foto?.buffer, req.user.id);
  }

  /** Pantalla 3: historial de sitios visitados por el usuario autenticado. */
  @UseGuards(JwtAuthGuard)
  @Get('historial')
  historial(@Request() req: any) {
    return this.facade.historial(req.user.id);
  }

  // ─── Catálogo: lectura pública, escritura solo organizadores ───

  @Get('sitios')
  sitios() { return this.catalogo.activos(); }

  @Get('sitios/:id')
  sitio(@Param('id') id: string) { return this.catalogo.porId(id); }

  @UseGuards(JwtAuthGuard, RolesGuard) @Roles('ADMINISTRADOR')
  @Post('sitios')
  crear(@Body() dto: SitioArDto) { return this.catalogo.crear(dto); }

  @UseGuards(JwtAuthGuard, RolesGuard) @Roles('ADMINISTRADOR')
  @Put('sitios/:id')
  actualizar(@Param('id') id: string, @Body() dto: Partial<SitioArDto>) { return this.catalogo.actualizar(id, dto); }

  @UseGuards(JwtAuthGuard, RolesGuard) @Roles('ADMINISTRADOR')
  @Delete('sitios/:id')
  eliminar(@Param('id') id: string) { return this.catalogo.eliminar(id); }
}
