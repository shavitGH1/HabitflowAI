import { Module } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { cert, getApps, initializeApp } from 'firebase-admin/app';
import { Messaging, getMessaging } from 'firebase-admin/messaging';

export const FIREBASE_MESSAGING = 'FIREBASE_MESSAGING';

@Module({
  providers: [
    {
      provide: FIREBASE_MESSAGING,
      inject: [ConfigService],
      useFactory: (config: ConfigService): Messaging => {
        if (!getApps().length) {
          initializeApp({
            credential: cert({
              projectId: config.get<string>('FIREBASE_PROJECT_ID'),
              privateKey: config.get<string>('FIREBASE_PRIVATE_KEY')?.replace(/\\n/g, '\n'),
              clientEmail: config.get<string>('FIREBASE_CLIENT_EMAIL'),
            }),
          });
        }
        return getMessaging();
      },
    },
  ],
  exports: [FIREBASE_MESSAGING],
})
export class FirebaseModule {}
