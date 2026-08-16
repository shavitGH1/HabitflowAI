import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';

export type ResearchChunkDocument = HydratedDocument<ResearchChunk>;

@Schema({ timestamps: true })
export class ResearchChunk {
  @Prop({ required: true })
  sourceTitle: string;

  @Prop({ required: true })
  section: string;

  @Prop({ required: true })
  content: string;

  @Prop({ type: [Number], required: true })
  embedding: number[];
}

export const ResearchChunkSchema = SchemaFactory.createForClass(ResearchChunk);
