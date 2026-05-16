export const PILLARS = [
  'Achievement',
  'Growth',
  'Connection',
  'Exploration',
  'Purpose',
  'Structure',
] as const;

export type Pillar = (typeof PILLARS)[number];

export const PERSONA_TYPES = [
  'Achiever',
  'Grower',
  'Socializer',
  'Explorer',
  'Altruist',
  'Architect',
] as const;

export type PersonaType = (typeof PERSONA_TYPES)[number];

export const PILLAR_TO_PERSONA: Record<Pillar, PersonaType> = {
  Achievement: 'Achiever',
  Growth: 'Grower',
  Connection: 'Socializer',
  Exploration: 'Explorer',
  Purpose: 'Altruist',
  Structure: 'Architect',
};

export interface PillarDefinition {
  key: Pillar;
  persona: PersonaType;
  description: string;
}

export const PILLAR_DEFINITIONS: PillarDefinition[] = [
  {
    key: 'Achievement',
    persona: 'Achiever',
    description: 'Driven by results, milestones, and measurable wins. Thrives on goal completion and external benchmarks.',
  },
  {
    key: 'Growth',
    persona: 'Grower',
    description: 'Motivated by self-improvement, skill mastery, and learning curves. Values process over outcome.',
  },
  {
    key: 'Connection',
    persona: 'Socializer',
    description: 'Energized by community, accountability partners, and shared progress. Habits stick when others are involved.',
  },
  {
    key: 'Exploration',
    persona: 'Explorer',
    description: 'Curious, variety-seeking, novelty-driven. Loses motivation in repetitive routines without fresh angles.',
  },
  {
    key: 'Purpose',
    persona: 'Altruist',
    description: 'Habits matter when they serve something larger — family, cause, meaning. Internal "why" drives consistency.',
  },
  {
    key: 'Structure',
    persona: 'Architect',
    description: 'Thrives in systems, schedules, and predictable frameworks. Needs clear rules and tracked routines.',
  },
];

export interface OnboardingQuestion {
  id: number;
  text: string;
  primaryPillar: Pillar;
  weights: Partial<Record<Pillar, number>>;
}

// Each question contributes weights summing to ~1.0 across pillars.
// Primary pillar gets the largest share; secondary pillars get smaller cross-signals.
export const ONBOARDING_QUESTIONS: OnboardingQuestion[] = [
  {
    id: 1,
    text: 'Describe a recent goal you set for yourself. What pushed you to start it, and how did you measure progress?',
    primaryPillar: 'Achievement',
    weights: { Achievement: 0.6, Structure: 0.2, Growth: 0.2 },
  },
  {
    id: 2,
    text: 'Think of a habit or skill you stuck with for more than a month. What kept you coming back to it?',
    primaryPillar: 'Growth',
    weights: { Growth: 0.6, Achievement: 0.2, Purpose: 0.2 },
  },
  {
    id: 3,
    text: 'When you want to make a change in your life, do you tend to involve other people? Describe a specific example.',
    primaryPillar: 'Connection',
    weights: { Connection: 0.7, Purpose: 0.15, Achievement: 0.15 },
  },
  {
    id: 4,
    text: 'How do you react when a routine starts to feel boring or stale? Give an example.',
    primaryPillar: 'Exploration',
    weights: { Exploration: 0.7, Growth: 0.2, Structure: 0.1 },
  },
  {
    id: 5,
    text: 'What is the deeper reason you want to build better habits right now? Who or what is this for?',
    primaryPillar: 'Purpose',
    weights: { Purpose: 0.7, Connection: 0.2, Growth: 0.1 },
  },
  {
    id: 6,
    text: 'Describe your ideal daily routine. How structured or flexible is it, and why does that work for you?',
    primaryPillar: 'Structure',
    weights: { Structure: 0.7, Achievement: 0.15, Exploration: 0.15 },
  },
];
