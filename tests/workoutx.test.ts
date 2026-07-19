import {
  normalizeWorkoutXResponse,
  searchExercises,
} from '@/services/workoutx';
import { translateSearchTerm } from '@/utils/translations';
import { describe, expect, it } from 'vitest';

describe('WorkoutX', () => {
  it('normaliza resposta inglesa para texto em português', () => {
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
        name: 'Levantamento terra com barra',
        gifUrl: 'https://api.workoutxapp.com/v1/gifs/0032',
        bodyPart: 'Pernas (superior)',
        equipment: 'Barra',
        target: 'Glúteos',
      },
    ]);
  });

  it('converte termos de pesquisa em português para a API', () => {
    expect(translateSearchTerm('supino')).toBe('bench press');
    expect(translateSearchTerm('rosca bíceps')).toBe('bicep curl');
    expect(translateSearchTerm('levantamento terra')).toBe('deadlift');
  });

  it('traduz palavras recorrentes do catálogo', () => {
    expect(normalizeWorkoutXResponse({ data: [{ id: 'x1', name: 'Cable Rope Lat Pulldown With V-bar Attachment', bodyPart: 'Back', equipment: 'Rope', target: 'Lats' }] })[0]).toMatchObject({
      name: 'Polia Corda Dorsal Puxada Com Barra V Acessório',
      bodyPart: 'Costas',
      equipment: 'Corda',
      target: 'Grande dorsal',
    });
    expect(normalizeWorkoutXResponse({ data: [{ id: 'x2', name: 'Olympic Barbell Wide-grip Upright Row', bodyPart: 'Shoulders', equipment: 'Olympic Barbell', target: 'Upper Back' }] })[0]).toMatchObject({
      name: 'Barra Olímpica Pegada Aberta Alta Remada',
      equipment: 'Barra olímpica',
      target: 'Costas superiores',
    });
  });

  it('ignora registros quebrados', () => {
    expect(
      normalizeWorkoutXResponse([{ name: 'Sem ID' }, null, 'inválido']),
    ).toEqual([]);
  });

  it('explica quando chave não foi configurada', async () => {
    await expect(searchExercises({ name: 'supino' })).rejects.toThrow(
      'Chave WorkoutX ausente',
    );
  });
});

