import {
  Body,
  Controller,
  Get,
  Post,
  Request,
  UseGuards,
} from '@nestjs/common';
import { BoletosService } from './boletos.service';
import { ComprarBoletoDto } from './dto/comprar-boleto.dto';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';
import { RolesGuard } from '../common/guards/roles.guard';
import { Roles } from '../common/decorators/roles.decorator';
import type { RequestConUsuario } from '../common/request-con-usuario';

@Controller('boletos')
export class BoletosController {
  constructor(private readonly boletosService: BoletosService) {}

  // El body se valida con ComprarBoletoDto (#52); el precio lo calcula el servicio.
  @UseGuards(JwtAuthGuard)
  @Post('comprar')
  comprar(@Body() dto: ComprarBoletoDto, @Request() req: RequestConUsuario) {
    return this.boletosService.comprar(req.user.id, dto);
  }

  @UseGuards(JwtAuthGuard)
  @Get('mis-boletos')
  misBoletos(@Request() req: RequestConUsuario) {
    return this.boletosService.misBoletos(req.user.id);
  }

  @UseGuards(JwtAuthGuard, RolesGuard)
  @Roles('ADMINISTRADOR')
  @Post('validar-qr')
  validarQr(@Body('qr') qr: string) {
    return this.boletosService.validarQr(qr);
  }
}
