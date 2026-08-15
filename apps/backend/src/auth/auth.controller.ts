import { Body, Controller, Get, HttpCode, Post, Req, UseGuards } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { ThrottlerGuard } from '@nestjs/throttler';
import { ApiBearerAuth, ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { AuthService } from './auth.service';
import { CheckEmailDto } from './dto/check-email.dto';
import { GoogleIdTokenDto } from './dto/google-id-token.dto';
import { GoogleRegisterDto } from './dto/google-register.dto';
import { LoginDto } from './dto/login.dto';
import { RefreshDto } from './dto/refresh.dto';
import { RegisterDto } from './dto/register.dto';
import { UpdateFcmTokenDto } from './dto/update-fcm-token.dto';
import { OnboardingSuggestionsDto } from './dto/onboarding-suggestions.dto';
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

  @Post('onboarding-suggestions')
  @HttpCode(200)
  @UseGuards(ThrottlerGuard)
  @ApiOperation({ summary: 'Get goal-tailored quick-pick suggestions for the onboarding background questions' })
  @ApiResponse({ status: 200, description: 'Three suggested answers per background question, tailored to the goal' })
  @ApiResponse({ status: 400, description: 'Invalid goal' })
  @ApiResponse({ status: 429, description: 'Too many requests' })
  onboardingSuggestions(@Body() dto: OnboardingSuggestionsDto) {
    return this.authService.getOnboardingSuggestions(dto.goal);
  }

  @Post('register')
  @ApiOperation({ summary: 'Register a new user — classifies persona and generates portfolio' })
  @ApiResponse({ status: 201, description: 'User registered; returns portfolioSummary and coreGoals' })
  @ApiResponse({ status: 400, description: 'Email already in use, wrong number of answers, or AI classification failed' })
  register(@Body() dto: RegisterDto) {
    return this.authService.register(dto);
  }

  @Post('register-google')
  @ApiOperation({ summary: "Complete registration for a new Google Sign-In user (after GET /auth/google/callback returned isNewUser: true)" })
  @ApiResponse({ status: 201, description: 'User registered; returns access + refresh tokens, portfolioSummary and coreGoals' })
  @ApiResponse({ status: 400, description: 'Wrong number of answers, AI classification failed, or account already exists' })
  @ApiResponse({ status: 401, description: 'Signup token expired or invalid — restart Google Sign-In' })
  registerGoogle(@Body() dto: GoogleRegisterDto) {
    return this.authService.registerViaGoogle(dto);
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

  @Get('google')
  @UseGuards(AuthGuard('google'))
  @ApiOperation({ summary: 'Initiate Google OAuth consent flow' })
  @ApiResponse({ status: 302, description: 'Redirects to Google consent screen' })
  googleAuth() {}

  @Get('google/callback')
  @UseGuards(AuthGuard('google'))
  @ApiOperation({ summary: 'Google OAuth callback — logs in an existing account, or hands back a signup token for a new one' })
  @ApiResponse({ status: 200, description: 'Existing account: access + refresh tokens (isNewUser: false). New account: signupToken + Google profile info (isNewUser: true) — finish via POST /auth/register-google.' })
  googleCallback(@Req() req: { user: { email: string; firstName: string; lastName: string } }) {
    return this.authService.handleGoogleAuth(req.user);
  }

  @Post('google/verify')
  @HttpCode(200)
  @UseGuards(ThrottlerGuard)
  @ApiOperation({ summary: 'Verify a Google ID token from the native Android Sign-In flow — logs in or hands back a signup token' })
  @ApiResponse({ status: 200, description: 'Existing account: access + refresh tokens (isNewUser: false). New account: signupToken + Google profile info (isNewUser: true) — finish via POST /auth/register-google.' })
  @ApiResponse({ status: 401, description: 'Invalid or expired Google ID token' })
  @ApiResponse({ status: 429, description: 'Too many requests' })
  verifyGoogleIdToken(@Body() dto: GoogleIdTokenDto) {
    return this.authService.verifyGoogleIdToken(dto.idToken);
  }
}
