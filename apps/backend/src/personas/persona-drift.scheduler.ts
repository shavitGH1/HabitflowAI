import { Injectable } from '@nestjs/common';
import { Cron } from '@nestjs/schedule';
import { logger } from '../logger';
import { PersonasService } from './personas.service';

@Injectable()
export class PersonaDriftScheduler {
  constructor(private readonly personasService: PersonasService) {}

  @Cron('0 9 * * 1', { name: 'weekly-drift-check' })
  async handleWeeklyDriftCheck(): Promise<void> {
    logger.info('weekly drift cron triggered');
    await this.personasService.evaluateAllUsersDrift();
  }
}
