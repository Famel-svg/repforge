import type { SQLiteDatabase } from 'expo-sqlite';

import type { Exercise, WorkoutXExercise } from '@/types';

type ExerciseRow = {
  id: number;
  sheet_id: number;
  name: string;
  gif_url: string | null;
  position: number;
  entry_id: number | null;
  sets: number | null;
  reps: number | null;
  weight_kg: number | null;
  recorded_at: string | null;
};

export const LIST_EXERCISES_SQL = `
  SELECT
    e.id,
    e.sheet_id,
    e.name,
    e.gif_url,
    e.position,
    latest.id AS entry_id,
    latest.sets,
    latest.reps,
    latest.weight_kg,
    latest.recorded_at
  FROM exercises e
  LEFT JOIN entries latest
    ON latest.id = (
      SELECT id
      FROM entries
      WHERE exercise_id = e.id
      ORDER BY recorded_at DESC, id DESC
      LIMIT 1
    )
  WHERE e.sheet_id = ?
  ORDER BY e.position ASC, e.id ASC
`;

function mapExercise(row: ExerciseRow): Exercise {
  return {
    id: row.id,
    sheetId: row.sheet_id,
    name: row.name,
    gifUrl: row.gif_url,
    position: row.position,
    latestEntry:
      row.entry_id === null
        ? null
        : {
            id: row.entry_id,
            exerciseId: row.id,
            sets: row.sets ?? 0,
            reps: row.reps ?? 0,
            weightKg: row.weight_kg ?? 0,
            recordedAt: row.recorded_at ?? '',
          },
  };
}

export async function listExercises(
  db: SQLiteDatabase,
  sheetId: number,
): Promise<Exercise[]> {
  const rows = await db.getAllAsync<ExerciseRow>(
    LIST_EXERCISES_SQL,
    sheetId,
  );
  return rows.map(mapExercise);
}

export async function addExercise(
  db: SQLiteDatabase,
  sheetId: number,
  exercise: Pick<WorkoutXExercise, 'name' | 'gifUrl'>,
): Promise<number> {
  let insertedId = 0;

  await db.withExclusiveTransactionAsync(async (txn) => {
    const positionRow = await txn.getFirstAsync<{ next_position: number }>(
      'SELECT COALESCE(MAX(position), -1) + 1 AS next_position FROM exercises WHERE sheet_id = ?',
      sheetId,
    );
    const result = await txn.runAsync(
      'INSERT INTO exercises (sheet_id, name, gif_url, position) VALUES (?, ?, ?, ?)',
      sheetId,
      exercise.name.trim(),
      exercise.gifUrl,
      positionRow?.next_position ?? 0,
    );
    insertedId = result.lastInsertRowId;
    await txn.runAsync(
      'UPDATE sheets SET updated_at = ? WHERE id = ?',
      new Date().toISOString(),
      sheetId,
    );
  });

  return insertedId;
}

export async function deleteExercise(
  db: SQLiteDatabase,
  exerciseId: number,
): Promise<void> {
  await db.withExclusiveTransactionAsync(async (txn) => {
    const exercise = await txn.getFirstAsync<{ sheet_id: number }>(
      'SELECT sheet_id FROM exercises WHERE id = ?',
      exerciseId,
    );
    if (!exercise) {
      return;
    }

    await txn.runAsync('DELETE FROM exercises WHERE id = ?', exerciseId);
    await txn.runAsync(
      'UPDATE sheets SET updated_at = ? WHERE id = ?',
      new Date().toISOString(),
      exercise.sheet_id,
    );
  });
}
