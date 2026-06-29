import { Controller, Get, Query, UseGuards } from '@nestjs/common';
import { SyncService } from './sync.service';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';

@Controller('sync')
export class SyncController {
  constructor(private syncService: SyncService) {}

  @UseGuards(JwtAuthGuard)
  @Get('wear')
  getWearPayload(@Query('since') since?: string) {
    return this.syncService.getWearPayload(since);
  }
}
