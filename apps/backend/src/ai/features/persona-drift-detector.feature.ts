import { Injectable } from '@nestjs/common';
import { GeminiClient } from '../gemini.client';
import { PILLARS, PILLAR_TO_PERSONA, Pillar, PersonaType } from '../pillars';
import {
  BehaviorSnapshot,
  buildDriftDetectorPrompt,
} from '../prompts/persona-drift-detector.prompt';
import { driftDetectorOutputSchema } from '../schemas/persona-drift-detector.schema';

export const DRIFT_THRESHOLD = 0.3;

export interface DriftDetectorInput {
  currentPersona: PersonaType;
  baselineBreakdown: Record<Pillar, number>;
  behaviorSnapshot: BehaviorSnapshot;
}

export interface DriftResult {
  driftDetected: boolean;
  driftScore: number;
  newSuggestedPersona: PersonaType | null;
  currentBreakdown: Record<Pillar, number>;
  rationale: string;
}

@Injectable()
export class PersonaDriftDetectorFeature {
  constructor(private readonly gemini: GeminiClient) {}

  async detect(input: DriftDetectorInput): Promise<DriftResult> {
    const prompt = buildDriftDetectorPrompt(input);
    const output = await this.gemini.generateJson(prompt, driftDetectorOutputSchema);

    const driftScore = this.totalVariationDistance(
      input.baselineBreakdown,
      output.currentBreakdown,
    );
    const driftDetected = driftScore > DRIFT_THRESHOLD;
    const dominantPersona = this.dominantPersona(output.currentBreakdown);
    const newSuggestedPersona =
      driftDetected && dominantPersona !== input.currentPersona ? dominantPersona : null;

    return {
      driftDetected,
      driftScore: Number(driftScore.toFixed(4)),
      newSuggestedPersona,
      currentBreakdown: output.currentBreakdown,
      rationale: output.rationale,
    };
  }

  private totalVariationDistance(
    baseline: Record<Pillar, number>,
    current: Record<Pillar, number>,
  ): number {
    const baseDist = this.normalize(baseline);
    const currDist = this.normalize(current);
    const sum = PILLARS.reduce((acc, p) => acc + Math.abs(baseDist[p] - currDist[p]), 0);
    return sum / 2;
  }

  private normalize(scores: Record<Pillar, number>): Record<Pillar, number> {
    const total = PILLARS.reduce((acc, p) => acc + (scores[p] ?? 0), 0);
    const result = {} as Record<Pillar, number>;
    for (const p of PILLARS) {
      result[p] = total > 0 ? (scores[p] ?? 0) / total : 0;
    }
    return result;
  }

  private dominantPersona(scores: Record<Pillar, number>): PersonaType {
    const top = PILLARS.reduce((best, p) => (scores[p] > scores[best] ? p : best), PILLARS[0]);
    return PILLAR_TO_PERSONA[top];
  }
}
