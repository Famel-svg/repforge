import axios, { isAxiosError } from 'axios';

import { workoutXApiUrl, workoutXProxyOrigin } from '@/config';
import type { WorkoutXExercise } from '@/types';
import {
  translateBodyPart,
  translateEquipment,
  translateExerciseName,
  translateMuscle,
  translateSearchTerm,
} from '@/utils/translations';

export const BODY_PARTS = [
  'Costas',
  'Cardio',
  'Peito',
  'Antebraços',
  'Pernas (inferior)',
  'Pescoço',
  'Ombros',
  'Braços',
  'Pernas (superior)',
  'Abdômen',
];

export const BODY_PARTS_MAP: Record<string, string> = {
  'Costas': 'back',
  'Cardio': 'cardio',
  'Peito': 'chest',
  'Antebraços': 'lower arms',
  'Pernas (inferior)': 'lower legs',
  'Pescoço': 'neck',
  'Ombros': 'shoulders',
  'Braços': 'upper arms',
  'Pernas (superior)': 'upper legs',
  'Abdômen': 'waist',
};

export const EQUIPMENT = [
  'Assistido',
  'Halter barra',
  'Peso corporal',
  'Bosu',
  'Polia',
  'Halter',
  'Elíptica',
  'Barra W',
  'Kettlebell',
  'Máquina alavanca',
  'Bola medicinal',
  'Faixa elástica',
  'Rolo',
  'Corda',
  'Máquina Smith',
  'Bola de estabilidade',
  'Com peso',
];

export const EQUIPMENT_MAP: Record<string, string> = {
  'Assistido': 'assisted',
  'Halter barra': 'barbell',
  'Peso corporal': 'body weight',
  'Bosu': 'bosu ball',
  'Polia': 'cable',
  'Halter': 'dumbbell',
  'Elíptica': 'elliptical machine',
  'Barra W': 'ez barbell',
  'Kettlebell': 'kettlebell',
  'Máquina alavanca': 'leverage machine',
  'Bola medicinal': 'medicine ball',
  'Faixa elástica': 'resistance band',
  'Rolo': 'roller',
  'Corda': 'rope',
  'Máquina Smith': 'smith machine',
  'Bola de estabilidade': 'stability ball',
  'Com peso': 'weighted',
};

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
        name: translateExerciseName(raw.name),
        gifUrl: raw.gifUrl ?? raw.gif_url ?? null,
        target: translateMuscle(raw.target),
        bodyPart: translateBodyPart(raw.bodyPart ?? raw.body_part),
        equipment: translateEquipment(raw.equipment),
      };
    })
    .filter((item): item is WorkoutXExercise => item !== null);
}


async function hydrateGifUrls(
  exercises: WorkoutXExercise[],
  apiKey: string | null,
  installId: string | null,
): Promise<WorkoutXExercise[]> {
  const { downloadGif } = await import('@/utils/downloadGif');
  const hydrated: WorkoutXExercise[] = [];

  for (let index = 0; index < exercises.length; index += 6) {
    const chunk = exercises.slice(index, index + 6);
    hydrated.push(
      ...(await Promise.all(
        chunk.map(async (exercise) => {
          if (!exercise.gifUrl) return exercise;
          const localGifUrl = await downloadGif(
            exercise.gifUrl,
            apiKey,
            installId,
          );
          return localGifUrl ? { ...exercise, gifUrl: localGifUrl } : exercise;
        }),
      )),
    );
  }

  return hydrated;
}
export type SearchExerciseParams = {
  apiKey?: string | null;
  installId?: string | null;
  bodyPart?: string;
  equipment?: string;
  name?: string;
  limit?: number;
};

async function fetchFromEndpoint(
  endpoint: string,
  apiKey: string | null,
  installId: string | null,
  limit: number,
): Promise<WorkoutXExercise[]> {
  const response = await axios.get(`${workoutXApiUrl}${endpoint}`, {
    headers: workoutXProxyOrigin
      ? { 'X-RepForge-Install': installId ?? 'unknown' }
      : { 'X-WorkoutX-Key': apiKey ?? '' },
    params: { limit },
    timeout: 12_000,
  });
  return normalizeWorkoutXResponse(response.data).slice(0, limit);
}

export async function searchExercises({
  apiKey,
  installId,
  bodyPart,
  equipment,
  name,
  limit = 20,
}: SearchExerciseParams): Promise<WorkoutXExercise[]> {
  const key = apiKey?.trim();
  const deviceId = installId?.trim() ?? null;
  if (!workoutXProxyOrigin && !key) {
    throw new Error(
      'Chave WorkoutX ausente. Configure sua chave em Config.',
    );
  }

  try {
    const trimmedName = name?.trim();
    const apiName = trimmedName ? translateSearchTerm(trimmedName) : '';
    let results: WorkoutXExercise[] = [];

    if (trimmedName) {
      results = await fetchFromEndpoint(
        `/exercises/name/${encodeURIComponent(apiName)}`,
        key ?? null,
        deviceId,
        limit,
      );
    } else if (bodyPart) {
      const apiBodyPart = BODY_PARTS_MAP[bodyPart] ?? bodyPart;
      results = await fetchFromEndpoint(
        `/exercises/bodyPart/${encodeURIComponent(apiBodyPart)}`,
        key ?? null,
        deviceId,
        limit,
      );
    } else if (equipment) {
      const apiEquipment = EQUIPMENT_MAP[equipment] ?? equipment;
      results = await fetchFromEndpoint(
        `/exercises/equipment/${encodeURIComponent(apiEquipment)}`,
        key ?? null,
        deviceId,
        limit,
      );
    } else {
      results = await fetchFromEndpoint('/exercises', key ?? null, deviceId, limit);
    }

    return hydrateGifUrls(results.slice(0, limit), key ?? null, deviceId);
  } catch (error) {
    if (error instanceof Error && error.message.startsWith('Chave WorkoutX')) {
      throw error;
    }
    if (isAxiosError(error) && error.response?.status === 401) {
      throw new Error('Chave WorkoutX inválida.');
    }
    if (isAxiosError(error) && error.response?.status === 429) {
      throw new Error('Limite de buscas atingido. Tente novamente amanhã.');
    }
    if (isAxiosError(error) && !error.response) {
      throw new Error('Sem conexão — verifique sua internet.');
    }
    throw new Error('Não foi possível buscar exercícios agora.');
  }
}



