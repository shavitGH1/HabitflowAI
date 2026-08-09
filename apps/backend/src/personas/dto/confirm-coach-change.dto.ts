import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsIn, IsNotEmpty, IsString, ValidateIf } from 'class-validator';
import { PERSONA_TYPES, PersonaType } from '../../ai/pillars';

export const COACH_CHANGE_TYPES = ['personaSwitch', 'adjustDifficulty', 'forfeitGoal'] as const;
export type CoachChangeType = (typeof COACH_CHANGE_TYPES)[number];

export class ConfirmCoachChangeDto {
  @IsIn(COACH_CHANGE_TYPES)
  @ApiProperty({ enum: COACH_CHANGE_TYPES, description: 'The proposedChange.type returned by POST /personas/coach-chat' })
  type: CoachChangeType;

  @ValidateIf((o: ConfirmCoachChangeDto) => o.type === 'personaSwitch')
  @IsIn(PERSONA_TYPES)
  @ApiPropertyOptional({ enum: PERSONA_TYPES, description: 'Required when type is personaSwitch' })
  suggestedPersona?: PersonaType;

  @ValidateIf((o: ConfirmCoachChangeDto) => o.type === 'adjustDifficulty')
  @IsIn(['increase', 'decrease'])
  @ApiPropertyOptional({ enum: ['increase', 'decrease'], description: 'Required when type is adjustDifficulty' })
  direction?: 'increase' | 'decrease';

  @ValidateIf((o: ConfirmCoachChangeDto) => o.type === 'forfeitGoal')
  @IsString()
  @IsNotEmpty()
  @ApiPropertyOptional({ description: 'Required when type is forfeitGoal — must match the active goal id' })
  goalId?: string;
}
