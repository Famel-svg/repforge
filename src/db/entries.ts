import type { SQLiteDatabase } from 'expo-sqlite';

import type { Entry } from '@/types';

type EntryRow = {
  id: number;
  exercise_id: number;
  sets: number;
  reps: number;
  weight_kg: number;
  recorded_at: string;
};

function mapEntry(row: EntryRow): Entry {
  return {
    id: row.id,
    exerciseId: row.exercise_id,
    sets: row.sets,
    reps: row.reps,
    weightKg: row.weight_kg,
    recordedAt: row.recorded_at,
  };
}

export async function listEntries(
  db: SQLiteDatabase,
  exerciseId: number,
): Promise<Entry[]> {
  const rows = await db.getAllAsync<EntryRow>(
    `SELECT id, exercise_id, sets, reps, weight_kg, recorded_at
     FROM entries
     WHERE exercise_id = ?
     ORDER BY recorded_at DESC, id DESC`,
    exerciseId,
  );
  return rows.map(mapEntry);
}

export async function addEntry(
  db: SQLiteDatabase,
  exerciseId: number,
  values: Pick<Entry, 'sets' | 'reps' | 'weightKg'>,
): Promise<number> {
  let insertedId = 0;

  await db.withExclusiveTransactionAsync(async (txn) => {
    const exercise = await txn.getFirstAsync<{ sheet_id: number }>(
      'SELECT sheet_id FROM exercises WHERE id = ?',
      exerciseId,
    );
    if (!exercise) {
      throw new Error('Exercício não encontrado.');
    }

    const result = await txn.runAsync(
      `INSERT INTO entries (exercise_id, sets, reps, weight_kg, recorded_at)
       VALUES (?, ?, ?, ?, ?)`,
      exerciseId,
      values.sets,
      values.reps,
      values.weightKg,
      new Date().toISOString(),
    );
    insertedId = result.lastInsertRowId;
    await txn.runAsync(
      'UPDATE sheets SET updated_at = ? WHERE id = ?',
      new Date().toISOString(),
      exercise.sheet_id,
    );
  });

  return insertedId;
}

export async function deleteEntry(
  db: SQLiteDatabase,
  entryId: number,
): Promise<void> {
  await db.withExclusiveTransactionAsync(async (txn) => {
    const entry = await txn.getFirstAsync<{ sheet_id: number }>(
      `SELECT e.sheet_id
       FROM entries n
       JOIN exercises e ON e.id = n.exercise_id
       WHERE n.id = ?`,
      entryId,
    );
    if (!entry) {
      return;
    }

    await txn.runAsync('DELETE FROM entries WHERE id = ?', entryId);
    await txn.runAsync(
      'UPDATE sheets SET updated_at = ? WHERE id = ?',
      new Date().toISOString(),
      entry.sheet_id,
    );
  });
}
