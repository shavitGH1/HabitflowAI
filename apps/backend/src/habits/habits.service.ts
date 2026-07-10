import { ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { HabitData, HabitRepository } from './habit.repository';
import { CreateHabitDto } from './dto/create-habit.dto';
import { UpdateHabitDto } from './dto/update-habit.dto';

@Injectable()
export class HabitsService {
  constructor(private readonly habitRepository: HabitRepository) {}

  async createHabit(userId: string, dto: CreateHabitDto): Promise<HabitData> {
    return this.habitRepository.createHabit({
      userId,
      title: dto.title,
      description: dto.description,
      frequency: dto.frequency,
      targetCount: dto.targetCount,
    });
  }

  async findByUserId(userId: string): Promise<HabitData[]> {
    return this.habitRepository.findByUserId(userId);
  }

  async updateHabit(userId: string, id: string, dto: UpdateHabitDto): Promise<HabitData> {
    const habit = await this.habitRepository.findById(id);
    if (!habit) throw new NotFoundException('Habit not found');
    if (habit.userId !== userId) throw new ForbiddenException('You do not own this habit');
    return (await this.habitRepository.updateHabit(id, dto))!;
  }

  async deleteHabit(userId: string, id: string): Promise<HabitData> {
    const habit = await this.habitRepository.findById(id);
    if (!habit) throw new NotFoundException('Habit not found');
    if (habit.userId !== userId) throw new ForbiddenException('You do not own this habit');
    return (await this.habitRepository.deleteHabit(id))!;
  }

  async completeHabit(userId: string, id: string): Promise<HabitData> {
    const habit = await this.habitRepository.findById(id);
    if (!habit) throw new NotFoundException('Habit not found');
    if (habit.userId !== userId) throw new ForbiddenException('You do not own this habit');
    return (await this.habitRepository.completeHabit(id))!;
  }
}
