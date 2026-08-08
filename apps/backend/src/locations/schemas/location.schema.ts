import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';

export type LocationRecordDocument = HydratedDocument<LocationRecord>;

@Schema({ timestamps: true })
export class LocationRecord {
  @Prop({ required: true })
  userId: string;

  @Prop()
  habitId?: string;

  @Prop({ default: '' })
  taskTitle?: string;

  @Prop({ default: '' })
  placeName?: string;

  @Prop({ default: '' })
  address?: string;

  @Prop({ required: true })
  latitude: number;

  @Prop({ required: true })
  longitude: number;

  @Prop({ default: Date.now })
  timestamp: number;

  @Prop({ default: '' })
  personaType: string;

  @Prop({ default: false })
  isPublic: boolean;
}

export const LocationRecordSchema = SchemaFactory.createForClass(LocationRecord);
LocationRecordSchema.index({ userId: 1 });
LocationRecordSchema.index({ createdAt: -1 });
LocationRecordSchema.index({ latitude: 1, longitude: 1 });
