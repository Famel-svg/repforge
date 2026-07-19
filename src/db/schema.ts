import type { SQLiteDatabase } from 'expo-sqlite';

export const DATABASE_VERSION = 2;

export const SCHEMA_SQL = `
  CREATE TABLE IF NOT EXISTS sheets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL CHECK(length(trim(name)) > 0),
    updated_at TEXT NOT NULL
  );

  CREATE TABLE IF NOT EXISTS exercises (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sheet_id INTEGER NOT NULL REFERENCES sheets(id) ON DELETE CASCADE,
    name TEXT NOT NULL CHECK(length(trim(name)) > 0),
    gif_url TEXT,
    position INTEGER NOT NULL CHECK(position >= 0)
  );

  CREATE TABLE IF NOT EXISTS entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    exercise_id INTEGER NOT NULL REFERENCES exercises(id) ON DELETE CASCADE,
    sets INTEGER NOT NULL CHECK(sets > 0),
    reps INTEGER NOT NULL CHECK(reps > 0),
    weight_kg REAL NOT NULL CHECK(weight_kg >= 0),
    recorded_at TEXT NOT NULL
  );

  CREATE INDEX IF NOT EXISTS idx_exercises_sheet_position
    ON exercises(sheet_id, position);
  CREATE INDEX IF NOT EXISTS idx_entries_exercise_recorded
    ON entries(exercise_id, recorded_at DESC, id DESC);

  CREATE TABLE IF NOT EXISTS app_settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
  );
`;

export async function migrateDatabase(db: SQLiteDatabase): Promise<void> {
  await db.execAsync('PRAGMA journal_mode = WAL; PRAGMA foreign_keys = ON;');

  const result = await db.getFirstAsync<{ user_version: number }>(
    'PRAGMA user_version',
  );
  const currentVersion = result?.user_version ?? 0;

  if (currentVersion > DATABASE_VERSION) {
    throw new Error('Banco criado por uma versão mais recente do RepForge.');
  }

  if (currentVersion === 0) {
    await db.execAsync(SCHEMA_SQL);
    await db.execAsync(`PRAGMA user_version = ${DATABASE_VERSION}`);
    return;
  }

  if (currentVersion < 2) {
    await db.execAsync(`
      CREATE TABLE IF NOT EXISTS app_settings (
        key TEXT PRIMARY KEY,
        value TEXT NOT NULL
      );
    `);
    await db.execAsync(`PRAGMA user_version = ${DATABASE_VERSION}`);
  }
}
