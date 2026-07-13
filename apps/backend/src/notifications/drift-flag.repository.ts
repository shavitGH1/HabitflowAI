import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { DriftFlag, DriftFlagDocument } from './schemas/drift-flag.schema';

export interface CreateDriftFlagInput {
  userId: string;
  detectedAt: Date;
  driftScore: number;
  suggestedPersona: string;
}

@Injectable()
export class DriftFlagRepository {
  constructor(
    @InjectModel(DriftFlag.name)
    private readonly driftFlagModel: Model<DriftFlagDocument>,
  ) {}

  async create(input: CreateDriftFlagInput): Promise<void> {
    await new this.driftFlagModel(input).save();
  }
}
