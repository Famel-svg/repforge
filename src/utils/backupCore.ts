import type { WorkoutBackupV1 } from '@/types';

type RunResult = { lastInsertRowId: number; changes: number };

export type SqlRunner = {
  runAsync(source: string, ...params: any[]): Promise<RunResult>;
  getAllAsync<T>(source: string, ...params: any[]): Promise<T[]>;
};

export type TransactionDatabase = SqlRunner & {
  withExclusiveTransactionAsync(
    task: (transaction: SqlRunner) => Promise<void>,
  ): Promise<void>;
};

type SheetRow = { id: number; name: string; updated_at: string };
type ExerciseRow = {
  id: number;
  name: string;
  gif_url: string | null;
  position: number;
};
type EntryRow = {
  sets: number;
  reps: number;
  weight_kg: number;
  recorded_at: string;
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}

function isIsoDate(value: unknown): value is string {
  return (
    typeof value === 'string' &&
    value.length > 0 &&
    !Number.isNaN(Date.parse(value))
  );
}

function requireString(value: unknown, field: string): string {
  if (typeof value !== 'string' || !value.trim()) {
    throw new Error(`Campo inválido: ${field}.`);
  }
  return value.trim();
}

function requireInteger(value: unknown, field: string, minimum: number): number {
  if (!Number.isInteger(value) || (value as number) < minimum) {
    throw new Error(`Campo inválido: ${field}.`);
  }
  return value as number;
}

function requireNumber(value: unknown, field: string, minimum: number): number {
  if (
    typeof value !== 'number' ||
    !Number.isFinite(value) ||
    value < minimum
  ) {
    throw new Error(`Campo inválido: ${field}.`);
  }
  return value;
}

export function parseBackup(input: unknown): WorkoutBackupV1 {
  if (!isRecord(input) || input.version !== 1 || !Array.isArray(input.sheets)) {
    throw new Error('Backup inválido ou versão não suportada.');
  }
  if (!isIsoDate(input.exported_at ?? input.exportedAt)) {
    throw new Error('Data de exportação inválida.');
  }

  return {
    version: 1,
    exportedAt: String(input.exported_at ?? input.exportedAt),
    sheets: input.sheets.map((sheetValue, sheetIndex) => {
      if (!isRecord(sheetValue) || !Array.isArray(sheetValue.exercises)) {
        throw new Error(`Ficha ${sheetIndex + 1} inválida.`);
      }
      const updatedAt = sheetValue.updated_at ?? sheetValue.updatedAt;
      if (!isIsoDate(updatedAt)) {
        throw new Error(`Data da ficha ${sheetIndex + 1} inválida.`);
      }

      return {
        name: requireString(sheetValue.name, `sheets[${sheetIndex}].name`),
        updatedAt,
        exercises: sheetValue.exercises.map(
          (exerciseValue, exerciseIndex) => {
            if (
              !isRecord(exerciseValue) ||
              !Array.isArray(exerciseValue.entries)
            ) {
              throw new Error(
                `Exercício ${exerciseIndex + 1} da ficha ${sheetIndex + 1} inválido.`,
              );
            }
            const gifUrl = exerciseValue.gif_url ?? exerciseValue.gifUrl;
            if (gifUrl !== null && gifUrl !== undefined && typeof gifUrl !== 'string') {
              throw new Error('URL de GIF inválida.');
            }

            return {
              name: requireString(
                exerciseValue.name,
                `exercises[${exerciseIndex}].name`,
              ),
              gifUrl: gifUrl ? String(gifUrl) : null,
              position: requireInteger(
                exerciseValue.position,
                `exercises[${exerciseIndex}].position`,
                0,
              ),
              entries: exerciseValue.entries.map((entryValue, entryIndex) => {
                if (!isRecord(entryValue)) {
                  throw new Error(`Entrada ${entryIndex + 1} inválida.`);
                }
                const recordedAt =
                  entryValue.recorded_at ?? entryValue.recordedAt;
                if (!isIsoDate(recordedAt)) {
                  throw new Error(`Data da entrada ${entryIndex + 1} inválida.`);
                }

                return {
                  sets: requireInteger(entryValue.sets, 'sets', 1),
                  reps: requireInteger(entryValue.reps, 'reps', 1),
                  weightKg: requireNumber(
                    entryValue.weight_kg ?? entryValue.weightKg,
                    'weight_kg',
                    0,
                  ),
                  recordedAt,
                };
              }),
            };
          },
        ),
      };
    }),
  };
}

export async function buildBackup(
  db: SqlRunner,
): Promise<WorkoutBackupV1> {
  const sheets = await db.getAllAsync<SheetRow>(
    'SELECT id, name, updated_at FROM sheets ORDER BY id ASC',
  );

  return {
    version: 1,
    exportedAt: new Date().toISOString(),
    sheets: await Promise.all(
      sheets.map(async (sheet) => {
        const exercises = await db.getAllAsync<ExerciseRow>(
          `SELECT id, name, gif_url, position
           FROM exercises WHERE sheet_id = ?
           ORDER BY position ASC, id ASC`,
          sheet.id,
        );
        return {
          name: sheet.name,
          updatedAt: sheet.updated_at,
          exercises: await Promise.all(
            exercises.map(async (exercise) => {
              const entries = await db.getAllAsync<EntryRow>(
                `SELECT sets, reps, weight_kg, recorded_at
                 FROM entries WHERE exercise_id = ?
                 ORDER BY recorded_at ASC, id ASC`,
                exercise.id,
              );
              return {
                name: exercise.name,
                gifUrl: exercise.gif_url,
                position: exercise.position,
                entries: entries.map((entry) => ({
                  sets: entry.sets,
                  reps: entry.reps,
                  weightKg: entry.weight_kg,
                  recordedAt: entry.recorded_at,
                })),
              };
            }),
          ),
        };
      }),
    ),
  };
}

export function serializeBackup(backup: WorkoutBackupV1): string {
  return JSON.stringify(
    {
      exported_at: backup.exportedAt,
      version: backup.version,
      sheets: backup.sheets.map((sheet) => ({
        name: sheet.name,
        updated_at: sheet.updatedAt,
        exercises: sheet.exercises.map((exercise) => ({
          name: exercise.name,
          gif_url: exercise.gifUrl,
          position: exercise.position,
          entries: exercise.entries.map((entry) => ({
            sets: entry.sets,
            reps: entry.reps,
            weight_kg: entry.weightKg,
            recorded_at: entry.recordedAt,
          })),
        })),
      })),
    },
    null,
    2,
  );
}

async function insertBackupRows(
  transaction: SqlRunner,
  backup: WorkoutBackupV1,
): Promise<void> {
  for (const sheet of backup.sheets) {
    const sheetResult = await transaction.runAsync(
      'INSERT INTO sheets (name, updated_at) VALUES (?, ?)',
      sheet.name,
      sheet.updatedAt,
    );

    for (const exercise of sheet.exercises) {
      const exerciseResult = await transaction.runAsync(
        `INSERT INTO exercises (sheet_id, name, gif_url, position)
         VALUES (?, ?, ?, ?)`,
        sheetResult.lastInsertRowId,
        exercise.name,
        exercise.gifUrl,
        exercise.position,
      );

      for (const entry of exercise.entries) {
        await transaction.runAsync(
          `INSERT INTO entries
            (exercise_id, sets, reps, weight_kg, recorded_at)
           VALUES (?, ?, ?, ?, ?)`,
          exerciseResult.lastInsertRowId,
          entry.sets,
          entry.reps,
          entry.weightKg,
          entry.recordedAt,
        );
      }
    }
  }
}

export async function importBackupData(
  db: TransactionDatabase,
  input: unknown,
): Promise<number> {
  const backup = parseBackup(input);
  await db.withExclusiveTransactionAsync((transaction) =>
    insertBackupRows(transaction, backup),
  );
  return backup.sheets.length;
}
