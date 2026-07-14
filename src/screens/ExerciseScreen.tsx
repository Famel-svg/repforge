import { useFocusEffect } from '@react-navigation/native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useSQLiteContext } from 'expo-sqlite';
import { useCallback, useState } from 'react';
import {
  Alert,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

import { AppButton } from '@/components/AppButton';
import { EmptyState } from '@/components/EmptyState';
import { ExerciseImage } from '@/components/ExerciseImage';
import { addEntry, deleteEntry, listEntries } from '@/db/entries';
import type { RootStackParamList } from '@/navigation/types';
import { colors, radius, spacing } from '@/theme';
import type { Entry } from '@/types';
import { formatDateTime, normalizeDecimal } from '@/utils/format';

type Props = NativeStackScreenProps<RootStackParamList, 'Exercise'>;

type SetData = {
  reps: string;
  weight: string;
};

export function ExerciseScreen({ route }: Props) {
  const db = useSQLiteContext();
  const { exerciseId, exerciseName, gifUrl } = route.params;
  const [entries, setEntries] = useState<Entry[]>([]);
  const [sets, setSets] = useState<SetData[]>([{ reps: '', weight: '' }]);
  const [saving, setSaving] = useState(false);
  const [expandedEntryId, setExpandedEntryId] = useState<number | null>(null);

  const load = useCallback(async () => {
    try {
      setEntries(await listEntries(db, exerciseId));
    } catch (error) {
      Alert.alert('Erro', error instanceof Error ? error.message : 'Falha ao carregar histórico.');
    }
  }, [db, exerciseId]);

  useFocusEffect(
    useCallback(() => {
      void load();
    }, [load]),
  );

  function addSet() {
    setSets((prev) => [...prev, { reps: '', weight: '' }]);
  }

  function removeSet(index: number) {
    if (sets.length <= 1) return;
    setSets((prev) => prev.filter((_, i) => i !== index));
  }

  function updateSet(index: number, field: keyof SetData, value: string) {
    setSets((prev) => prev.map((s, i) => (i === index ? { ...s, [field]: value } : s)));
  }

  async function handleSave() {
    const validSets = sets.filter(
      (s) => s.reps.trim() !== '' && s.weight.trim() !== '',
    );

    if (validSets.length === 0) {
      Alert.alert('Dados incompletos', 'Preencha pelo menos uma série com repetições e carga.');
      return;
    }

    for (const s of validSets) {
      const parsedReps = Number(s.reps);
      const parsedWeight = normalizeDecimal(s.weight);

      if (!Number.isInteger(parsedReps) || parsedReps <= 0) {
        Alert.alert('Repetições inválidas', 'Informe um número inteiro maior que zero.');
        return;
      }
      if (!Number.isFinite(parsedWeight) || parsedWeight < 0) {
        Alert.alert('Carga inválida', 'Informe uma carga igual ou maior que zero.');
        return;
      }
    }

    setSaving(true);
    try {
      for (const s of validSets) {
        await addEntry(db, exerciseId, {
          sets: validSets.length,
          reps: Number(s.reps),
          weightKg: normalizeDecimal(s.weight),
        });
      }
      setSets([{ reps: '', weight: '' }]);
      await load();
    } catch (error) {
      Alert.alert('Registro não salvo', error instanceof Error ? error.message : 'Tente novamente.');
    } finally {
      setSaving(false);
    }
  }

  function confirmDelete(entry: Entry) {
    Alert.alert(
      'Excluir registro?',
      `${entry.sets} x ${entry.reps} · ${entry.weightKg} kg`,
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Excluir',
          style: 'destructive',
          onPress: () => {
            void deleteEntry(db, entry.id).then(load);
          },
        },
      ],
    );
  }

  function toggleExpand(entryId: number) {
    setExpandedEntryId((prev) => (prev === entryId ? null : entryId));
  }


  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={styles.screen}
    >
      <ScrollView
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
      >
        <View style={styles.exerciseHeader}>
          <ExerciseImage size={112} uri={gifUrl} />
          <View style={styles.exerciseTitleBlock}>
            <Text style={styles.eyebrow}>EXERCÍCIO</Text>
            <Text style={styles.exerciseTitle}>{exerciseName}</Text>
          </View>
        </View>

        <View style={styles.form}>
          <Text style={styles.sectionTitle}>Novo registro</Text>
          {sets.map((set, index) => (
            <View key={index} style={styles.setRow}>
              <Text style={styles.setLabel}>Série {index + 1}</Text>
              <View style={styles.inputRow}>
                <View style={styles.inputGroup}>
                  <Text style={styles.inputLabel}>Reps</Text>
                  <TextInput
                    keyboardType="number-pad"
                    onChangeText={(v) => updateSet(index, 'reps', v)}
                    placeholder="0"
                    placeholderTextColor={colors.textMuted}
                    style={styles.input}
                    value={set.reps}
                  />
                </View>
                <View style={styles.inputGroup}>
                  <Text style={styles.inputLabel}>Carga kg</Text>
                  <TextInput
                    keyboardType="decimal-pad"
                    onChangeText={(v) => updateSet(index, 'weight', v)}
                    placeholder="0"
                    placeholderTextColor={colors.textMuted}
                    style={styles.input}
                    value={set.weight}
                  />
                </View>
                {sets.length > 1 && (
                  <Pressable hitSlop={8} onPress={() => removeSet(index)} style={styles.removeSetBtn}>
                    <Text style={styles.removeSetText}>X</Text>
                  </Pressable>
                )}
              </View>
            </View>
          ))}
          <Pressable onPress={addSet} style={styles.addSetBtn}>
            <Text style={styles.addSetText}>+ Adicionar série</Text>
          </Pressable>
          <AppButton
            label="Salvar registro"
            loading={saving}
            onPress={() => void handleSave()}
          />
        </View>

        <Text style={styles.sectionTitle}>Histórico</Text>
        {entries.length === 0 ? (
          <EmptyState
            description="A primeira carga salva aparecerá aqui."
            title="Nenhum registro"
          />
        ) : (
          <View style={styles.history}>
            {entries.map((entry) => {
              const isExpanded = expandedEntryId === entry.id;
              const isLatest = entries.indexOf(entry) === 0;
              return (
                <View key={entry.id} style={[styles.entry, isLatest && styles.entryLatest]}>
                  <Pressable
                    onPress={() => toggleExpand(entry.id)}
                    style={styles.entryHeader}
                  >
                    <View style={styles.entrySummary}>
                      <Text style={styles.entryWeight}>{entry.weightKg} kg</Text>
                      <Text style={styles.entryMeta}>
                        {entry.sets}x{entry.reps}
                      </Text>
                      {isLatest && <Text style={styles.latestBadge}>ÚLTIMA</Text>}
                    </View>
                    <View style={styles.entryActions}>
                      <Text style={styles.expandIcon}>{isExpanded ? '▼' : '▶'}</Text>
                      <Pressable hitSlop={12} onPress={() => confirmDelete(entry)}>
                        <Text style={styles.deleteText}>Excluir</Text>
                      </Pressable>
                    </View>
                  </Pressable>
                  {isExpanded && (
                    <View style={styles.entryExpanded}>
                      <View style={styles.expandedRow}>
                        <Text style={styles.expandedLabel}>Séries:</Text>
                        <Text style={styles.expandedValue}>{entry.sets}</Text>
                      </View>
                      <View style={styles.expandedRow}>
                        <Text style={styles.expandedLabel}>Repetições:</Text>
                        <Text style={styles.expandedValue}>{entry.reps}</Text>
                      </View>
                      <View style={styles.expandedRow}>
                        <Text style={styles.expandedLabel}>Carga:</Text>
                        <Text style={styles.expandedValue}>{entry.weightKg} kg</Text>
                      </View>
                      <View style={styles.expandedRow}>
                        <Text style={styles.expandedLabel}>Data:</Text>
                        <Text style={styles.expandedValue}>{formatDateTime(entry.recordedAt)}</Text>
                      </View>
                    </View>
                  )}
                </View>
              );
            })}
          </View>
        )}
      </ScrollView>
    </KeyboardAvoidingView>
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
    gap: spacing.lg,
  },
  exerciseHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
  },
  exerciseTitleBlock: {
    flex: 1,
    gap: spacing.xs,
  },
  eyebrow: {
    color: colors.primary,
    fontSize: 12,
    fontWeight: '900',
    letterSpacing: 1.2,
  },
  exerciseTitle: {
    color: colors.text,
    fontSize: 24,
    fontWeight: '900',
    textTransform: 'capitalize',
  },
  form: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.lg,
    padding: spacing.md,
    gap: spacing.md,
    backgroundColor: colors.surface,
  },
  sectionTitle: {
    color: colors.text,
    fontSize: 20,
    fontWeight: '900',
  },
  setRow: {
    gap: spacing.xs,
  },
  setLabel: {
    color: colors.textMuted,
    fontSize: 12,
    fontWeight: '700',
  },
  inputRow: {
    flexDirection: 'row',
    gap: spacing.sm,
    alignItems: 'center',
  },
  inputGroup: {
    flex: 1,
    gap: spacing.xs,
  },
  inputLabel: {
    color: colors.textMuted,
    fontSize: 12,
  },
  input: {
    minHeight: 50,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    paddingHorizontal: spacing.sm,
    color: colors.text,
    backgroundColor: colors.background,
    fontSize: 16,
    textAlign: 'center',
  },
  removeSetBtn: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: colors.danger,
    alignItems: 'center',
    justifyContent: 'center',
  },
  removeSetText: {
    color: '#fff',
    fontWeight: '900',
    fontSize: 14,
  },
  addSetBtn: {
    padding: spacing.sm,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    borderStyle: 'dashed',
  },
  addSetText: {
    color: colors.primary,
    fontWeight: '700',
  },
  history: {
    gap: spacing.sm,
  },
  entry: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    backgroundColor: colors.surface,
    overflow: 'hidden',
  },
  entryLatest: {
    borderColor: colors.primary,
  },
  entryHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: spacing.md,
  },
  entrySummary: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
  },
  entryWeight: {
    color: colors.success,
    fontSize: 20,
    fontWeight: '900',
  },
  entryMeta: {
    color: colors.text,
    fontWeight: '600',
  },
  latestBadge: {
    color: colors.primary,
    fontSize: 10,
    fontWeight: '900',
    backgroundColor: colors.primary + '20',
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 4,
    overflow: 'hidden',
  },
  entryActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
  },
  expandIcon: {
    color: colors.textMuted,
    fontSize: 12,
  },
  deleteText: {
    color: colors.danger,
    fontWeight: '700',
  },
  entryExpanded: {
    padding: spacing.md,
    paddingTop: 0,
    gap: spacing.xs,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: colors.border,
  },
  expandedRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  expandedLabel: {
    color: colors.textMuted,
    fontSize: 14,
  },
  expandedValue: {
    color: colors.text,
    fontSize: 14,
    fontWeight: '600',
  },
});

