import { useFocusEffect } from '@react-navigation/native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useSQLiteContext } from 'expo-sqlite';
import { useCallback, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';

import { BottomTabBar } from '@/components/BottomTabBar';
import { EmptyState } from '@/components/EmptyState';
import { StatCard } from '@/components/StatCard';
import {
  getTrainingTrackStats,
  type TrainingDay,
  type TrainingTrackStats,
} from '@/db/stats';
import type { RootStackParamList } from '@/navigation/types';
import { colors, radius, spacing } from '@/theme';

type Props = NativeStackScreenProps<RootStackParamList, 'Track'>;

const RECENT_TRAINING_DAYS_LIMIT = 6;

function formatDayCount(value: number): string {
  return `${value} ${value === 1 ? 'dia' : 'dias'}`;
}

function formatEntryCount(value: number): string {
  return `${value} ${value === 1 ? 'registro' : 'registros'}`;
}

function formatVolume(value: number): string {
  return `${Math.round(value).toLocaleString('pt-BR')} kg`;
}

function getStreakHint(value: number): string {
  if (value === 0) {
    return 'Retome hoje';
  }

  return value === 1 ? '1 dia seguido' : `${value} dias seguidos`;
}

function DayCell({ day }: { day: TrainingDay }) {
  return (
    <View style={[styles.dayCell, day.trained && styles.trainedDayCell]}>
      <Text style={[styles.dayWeekday, day.trained && styles.trainedDayText]}>
        {day.weekday}
      </Text>
      <Text style={[styles.dayLabel, day.trained && styles.trainedDayText]}>
        {day.label}
      </Text>
      <Text style={[styles.dayStatus, day.trained && styles.trainedDayStatus]}>
        {day.trained ? 'Treino' : 'Livre'}
      </Text>
    </View>
  );
}

function RecentTrainingDay({ day }: { day: TrainingDay }) {
  return (
    <View style={styles.recentItem}>
      <View style={styles.recentDateBadge}>
        <Text style={styles.recentWeekday}>{day.weekday}</Text>
        <Text style={styles.recentDate}>{day.label}</Text>
      </View>
      <View style={styles.recentBody}>
        <Text style={styles.recentTitle}>Treino registrado</Text>
        <Text style={styles.recentMeta}>
          {formatEntryCount(day.entries)} · {formatVolume(day.volume)}
        </Text>
      </View>
    </View>
  );
}

export function TrainingTrackScreen({ navigation }: Props) {
  const db = useSQLiteContext();
  const [stats, setStats] = useState<TrainingTrackStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadStats = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const nextStats = await getTrainingTrackStats(db);
      setStats(nextStats);
    } catch (loadError) {
      setError(
        loadError instanceof Error
          ? loadError.message
          : 'Não foi possível carregar sua sequência.',
      );
    } finally {
      setLoading(false);
    }
  }, [db]);

  useFocusEffect(
    useCallback(() => {
      let active = true;

      async function loadFocusedStats() {
        setLoading(true);
        setError(null);

        try {
          const nextStats = await getTrainingTrackStats(db);

          if (active) {
            setStats(nextStats);
          }
        } catch (loadError) {
          if (active) {
            setError(
              loadError instanceof Error
                ? loadError.message
                : 'Não foi possível carregar sua sequência.',
            );
          }
        } finally {
          if (active) {
            setLoading(false);
          }
        }
      }

      void loadFocusedStats();

      return () => {
        active = false;
      };
    }, [db]),
  );

  const recentTrainingDays = useMemo(
    () =>
      [...(stats?.last28Days ?? [])]
        .filter((day) => day.trained)
        .reverse()
        .slice(0, RECENT_TRAINING_DAYS_LIMIT),
    [stats?.last28Days],
  );

  const hasTraining = (stats?.totalTrainingDays ?? 0) > 0;

  return (
    <View style={styles.screen}>
      <ScrollView contentContainerStyle={styles.content}>
      <View style={styles.hero}>
        <Text style={styles.eyebrow}>TRACK DE TREINO</Text>
        <Text style={styles.heroTitle}>Consistência visível.</Text>
        <Text style={styles.heroText}>
          Acompanhe dias treinados, sequência atual e ritmo do mês.
        </Text>
      </View>

      {loading ? (
        <View style={styles.stateCard}>
          <ActivityIndicator color={colors.primary} />
          <Text style={styles.stateText}>Carregando track de treinos...</Text>
        </View>
      ) : error ? (
        <View style={styles.stateCard}>
          <Text style={styles.stateTitle}>Erro ao carregar</Text>
          <Text style={styles.stateText}>{error}</Text>
          <Pressable
            accessibilityRole="button"
            onPress={() => void loadStats()}
            style={({ pressed }) => [
              styles.retryButton,
              pressed && styles.pressed,
            ]}
          >
            <Text style={styles.retryText}>Tentar novamente</Text>
          </Pressable>
        </View>
      ) : !hasTraining ? (
        <EmptyState
          description="Registre seu primeiro treino para começar a montar sua sequência de dias treinados."
          title="Nenhum treino registrado"
        />
      ) : stats ? (
        <>
          <View style={styles.statsSection}>
            <View style={styles.statsGrid}>
              <StatCard
                hint={getStreakHint(stats.currentStreak)}
                label="Sequência atual"
                tone="primary"
                value={formatDayCount(stats.currentStreak)}
              />
              <StatCard
                hint="Maior marca"
                label="Melhor sequência"
                value={formatDayCount(stats.bestStreak)}
              />
            </View>
            <View style={styles.statsGrid}>
              <StatCard
                hint="Mês atual"
                label="Dias mês"
                value={formatDayCount(stats.thisMonthTrainingDays)}
              />
              <StatCard
                hint="Desde o início"
                label="Total"
                value={formatDayCount(stats.totalTrainingDays)}
              />
            </View>
          </View>

          <View style={styles.sectionCard}>
            <View style={styles.sectionHeader}>
              <View>
                <Text style={styles.sectionTitle}>Últimos 28 dias</Text>
                <Text style={styles.sectionSubtitle}>
                  Blocos destacados indicam dias com treino.
                </Text>
              </View>
              <View style={styles.legend}>
                <View style={styles.legendDot} />
                <Text style={styles.legendText}>Treino</Text>
              </View>
            </View>

            <View style={styles.daysGrid}>
              {stats.last28Days.map((day) => (
                <DayCell day={day} key={day.date} />
              ))}
            </View>
          </View>

          <View style={styles.sectionCard}>
            <View style={styles.sectionHeader}>
              <View>
                <Text style={styles.sectionTitle}>Dias treinados recentes</Text>
                <Text style={styles.sectionSubtitle}>
                  Últimos treinos dentro da janela de 28 dias.
                </Text>
              </View>
            </View>

            {recentTrainingDays.length > 0 ? (
              <View style={styles.recentList}>
                {recentTrainingDays.map((day) => (
                  <RecentTrainingDay day={day} key={day.date} />
                ))}
              </View>
            ) : (
              <View style={styles.quietCard}>
                <Text style={styles.quietTitle}>Sem treino recente</Text>
                <Text style={styles.quietText}>
                  Seu histórico existe, mas nenhum treino caiu nos últimos 28 dias.
                </Text>
              </View>
            )}
          </View>
        </>
      ) : null}
      </ScrollView>

      <BottomTabBar
        active="track"
        onNavigate={(routeName) => navigation.replace(routeName)}
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
    flexGrow: 1,
    paddingBottom: spacing.xl,
  },
  hero: {
    paddingHorizontal: spacing.md,
    paddingTop: spacing.lg,
    paddingBottom: spacing.md,
  },
  eyebrow: {
    color: colors.primary,
    fontSize: 12,
    fontWeight: '900',
    letterSpacing: 1.4,
  },
  heroTitle: {
    color: colors.text,
    fontSize: 28,
    fontWeight: '900',
    marginTop: spacing.xs,
  },
  heroText: {
    color: colors.textMuted,
    lineHeight: 21,
    marginTop: spacing.sm,
  },
  stateCard: {
    alignItems: 'center',
    justifyContent: 'center',
    margin: spacing.md,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.lg,
    padding: spacing.xl,
    backgroundColor: colors.surface,
    gap: spacing.sm,
  },
  stateTitle: {
    color: colors.text,
    fontSize: 18,
    fontWeight: '900',
    textAlign: 'center',
  },
  stateText: {
    color: colors.textMuted,
    lineHeight: 21,
    textAlign: 'center',
  },
  retryButton: {
    minHeight: 48,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radius.md,
    paddingHorizontal: spacing.md,
    backgroundColor: colors.primary,
  },
  retryText: {
    color: '#FFFFFF',
    fontWeight: '800',
  },
  pressed: {
    opacity: 0.75,
  },
  statsSection: {
    paddingHorizontal: spacing.md,
    paddingBottom: spacing.sm,
    gap: spacing.sm,
  },
  statsGrid: {
    flexDirection: 'row',
    gap: spacing.sm,
  },
  sectionCard: {
    marginHorizontal: spacing.md,
    marginTop: spacing.sm,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    padding: spacing.md,
    backgroundColor: colors.surface,
    gap: spacing.md,
  },
  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: spacing.sm,
  },
  sectionTitle: {
    color: colors.text,
    fontSize: 18,
    fontWeight: '900',
  },
  sectionSubtitle: {
    color: colors.textMuted,
    fontSize: 12,
    fontWeight: '700',
    lineHeight: 18,
    marginTop: spacing.xs,
  },
  legend: {
    minHeight: 28,
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.sm,
    paddingHorizontal: spacing.sm,
    gap: spacing.xs,
  },
  legendDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: colors.primary,
  },
  legendText: {
    color: colors.textMuted,
    fontSize: 11,
    fontWeight: '800',
  },
  daysGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.xs,
  },
  dayCell: {
    width: '13.1%',
    minHeight: 72,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.sm,
    paddingVertical: spacing.sm,
    paddingHorizontal: 2,
    backgroundColor: colors.background,
    gap: 2,
  },
  trainedDayCell: {
    borderColor: colors.primary,
    backgroundColor: colors.primary + '20',
  },
  dayWeekday: {
    color: colors.textMuted,
    fontSize: 10,
    fontWeight: '800',
  },
  dayLabel: {
    color: colors.text,
    fontSize: 11,
    fontWeight: '900',
  },
  dayStatus: {
    color: colors.textMuted,
    fontSize: 9,
    fontWeight: '800',
  },
  trainedDayText: {
    color: colors.text,
  },
  trainedDayStatus: {
    color: colors.primary,
  },
  recentList: {
    gap: spacing.sm,
  },
  recentItem: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    padding: spacing.sm,
    backgroundColor: colors.surfaceRaised,
    gap: spacing.sm,
  },
  recentDateBadge: {
    width: 64,
    minHeight: 56,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: radius.sm,
    backgroundColor: colors.background,
    gap: 2,
  },
  recentWeekday: {
    color: colors.primary,
    fontSize: 11,
    fontWeight: '900',
  },
  recentDate: {
    color: colors.text,
    fontSize: 15,
    fontWeight: '900',
  },
  recentBody: {
    flex: 1,
    gap: spacing.xs,
  },
  recentTitle: {
    color: colors.text,
    fontSize: 15,
    fontWeight: '900',
  },
  recentMeta: {
    color: colors.textMuted,
    fontSize: 12,
    fontWeight: '700',
  },
  quietCard: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    padding: spacing.md,
    backgroundColor: colors.background,
    gap: spacing.xs,
  },
  quietTitle: {
    color: colors.text,
    fontSize: 15,
    fontWeight: '900',
  },
  quietText: {
    color: colors.textMuted,
    lineHeight: 20,
  },
});




