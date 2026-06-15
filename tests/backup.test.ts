import { DatabaseSync, type StatementResultingChanges } from 'node:sqlite';

import { SCHEMA_SQL } from '@/db/schema';
import {
  buildBackup,
  importBackupData,
  parseBackup,
  serializeBackup,
  type SqlRunner,
  type TransactionDatabase,
} from '@/utils/backupCore';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

function createAdapter(db: DatabaseSync): TransactionDatabase {
  const runner: SqlRunner = {
    async runAsync(source, ...params) {
      const result = db.prepare(source).run(...params) as StatementResultingChanges;
      return {
        lastInsertRowId: Number(result.lastInsertRowid),
        changes: Number(result.changes),
      };
    },
    async getAllAsync<T>(source: string, ...params: unknown[]) {
      return db.prepare(source).all(...params) as T[];
    },
  };

  return {
    ...runner,
    async withExclusiveTransactionAsync(task) {
      db.exec('BEGIN IMMEDIATE');
      try {
        await task(runner);
        db.exec('COMMIT');
      } catch (error) {
        db.exec('ROLLBACK');
        throw error;
      }
    },
  };
}

const validBackup = {
  exported_at: '2026-06-14T10:30:00.000Z',
  version: 1,
  sheets: [
    {
      name: 'Ficha A',
      updated_at: '2026-06-14T10:00:00.000Z',
      exercises: [
        {
          name: 'Supino',
          gif_url: 'https://example.com/supino.gif',
          position: 0,
          entries: [
            {
              sets: 3,
              reps: 10,
              weight_kg: 80,
              recorded_at: '2026-06-14T09:00:00.000Z',
            },
          ],
        },
      ],
    },
  ],
};

describe('backup JSON', () => {
  let sqlite: DatabaseSync;
  let db: TransactionDatabase;

  beforeEach(() => {
    sqlite = new DatabaseSync(':memory:');
    sqlite.exec('PRAGMA foreign_keys = ON;');
    sqlite.exec(SCHEMA_SQL);
    db = createAdapter(sqlite);
  });

  afterEach(() => {
    sqlite.close();
  });

  it('valida, importa e exporta formato v1', async () => {
    const parsed = parseBackup(validBackup);
    expect(parsed.sheets[0].exercises[0].entries[0].weightKg).toBe(80);

    await expect(importBackupData(db, validBackup)).resolves.toBe(1);
    const exported = await buildBackup(db);
    const serialized = JSON.parse(serializeBackup(exported));

    expect(serialized.version).toBe(1);
    expect(serialized.sheets[0].exercises[0].entries[0]).toMatchObject({
      sets: 3,
      reps: 10,
      weight_kg: 80,
    });
  });

  it('rejeita JSON inválido antes de alterar o banco', async () => {
    await expect(
      importBackupData(db, { version: 2, sheets: [] }),
    ).rejects.toThrow('Backup inválido');

    expect(
      sqlite.prepare('SELECT COUNT(*) AS total FROM sheets').get(),
    ).toEqual({ total: 0 });
  });

  it('faz rollback completo quando uma inserção falha', async () => {
    sqlite.exec(`
      CREATE TRIGGER reject_failed_sheet
      BEFORE INSERT ON sheets
      WHEN NEW.name = 'Falha'
      BEGIN
        SELECT RAISE(ABORT, 'falha simulada');
      END;
    `);
    const input = structuredClone(validBackup);
    input.sheets.push({
      name: 'Falha',
      updated_at: '2026-06-14T11:00:00.000Z',
      exercises: [],
    });

    await expect(importBackupData(db, input)).rejects.toThrow();
    expect(
      sqlite.prepare('SELECT COUNT(*) AS total FROM sheets').get(),
    ).toEqual({ total: 0 });
  });
});
