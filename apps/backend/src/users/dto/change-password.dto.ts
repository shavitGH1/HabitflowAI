import { ApiProperty } from '@nestjs/swagger';
import { IsNotEmpty, IsString, MinLength } from 'class-validator';

export class ChangePasswordDto {
  @IsString()
  @IsNotEmpty()
  @ApiProperty({ description: "The user's current password, verified before the change is accepted" })
  currentPassword: string;

  @IsString()
  @MinLength(6)
  @ApiProperty({ minLength: 6 })
  newPassword: string;
}
