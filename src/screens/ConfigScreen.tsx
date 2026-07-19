import type { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import { useFocusEffect } from '@react-navigation/native';
import type { CompositeScreenProps } from '@react-navigation/native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useSQLiteContext } from 'expo-sqlite';
import { useCallback, useState } from 'react';
import {
  Alert,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

import { AppButton } from '@/components/AppButton';
import {
  clearWorkoutXKey,
  getWorkoutXKey,
  setWorkoutXKey,
} from '@/db/settings';
import { listSheets } from '@/db/sheets';
import type { MainTabParamList, RootStackParamList } from '@/navigation/types';
import { colors, radius, spacing } from '@/theme';
import { exportBackupFile, pickAndImportBackup } from '@/utils/backupFile';

type Props = CompositeScreenProps<
  BottomTabScreenProps<MainTabParamList, 'Config'>,
  NativeStackScreenProps<RootStackParamList>
>;

export function ConfigScreen(_props: Props) {
  const db = useSQLiteContext();
  const [sheetCount, setSheetCount] = useState(0);
  const [backupAction, setBackupAction] = useState<'import' | 'export' | null>(null);
  const [workoutXKey, setWorkoutXKeyInput] = useState('');
  const [hasWorkoutXKey, setHasWorkoutXKey] = useState(false);
  const [savingKey, setSavingKey] = useState(false);
  const backupBusy = backupAction !== null;

  const load = useCallback(async () => {
    try {
      const [sheets, savedKey] = await Promise.all([
        listSheets(db),
        getWorkoutXKey(db),
      ]);
      setSheetCount(sheets.length);
      setHasWorkoutXKey(savedKey !== null);
      setWorkoutXKeyInput(savedKey ?? '');
    } catch {
      setSheetCount(0);
    }
  }, [db]);

  useFocusEffect(
    useCallback(() => {
      void load();
    }, [load]),
  );

  async function handleExport() {
    setBackupAction('export');
    try {
      await exportBackupFile(db);
    } catch (error) {
      Alert.alert(
        'Exportação falhou',
        error instanceof Error ? error.message : 'Tente novamente.',
      );
    } finally {
      setBackupAction(null);
    }
  }

  async function handleImport() {
    setBackupAction('import');
    try {
      const count = await pickAndImportBackup(db);
      if (count !== null) {
        await load();
        Alert.alert('Backup importado', `${count} planilha(s) adicionada(s).`);
      }
    } catch (error) {
      Alert.alert(
        'Importação falhou',
        error instanceof Error ? error.message : 'Banco não alterado.',
      );
    } finally {
      setBackupAction(null);
    }
  }

  async function handleSaveWorkoutXKey() {
    const key = workoutXKey.trim();
    if (!key.startsWith('wx_')) {
      Alert.alert('Chave inválida', 'A chave WorkoutX deve começar com wx_.');
      return;
    }

    setSavingKey(true);
    try {
      await setWorkoutXKey(db, key);
      setHasWorkoutXKey(true);
      Alert.alert('Chave salva', 'Busca de exercícios liberada neste aparelho.');
    } catch (error) {
      Alert.alert(
        'Chave não salva',
        error instanceof Error ? error.message : 'Tente novamente.',
      );
    } finally {
      setSavingKey(false);
    }
  }

  async function handleClearWorkoutXKey() {
    setSavingKey(true);
    try {
      await clearWorkoutXKey(db);
      setWorkoutXKeyInput('');
      setHasWorkoutXKey(false);
    } catch (error) {
      Alert.alert(
        'Chave não removida',
        error instanceof Error ? error.message : 'Tente novamente.',
      );
    } finally {
      setSavingKey(false);
    }
  }

  return (
    <View style={styles.screen}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.hero}>
          <Text style={styles.eyebrow}>CONFIG</Text>
          <Text style={styles.title}>Dados e backup.</Text>
          <Text style={styles.text}>
            Importe e exporte suas planilhas em JSON. Bom lugar para trocar de
            aparelho ou guardar uma cópia local.
          </Text>
        </View>

        <View style={styles.card}>
          <View style={styles.cardHeader}>
            <View>
              <Text style={styles.cardTitle}>WorkoutX API</Text>
              <Text style={styles.cardText}>
                {hasWorkoutXKey
                  ? 'Chave configurada neste aparelho'
                  : 'Cole sua chave wx_ para buscar exercícios'}
              </Text>
            </View>
          </View>

          <TextInput
            autoCapitalize="none"
            autoCorrect={false}
            onChangeText={setWorkoutXKeyInput}
            placeholder="wx_sua_chave"
            placeholderTextColor={colors.textMuted}
            secureTextEntry
            style={styles.input}
            value={workoutXKey}
          />

          <View style={styles.actionStack}>
            <AppButton
              disabled={!workoutXKey.trim() || savingKey}
              label="Salvar chave"
              loading={savingKey}
              onPress={() => void handleSaveWorkoutXKey()}
            />
            {hasWorkoutXKey ? (
              <AppButton
                disabled={savingKey}
                label="Remover chave"
                onPress={() => void handleClearWorkoutXKey()}
                variant="secondary"
              />
            ) : null}
          </View>
        </View>

        <View style={styles.card}>
          <View style={styles.cardHeader}>
            <View>
              <Text style={styles.cardTitle}>Backup local</Text>
              <Text style={styles.cardText}>
                {sheetCount} {sheetCount === 1 ? 'planilha salva' : 'planilhas salvas'}
              </Text>
            </View>
          </View>

          <View style={styles.actionStack}>
            <AppButton
              disabled={backupBusy}
              label="Importar backup"
              loading={backupAction === 'import'}
              onPress={() => void handleImport()}
              variant="secondary"
            />
            <AppButton
              disabled={backupBusy}
              label="Exportar backup"
              loading={backupAction === 'export'}
              onPress={() => void handleExport()}
            />
          </View>
        </View>

        <View style={styles.note}>
          <Text style={styles.noteTitle}>Formato</Text>
          <Text style={styles.noteText}>
            O arquivo exportado contém planilhas, exercícios e histórico de
            cargas. A importação adiciona dados, sem apagar seu banco atual.
          </Text>
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background,
  },
  content: {
    padding: spacing.md,
    paddingBottom: spacing.xl,
    gap: spacing.md,
  },
  hero: {
    gap: spacing.sm,
  },
  eyebrow: {
    color: colors.primary,
    fontSize: 12,
    fontWeight: '900',
    letterSpacing: 1.4,
  },
  title: {
    color: colors.text,
    fontSize: 28,
    fontWeight: '900',
  },
  text: {
    color: colors.textMuted,
    lineHeight: 21,
  },
  card: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.lg,
    padding: spacing.md,
    backgroundColor: colors.surface,
    gap: spacing.md,
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  cardTitle: {
    color: colors.text,
    fontSize: 20,
    fontWeight: '900',
  },
  cardText: {
    color: colors.textMuted,
    marginTop: spacing.xs,
  },
  input: {
    minHeight: 52,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    paddingHorizontal: spacing.md,
    color: colors.text,
    backgroundColor: colors.background,
    fontSize: 16,
  },
  actionStack: {
    gap: spacing.sm,
  },
  note: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    padding: spacing.md,
    backgroundColor: colors.surfaceRaised,
    gap: spacing.xs,
  },
  noteTitle: {
    color: colors.text,
    fontWeight: '900',
  },
  noteText: {
    color: colors.textMuted,
    lineHeight: 20,
  },
});
