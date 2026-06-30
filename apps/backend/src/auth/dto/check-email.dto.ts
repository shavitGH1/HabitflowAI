import { ApiProperty } from '@nestjs/swagger';
import { IsEmail } from 'class-validator';

export class CheckEmailDto {
  @IsEmail()
  @ApiProperty({ example: 'user@example.com' })
  email: string;
}
