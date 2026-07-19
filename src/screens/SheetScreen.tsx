import { useFocusEffect } from '@react-navigation/native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useSQLiteContext } from 'expo-sqlite';
import { useCallback, useState } from 'react';
import {
  Alert,
  FlatList,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AppButton } from '@/components/AppButton';
import { EmptyState } from '@/components/EmptyState';
import { ExerciseImage } from '@/components/ExerciseImage';
import { deleteExercise, listExercises } from '@/db/exercises';
import { getSheet } from '@/db/sheets';
import type { RootStackParamList } from '@/navigation/types';
import { colors, radius, spacing, touch } from '@/theme';
import type { Exercise, Sheet } from '@/types';
import { formatDateTime } from '@/utils/format';

type Props = NativeStackScreenProps<RootStackParamList, 'Sheet'>;

export function SheetScreen({ navigation, route }: Props) {
  const db = useSQLiteContext();
  const { sheetId } = route.params;
  const [sheet, setSheet] = useState<Sheet | null>(null);
  const [exercises, setExercises] = useState<Exercise[]>([]);

  const load = useCallback(async () => {
    try {
      const [nextSheet, nextExercises] = await Promise.all([
        getSheet(db, sheetId),
        listExercises(db, sheetId),
      ]);
      setSheet(nextSheet);
      setExercises(nextExercises);
      if (nextSheet) {
        navigation.setOptions({ title: nextSheet.name });
      }
    } catch (error) {
      Alert.alert('Erro', error instanceof Error ? error.message : 'Falha ao carregar ficha.');
    }
  }, [db, navigation, sheetId]);

  useFocusEffect(
    useCallback(() => {
      void load();
    }, [load]),
  );

  function confirmDelete(exercise: Exercise) {
    Alert.alert('Remover exercício?', exercise.name, [
      { text: 'Cancelar', style: 'cancel' },
      {
        text: 'Remover',
        style: 'destructive',
        onPress: () => {
          void deleteExercise(db, exercise.id).then(load);
        },
      },
    ]);
  }

  return (
    <View style={styles.screen}>
      <View style={styles.summary}>
        <Text style={styles.eyebrow}>FICHA</Text>
        <Text style={styles.summaryTitle}>
          {exercises.length} {exercises.length === 1 ? 'exercício' : 'exercícios'}
        </Text>
        {sheet ? (
          <Text style={styles.updated}>
            Última alteração: {formatDateTime(sheet.updatedAt)}
          </Text>
        ) : null}
      </View>

      <FlatList
        style={styles.listFrame}
        contentContainerStyle={
          exercises.length === 0 ? styles.emptyList : styles.list
        }
        data={exercises}
        keyExtractor={(item) => String(item.id)}
        ListEmptyComponent={
          <EmptyState
            description="Busque um movimento na WorkoutX e salve nesta ficha."
            title="Ficha sem exercícios"
          />
        }
        renderItem={({ item }) => (
          <View style={styles.card}>
            <Pressable
              accessibilityRole="button"
              onPress={() =>
                navigation.navigate('Exercise', {
                  exerciseId: item.id,
                  exerciseName: item.name,
                  gifUrl: item.gifUrl,
                })
              }
              style={({ pressed }) => [styles.cardOpen, pressed && styles.pressed]}
            >
              <ExerciseImage size={56} uri={item.gifUrl} />
              <View style={styles.cardBody}>
                <Text numberOfLines={1} style={styles.name}>
                  {item.name}
                </Text>
                {item.latestEntry ? (
                  <Text style={styles.latest}>
                    {item.latestEntry.sets} x {item.latestEntry.reps} ·{' '}
                    {item.latestEntry.weightKg} kg
                  </Text>
                ) : (
                  <Text style={styles.noEntry}>Sem carga registrada</Text>
                )}
              </View>
            </Pressable>
            <Pressable
              accessibilityLabel={`Remover ${item.name}`}
              accessibilityRole="button"
              onPress={() => confirmDelete(item)}
              style={({ pressed }) => [styles.remove, pressed && styles.removePressed]}
            >
              <Text style={styles.removeText}>×</Text>
            </Pressable>
          </View>
        )}
      />

      <SafeAreaView edges={['bottom']} style={styles.footer}>
        <AppButton
          label="+ Adicionar exercício"
          onPress={() => navigation.navigate('SearchExercise', { sheetId })}
        />
      </SafeAreaView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background,
  },
  summary: {
    paddingHorizontal: spacing.md,
    paddingTop: spacing.md,
    paddingBottom: spacing.sm,
    gap: spacing.xs,
  },
  eyebrow: {
    color: colors.primary,
    fontSize: 12,
    fontWeight: '900',
    letterSpacing: 1.4,
  },
  summaryTitle: {
    color: colors.text,
    fontSize: 22,
    fontWeight: '900',
  },
  updated: {
    color: colors.textMuted,
    fontSize: 12,
    lineHeight: 16,
  },
  listFrame: {
    flex: 1,
  },
  list: {
    paddingHorizontal: spacing.md,
    paddingTop: spacing.sm,
    paddingBottom: spacing.md,
    gap: spacing.sm,
  },
  emptyList: {
    flexGrow: 1,
    justifyContent: 'center',
    padding: spacing.md,
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    backgroundColor: colors.surface,
    overflow: 'hidden',
  },
  cardOpen: {
    flex: 1,
    minHeight: 72,
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    padding: spacing.sm,
  },
  pressed: {
    opacity: 0.76,
  },
  cardBody: {
    flex: 1,
    gap: spacing.sm,
  },
  name: {
    color: colors.text,
    fontSize: 15,
    fontWeight: '800',
    textTransform: 'capitalize',
  },
  latest: {
    color: colors.success,
    fontSize: 13,
    fontWeight: '700',
  },
  noEntry: {
    color: colors.textMuted,
    fontSize: 13,
  },
  remove: {
    minHeight: touch.min,
    minWidth: touch.min,
    alignItems: 'center',
    justifyContent: 'center',
  },
  removePressed: {
    backgroundColor: colors.danger + '14',
  },
  removeText: {
    color: colors.danger,
    fontSize: 24,
    lineHeight: 26,
    fontWeight: '500',
  },
  footer: {
    padding: spacing.md,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: colors.border,
  },
});
