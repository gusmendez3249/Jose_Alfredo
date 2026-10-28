import { Controller, Get, Post, Body, Param, Request, UseGuards } from '@nestjs/common';
import { ChatService } from './chat.service';
import { EnviarMensajeDto } from './dto/enviar-mensaje.dto';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';

@Controller('stream/chat')
export class ChatController {
  constructor(private readonly chatService: ChatService) {}

  @Get(':eventoId')
  getMensajes(@Param('eventoId') eventoId: string) {
    return this.chatService.getMensajes(eventoId);
  }

  // Requiere sesión: el remitente sale del token, no del body (evita suplantación).
  @UseGuards(JwtAuthGuard)
  @Post()
  enviarMensaje(@Body() dto: EnviarMensajeDto, @Request() req: any) {
    return this.chatService.enviarMensaje(req.user.id, dto);
  }
}
