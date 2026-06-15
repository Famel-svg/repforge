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

import { AppButton } from '@/components/AppButton';
import { EmptyState } from '@/components/EmptyState';
import { ExerciseImage } from '@/components/ExerciseImage';
import { deleteExercise, listExercises } from '@/db/exercises';
import { getSheet } from '@/db/sheets';
import type { RootStackParamList } from '@/navigation/types';
import { colors, radius, spacing } from '@/theme';
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
      {sheet ? (
        <Text style={styles.updated}>
          Última alteração: {formatDateTime(sheet.updatedAt)}
        </Text>
      ) : null}

      <FlatList
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
          <Pressable
            onPress={() =>
              navigation.navigate('Exercise', {
                exerciseId: item.id,
                exerciseName: item.name,
                gifUrl: item.gifUrl,
              })
            }
            style={({ pressed }) => [styles.card, pressed && styles.pressed]}
          >
            <ExerciseImage uri={item.gifUrl} />
            <View style={styles.cardBody}>
              <Text numberOfLines={2} style={styles.name}>
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
            <Pressable
              hitSlop={10}
              onPress={() => confirmDelete(item)}
              style={styles.remove}
            >
              <Text style={styles.removeText}>Remover</Text>
            </Pressable>
          </Pressable>
        )}
      />

      <View style={styles.footer}>
        <AppButton
          label="+ Adicionar exercício"
          onPress={() => navigation.navigate('SearchExercise', { sheetId })}
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background,
  },
  updated: {
    color: colors.textMuted,
    paddingHorizontal: spacing.md,
    paddingTop: spacing.md,
  },
  list: {
    padding: spacing.md,
    gap: spacing.sm,
  },
  emptyList: {
    flexGrow: 1,
    justifyContent: 'center',
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    padding: spacing.sm,
    backgroundColor: colors.surface,
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
    fontSize: 16,
    fontWeight: '800',
    textTransform: 'capitalize',
  },
  latest: {
    color: colors.success,
    fontWeight: '700',
  },
  noEntry: {
    color: colors.textMuted,
    fontSize: 13,
  },
  remove: {
    alignSelf: 'flex-start',
    padding: spacing.sm,
  },
  removeText: {
    color: colors.danger,
    fontSize: 12,
    fontWeight: '700',
  },
  footer: {
    padding: spacing.md,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: colors.border,
  },
});
