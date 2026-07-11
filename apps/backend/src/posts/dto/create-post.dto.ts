import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { IsNotEmpty, IsString } from 'class-validator';

export class CreatePostDto {
  @IsString()
  @IsNotEmpty()
  @ApiProperty({ example: 'Morning Run' })
  habitName: string;

  @IsString()
  @IsNotEmpty()
  @ApiProperty({ example: 'Completed my 5 km run today!' })
  completionNote: string;

  @ApiPropertyOptional({ type: 'string', format: 'binary', description: 'Optional post image' })
  image?: Express.Multer.File;
}
