import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

export type DriftFlagDocument = DriftFlag & Document;

@Schema({ timestamps: true })
export class DriftFlag {
  @Prop({ required: true })
  userId: string;

  @Prop({ required: true })
  detectedAt: Date;

  @Prop()
  driftScore: number;

  @Prop()
  suggestedPersona: string;

  @Prop({ default: false })
  dismissed: boolean;
}

export const DriftFlagSchema = SchemaFactory.createForClass(DriftFlag);
DriftFlagSchema.index({ userId: 1 });
