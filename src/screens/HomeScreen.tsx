import { useFocusEffect } from '@react-navigation/native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { useSQLiteContext } from 'expo-sqlite';
import { useCallback, useState } from 'react';
import { Alert, ScrollView, StyleSheet, Text, View } from 'react-native';

import { AppButton } from '@/components/AppButton';
import { BottomTabBar } from '@/components/BottomTabBar';
import { StatCard } from '@/components/StatCard';
import { getDashboardStats, type DashboardStats } from '@/db/stats';
import type { RootStackParamList } from '@/navigation/types';
import { colors, radius, spacing } from '@/theme';

type Props = NativeStackScreenProps<RootStackParamList, 'Home'>;

function formatCount(value: number | undefined): string {
  return value === undefined ? '...' : String(value);
}

function formatVolume(value: number | undefined): string {
  if (value === undefined) return '...';
  return `${Math.round(value).toLocaleString('pt-BR')} kg`;
}

function formatCompactVolume(value: number): string {
  if (value >= 1000) {
    return `${(Math.round(value / 100) / 10).toLocaleString('pt-BR')}k`;
  }
  return String(Math.round(value));
}

export function HomeScreen({ navigation }: Props) {
  const db = useSQLiteContext();
  const [stats, setStats] = useState<DashboardStats | null>(null);

  const dailyVolumes = stats?.last7Days ?? [];
  const maxDailyVolume = Math.max(1, ...dailyVolumes.map((day) => day.volume));

  const loadStats = useCallback(async () => {
    try {
      setStats(await getDashboardStats(db));
    } catch (error) {
      Alert.alert('Erro', error instanceof Error ? error.message : 'Falha ao carregar painel.');
    }
  }, [db]);

  useFocusEffect(
    useCallback(() => {
      void loadStats();
    }, [loadStats]),
  );

  return (
    <View style={styles.screen}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.hero}>
          <Text style={styles.eyebrow}>HOME</Text>
          <Text style={styles.heroTitle}>Forje consistência, não só carga.</Text>
          <Text style={styles.heroText}>
            Veja o pulso do treino, entre nas planilhas e acompanhe os dias treinados.
          </Text>
        </View>

        <View style={styles.statsGrid}>
          <StatCard label="Planilhas" value={formatCount(stats?.totals.sheets)} />
          <StatCard label="Exercícios" value={formatCount(stats?.totals.exercises)} />
        </View>
        <View style={styles.statsGrid}>
          <StatCard label="Registros" value={formatCount(stats?.totals.entries)} />
          <StatCard
            hint="Séries x reps x kg"
            label="Volume semana"
            tone="primary"
            value={formatVolume(stats?.totals.weeklyVolume)}
          />
        </View>

        <View style={styles.chartCard}>
          <View style={styles.chartHeader}>
            <View>
              <Text style={styles.chartTitle}>Últimos 7 dias</Text>
              <Text style={styles.chartSubtitle}>Volume por dia</Text>
            </View>
            <AppButton
              compact
              label="Track"
              onPress={() => navigation.replace('Track')}
              variant="secondary"
            />
          </View>
          <View style={styles.barChart}>
            {dailyVolumes.map((day) => {
              const barHeight = day.volume === 0 ? 4 : Math.max(8, (day.volume / maxDailyVolume) * 96);

              return (
                <View key={day.date} style={styles.barColumn}>
                  <Text style={styles.barValue}>{formatCompactVolume(day.volume)}</Text>
                  <View style={styles.barTrack}>
                    <View style={[styles.barFill, { height: barHeight }]} />
                  </View>
                  <Text style={styles.barLabel}>{day.label}</Text>
                </View>
              );
            })}
          </View>
        </View>

        <View style={styles.quickGrid}>
          <AppButton label="Abrir planilhas" onPress={() => navigation.replace('Sheets')} />
          <AppButton label="Ver dias treinados" onPress={() => navigation.replace('Track')} variant="secondary" />
        </View>
      </ScrollView>

      <BottomTabBar active="home" onNavigate={(routeName) => navigation.replace(routeName)} />
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
    gap: spacing.md,
  },
  hero: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.lg,
    padding: spacing.lg,
    backgroundColor: colors.surface,
    gap: spacing.sm,
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
  },
  heroText: {
    color: colors.textMuted,
    lineHeight: 21,
  },
  statsGrid: {
    flexDirection: 'row',
    gap: spacing.sm,
  },
  chartCard: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.lg,
    padding: spacing.md,
    backgroundColor: colors.surface,
    gap: spacing.md,
  },
  chartHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: spacing.sm,
  },
  chartTitle: {
    color: colors.text,
    fontSize: 18,
    fontWeight: '900',
  },
  chartSubtitle: {
    color: colors.textMuted,
    fontSize: 12,
    marginTop: 2,
  },
  barChart: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: spacing.xs,
  },
  barColumn: {
    flex: 1,
    alignItems: 'center',
    gap: spacing.xs,
  },
  barValue: {
    color: colors.textMuted,
    fontSize: 10,
    fontWeight: '700',
  },
  barTrack: {
    width: '100%',
    height: 96,
    justifyContent: 'flex-end',
    alignItems: 'center',
    borderRadius: radius.sm,
    backgroundColor: colors.background,
    overflow: 'hidden',
  },
  barFill: {
    width: '70%',
    borderTopLeftRadius: radius.sm,
    borderTopRightRadius: radius.sm,
    backgroundColor: colors.primary,
  },
  barLabel: {
    color: colors.textMuted,
    fontSize: 10,
    fontWeight: '700',
  },
  quickGrid: {
    gap: spacing.sm,
  },
});

