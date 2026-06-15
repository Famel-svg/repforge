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

export function ExerciseScreen({ route }: Props) {
  const db = useSQLiteContext();
  const { exerciseId, exerciseName, gifUrl } = route.params;
  const [entries, setEntries] = useState<Entry[]>([]);
  const [sets, setSets] = useState('');
  const [reps, setReps] = useState('');
  const [weight, setWeight] = useState('');
  const [saving, setSaving] = useState(false);

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

  async function handleSave() {
    const parsedSets = Number(sets);
    const parsedReps = Number(reps);
    const parsedWeight = normalizeDecimal(weight);

    if (!Number.isInteger(parsedSets) || parsedSets <= 0) {
      Alert.alert('Séries inválidas', 'Informe um número inteiro maior que zero.');
      return;
    }
    if (!Number.isInteger(parsedReps) || parsedReps <= 0) {
      Alert.alert('Repetições inválidas', 'Informe um número inteiro maior que zero.');
      return;
    }
    if (!Number.isFinite(parsedWeight) || parsedWeight < 0) {
      Alert.alert('Carga inválida', 'Informe uma carga igual ou maior que zero.');
      return;
    }

    setSaving(true);
    try {
      await addEntry(db, exerciseId, {
        sets: parsedSets,
        reps: parsedReps,
        weightKg: parsedWeight,
      });
      setSets('');
      setReps('');
      setWeight('');
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
          <View style={styles.inputRow}>
            <LabeledInput
              label="Séries"
              onChangeText={setSets}
              value={sets}
            />
            <LabeledInput
              label="Repetições"
              onChangeText={setReps}
              value={reps}
            />
            <LabeledInput
              decimal
              label="Carga kg"
              onChangeText={setWeight}
              value={weight}
            />
          </View>
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
            {entries.map((entry) => (
              <View key={entry.id} style={styles.entry}>
                <View style={styles.entryBody}>
                  <Text style={styles.entryWeight}>{entry.weightKg} kg</Text>
                  <Text style={styles.entryMeta}>
                    {entry.sets} séries x {entry.reps} reps
                  </Text>
                  <Text style={styles.entryDate}>
                    {formatDateTime(entry.recordedAt)}
                  </Text>
                </View>
                <Pressable hitSlop={12} onPress={() => confirmDelete(entry)}>
                  <Text style={styles.deleteText}>Excluir</Text>
                </Pressable>
              </View>
            ))}
          </View>
        )}
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

type InputProps = {
  label: string;
  value: string;
  onChangeText: (value: string) => void;
  decimal?: boolean;
};

function LabeledInput({ label, value, onChangeText, decimal }: InputProps) {
  return (
    <View style={styles.inputGroup}>
      <Text style={styles.inputLabel}>{label}</Text>
      <TextInput
        keyboardType={decimal ? 'decimal-pad' : 'number-pad'}
        onChangeText={onChangeText}
        placeholder="0"
        placeholderTextColor={colors.textMuted}
        style={styles.input}
        value={value}
      />
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
  inputRow: {
    flexDirection: 'row',
    gap: spacing.sm,
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
  history: {
    gap: spacing.sm,
  },
  entry: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    padding: spacing.md,
    backgroundColor: colors.surface,
  },
  entryBody: {
    flex: 1,
    gap: 3,
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
  entryDate: {
    color: colors.textMuted,
    fontSize: 12,
  },
  deleteText: {
    color: colors.danger,
    fontWeight: '700',
  },
});
