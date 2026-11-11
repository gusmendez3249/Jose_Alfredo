import { Controller, Get, Post, Body, Param, UseGuards } from '@nestjs/common';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { RolesGuard } from '../common/guards/roles.guard';
import { Roles } from '../common/decorators/roles.decorator';
import { BiografiasService } from './biografias.service';
import { BiografiaAltaDto } from './factories/biografia.factory';

@Controller('biografias')
export class BiografiasController {
  constructor(private readonly biografiasService: BiografiasService) {}

  @Get()
  findAll() {
    return this.biografiasService.findAll();
  }

  // Solo administradores pueden dar de alta biografías.
  @UseGuards(JwtAuthGuard, RolesGuard) @Roles('ADMINISTRADOR')
  @Post('alta')
  altaBiografia(@Body() body: BiografiaAltaDto) {
    return this.biografiasService.altaConFactory(body);
  }

  @Get(':id')
  findOne(@Param('id') id: string) {
    return this.biografiasService.findOne(id);
  }
}
