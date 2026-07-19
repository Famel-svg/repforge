import type { SQLiteDatabase } from 'expo-sqlite';

const WORKOUTX_KEY = 'workoutx_api_key';

export async function getWorkoutXKey(
  db: SQLiteDatabase,
): Promise<string | null> {
  const row = await db.getFirstAsync<{ value: string }>(
    'SELECT value FROM app_settings WHERE key = ?',
    WORKOUTX_KEY,
  );
  return row?.value ?? null;
}

export async function setWorkoutXKey(
  db: SQLiteDatabase,
  value: string,
): Promise<void> {
  await db.runAsync(
    `INSERT INTO app_settings (key, value)
     VALUES (?, ?)
     ON CONFLICT(key) DO UPDATE SET value = excluded.value`,
    WORKOUTX_KEY,
    value.trim(),
  );
}

export async function clearWorkoutXKey(db: SQLiteDatabase): Promise<void> {
  await db.runAsync('DELETE FROM app_settings WHERE key = ?', WORKOUTX_KEY);
}
