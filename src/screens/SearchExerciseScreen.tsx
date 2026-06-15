import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useSQLiteContext } from 'expo-sqlite';
import { useState } from 'react';
import {
  Alert,
  FlatList,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

import { AppButton } from '@/components/AppButton';
import { EmptyState } from '@/components/EmptyState';
import { ExerciseImage } from '@/components/ExerciseImage';
import { SelectField } from '@/components/SelectField';
import { addExercise } from '@/db/exercises';
import type { RootStackParamList } from '@/navigation/types';
import {
  BODY_PARTS,
  EQUIPMENT,
  searchExercises,
} from '@/services/workoutx';
import { colors, radius, spacing } from '@/theme';
import type { WorkoutXExercise } from '@/types';

type Props = NativeStackScreenProps<RootStackParamList, 'SearchExercise'>;

export function SearchExerciseScreen({ navigation, route }: Props) {
  const db = useSQLiteContext();
  const [name, setName] = useState('');
  const [bodyPart, setBodyPart] = useState('');
  const [equipment, setEquipment] = useState('');
  const [results, setResults] = useState<WorkoutXExercise[]>([]);
  const [searched, setSearched] = useState(false);
  const [loading, setLoading] = useState(false);
  const [addingId, setAddingId] = useState<string | null>(null);

  async function handleSearch() {
    setLoading(true);
    try {
      const nextResults = await searchExercises({
        name,
        bodyPart,
        equipment,
        limit: 20,
      });
      setResults(nextResults);
      setSearched(true);
    } catch (error) {
      Alert.alert('Busca indisponível', error instanceof Error ? error.message : 'Tente novamente.');
    } finally {
      setLoading(false);
    }
  }

  async function handleAdd(exercise: WorkoutXExercise) {
    setAddingId(exercise.id);
    try {
      await addExercise(db, route.params.sheetId, exercise);
      navigation.goBack();
    } catch (error) {
      Alert.alert('Exercício não adicionado', error instanceof Error ? error.message : 'Tente novamente.');
      setAddingId(null);
    }
  }

  return (
    <View style={styles.screen}>
      <View style={styles.filters}>
        <TextInput
          autoCapitalize="none"
          onChangeText={setName}
          onSubmitEditing={() => void handleSearch()}
          placeholder="Nome do exercício"
          placeholderTextColor={colors.textMuted}
          returnKeyType="search"
          style={styles.searchInput}
          value={name}
        />
        <View style={styles.selectRow}>
          <SelectField
            label="Parte do corpo"
            onChange={setBodyPart}
            options={BODY_PARTS}
            value={bodyPart}
          />
          <SelectField
            label="Equipamento"
            onChange={setEquipment}
            options={EQUIPMENT}
            value={equipment}
          />
        </View>
        <AppButton
          label="Buscar na WorkoutX"
          loading={loading}
          onPress={() => void handleSearch()}
        />
      </View>

      <FlatList
        contentContainerStyle={
          results.length === 0 ? styles.emptyList : styles.list
        }
        data={results}
        keyExtractor={(item) => item.id}
        keyboardShouldPersistTaps="handled"
        ListEmptyComponent={
          <EmptyState
            description={
              searched
                ? 'Ajuste os filtros e tente outra busca.'
                : 'Use nome ou filtros para encontrar até 20 movimentos.'
            }
            title={searched ? 'Nenhum resultado' : 'Busque um exercício'}
          />
        }
        renderItem={({ item }) => (
          <View style={styles.card}>
            <ExerciseImage uri={item.gifUrl} />
            <View style={styles.cardBody}>
              <Text numberOfLines={2} style={styles.name}>
                {item.name}
              </Text>
              <Text style={styles.meta}>
                {[item.target, item.equipment].filter(Boolean).join(' · ')}
              </Text>
              <AppButton
                compact
                disabled={addingId !== null}
                label="Adicionar"
                loading={addingId === item.id}
                onPress={() => void handleAdd(item)}
              />
            </View>
          </View>
        )}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background,
  },
  filters: {
    gap: spacing.sm,
    padding: spacing.md,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: colors.border,
  },
  searchInput: {
    minHeight: 52,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    paddingHorizontal: spacing.md,
    color: colors.text,
    backgroundColor: colors.surface,
    fontSize: 16,
  },
  selectRow: {
    flexDirection: 'row',
    gap: spacing.sm,
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
    gap: spacing.md,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    padding: spacing.sm,
    backgroundColor: colors.surface,
  },
  cardBody: {
    flex: 1,
    justifyContent: 'center',
    gap: spacing.sm,
  },
  name: {
    color: colors.text,
    fontSize: 16,
    fontWeight: '800',
    textTransform: 'capitalize',
  },
  meta: {
    color: colors.textMuted,
    fontSize: 13,
    textTransform: 'capitalize',
  },
});
