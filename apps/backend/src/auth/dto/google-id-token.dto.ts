import { ApiProperty } from '@nestjs/swagger';
import { IsNotEmpty, IsString } from 'class-validator';

export class GoogleIdTokenDto {
  @IsString()
  @IsNotEmpty()
  @ApiProperty({ description: 'ID token from the native Android Credential Manager Google Sign-In flow' })
  idToken: string;
}
