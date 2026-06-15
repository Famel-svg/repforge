export type Sheet = {
  id: number;
  name: string;
  updatedAt: string;
};

export type Exercise = {
  id: number;
  sheetId: number;
  name: string;
  gifUrl: string | null;
  position: number;
  latestEntry: Entry | null;
};

export type Entry = {
  id: number;
  exerciseId: number;
  sets: number;
  reps: number;
  weightKg: number;
  recordedAt: string;
};

export type WorkoutXExercise = {
  id: string;
  name: string;
  gifUrl: string | null;
  target: string;
  bodyPart: string;
  equipment: string;
};

export type BackupEntry = Omit<Entry, 'id' | 'exerciseId'>;

export type BackupExercise = {
  name: string;
  gifUrl: string | null;
  position: number;
  entries: BackupEntry[];
};

export type BackupSheet = {
  name: string;
  updatedAt: string;
  exercises: BackupExercise[];
};

export type WorkoutBackupV1 = {
  exportedAt: string;
  version: 1;
  sheets: BackupSheet[];
};
