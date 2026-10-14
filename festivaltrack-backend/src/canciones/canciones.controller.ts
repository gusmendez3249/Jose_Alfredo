import { Controller, Get, Post, Put, Delete, Body, Param, UseGuards } from '@nestjs/common';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { RolesGuard } from '../common/guards/roles.guard';
import { Roles } from '../common/decorators/roles.decorator';
import { CancionesService } from './canciones.service';

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

  @Post()
  create(@Body() body: any) {
    return this.cancionesService.create(body);
  }

  @Delete(':id')
  remove(@Param('id') id: string) {
    return this.cancionesService.remove(id);
  }
}
