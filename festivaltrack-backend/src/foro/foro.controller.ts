import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Post,
  Query,
  Req,
  UseGuards,
} from '@nestjs/common';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { OptionalJwtAuthGuard } from '../common/guards/optional-jwt-auth.guard';
import { ForoService } from './foro.service';

@Controller('foro')
export class ForoController {
  constructor(private readonly foro: ForoService) {}

  @UseGuards(OptionalJwtAuthGuard)
  @Get()
  listar(
    @Req() req: any,
    @Query('tipo') tipo?: string,
    @Query('before') before?: string,
    @Query('limit') limit?: string,
  ) {
    return this.foro.listar(req.user, { tipo, before, limit: Number(limit) });
  }

  @UseGuards(JwtAuthGuard)
  @Post()
  crear(@Req() req: any, @Body() body: any) {
    return this.foro.crear(req.user, body);
  }

  @UseGuards(JwtAuthGuard)
  @Delete('comentarios/:id')
  eliminarComentario(@Req() req: any, @Param('id') id: string) {
    return this.foro.eliminarComentario(req.user, id);
  }

  @UseGuards(JwtAuthGuard)
  @Delete(':id')
  eliminar(@Req() req: any, @Param('id') id: string) {
    return this.foro.eliminar(req.user, id);
  }

  @UseGuards(JwtAuthGuard)
  @Post(':id/like')
  like(@Req() req: any, @Param('id') id: string) {
    return this.foro.toggleLike(req.user, id);
  }

  @Get(':id/comentarios')
  comentarios(@Param('id') id: string) {
    return this.foro.comentarios(id);
  }

  @UseGuards(JwtAuthGuard)
  @Post(':id/comentarios')
  comentar(@Req() req: any, @Param('id') id: string, @Body('texto') texto?: string) {
    return this.foro.comentar(req.user, id, texto);
  }
}
