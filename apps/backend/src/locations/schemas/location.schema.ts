import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';

export type LocationRecordDocument = HydratedDocument<LocationRecord>;

@Schema({ timestamps: true })
export class LocationRecord {
  @Prop({ required: true })
  userId: string;

  @Prop()
  habitId?: string;

  @Prop({ required: true })
  latitude: number;

  @Prop({ required: true })
  longitude: number;

  @Prop({ default: Date.now })
  timestamp: number;
}

export const LocationRecordSchema = SchemaFactory.createForClass(LocationRecord);
