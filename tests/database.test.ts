import { DatabaseSync } from 'node:sqlite';

import { LIST_EXERCISES_SQL } from '@/db/exercises';
import { COPY_SHEET_EXERCISES_SQL } from '@/db/sheets';
import { SCHEMA_SQL } from '@/db/schema';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

describe('schema SQLite', () => {
  let db: DatabaseSync;

  beforeEach(() => {
    db = new DatabaseSync(':memory:');
    db.exec('PRAGMA foreign_keys = ON;');
    db.exec(SCHEMA_SQL);
  });

  afterEach(() => {
    db.close();
  });

  it('remove exercícios e entradas em cascata ao excluir ficha', () => {
    const sheet = db
      .prepare('INSERT INTO sheets (name, updated_at) VALUES (?, ?)')
      .run('Treino A', '2026-06-14T10:00:00.000Z');
    const exercise = db
      .prepare(
        'INSERT INTO exercises (sheet_id, name, gif_url, position) VALUES (?, ?, ?, ?)',
      )
      .run(sheet.lastInsertRowid, 'Supino', null, 0);
    db.prepare(
      `INSERT INTO entries
        (exercise_id, sets, reps, weight_kg, recorded_at)
       VALUES (?, ?, ?, ?, ?)`,
    ).run(exercise.lastInsertRowid, 3, 10, 80, '2026-06-14T11:00:00.000Z');

    db.prepare('DELETE FROM sheets WHERE id = ?').run(sheet.lastInsertRowid);

    expect(
      db.prepare('SELECT COUNT(*) AS total FROM exercises').get(),
    ).toEqual({ total: 0 });
    expect(db.prepare('SELECT COUNT(*) AS total FROM entries').get()).toEqual({
      total: 0,
    });
  });

  it('duplica ficha com exercícios sem copiar entradas', () => {
    const sheet = db
      .prepare('INSERT INTO sheets (name, updated_at) VALUES (?, ?)')
      .run('Treino A', '2026-06-14T10:00:00.000Z');
    const first = db
      .prepare(
        'INSERT INTO exercises (sheet_id, name, gif_url, position) VALUES (?, ?, ?, ?)',
      )
      .run(sheet.lastInsertRowid, 'Supino', 'https://example.com/supino.gif', 0);
    const second = db
      .prepare(
        'INSERT INTO exercises (sheet_id, name, gif_url, position) VALUES (?, ?, ?, ?)',
      )
      .run(sheet.lastInsertRowid, 'Rosca', null, 1);
    const insertEntry = db.prepare(
      `INSERT INTO entries
        (exercise_id, sets, reps, weight_kg, recorded_at)
       VALUES (?, ?, ?, ?, ?)`,
    );
    insertEntry.run(
      first.lastInsertRowid,
      3,
      10,
      80,
      '2026-06-14T11:00:00.000Z',
    );
    insertEntry.run(
      second.lastInsertRowid,
      4,
      8,
      22.5,
      '2026-06-14T11:30:00.000Z',
    );

    const duplicated = db
      .prepare('INSERT INTO sheets (name, updated_at) VALUES (?, ?)')
      .run('Treino A (cópia)', '2026-06-14T12:00:00.000Z');
    db.prepare(COPY_SHEET_EXERCISES_SQL).run(
      duplicated.lastInsertRowid,
      sheet.lastInsertRowid,
    );

    expect(
      db.prepare('SELECT name, updated_at FROM sheets WHERE id = ?').get(
        duplicated.lastInsertRowid,
      ),
    ).toEqual({
      name: 'Treino A (cópia)',
      updated_at: '2026-06-14T12:00:00.000Z',
    });
    expect(
      db
        .prepare(
          `SELECT name, gif_url, position
           FROM exercises
           WHERE sheet_id = ?
           ORDER BY position ASC`,
        )
        .all(duplicated.lastInsertRowid),
    ).toEqual([
      {
        name: 'Supino',
        gif_url: 'https://example.com/supino.gif',
        position: 0,
      },
      { name: 'Rosca', gif_url: null, position: 1 },
    ]);
    expect(
      db
        .prepare(
          `SELECT COUNT(*) AS total
           FROM entries n
           JOIN exercises e ON e.id = n.exercise_id
           WHERE e.sheet_id = ?`,
        )
        .get(duplicated.lastInsertRowid),
    ).toEqual({ total: 0 });
    expect(db.prepare('SELECT COUNT(*) AS total FROM entries').get()).toEqual({
      total: 2,
    });
  });

  it('ordena exercícios por posição e retorna entrada mais recente', () => {
    const sheet = db
      .prepare('INSERT INTO sheets (name, updated_at) VALUES (?, ?)')
      .run('Treino B', '2026-06-14T10:00:00.000Z');
    const second = db
      .prepare(
        'INSERT INTO exercises (sheet_id, name, gif_url, position) VALUES (?, ?, ?, ?)',
      )
      .run(sheet.lastInsertRowid, 'Rosca', null, 1);
    db.prepare(
      'INSERT INTO exercises (sheet_id, name, gif_url, position) VALUES (?, ?, ?, ?)',
    ).run(sheet.lastInsertRowid, 'Agachamento', null, 0);
    const insertEntry = db.prepare(
      `INSERT INTO entries
        (exercise_id, sets, reps, weight_kg, recorded_at)
       VALUES (?, ?, ?, ?, ?)`,
    );
    insertEntry.run(
      second.lastInsertRowid,
      3,
      12,
      20,
      '2026-06-14T11:00:00.000Z',
    );
    insertEntry.run(
      second.lastInsertRowid,
      4,
      8,
      25,
      '2026-06-14T12:00:00.000Z',
    );

    const rows = db
      .prepare(LIST_EXERCISES_SQL)
      .all(sheet.lastInsertRowid) as {
      name: string;
      sets: number | null;
      reps: number | null;
      weight_kg: number | null;
    }[];

    expect(rows.map((row) => row.name)).toEqual(['Agachamento', 'Rosca']);
    expect(rows[1]).toMatchObject({ sets: 4, reps: 8, weight_kg: 25 });
  });
});
