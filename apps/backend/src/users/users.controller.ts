import {
  Body,
  Controller,
  Get,
  Patch,
  Post,
  Query,
  Req,
  UploadedFile,
  UseGuards,
  UseInterceptors,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import {
  ApiBearerAuth,
  ApiBody,
  ApiConsumes,
  ApiOperation,
  ApiResponse,
  ApiTags,
} from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { ChangePasswordDto } from './dto/change-password.dto';
import { UpdateNameDto } from './dto/update-name.dto';
import { UpdateProfileDto } from './dto/update-profile.dto';
import { UsersService } from './users.service';

@ApiTags('users')
@Controller('users')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  @Get()
  @ApiOperation({ summary: 'List all users (id, email, name) for member picker and search' })
  @ApiResponse({ status: 200, description: 'Array of { id, email, firstName, lastName, profilePicture }' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  getAllUsers(@Req() req: { user: { id: string } }) {
    return this.usersService.getAllUsers();
  }

  @Get('me/home')
  @ApiOperation({ summary: 'Get persona, goals and daily tasks for the authenticated user' })
  @ApiResponse({ status: 200, description: 'Home page data including persona, goals, daily tasks, and portfolio fields' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 404, description: 'User not found' })
  getHome(
    @Req() req: { user: { id: string } },
    @Query('force') force?: string,
  ) {
    return this.usersService.getHomePageData(req.user.id, force === 'true');
  }

  @Patch('me/profile')
  @ApiOperation({ summary: 'Update the authenticated user profile picture (preset key or uploaded URL)' })
  @ApiResponse({ status: 200, description: '{ profilePicture, success }' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 404, description: 'User not found' })
  updateProfile(
    @Req() req: { user: { id: string } },
    @Body() dto: UpdateProfileDto,
  ) {
    return this.usersService.updateProfilePicture(req.user.id, dto.profilePicture ?? '');
  }

  @Patch('me/name')
  @ApiOperation({ summary: "Update the authenticated user's first/last name — limited to once every 3 months" })
  @ApiResponse({ status: 200, description: '{ firstName, lastName, nameChangedAt, success }' })
  @ApiResponse({ status: 400, description: 'Still within the 3-month cooldown since the last name change' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 404, description: 'User not found' })
  updateName(
    @Req() req: { user: { id: string } },
    @Body() dto: UpdateNameDto,
  ) {
    return this.usersService.updateName(req.user.id, dto.firstName, dto.lastName);
  }

  @Patch('me/password')
  @ApiOperation({ summary: "Change the authenticated user's password" })
  @ApiResponse({ status: 200, description: '{ success }' })
  @ApiResponse({ status: 401, description: 'Missing/invalid access token, or current password is incorrect' })
  @ApiResponse({ status: 404, description: 'User not found' })
  changePassword(
    @Req() req: { user: { id: string } },
    @Body() dto: ChangePasswordDto,
  ) {
    return this.usersService.changePassword(req.user.id, dto.currentPassword, dto.newPassword);
  }

  @Post('me/avatar')
  @UseInterceptors(FileInterceptor('image'))
  @ApiConsumes('multipart/form-data')
  @ApiOperation({ summary: 'Upload a profile picture for the authenticated user' })
  @ApiBody({
    schema: {
      type: 'object',
      required: ['image'],
      properties: {
        image: { type: 'string', format: 'binary' },
      },
    },
  })
  @ApiResponse({ status: 201, description: '{ profilePicture, success }' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  @ApiResponse({ status: 404, description: 'User not found' })
  uploadAvatar(
    @Req() req: { user: { id: string } },
    @UploadedFile() file: Express.Multer.File,
  ) {
    return this.usersService.uploadAvatar(req.user.id, file);
  }
}
