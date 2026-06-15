import * as DocumentPicker from 'expo-document-picker';
import { File, Paths } from 'expo-file-system';
import * as Sharing from 'expo-sharing';
import type { SQLiteDatabase } from 'expo-sqlite';

import {
  buildBackup,
  importBackupData,
  serializeBackup,
} from '@/utils/backupCore';

export async function exportBackupFile(db: SQLiteDatabase): Promise<void> {
  const backup = await buildBackup(db);
  const date = backup.exportedAt.slice(0, 10);
  const file = new File(Paths.cache, `repforge-backup-${date}.json`);
  file.write(serializeBackup(backup));

  if (!(await Sharing.isAvailableAsync())) {
    throw new Error('Compartilhamento não disponível neste dispositivo.');
  }

  await Sharing.shareAsync(file.uri, {
    mimeType: 'application/json',
    dialogTitle: 'Exportar backup do RepForge',
  });
}

export async function pickAndImportBackup(
  db: SQLiteDatabase,
): Promise<number | null> {
  const result = await DocumentPicker.getDocumentAsync({
    type: 'application/json',
    copyToCacheDirectory: true,
    multiple: false,
  });

  if (result.canceled) {
    return null;
  }

  const file = new File(result.assets[0].uri);
  let parsed: unknown;
  try {
    parsed = JSON.parse(await file.text());
  } catch {
    throw new Error('Arquivo JSON inválido ou corrompido.');
  }

  return importBackupData(db, parsed);
}
