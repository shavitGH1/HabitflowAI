import * as dotenv from 'dotenv';
dotenv.config();

import mongoose from 'mongoose';
import { GoogleGenAI } from '@google/genai';
import { ResearchChunk, ResearchChunkSchema } from '../src/research-chunks/schemas/research-chunk.schema';

const EMBEDDING_MODEL = 'gemini-embedding-001';

const SOURCE_TITLE =
  'The Architecture of Digital Behavior: Neurobiology, Motivational Frameworks, and UX Persona Design';

const CURATED_CHUNKS: { section: string; content: string }[] = [
  {
    section: 'Neuroscience of Habit Formation',
    content:
      'Habit formation is the process through which deliberate, goal-directed behaviors transition into automatic, cue-driven actions requiring minimal cognitive load. A habit follows a neurological loop of four stages: the cue (the trigger), the routine (the executed behavior), the reward (the neurochemical feedback), and the craving (the anticipatory mechanism that sustains the loop). The popular "21 days to form a habit" claim is false: Lally et al. (2010) found the transition to automaticity takes an average of 66 days, ranging from 18 to 254 days depending on behavior complexity and individual differences. During this period, dopamine functions not as a pleasure chemical but as a motivational signal that generates anticipation and craving, bridging the gap between cue and routine.',
  },
  {
    section: 'Neurobiological Substrates of Habit Crystallization',
    content:
      'Early in learning a new behavior, the brain operates in a goal-directed mode via the prefrontal cortex and the associative striatum, staying sensitive to reward devaluation. As a behavior repeats consistently in a stable context, control migrates to the sensorimotor striatum (the dorsolateral striatum, DLS), which fires intensely at the start and end of a behavioral sequence while staying quiet during execution — a phenomenon called "chunking" that encodes a complex sequence as one automated unit and sharply reduces cognitive load. Once chunked in the DLS, the habit becomes a stimulus-response habit largely insensitive to outcome devaluation. The infralimbic cortex acts as an on/off switch that can block even a deeply ingrained habit instantly, meaning habits stay under continuous cortical control. Hyper-activation of these habit-forming structures has been linked to the excessive, maladaptive habit formation seen in obsessive-compulsive disorder.',
  },
  {
    section: 'The Intention-Behavior Gap and Implementation Intentions',
    content:
      'Explicit goal intentions ("I intend to exercise more") explain only 28% to 35% of the variance in actual future behavior — the intention-behavior gap, caused by self-regulatory depletion, temporal discounting, forgetfulness, and contextual friction. Psychologist Peter Gollwitzer\'s "implementation intentions" bridge this gap by restructuring an abstract goal into a specific if-then plan: "If situation X occurs, then I will initiate behavior Y." This pre-decided link delegates the initiation of behavior from effortful executive control to automatic environmental triggers. A landmark meta-analysis of 94 tests (Gollwitzer & Sheeran, 2006) found a medium-to-large effect size (d=0.65) on goal attainment versus goal intentions alone; a 2024 meta-analysis of 642 tests confirmed the effect, with implementation intentions yielding d=0.51 for healthy eating and d=0.31 for sustained physical activity. The mechanism works via heightened cue accessibility (the trigger becomes chronically accessible in working memory) and automated response initiation that bypasses effortful self-regulation, even under ego depletion.',
  },
  {
    section: 'The Fogg Behavior Model (B=MAP)',
    content:
      'BJ Fogg\'s B=MAP model states that Behavior occurs only when Motivation, Ability, and a Prompt converge simultaneously at the moment of decision; if any one element is missing, the behavior fails to fire. Motivation is the most volatile, unreliable variable (it fluctuates with fatigue, emotion, and cognitive load), so a sustainable habit architecture should prioritize Ability — making the action as simple as possible — over trying to inflate motivation. Ability is governed by a "simplicity chain" of six links: time, money, physical effort, mental effort, social deviance, and non-routine disruption; the behavior fails at the weakest link. Fogg\'s "Tiny Habits" method reduces a desired behavior to under thirty seconds (maximizing Ability), anchors it to an existing reliable routine (the Prompt), and uses immediate self-celebration to trigger a dopamine release that compensates for low initial Motivation — it is this immediate emotional reinforcement, not repetition alone, that speeds up the neurological wiring of the habit loop.',
  },
  {
    section: 'Self-Determination Theory (SDT)',
    content:
      "Deci and Ryan's Self-Determination Theory differentiates extrinsic motivation (acting for a separable outcome, like a badge) from intrinsic motivation (acting for the activity's own satisfaction). Optimal well-being and sustained intrinsic motivation require three needs to be continuously satisfied: Autonomy (perceived control over one's choices — supported by customizable pathways, flexible goals, voluntary participation), Competence (mastery and efficacy — supported by immediate clear feedback, an optimal 'Goldilocks zone' of challenge, visible progress), and Relatedness (belonging and social connection — supported by team goals, collaborative challenges, shared accountability). A key risk is the overjustification effect: intrinsically interesting behaviors degrade if heavy extrinsic rewards are layered on top. Extrinsic rewards should therefore function as informational feedback affirming competence, not as behavioral bribes that control the user.",
  },
  {
    section: "Reiss's 16 Basic Desires Profile",
    content:
      "Dr. Steven Reiss's 16 Basic Desires is an empirically derived taxonomy arguing that all human behavior is driven by varying sensitivities to 16 distinct evolutionary needs — what motivates one user may alienate another. Selected desires and their UX implications: Power (competitive leaderboards, admin privileges, public ranking), Independence (isolated non-social task management, deep customization), Curiosity (mystery rewards, easter eggs, exploratory interfaces), Acceptance (normative messaging, peer validation, supportive feedback), Order (strict streaks, rigid scheduling, implementation-intention templates, clean UI), Saving (digital inventories, point hoarding, streak-protection mechanics), Honor (community rule enforcement, loyalty badges, streak commitments), Idealism (charity-linked productivity, mentoring, prosocial outcomes), Social Contact (forums, synchronous multiplayer, accountability partnerships), Family (pet-raising / tamagotchi mechanics), Status (exclusive visual flair, VIP tiers, mastery badges), Vengeance (PvP competition, defensive point-protection), Romance (highly aesthetic interfaces, avatar customization), Eating (nutrition tracking, recipe rewards), Physical Exercise (biometric integrations, step tracking), and Tranquility (mindfulness features, dark modes, stress-free progression, data privacy).",
  },
  {
    section: 'The Six Core Motivators of Play',
    content:
      'In gameful design, researchers like Raph Koster and Nicole Lazzaro map psychological needs onto interactive systems as six core motivators of play: Mastery (Competence) — the drive to comprehend complex systems, rewarded by dopamine on successful problem-solving; Fun/Discovery (Curiosity) — pursuit of novel content and the dopamine-driven anticipation of surprise ("Easy Fun"); Competition (Power/Status) — striving for hierarchy and dominance ("Hard Fun"); Immersion/Meditation (Tranquility) — "Flow" states that reduce anxiety and provide cognitive sanctuary ("Serious Fun"); Connection (Relatedness) — the system as a conduit for social bonding and mutual accountability ("People Fun"); and Self-Expression (Autonomy) — using the environment to project identity and exert personal agency.',
  },
  {
    section: "Marczewski's Gamification User Types (The Hexad Framework)",
    content:
      "Andrzej Marczewski's Gamification User Types Hexad, validated by Tondello et al. via factor analysis, segments users by their receptivity to specific motivational elements, proving a one-size-fits-all gamification approach is flawed. Philanthropists (Purpose & Meaning) are driven by altruism and community enrichment — served by gifting mechanics, mentoring, communal goal tracking. Socialisers (Relatedness) use the system to interact and collaborate — served by guilds, social networks, collaborative quests. Free Spirits (Autonomy) want self-expression and exploration free of rigid rules — served by customization, exploratory interfaces, non-linear progression. Achievers (Competence & Mastery) want to prove proficiency against structural friction — served by progression tracking, tiered badges, skill trees. Players (Extrinsic Rewards) optimize purely for points, badges, and prizes regardless of the activity's meaning — served by points systems and virtual economies. Disruptors (Change & Agency) test boundaries and instigate change — served by modding, voting, and feedback loops.",
  },
  {
    section: 'Persona: Achievement — The Achiever',
    content:
      "The Achiever persona is outcome-oriented, driven by Power, Status, and Competence, and evaluates a system by how efficiently it lets them reach measurable, discrete milestones. Achievers engage heavily with quantitative tracking over qualitative experience — a 100-day streak matters more than the feeling of the habit itself — and use implementation intentions strictly for optimization, chaining tasks tightly together. Their dopaminergic system is highly sensitive to task completion; a closed progress bar or a badge triggers an immediate reward. Optimal interventions: tiered leveling, deep data visualization (graphs, heatmaps), quantifiable progress metrics, and the 'goal-gradient effect' (motivation accelerates as visual distance to the goal shrinks). Critical risk: Achievers are highly susceptible to the 'what-the-hell effect' when a long streak breaks — the psychological blow of losing accumulated digital capital can cause total abandonment, so systems need forgiveness mechanics like streak freezes or weekend exemptions.",
  },
  {
    section: 'Persona: Growth — The Grower',
    content:
      'The Grower persona is driven by Competence and Curiosity rooted in intrinsic mastery, valuing the continuous process of self-improvement over the final outcome, with a high tolerance for failure as long as it yields learning. Growers seek qualitative tracking (e.g. mood mapped against sleep) and complex skill acquisition, intuitively calibrating the Fogg Ability parameter to scale habits to their daily cognitive load. Their reinforcement is internal — once a habit is fully automated (chunked in the dorsolateral striatum) they may lose interest and abandon the app, since the loss of conscious effort reduces their sense of active growth. Retention requires scaffolded learning paths, reflective journaling prompts, and adaptive difficulty using a continuous challenge-skill balance (Csikszentmihalyi\'s Flow theory) — static, repetitive stimulus-response loops cause rapid boredom for this persona.',
  },
  {
    section: 'Persona: Connection — The Socializer',
    content:
      "The Socializer persona is dictated by the need for Relatedness plus Reiss's Social Contact and Acceptance desires; motivation is effectively outsourced to the community, and digital habit tracking is a medium for bonding and collective achievement rather than solitary efficiency. Socializers thrive in group challenges and as accountability partners, and their compliance is driven by desire for social affirmation and fear of letting a team down. Optimal interventions: asynchronous multiplayer mechanics, shared team health bars, social commitments, peer-to-peer nudging, and Oinas-Kukkonen's Persuasive System Design principles (praise, normative influence, social comparison). Critical vulnerability: platform isolation — if their accountability group or social network churns, the Socializer's habits collapse simultaneously, so the system needs robust matchmaking to keep them in an active community.",
  },
  {
    section: 'Persona: Exploration — The Explorer',
    content:
      'The Explorer persona is driven by Independence, Autonomy, and Curiosity, correlating with the Hexad "Free Spirit," and has a fundamental aversion to rigid structures and linear, repetitive stimulus-response habit loops — automated, chunked behavior feels restrictive, and routine feels like stagnation. Explorers constantly seek new features, alter their routines, and abandon trackers that demand linear repetition; their dopaminergic system is primed for novelty and prediction-error anticipation rather than static, predictable rewards. Engagement requires variable-ratio reward schedules, mystery boxes, unlockable content, branching pathways, and a wide variety of rotating micro-habits rather than 66 days of repeating one task. The core design danger: Explorers biologically struggle with the repetition needed for a habit to actually crystallize, so the system must disguise repetition through varied visual contexts and randomized positive reinforcement to keep them engaged long enough for the habit to take root.',
  },
  {
    section: 'Persona: Purpose — The Altruist',
    content:
      'The Altruist persona is governed by Reiss\'s Idealism, Honor, and Family desires, correlating with the Hexad "Philanthropist" type; behavioral change cannot be sustained by selfish accumulation and must be anchored to a macro-level internal "why." Altruists prioritize habits that benefit family, community, or a cause; they are resilient against superficial gamification (points and badges read as trivial) but respond strongly to narrative-driven design and prosocial outcomes — behavior is sustained by alignment with core values, not sensory pleasure or operant conditioning. Effective interventions translate personal habits into real-world charitable impact (miles run into meals donated, focus time into trees planted) and let them mentor other users or contribute to community knowledge. Critical pitfall: perceived inauthenticity — Altruists will permanently abandon a platform that feels manipulative, commodified, or purely transactional, since that violates their need for Honor.',
  },
  {
    section: 'Persona: Structure — The Architect',
    content:
      "The Architect persona is motivated by Order, Saving, and Tranquility, operating in a state of high cognitive vigilance that seeks to reduce decision fatigue and environmental anxiety through hyper-organization and predictability. Architects extensively use Gollwitzer's implementation intentions, building complex nested if-then plans to cover every contextual friction point, and rely on the digital system as an external prefrontal cortex to offload executive function, reducing daily cognitive load through rigid, unbending rules. Effective interventions: deep customization of triggers, seamless calendar integration, robust data export, rigid rule-setting parameters, and a highly minimalist interface that reduces mental effort and visual noise per Fogg's Ability principle. Architects become overwhelmed and frustrated by unpredicted changes, intrusive gamification notifications, or chaotic social features — they need deterministic, reliable feedback loops and explicitly reject the probabilistic variable rewards that appeal to Explorers.",
  },
  {
    section: 'Habitica Case Study: Operant Conditioning and SDT',
    content:
      "Habitica translates habit tracking into an RPG built on B.F. Skinner's operant conditioning: positive reinforcement (gold, XP, item drops on completing a task, bridging the temporal gap between an action and its long-term benefit), negative punishment via loss aversion (missed 'Dailies' damage the user's avatar, and because of the IKEA/endowment effect from time invested in the avatar, the threat of damage forces compliance), and variable rewards (randomized pet/egg/equipment drops on a variable-ratio schedule, which behaviorally produces the highest, most extinction-resistant response rate). Despite this operant surface, Habitica sustains long-term engagement by also satisfying SDT's three needs: Autonomy (users define their own habits and quest parameters), Competence (RPG leveling and visible mastery progression), and Relatedness (the Party system implements collective consequence — if one member misses their dailies, a boss damages the entire party, leveraging social pressure and altruism to keep Socializers and Altruists locked in to protect their peers).",
  },
  {
    section: 'Habitica Case Study: The Danger of Counterproductive Gamification',
    content:
      "Empirical research by Diefenbach and Müssig (2019) into Habitica found the imposition of rigid external rewards on complex human behavior generates real adverse outcomes. Gamification-induced procrastination: users optimize for point accumulation, delaying large cognitively demanding tasks in favor of trivial micro-tasks that farm XP, rewarding the illusion of productivity over real achievement. Punishment during high productivity: Habitica punishes missed daily check-ins even if the user was highly productive elsewhere (e.g. a 14-hour real-world project) and simply fell asleep before logging in — a temporal misalignment that causes frustration and app abandonment. Task redefinition: to avoid avatar damage and social embarrassment, users redefine a difficult 'Daily' into a due-date-free 'Habit,' bypassing accountability and undermining the actual goal. Anxiety and overjustification: constant fear of losing a streak or damaging the party shifts motivation from intrinsic self-improvement to extrinsic fear-avoidance, eroding the user's genuine internal desire to maintain the habit over time.",
  },
  {
    section: 'Synthesis and UX Design Implications',
    content:
      "Successful digital habit systems require synthesizing neurobiology, cognitive planning, and psychological profiling to reduce cognitive friction — the same friction reduction that drives the brain's own transition from goal-directed prefrontal control to automated, chunked striatal execution. The Fogg Behavior Model and Gollwitzer's implementation intentions are the tactical bridges that let users bypass motivational depletion and push behaviors toward automaticity. Critically, external mechanics must be mapped to the user's psychological typology — competitive leaderboards should not be forced onto Architect-type users, nor rigid daily streaks onto Explorer-type users. As Habitica's counterproductive effects show, gamification is not a universal panacea: digital behavioral design must subordinate Operant Conditioning to Self-Determination Theory, treating points, badges, and streaks as informational feedback that supports the user's innate pursuit of autonomy, competence, and relatedness rather than as manipulative bribes that extract engagement.",
  },
];

async function main(): Promise<void> {
  const mongoUri = process.env.MONGO_URI;
  const apiKey = process.env.GEMINI_API_KEY;
  if (!mongoUri) throw new Error('MONGO_URI is not set');
  if (!apiKey) throw new Error('GEMINI_API_KEY is not set');

  await mongoose.connect(mongoUri);
  const ResearchChunkModel = mongoose.model(ResearchChunk.name, ResearchChunkSchema);
  const ai = new GoogleGenAI({ apiKey });

  let inserted = 0;
  let skipped = 0;

  for (const chunk of CURATED_CHUNKS) {
    const exists = await ResearchChunkModel.findOne({
      sourceTitle: SOURCE_TITLE,
      section: chunk.section,
    }).exec();
    if (exists) {
      skipped++;
      continue;
    }

    const response = await ai.models.embedContent({
      model: EMBEDDING_MODEL,
      contents: chunk.content,
    });
    const embedding = response.embeddings?.[0]?.values;
    if (!embedding) {
      console.warn(`No embedding returned for "${chunk.section}", skipping`);
      continue;
    }

    await ResearchChunkModel.create({
      sourceTitle: SOURCE_TITLE,
      section: chunk.section,
      content: chunk.content,
      embedding,
    });
    inserted++;
  }

  console.log(`Seeded ${inserted} research chunks, skipped ${skipped} already present.`);
  await mongoose.disconnect();
}

main().catch(error => {
  console.error(error);
  process.exit(1);
});
