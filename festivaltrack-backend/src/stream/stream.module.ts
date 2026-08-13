import { Module } from '@nestjs/common';
import { StreamStatusController } from './stream-status.controller';

@Module({
  controllers: [StreamStatusController],
})
export class StreamModule {}
