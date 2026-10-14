import { Controller, Get, Post, Delete, Body, Param, UseGuards } from '@nestjs/common';
import { CancionesService } from './canciones.service';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { RolesGuard } from '../common/guards/roles.guard';
import { Roles } from '../common/decorators/roles.decorator';

@Controller('canciones')
export class CancionesController {
  constructor(private readonly cancionesService: CancionesService) {}

  @Get()
  findAll() {
    return this.cancionesService.findAll();
  }

  @UseGuards(JwtAuthGuard, RolesGuard) @Roles('ADMINISTRADOR')
  @Post()
  create(@Body() body: any) {
    return this.cancionesService.create(body);
  }

  @UseGuards(JwtAuthGuard, RolesGuard) @Roles('ADMINISTRADOR')
  @Delete(':id')
  remove(@Param('id') id: string) {
    return this.cancionesService.remove(id);
  }
}
