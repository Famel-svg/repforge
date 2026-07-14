import type { Entry } from '@/types';

export type LiftEntry = Pick<Entry, 'sets' | 'reps' | 'weightKg'>;

export type PersonalRecordStatus = {
  weight: boolean;
  oneRepMax: boolean;
};

export function calculateVolume(entry: LiftEntry): number {
  return entry.sets * entry.reps * entry.weightKg;
}

export function estimateOneRepMax(entry: Pick<LiftEntry, 'reps' | 'weightKg'>): number {
  if (entry.reps <= 0 || entry.weightKg <= 0) {
    return 0;
  }

  return entry.weightKg * (1 + entry.reps / 30);
}

export function detectPersonalRecords(
  entry: LiftEntry,
  previousEntries: LiftEntry[],
): PersonalRecordStatus {
  if (previousEntries.length === 0) {
    return { weight: false, oneRepMax: false };
  }

  const previousMaxWeight = Math.max(...previousEntries.map((item) => item.weightKg));
  const previousMaxOneRepMax = Math.max(
    ...previousEntries.map((item) => estimateOneRepMax(item)),
  );

  return {
    weight: entry.weightKg > previousMaxWeight,
    oneRepMax: estimateOneRepMax(entry) > previousMaxOneRepMax,
  };
}
