import { Controller, Get, Post, Body, Param } from '@nestjs/common';
import { ChatService } from './chat.service';

@Controller('stream/chat')
export class ChatController {
  constructor(private readonly chatService: ChatService) {}

  @Get(':eventoId')
  getMensajes(@Param('eventoId') eventoId: string) {
    return this.chatService.getMensajes(eventoId);
  }

  @Post()
  enviarMensaje(@Body() body: any) {
    return this.chatService.enviarMensaje(body);
  }
}
