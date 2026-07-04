import { Injectable } from '@nestjs/common';

export type MotivationVote = 'up' | 'down';

export interface FeedbackTally {
  positiveFeedbackCount: number;
  negativeFeedbackCount: number;
}

@Injectable()
export class MotivationFeedbackStore {
  private readonly tallies = new Map<string, FeedbackTally>();

  record(userId: string, vote: MotivationVote): FeedbackTally {
    const tally = this.tallies.get(userId) ?? {
      positiveFeedbackCount: 0,
      negativeFeedbackCount: 0,
    };
    if (vote === 'up') tally.positiveFeedbackCount += 1;
    else tally.negativeFeedbackCount += 1;
    this.tallies.set(userId, tally);
    return tally;
  }

  get(userId: string): FeedbackTally {
    return (
      this.tallies.get(userId) ?? { positiveFeedbackCount: 0, negativeFeedbackCount: 0 }
    );
  }
}
