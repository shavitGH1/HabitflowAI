import { Body, Controller, HttpCode, Post, Req, UseGuards } from '@nestjs/common';
import { ThrottlerGuard } from '@nestjs/throttler';
import { ApiBearerAuth, ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { AuthService } from './auth.service';
import { CheckEmailDto } from './dto/check-email.dto';
import { LoginDto } from './dto/login.dto';
import { RefreshDto } from './dto/refresh.dto';
import { RegisterDto } from './dto/register.dto';
import { UpdateFcmTokenDto } from './dto/update-fcm-token.dto';
import { JwtAuthGuard } from './guards/jwt-auth.guard';

@ApiTags('auth')
@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Post('check-email')
  @HttpCode(200)
  @ApiOperation({ summary: 'Check whether an email address is available for registration' })
  @ApiResponse({ status: 200, description: 'Returns { available: boolean }' })
  checkEmail(@Body() dto: CheckEmailDto) {
    return this.authService.checkEmail(dto.email);
  }

  @Post('register')
  @ApiOperation({ summary: 'Register a new user — classifies persona and generates portfolio' })
  @ApiResponse({ status: 201, description: 'User registered; returns portfolioSummary and coreGoals' })
  @ApiResponse({ status: 400, description: 'Email already in use, wrong number of answers, or AI classification failed' })
  register(@Body() dto: RegisterDto) {
    return this.authService.register(dto);
  }

  @Post('login')
  @HttpCode(200)
  @UseGuards(ThrottlerGuard)
  @ApiOperation({ summary: 'Login and receive JWT access + refresh tokens' })
  @ApiResponse({ status: 200, description: 'Login successful' })
  @ApiResponse({ status: 401, description: 'Invalid credentials' })
  @ApiResponse({ status: 429, description: 'Too many requests' })
  login(@Body() dto: LoginDto) {
    return this.authService.login(dto);
  }

  @Post('refresh')
  @HttpCode(200)
  @ApiOperation({ summary: 'Exchange a refresh token for a new access token' })
  @ApiResponse({ status: 200, description: 'Access token refreshed' })
  @ApiResponse({ status: 401, description: 'Invalid or expired refresh token' })
  refresh(@Body() dto: RefreshDto) {
    return this.authService.refresh(dto);
  }

  @Post('fcm-token')
  @HttpCode(200)
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Update the FCM push notification token' })
  @ApiResponse({ status: 200, description: 'FCM token updated' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  updateFcmToken(@Req() req: { user: { id: string } }, @Body() dto: UpdateFcmTokenDto) {
    return this.authService.updateFcmToken(req.user.id, dto.fcmToken);
  }

  @Post('logout')
  @HttpCode(200)
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Invalidate the current refresh token' })
  @ApiResponse({ status: 200, description: 'Logged out successfully' })
  @ApiResponse({ status: 401, description: 'Missing or invalid access token' })
  logout(@Req() req: { user: { id: string } }) {
    return this.authService.logout(req.user.id);
  }
}
