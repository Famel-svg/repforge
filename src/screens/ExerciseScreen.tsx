import { useFocusEffect } from '@react-navigation/native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import * as Haptics from 'expo-haptics';
import { useSQLiteContext } from 'expo-sqlite';
import { useCallback, useEffect, useRef, useState } from 'react';
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
import { SafeAreaView } from 'react-native-safe-area-context';

import { AppButton } from '@/components/AppButton';
import { EmptyState } from '@/components/EmptyState';
import { ExerciseImage } from '@/components/ExerciseImage';
import { addEntry, deleteEntry, listEntries } from '@/db/entries';
import type { RootStackParamList } from '@/navigation/types';
import { colors, radius, spacing, touch, typography } from '@/theme';
import type { Entry } from '@/types';
import {
  calculateVolume,
  detectPersonalRecords,
  estimateOneRepMax,
} from '@/utils/calculations';
import { formatDateTime, normalizeDecimal } from '@/utils/format';
import {
  clampRestSeconds,
  DEFAULT_REST_SECONDS,
  formatRestTime,
  REST_STEP_SECONDS,
} from '@/utils/timer';

type Props = NativeStackScreenProps<RootStackParamList, 'Exercise'>;

type SetData = {
  reps: string;
  weight: string;
};

const REP_STEP = 1;
const WEIGHT_STEP = 2.5;

function formatLiftNumber(value: number): string {
  return Number.isInteger(value) ? String(value) : value.toFixed(1).replace('.', ',');
}

function formatInputNumber(value: number): string {
  return String(value).replace('.', ',');
}

function setsFromEntry(entry: Entry): SetData[] {
  return [
    {
      reps: String(entry.reps),
      weight: formatInputNumber(entry.weightKg),
    },
  ];
}

function emptySet(): SetData {
  return { reps: '', weight: '' };
}

function stepReps(current: string, delta: number): string {
  const parsed = Number(current);
  const base = Number.isFinite(parsed) && current.trim() !== '' ? parsed : 0;
  return String(Math.max(1, Math.round(base + delta)));
}

function stepWeight(current: string, delta: number): string {
  const parsed = normalizeDecimal(current);
  const base = Number.isFinite(parsed) && current.trim() !== '' ? parsed : 0;
  const next = Math.max(0, Math.round((base + delta) * 10) / 10);
  return formatInputNumber(next);
}

function ForgeStepper({
  label,
  value,
  onChangeText,
  onStep,
  keyboardType,
}: {
  label: string;
  value: string;
  onChangeText: (value: string) => void;
  onStep: (delta: number) => void;
  keyboardType: 'number-pad' | 'decimal-pad';
}) {
  return (
    <View style={styles.stepper}>
      <Text style={styles.stepperLabel}>{label}</Text>
      <View style={styles.stepperRow}>
        <Pressable
          accessibilityLabel={`Diminuir ${label}`}
          accessibilityRole="button"
          hitSlop={4}
          onPress={() => {
            void Haptics.selectionAsync();
            onStep(-1);
          }}
          style={({ pressed }) => [styles.stepBtn, pressed && styles.pressed]}
        >
          <Text style={styles.stepBtnText}>−</Text>
        </Pressable>
        <TextInput
          keyboardType={keyboardType}
          onChangeText={onChangeText}
          placeholder="0"
          placeholderTextColor={colors.textMuted}
          selectTextOnFocus
          style={styles.stepperInput}
          value={value}
        />
        <Pressable
          accessibilityLabel={`Aumentar ${label}`}
          accessibilityRole="button"
          hitSlop={4}
          onPress={() => {
            void Haptics.selectionAsync();
            onStep(1);
          }}
          style={({ pressed }) => [styles.stepBtn, pressed && styles.pressed]}
        >
          <Text style={styles.stepBtnText}>+</Text>
        </Pressable>
      </View>
    </View>
  );
}

export function ExerciseScreen({ route }: Props) {
  const db = useSQLiteContext();
  const { exerciseId, exerciseName, gifUrl } = route.params;
  const [entries, setEntries] = useState<Entry[]>([]);
  const [sets, setSets] = useState<SetData[]>([emptySet()]);
  const [saving, setSaving] = useState(false);
  const [historyOpen, setHistoryOpen] = useState(false);
  const [expandedEntryId, setExpandedEntryId] = useState<number | null>(null);
  const [restDurationSeconds, setRestDurationSeconds] = useState(DEFAULT_REST_SECONDS);
  const [restRemainingSeconds, setRestRemainingSeconds] = useState(0);
  const didPrefill = useRef(false);

  const restTimerActive = restRemainingSeconds > 0;
  const latestEntry = entries[0] ?? null;

  useEffect(() => {
    if (!restTimerActive) {
      return undefined;
    }

    const interval = setInterval(() => {
      setRestRemainingSeconds((seconds) => {
        const next = Math.max(0, seconds - 1);
        if (next === 0 && seconds > 0) {
          void Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
        }
        return next;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [restTimerActive]);

  const load = useCallback(async () => {
    try {
      const nextEntries = await listEntries(db, exerciseId);
      setEntries(nextEntries);

      if (!didPrefill.current && nextEntries[0]) {
        didPrefill.current = true;
        setSets(setsFromEntry(nextEntries[0]));
      }
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
    setSets((prev) => {
      const last = prev[prev.length - 1];
      return [
        ...prev,
        last ? { reps: last.reps, weight: last.weight } : emptySet(),
      ];
    });
  }

  function removeSet(index: number) {
    if (sets.length <= 1) return;
    setSets((prev) => prev.filter((_, i) => i !== index));
  }

  function updateSet(index: number, field: keyof SetData, value: string) {
    setSets((prev) => prev.map((s, i) => (i === index ? { ...s, [field]: value } : s)));
  }

  function changeRestSeconds(delta: number) {
    void Haptics.selectionAsync();
    setRestDurationSeconds((seconds) => clampRestSeconds(seconds + delta));
    setRestRemainingSeconds((seconds) =>
      seconds > 0 ? clampRestSeconds(seconds + delta) : seconds,
    );
  }

  function startRestTimer() {
    void Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
    setRestRemainingSeconds(restDurationSeconds);
  }

  function stopRestTimer() {
    void Haptics.selectionAsync();
    setRestRemainingSeconds(0);
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
      const lastValid = validSets[validSets.length - 1];
      setSets([
        {
          reps: lastValid.reps,
          weight: lastValid.weight,
        },
      ]);
      await Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
      startRestTimer();
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
      `${entry.sets} x ${entry.reps} · ${formatLiftNumber(entry.weightKg)} kg`,
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
      keyboardVerticalOffset={Platform.OS === 'ios' ? 88 : 0}
      style={styles.screen}
    >
      {restTimerActive ? (
        <View style={styles.restHero}>
          <Text style={styles.restHeroLabel}>DESCANSO</Text>
          <Text style={styles.restHeroTime}>{formatRestTime(restRemainingSeconds)}</Text>
          <View style={styles.restHeroActions}>
            <Pressable
              accessibilityLabel="Diminuir descanso em 15 segundos"
              accessibilityRole="button"
              onPress={() => changeRestSeconds(-REST_STEP_SECONDS)}
              style={({ pressed }) => [styles.restChip, pressed && styles.pressed]}
            >
              <Text style={styles.restChipText}>−15s</Text>
            </Pressable>
            <Pressable
              accessibilityLabel="Aumentar descanso em 30 segundos"
              accessibilityRole="button"
              onPress={() => changeRestSeconds(REST_STEP_SECONDS * 2)}
              style={({ pressed }) => [styles.restChip, pressed && styles.pressed]}
            >
              <Text style={styles.restChipText}>+30s</Text>
            </Pressable>
            <Pressable
              accessibilityLabel="Pular descanso"
              accessibilityRole="button"
              onPress={stopRestTimer}
              style={({ pressed }) => [
                styles.restChip,
                styles.restChipPrimary,
                pressed && styles.pressed,
              ]}
            >
              <Text style={styles.restChipPrimaryText}>Pular</Text>
            </Pressable>
          </View>
        </View>
      ) : null}

      <ScrollView
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
      >
        <View style={styles.exerciseHeader}>
          <ExerciseImage size={72} uri={gifUrl} />
          <View style={styles.exerciseTitleBlock}>
            <Text style={styles.eyebrow}>FORGE</Text>
            <Text numberOfLines={2} style={styles.exerciseTitle}>
              {exerciseName}
            </Text>
            {latestEntry ? (
              <Text style={styles.lastLine}>
                Última · {latestEntry.sets}x{latestEntry.reps} ·{' '}
                {formatLiftNumber(latestEntry.weightKg)} kg
              </Text>
            ) : (
              <Text style={styles.lastLine}>Primeiro registro deste exercício</Text>
            )}
          </View>
        </View>

        <View style={styles.form}>
          {!restTimerActive ? (
            <View style={styles.restSetup}>
              <View>
                <Text style={styles.restSetupLabel}>Descanso</Text>
                <Text style={styles.restSetupHint}>Após salvar a série</Text>
              </View>
              <View style={styles.restSetupControls}>
                <Pressable
                  accessibilityLabel="Diminuir descanso"
                  accessibilityRole="button"
                  onPress={() => changeRestSeconds(-REST_STEP_SECONDS)}
                  style={({ pressed }) => [styles.restMiniBtn, pressed && styles.pressed]}
                >
                  <Text style={styles.restMiniBtnText}>−</Text>
                </Pressable>
                <Pressable
                  accessibilityLabel="Iniciar descanso"
                  accessibilityRole="button"
                  onPress={startRestTimer}
                >
                  <Text style={styles.restSetupTime}>
                    {formatRestTime(restDurationSeconds)}
                  </Text>
                </Pressable>
                <Pressable
                  accessibilityLabel="Aumentar descanso"
                  accessibilityRole="button"
                  onPress={() => changeRestSeconds(REST_STEP_SECONDS)}
                  style={({ pressed }) => [styles.restMiniBtn, pressed && styles.pressed]}
                >
                  <Text style={styles.restMiniBtnText}>+</Text>
                </Pressable>
              </View>
            </View>
          ) : null}

          {sets.map((set, index) => (
            <View key={index} style={styles.setCard}>
              <View style={styles.setHeader}>
                <Text style={styles.setLabel}>Série {index + 1}</Text>
                {sets.length > 1 ? (
                  <Pressable
                    hitSlop={12}
                    onPress={() => removeSet(index)}
                    style={styles.removeSetHit}
                  >
                    <Text style={styles.removeSetText}>Remover</Text>
                  </Pressable>
                ) : null}
              </View>
              <View style={styles.steppers}>
                <ForgeStepper
                  keyboardType="number-pad"
                  label="Reps"
                  onChangeText={(v) => updateSet(index, 'reps', v)}
                  onStep={(dir) =>
                    updateSet(index, 'reps', stepReps(set.reps, dir * REP_STEP))
                  }
                  value={set.reps}
                />
                <ForgeStepper
                  keyboardType="decimal-pad"
                  label="Carga kg"
                  onChangeText={(v) => updateSet(index, 'weight', v.replace(/\./g, ','))}
                  onStep={(dir) =>
                    updateSet(index, 'weight', stepWeight(set.weight, dir * WEIGHT_STEP))
                  }
                  value={set.weight}
                />
              </View>
            </View>
          ))}

          <Pressable
            accessibilityLabel="Adicionar série"
            accessibilityRole="button"
            onPress={addSet}
            style={({ pressed }) => [styles.addSetBtn, pressed && styles.pressed]}
          >
            <Text style={styles.addSetText}>+ Adicionar série</Text>
          </Pressable>
        </View>

        <Pressable
          accessibilityLabel={historyOpen ? 'Fechar histórico' : 'Abrir histórico'}
          accessibilityRole="button"
          onPress={() => setHistoryOpen((open) => !open)}
          style={({ pressed }) => [styles.historyToggle, pressed && styles.pressed]}
        >
          <Text style={styles.historyToggleText}>
            Histórico {entries.length > 0 ? `(${entries.length})` : ''}
          </Text>
          <Text style={styles.historyChevron}>{historyOpen ? '▼' : '▶'}</Text>
        </Pressable>

        {historyOpen ? (
          entries.length === 0 ? (
            <EmptyState
              description="A primeira carga salva aparecerá aqui."
              title="Nenhum registro"
            />
          ) : (
            <View style={styles.history}>
              {entries.map((entry, index) => {
                const isExpanded = expandedEntryId === entry.id;
                const isLatest = index === 0;
                const personalRecords = detectPersonalRecords(
                  entry,
                  entries.slice(index + 1),
                );
                return (
                  <View key={entry.id} style={[styles.entry, isLatest && styles.entryLatest]}>
                    <Pressable
                      onPress={() => toggleExpand(entry.id)}
                      style={styles.entryHeader}
                    >
                      <View style={styles.entrySummary}>
                        <Text style={styles.entryWeight}>
                          {formatLiftNumber(entry.weightKg)} kg
                        </Text>
                        <Text style={styles.entryMeta}>
                          {entry.sets}x{entry.reps}
                        </Text>
                        {isLatest ? <Text style={styles.latestBadge}>ÚLTIMA</Text> : null}
                        {personalRecords.weight ? (
                          <Text style={styles.prBadge}>PR CARGA</Text>
                        ) : null}
                        {personalRecords.oneRepMax ? (
                          <Text style={styles.prBadge}>PR 1RM</Text>
                        ) : null}
                      </View>
                      <View style={styles.entryActions}>
                        <Text style={styles.expandIcon}>{isExpanded ? '▼' : '▶'}</Text>
                        <Pressable
                          hitSlop={12}
                          onPress={(event) => {
                            event.stopPropagation();
                            confirmDelete(entry);
                          }}
                        >
                          <Text style={styles.deleteText}>Excluir</Text>
                        </Pressable>
                      </View>
                    </Pressable>
                    {isExpanded ? (
                      <View style={styles.entryExpanded}>
                        <View style={styles.expandedRow}>
                          <Text style={styles.expandedLabel}>Volume</Text>
                          <Text style={styles.expandedValue}>
                            {formatLiftNumber(calculateVolume(entry))} kg
                          </Text>
                        </View>
                        <View style={styles.expandedRow}>
                          <Text style={styles.expandedLabel}>1RM estimado</Text>
                          <Text style={styles.expandedValue}>
                            {formatLiftNumber(estimateOneRepMax(entry))} kg
                          </Text>
                        </View>
                        <View style={styles.expandedRow}>
                          <Text style={styles.expandedLabel}>Data</Text>
                          <Text style={styles.expandedValue}>
                            {formatDateTime(entry.recordedAt)}
                          </Text>
                        </View>
                      </View>
                    ) : null}
                  </View>
                );
              })}
            </View>
          )
        ) : null}
      </ScrollView>

      <SafeAreaView edges={['bottom']} style={styles.footer}>
        <AppButton
          gym
          label="Registrar série"
          loading={saving}
          onPress={() => void handleSave()}
        />
      </SafeAreaView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background,
  },
  restHero: {
    alignItems: 'center',
    paddingVertical: spacing.md,
    paddingHorizontal: spacing.md,
    gap: spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: colors.primary,
    backgroundColor: colors.surface,
  },
  restHeroLabel: {
    color: colors.primary,
    fontSize: typography.label,
    fontWeight: '900',
    letterSpacing: 2,
  },
  restHeroTime: {
    color: colors.text,
    fontSize: typography.display,
    fontWeight: '900',
    fontVariant: ['tabular-nums'],
    lineHeight: 52,
  },
  restHeroActions: {
    flexDirection: 'row',
    gap: spacing.sm,
    width: '100%',
  },
  restChip: {
    flex: 1,
    minHeight: touch.min,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radius.md,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.surfaceRaised,
  },
  restChipPrimary: {
    borderColor: colors.primary,
    backgroundColor: colors.primary,
  },
  restChipText: {
    color: colors.text,
    fontWeight: '800',
  },
  restChipPrimaryText: {
    color: '#FFFFFF',
    fontWeight: '900',
  },
  content: {
    padding: spacing.md,
    paddingBottom: touch.gym + spacing.xl + spacing.md * 2,
    gap: spacing.md,
  },
  exerciseHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
  },
  exerciseTitleBlock: {
    flex: 1,
    gap: 4,
  },
  eyebrow: {
    color: colors.primary,
    fontSize: typography.label,
    fontWeight: '900',
    letterSpacing: 1.4,
  },
  exerciseTitle: {
    color: colors.text,
    fontSize: typography.heading,
    fontWeight: '900',
    textTransform: 'capitalize',
  },
  lastLine: {
    color: colors.textMuted,
    fontSize: typography.caption,
    fontWeight: '600',
  },
  form: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.lg,
    padding: spacing.md,
    gap: spacing.md,
    backgroundColor: colors.surface,
  },
  restSetup: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: spacing.md,
  },
  restSetupLabel: {
    color: colors.text,
    fontSize: typography.body,
    fontWeight: '900',
  },
  restSetupHint: {
    color: colors.textMuted,
    fontSize: typography.label,
    marginTop: 2,
  },
  restSetupControls: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
  },
  restMiniBtn: {
    width: touch.min,
    height: touch.min,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radius.md,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.surfaceRaised,
  },
  restMiniBtnText: {
    color: colors.text,
    fontSize: 22,
    fontWeight: '900',
  },
  restSetupTime: {
    minWidth: 64,
    textAlign: 'center',
    color: colors.primary,
    fontSize: 22,
    fontWeight: '900',
    fontVariant: ['tabular-nums'],
  },
  setCard: {
    gap: spacing.sm,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    padding: spacing.md,
    backgroundColor: colors.background,
  },
  setHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  setLabel: {
    color: colors.textMuted,
    fontSize: typography.label,
    fontWeight: '800',
  },
  removeSetHit: {
    minHeight: touch.min,
    justifyContent: 'center',
    paddingHorizontal: spacing.xs,
  },
  removeSetText: {
    color: colors.danger,
    fontWeight: '700',
    fontSize: typography.caption,
  },
  steppers: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
  stepper: {
    flexGrow: 1,
    flexBasis: 190,
    gap: spacing.xs,
  },
  stepperLabel: {
    color: colors.textMuted,
    fontSize: typography.label,
    fontWeight: '700',
    textAlign: 'center',
  },
  stepperRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.xs,
  },
  stepBtn: {
    width: touch.gym,
    height: touch.gym,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radius.md,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.surfaceRaised,
  },
  stepBtnText: {
    color: colors.text,
    fontSize: 28,
    fontWeight: '900',
    lineHeight: 30,
  },
  stepperInput: {
    flex: 1,
    minWidth: 64,
    minHeight: touch.gym,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    paddingHorizontal: spacing.xs,
    color: colors.text,
    backgroundColor: colors.surface,
    fontSize: typography.monoLg,
    fontWeight: '900',
    textAlign: 'center',
    fontVariant: ['tabular-nums'],
  },
  addSetBtn: {
    minHeight: touch.min,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    borderStyle: 'dashed',
  },
  addSetText: {
    color: colors.primary,
    fontWeight: '800',
  },
  historyToggle: {
    minHeight: touch.min,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: spacing.sm,
  },
  historyToggleText: {
    color: colors.text,
    fontSize: typography.heading,
    fontWeight: '900',
  },
  historyChevron: {
    color: colors.textMuted,
    fontSize: 14,
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
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
  entryWeight: {
    color: colors.success,
    fontSize: typography.heading,
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
  prBadge: {
    color: colors.success,
    fontSize: 10,
    fontWeight: '900',
    backgroundColor: colors.success + '20',
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
  footer: {
    padding: spacing.md,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: colors.border,
    backgroundColor: colors.background,
  },
  pressed: {
    opacity: 0.75,
  },
});
