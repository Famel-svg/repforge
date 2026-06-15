import {
  normalizeWorkoutXResponse,
  searchExercises,
} from '@/services/workoutx';
import { describe, expect, it } from 'vitest';

describe('WorkoutX', () => {
  it('normaliza resposta direta ou envelopada', () => {
    const normalized = normalizeWorkoutXResponse({
      data: [
        {
          id: '0032',
          name: 'Barbell Deadlift',
          gifUrl: 'https://api.workoutxapp.com/v1/gifs/0032',
          bodyPart: 'upper legs',
          equipment: 'barbell',
          target: 'glutes',
        },
      ],
    });

    expect(normalized).toEqual([
      {
        id: '0032',
        name: 'Barbell Deadlift',
        gifUrl: 'https://api.workoutxapp.com/v1/gifs/0032',
        bodyPart: 'upper legs',
        equipment: 'barbell',
        target: 'glutes',
      },
    ]);
  });

  it('ignora registros quebrados', () => {
    expect(
      normalizeWorkoutXResponse([{ name: 'Sem ID' }, null, 'inválido']),
    ).toEqual([]);
  });

  it('explica quando chave não foi configurada', async () => {
    const previous = process.env.EXPO_PUBLIC_WORKOUTX_KEY;
    delete process.env.EXPO_PUBLIC_WORKOUTX_KEY;

    await expect(searchExercises({ name: 'supino' })).rejects.toThrow(
      'Chave WorkoutX ausente',
    );

    if (previous) {
      process.env.EXPO_PUBLIC_WORKOUTX_KEY = previous;
    }
  });
});
