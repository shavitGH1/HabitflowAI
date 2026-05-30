import { Injectable, NestMiddleware, Logger } from '@nestjs/common';
import { Request, Response, NextFunction } from 'express';

@Injectable()
export class LoggingMiddleware implements NestMiddleware {
  private readonly logger = new Logger('HTTP');

  use(req: Request, res: Response, next: NextFunction) {
    const { ip, method, originalUrl } = req;
    const userAgent = req.get('user-agent') || '';
    const startTime = Date.now();

    this.logger.log(`Request: ${method} ${originalUrl} - ${ip} - ${userAgent}`);

    const originalSend = res.send;
    let responseBody: string;

    res.send = function (body: any) {
      if (Buffer.isBuffer(body)) {
        responseBody = body.toString('utf8');
      } else if (typeof body === 'string') {
        responseBody = body;
      } else if (typeof body === 'object') {
        responseBody = JSON.stringify(body);
      }
      return originalSend.apply(this, arguments);
    };

    res.on('finish', () => {
      const { statusCode } = res;
      const contentLength = res.get('content-length');
      const elapsedTime = Date.now() - startTime;

      if (statusCode >= 400) {
        this.logger.error(
          `Response: ${method} ${originalUrl} ${statusCode} ${contentLength} - ${elapsedTime}ms - Body: ${responseBody}`,
        );
      } else {
        this.logger.log(
          `Response: ${method} ${originalUrl} ${statusCode} ${contentLength} - ${elapsedTime}ms`,
        );
      }
    });

    next();
  }
}
