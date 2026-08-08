import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { DriftFlagRepository } from './drift-flag.repository';
import { FirebaseModule } from './firebase.module';
import { DriftFlag, DriftFlagSchema } from './schemas/drift-flag.schema';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: DriftFlag.name, schema: DriftFlagSchema }]),
    FirebaseModule,
  ],
  providers: [DriftFlagRepository],
  exports: [DriftFlagRepository, FirebaseModule],
})
export class NotificationsModule {}
