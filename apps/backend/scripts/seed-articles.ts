import * as dotenv from 'dotenv';
dotenv.config();

import mongoose from 'mongoose';
import { GoogleGenAI } from '@google/genai';
import { Article, ArticleSchema } from '../src/articles/schemas/article.schema';
import { logger } from '../src/logger';

const EMBEDDING_MODEL = 'gemini-embedding-001';

const CURATED_ARTICLES: { title: string; url: string }[] = [
  { title: 'Three Steps to Building Better Habits', url: 'https://jamesclear.com/three-steps-habit-change' },
  { title: 'How to Build a New Habit: This is Your Strategy Guide', url: 'https://jamesclear.com/habit-guide' },
  { title: 'Identity-Based Habits: How to Actually Stick to Your Goals', url: 'https://jamesclear.com/identity-based-habits' },
  { title: 'Habit Stacking: How to Build New Habits by Using Old Ones', url: 'https://jamesclear.com/habit-stacking' },
  { title: 'The Plateau of Latent Potential', url: 'https://jamesclear.com/plateau-of-latent-potential' },
  { title: 'Goals vs. Systems: Why Your System Matters More Than Your Goal', url: 'https://jamesclear.com/goals-systems' },
  { title: 'How to Stop Procrastinating by Using the "Seinfeld Strategy"', url: 'https://jamesclear.com/stop-procrastinating' },
  { title: 'The Habit Tracker: A Simple Trick for Building Better Habits', url: 'https://jamesclear.com/habit-tracker' },
  { title: 'Zen Habits: Small Steps Toward a Simpler, Calmer Life', url: 'https://zenhabits.net/' },
  { title: 'The Tiny Habits Method for Lasting Behavior Change', url: 'https://www.tinyhabits.com/' },
  { title: 'How Meditation Helps New Habits Stick', url: 'https://www.headspace.com/' },
  { title: 'The Psychology of Motivation and Habit Formation', url: 'https://www.apa.org/' },
  { title: 'How to Make Healthy Habits Stick, According to Mayo Clinic', url: 'https://www.mayoclinic.org/' },
  { title: 'The Science of Habit Formation', url: 'https://www.health.harvard.edu/' },
  { title: 'How Long Does It Actually Take to Form a Habit?', url: 'https://www.verywellmind.com/' },
  { title: 'The Power of Habit and Why We Do What We Do', url: 'https://www.psychologytoday.com/' },
  { title: 'Motivation Science: What Actually Drives Behavior Change', url: 'https://www.coursera.org/' },
  { title: 'A Practical Guide to Setting and Achieving Goals', url: 'https://www.mindtools.com/' },
  { title: 'Staying Consistent When Motivation Runs Out', url: 'https://www.verywellmind.com/' },
  { title: 'Building an Accountability System With Friends', url: 'https://www.psychologytoday.com/' },
  { title: 'Why Small Wins Matter More Than Big Ones', url: 'https://jamesclear.com/tiny-changes' },
  { title: 'How to Recover Quickly After Breaking a Streak', url: 'https://jamesclear.com/missing-workouts' },
  { title: 'Finding Purpose: Connecting Daily Habits to Long-Term Meaning', url: 'https://www.psychologytoday.com/' },
  { title: 'Structure and Routine: Designing a Schedule That Works', url: 'https://www.mindtools.com/' },
];

async function main(): Promise<void> {
  const mongoUri = process.env.MONGO_URI;
  const apiKey = process.env.GEMINI_API_KEY;
  if (!mongoUri) throw new Error('MONGO_URI is not set');
  if (!apiKey) throw new Error('GEMINI_API_KEY is not set');

  await mongoose.connect(mongoUri);
  const ArticleModel = mongoose.model(Article.name, ArticleSchema);
  const ai = new GoogleGenAI({ apiKey });

  let inserted = 0;
  let skipped = 0;

  for (const article of CURATED_ARTICLES) {
    const exists = await ArticleModel.findOne({ title: article.title }).exec();
    if (exists) {
      skipped++;
      continue;
    }

    const response = await ai.models.embedContent({
      model: EMBEDDING_MODEL,
      contents: `${article.title}`,
    });
    const embedding = response.embeddings?.[0]?.values;
    if (!embedding) {
      logger.warn(`No embedding returned for "${article.title}", skipping`);
      continue;
    }

    await ArticleModel.create({ title: article.title, url: article.url, embedding });
    inserted++;
  }

  logger.info(`Seeded ${inserted} articles, skipped ${skipped} already present.`);
  await mongoose.disconnect();
}

main().catch(error => {
  logger.error(error);
  process.exit(1);
});
