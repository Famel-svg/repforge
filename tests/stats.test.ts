import { DatabaseSync } from 'node:sqlite';

import { getDashboardStats } from '@/db/stats';
import { SCHEMA_SQL } from '@/db/schema';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

type StatsDb = Parameters<typeof getDashboardStats>[0];

function createAdapter(db: DatabaseSync): StatsDb {
  return {
    async getAllAsync<T>(source: string, ...params: unknown[]) {
      return db.prepare(source).all(...params) as T[];
    },
    async getFirstAsync<T>(source: string, ...params: unknown[]) {
      return db.prepare(source).get(...params) as T | null;
    },
  } as StatsDb;
}

function insertSheet(db: DatabaseSync, name: string): number {
  const result = db
    .prepare('INSERT INTO sheets (name, updated_at) VALUES (?, ?)')
    .run(name, '2026-06-14T10:00:00.000Z');
  return Number(result.lastInsertRowid);
}

function insertExercise(
  db: DatabaseSync,
  sheetId: number,
  name: string,
  position: number,
): number {
  const result = db
    .prepare(
      'INSERT INTO exercises (sheet_id, name, gif_url, position) VALUES (?, ?, ?, ?)',
    )
    .run(sheetId, name, null, position);
  return Number(result.lastInsertRowid);
}

function insertEntry(
  db: DatabaseSync,
  exerciseId: number,
  sets: number,
  reps: number,
  weightKg: number,
  recordedAt: string,
) {
  db.prepare(
    `INSERT INTO entries
      (exercise_id, sets, reps, weight_kg, recorded_at)
     VALUES (?, ?, ?, ?, ?)`,
  ).run(exerciseId, sets, reps, weightKg, recordedAt);
}

describe('dashboard stats', () => {
  let sqlite: DatabaseSync;

  beforeEach(() => {
    sqlite = new DatabaseSync(':memory:');
    sqlite.exec('PRAGMA foreign_keys = ON;');
    sqlite.exec(SCHEMA_SQL);
  });

  afterEach(() => {
    sqlite.close();
  });

  it('agrega totais e volume dos últimos 7 dias', async () => {
    const sheetA = insertSheet(sqlite, 'Treino A');
    const sheetB = insertSheet(sqlite, 'Treino B');
    const supino = insertExercise(sqlite, sheetA, 'Supino', 0);
    const rosca = insertExercise(sqlite, sheetA, 'Rosca', 1);
    insertExercise(sqlite, sheetB, 'Agachamento', 0);

    insertEntry(sqlite, supino, 3, 10, 50, '2026-06-08T10:00:00.000Z');
    insertEntry(sqlite, supino, 4, 8, 60, '2026-06-10T10:00:00.000Z');
    insertEntry(sqlite, rosca, 2, 12, 80, '2026-06-14T10:00:00.000Z');
    insertEntry(sqlite, rosca, 1, 1, 999, '2026-06-07T23:59:59.000Z');
    insertEntry(sqlite, rosca, 10, 10, 10, '2026-06-15T00:00:00.000Z');

    const stats = await getDashboardStats(
      createAdapter(sqlite),
      new Date('2026-06-14T12:00:00.000Z'),
    );

    expect(stats.totals).toEqual({
      entries: 5,
      exercises: 3,
      sheets: 2,
      weeklyVolume: 5340,
    });
    expect(stats.last7Days).toHaveLength(7);
    expect(stats.last7Days[0]).toMatchObject({
      date: '2026-06-08',
      label: '08/06',
      volume: 1500,
    });
    expect(stats.last7Days.map((day) => day.volume)).toEqual([
      1500, 0, 1920, 0, 0, 0, 1920,
    ]);
  });
});
