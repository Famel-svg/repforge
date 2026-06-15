import axios, { isAxiosError } from 'axios';

import type { WorkoutXExercise } from '@/types';

const API_URL = 'https://api.workoutxapp.com/v1';

export const BODY_PARTS = [
  'back',
  'cardio',
  'chest',
  'lower arms',
  'lower legs',
  'neck',
  'shoulders',
  'upper arms',
  'upper legs',
  'waist',
];

export const EQUIPMENT = [
  'assisted',
  'barbell',
  'body weight',
  'bosu ball',
  'cable',
  'dumbbell',
  'elliptical machine',
  'ez barbell',
  'kettlebell',
  'leverage machine',
  'medicine ball',
  'resistance band',
  'roller',
  'rope',
  'smith machine',
  'stability ball',
  'weighted',
];

type WorkoutXRaw = {
  id?: string | number;
  name?: string;
  gifUrl?: string;
  gif_url?: string;
  target?: string;
  bodyPart?: string;
  body_part?: string;
  equipment?: string;
};

export function normalizeWorkoutXResponse(input: unknown): WorkoutXExercise[] {
  let candidates: unknown = input;
  if (input && typeof input === 'object' && !Array.isArray(input)) {
    const object = input as Record<string, unknown>;
    candidates = object.data ?? object.results ?? object.exercises ?? [];
  }

  if (!Array.isArray(candidates)) {
    return [];
  }

  return candidates
    .map((item): WorkoutXExercise | null => {
      if (!item || typeof item !== 'object') {
        return null;
      }
      const raw = item as WorkoutXRaw;
      if (raw.id === undefined || typeof raw.name !== 'string') {
        return null;
      }

      return {
        id: String(raw.id),
        name: raw.name,
        gifUrl: raw.gifUrl ?? raw.gif_url ?? null,
        target: raw.target ?? '',
        bodyPart: raw.bodyPart ?? raw.body_part ?? '',
        equipment: raw.equipment ?? '',
      };
    })
    .filter((item): item is WorkoutXExercise => item !== null);
}

export type SearchExerciseParams = {
  bodyPart?: string;
  equipment?: string;
  name?: string;
  limit?: number;
};

export async function searchExercises({
  bodyPart,
  equipment,
  name,
  limit = 20,
}: SearchExerciseParams): Promise<WorkoutXExercise[]> {
  const apiKey = process.env.EXPO_PUBLIC_WORKOUTX_KEY;
  if (!apiKey) {
    throw new Error(
      'Chave WorkoutX ausente. Configure EXPO_PUBLIC_WORKOUTX_KEY no arquivo .env.',
    );
  }

  try {
    const response = await axios.get(`${API_URL}/exercises/search`, {
      headers: { 'X-WorkoutX-Key': apiKey },
      params: {
        bodyPart: bodyPart || undefined,
        equipment: equipment || undefined,
        query: name?.trim() || undefined,
        limit,
      },
      timeout: 12_000,
    });

    return normalizeWorkoutXResponse(response.data).slice(0, limit);
  } catch (error) {
    if (error instanceof Error && error.message.startsWith('Chave WorkoutX')) {
      throw error;
    }
    if (isAxiosError(error) && error.response?.status === 401) {
      throw new Error('Chave WorkoutX inválida.');
    }
    if (isAxiosError(error) && error.response?.status === 429) {
      throw new Error('Limite mensal da WorkoutX atingido.');
    }
    if (isAxiosError(error) && !error.response) {
      throw new Error('Sem conexão — verifique sua internet.');
    }
    throw new Error('Não foi possível buscar exercícios agora.');
  }
}
