import { Body, Controller, Delete, Get, Param, Patch, Post, Request, UseGuards } from '@nestjs/common';
import { EventosService } from './eventos.service';
import { CreateEventoDto } from './dto/create-evento.dto';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { RolesGuard } from '../common/guards/roles.guard';
import { Roles } from '../common/decorators/roles.decorator';

@Controller('eventos')
export class EventosController {
  constructor(private eventosService: EventosService) {}

  @Get()      findAll()                     { return this.eventosService.findAll(); }
  @Get(':id') findOne(@Param('id') id: string) { return this.eventosService.findOne(id); }

  @UseGuards(JwtAuthGuard, RolesGuard) @Roles('ADMINISTRADOR')
  @Post()
  create(@Body() dto: CreateEventoDto, @Request() req: any) {
    return this.eventosService.create(dto, req.user.id);
  }

  @UseGuards(JwtAuthGuard, RolesGuard) @Roles('ADMINISTRADOR')
  @Patch(':id')
  update(@Param('id') id: string, @Body() dto: Partial<CreateEventoDto>) {
    return this.eventosService.update(id, dto);
  }

  @UseGuards(JwtAuthGuard, RolesGuard) @Roles('ADMINISTRADOR')
  @Delete(':id')
  remove(@Param('id') id: string) { return this.eventosService.remove(id); }
}
