import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsEnum, IsMongoId, IsOptional } from 'class-validator';

export class ResolveHabitsDto {
  @IsMongoId()
  @ApiProperty({ example: '64f1a2b3c4d5e6f7a8b9c0d1', description: 'The new goal habits carry over to' })
  newGoalId: string;

  @IsOptional()
  @IsEnum(['link', 'archive'])
  @ApiPropertyOptional({
    enum: ['link', 'archive'],
    description: 'Skip the AI relevance check and apply this decision directly. Omit to run the check.',
  })
  decision?: 'link' | 'archive';
}
