import { PersonaType } from '../ai/pillars';
import { ConfirmCoachChangeResult } from '../personas/personas.service';

export const COACH_USER_ID = '000000000000000000000c0a';

export type CoachBand = 'EXCELLENT' | 'GOOD' | 'SLIPPING' | 'AT_RISK';

export type CoachTipId = 'RESTART_SMALL' | 'NARROW_FOCUS' | 'ADD_ONE';

export const BAND_SENTENCES: Record<CoachBand, string> = {
  EXCELLENT: 'You hit almost everything you planned this week.',
  GOOD: 'You kept most of your plan going this week.',
  SLIPPING: 'You are starting to drift away from your plan.',
  AT_RISK: 'Almost nothing was marked as done this week.',
};

export const PERSONA_LINES: Record<PersonaType, string> = {
  Achiever: 'Treat next week as a number to beat.',
  Grower: 'Look at the skill you are building, not the score.',
  Socializer: 'Tell someone what you are working on this week.',
  Explorer: 'Change the time or the place of one habit to keep it interesting.',
  Altruist: 'Remember who else benefits when you keep this going.',
  Architect: 'Give each habit a fixed slot in your schedule.',
};

export const TIPS: Record<CoachTipId, string> = {
  RESTART_SMALL: 'Restart small: pick one habit for tomorrow and ignore the rest.',
  NARROW_FOCUS: 'You held one habit but dropped the others. Shorten the list until it sticks.',
  ADD_ONE: 'You are steady enough to add one more habit.',
};

export const NOTHING_DONE_TODAY = 'Nothing was marked as done today.';

export const COACH_UNAVAILABLE =
  'I could not think that through properly just now, but here is where you stand.';

export const COACH_OFFLINE_REPLY =
  'I could not think that through properly just now — try me again in a moment.';

export const completedTodayLine = (titles: string[]): string =>
  `Today you completed: ${titles.join(', ')}.`;

export const notesTodayLine = (notes: string[]): string => `You noted: ${notes.join('; ')}.`;

export const personaSwitchLine = (persona: string): string =>
  `Your recent pattern looks closer to ${persona}. You can switch persona in the app.`;

export const confirmationLine = (result: ConfirmCoachChangeResult): string => {
  switch (result.type) {
    case 'personaSwitch':
      return `Done. Your persona is now ${result.personaType}.`;
    case 'adjustDifficulty':
      return 'Done. I adjusted today\'s plan for you.';
    case 'forfeitGoal':
      return `Done. "${result.goal.title}" is closed. Start a new goal whenever you are ready.`;
  }
};
