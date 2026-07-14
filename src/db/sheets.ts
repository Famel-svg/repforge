import type { SQLiteDatabase } from 'expo-sqlite';

import type { Sheet } from '@/types';

export const COPY_SHEET_EXERCISES_SQL = `
  INSERT INTO exercises (sheet_id, name, gif_url, position)
  SELECT ?, name, gif_url, position
  FROM exercises
  WHERE sheet_id = ?
  ORDER BY position ASC, id ASC
`;

type SheetRow = {
  id: number;
  name: string;
  updated_at: string;
};

function mapSheet(row: SheetRow): Sheet {
  return {
    id: row.id,
    name: row.name,
    updatedAt: row.updated_at,
  };
}

export async function listSheets(db: SQLiteDatabase): Promise<Sheet[]> {
  const rows = await db.getAllAsync<SheetRow>(
    'SELECT id, name, updated_at FROM sheets ORDER BY updated_at DESC, id DESC',
  );
  return rows.map(mapSheet);
}

export async function getSheet(
  db: SQLiteDatabase,
  id: number,
): Promise<Sheet | null> {
  const row = await db.getFirstAsync<SheetRow>(
    'SELECT id, name, updated_at FROM sheets WHERE id = ?',
    id,
  );
  return row ? mapSheet(row) : null;
}

export async function createSheet(
  db: SQLiteDatabase,
  name: string,
): Promise<number> {
  const trimmedName = name.trim();
  if (!trimmedName) {
    throw new Error('Informe um nome para a ficha.');
  }

  const result = await db.runAsync(
    'INSERT INTO sheets (name, updated_at) VALUES (?, ?)',
    trimmedName,
    new Date().toISOString(),
  );
  return result.lastInsertRowId;
}

export async function duplicateSheet(
  db: SQLiteDatabase,
  sourceSheetId: number,
): Promise<number> {
  let duplicatedId = 0;
  const updatedAt = new Date().toISOString();

  await db.withExclusiveTransactionAsync(async (txn) => {
    const source = await txn.getFirstAsync<{ name: string }>(
      'SELECT name FROM sheets WHERE id = ?',
      sourceSheetId,
    );

    if (!source) {
      throw new Error('Ficha não encontrada.');
    }

    const result = await txn.runAsync(
      'INSERT INTO sheets (name, updated_at) VALUES (?, ?)',
      `${source.name} (cópia)`,
      updatedAt,
    );
    duplicatedId = result.lastInsertRowId;

    await txn.runAsync(COPY_SHEET_EXERCISES_SQL, duplicatedId, sourceSheetId);
  });

  return duplicatedId;
}

export async function deleteSheet(
  db: SQLiteDatabase,
  id: number,
): Promise<void> {
  await db.runAsync('DELETE FROM sheets WHERE id = ?', id);
}
