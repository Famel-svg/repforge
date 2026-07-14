import type { SQLiteDatabase } from 'expo-sqlite';

export type DashboardTotals = {
  sheets: number;
  exercises: number;
  entries: number;
  weeklyVolume: number;
};

export type DailyVolume = {
  date: string;
  label: string;
  volume: number;
};

export type DashboardStats = {
  totals: DashboardTotals;
  last7Days: DailyVolume[];
};

export type TrainingDay = {
  date: string;
  entries: number;
  label: string;
  trained: boolean;
  volume: number;
  weekday: string;
};

export type TrainingTrackStats = {
  bestStreak: number;
  currentStreak: number;
  last28Days: TrainingDay[];
  thisMonthTrainingDays: number;
  totalTrainingDays: number;
};

type DayBucket = Pick<DailyVolume, 'date' | 'label'>;

type TotalsRow = {
  sheet_count: number;
  exercise_count: number;
  entry_count: number;
  weekly_volume: number | null;
};

type DailyVolumeRow = {
  day: string;
  volume: number | null;
};

type TrainingEntryRow = {
  recorded_at: string;
  reps: number;
  sets: number;
  weight_kg: number;
};

type TrainingDayAggregate = {
  entries: number;
  volume: number;
};

type StatsDatabase = Pick<SQLiteDatabase, 'getAllAsync' | 'getFirstAsync'>;

const DAY_MS = 24 * 60 * 60 * 1000;
const WEEKDAY_LABELS = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];

const dateKeyFormatters = new Map<string, Intl.DateTimeFormat>();

export const DASHBOARD_TOTALS_SQL = `
  SELECT
    (SELECT COUNT(*) FROM sheets) AS sheet_count,
    (SELECT COUNT(*) FROM exercises) AS exercise_count,
    (SELECT COUNT(*) FROM entries) AS entry_count,
    COALESCE((
      SELECT SUM(sets * reps * weight_kg)
      FROM entries
      WHERE recorded_at >= ? AND recorded_at < ?
    ), 0) AS weekly_volume
`;

export const DASHBOARD_DAILY_VOLUME_SQL = `
  SELECT
    substr(recorded_at, 1, 10) AS day,
    COALESCE(SUM(sets * reps * weight_kg), 0) AS volume
  FROM entries
  WHERE recorded_at >= ? AND recorded_at < ?
  GROUP BY day
  ORDER BY day ASC
`;

export const TRAINING_TRACK_SQL = `
  SELECT recorded_at, sets, reps, weight_kg
  FROM entries
  ORDER BY recorded_at ASC
`;

function startOfUtcDay(date: Date): Date {
  return new Date(
    Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()),
  );
}

function formatDayLabel(date: Date): string {
  const day = String(date.getUTCDate()).padStart(2, '0');
  const month = String(date.getUTCMonth() + 1).padStart(2, '0');
  return `${day}/${month}`;
}

function toDateKey(date: Date): string {
  return date.toISOString().slice(0, 10);
}

function getDefaultTimeZone(): string {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
  } catch {
    return 'UTC';
  }
}

function getDateKeyFormatter(timeZone: string): Intl.DateTimeFormat {
  const cached = dateKeyFormatters.get(timeZone);
  if (cached) return cached;

  const formatter = new Intl.DateTimeFormat('en-US', {
    day: '2-digit',
    month: '2-digit',
    timeZone,
    year: 'numeric',
  });
  dateKeyFormatters.set(timeZone, formatter);
  return formatter;
}

function toZonedDateKey(date: Date, timeZone: string): string {
  const parts = getDateKeyFormatter(timeZone).formatToParts(date);
  const year = parts.find((part) => part.type === 'year')?.value ?? '0000';
  const month = parts.find((part) => part.type === 'month')?.value ?? '01';
  const day = parts.find((part) => part.type === 'day')?.value ?? '01';
  return `${year}-${month}-${day}`;
}

function dateKeyToUtcDate(dateKey: string): Date {
  const [year, month, day] = dateKey.split('-').map(Number);
  return new Date(Date.UTC(year, month - 1, day));
}

function shiftDateKey(dateKey: string, amount: number): string {
  return toDateKey(
    new Date(dateKeyToUtcDate(dateKey).getTime() + amount * DAY_MS),
  );
}

function formatDateKeyLabel(dateKey: string): string {
  const [, month, day] = dateKey.split('-');
  return `${day}/${month}`;
}

function getDateKeyWeekday(dateKey: string): string {
  return WEEKDAY_LABELS[dateKeyToUtcDate(dateKey).getUTCDay()];
}

function countCurrentStreak(trainedDates: Set<string>, todayKey: string): number {
  let cursor = trainedDates.has(todayKey) ? todayKey : shiftDateKey(todayKey, -1);
  let streak = 0;

  while (trainedDates.has(cursor)) {
    streak += 1;
    cursor = shiftDateKey(cursor, -1);
  }

  return streak;
}

function countBestStreak(sortedDates: string[]): number {
  let best = 0;
  let current = 0;
  let previous: string | null = null;

  for (const dateKey of sortedDates) {
    const isConsecutive =
      previous !== null && shiftDateKey(previous, 1) === dateKey;
    current = isConsecutive ? current + 1 : 1;
    best = Math.max(best, current);
    previous = dateKey;
  }

  return best;
}

function aggregateTrainingRows(
  rows: TrainingEntryRow[],
  timeZone: string,
): Map<string, TrainingDayAggregate> {
  const dayMap = new Map<string, TrainingDayAggregate>();

  for (const row of rows) {
    const recordedAt = new Date(row.recorded_at);
    if (Number.isNaN(recordedAt.getTime())) continue;

    const dateKey = toZonedDateKey(recordedAt, timeZone);
    const current = dayMap.get(dateKey) ?? { entries: 0, volume: 0 };
    current.entries += 1;
    current.volume +=
      Number(row.sets ?? 0) * Number(row.reps ?? 0) * Number(row.weight_kg ?? 0);
    dayMap.set(dateKey, current);
  }

  return dayMap;
}

export function getLast7DayRange(now = new Date()): {
  days: DayBucket[];
  endIso: string;
  startIso: string;
} {
  const today = startOfUtcDay(now);
  const start = new Date(today.getTime() - 6 * DAY_MS);
  const end = new Date(today.getTime() + DAY_MS);
  const days = Array.from({ length: 7 }, (_, index) => {
    const date = new Date(start.getTime() + index * DAY_MS);
    return {
      date: toDateKey(date),
      label: formatDayLabel(date),
    };
  });

  return {
    days,
    endIso: end.toISOString(),
    startIso: start.toISOString(),
  };
}

export async function getDashboardStats(
  db: StatsDatabase,
  now = new Date(),
): Promise<DashboardStats> {
  const range = getLast7DayRange(now);
  const totalsRow = await db.getFirstAsync<TotalsRow>(
    DASHBOARD_TOTALS_SQL,
    range.startIso,
    range.endIso,
  );
  const dailyRows = await db.getAllAsync<DailyVolumeRow>(
    DASHBOARD_DAILY_VOLUME_SQL,
    range.startIso,
    range.endIso,
  );
  const volumeByDay = new Map(
    dailyRows.map((row) => [row.day, Number(row.volume ?? 0)]),
  );

  return {
    last7Days: range.days.map((day) => ({
      ...day,
      volume: volumeByDay.get(day.date) ?? 0,
    })),
    totals: {
      entries: Number(totalsRow?.entry_count ?? 0),
      exercises: Number(totalsRow?.exercise_count ?? 0),
      sheets: Number(totalsRow?.sheet_count ?? 0),
      weeklyVolume: Number(totalsRow?.weekly_volume ?? 0),
    },
  };
}

export async function getTrainingTrackStats(
  db: StatsDatabase,
  now = new Date(),
  timeZone = getDefaultTimeZone(),
): Promise<TrainingTrackStats> {
  const todayKey = toZonedDateKey(now, timeZone);
  const startKey = shiftDateKey(todayKey, -27);
  const rows = await db.getAllAsync<TrainingEntryRow>(TRAINING_TRACK_SQL);
  const dayMap = aggregateTrainingRows(rows, timeZone);
  const trainedDates = new Set(dayMap.keys());
  const sortedDates = [...trainedDates].sort();
  const currentMonth = todayKey.slice(0, 7);

  return {
    bestStreak: countBestStreak(sortedDates),
    currentStreak: countCurrentStreak(trainedDates, todayKey),
    last28Days: Array.from({ length: 28 }, (_, index) => {
      const dateKey = shiftDateKey(startKey, index);
      const record = dayMap.get(dateKey);

      return {
        date: dateKey,
        entries: record?.entries ?? 0,
        label: formatDateKeyLabel(dateKey),
        trained: record !== undefined,
        volume: record?.volume ?? 0,
        weekday: getDateKeyWeekday(dateKey),
      };
    }),
    thisMonthTrainingDays: sortedDates.filter((date) =>
      date.startsWith(currentMonth),
    ).length,
    totalTrainingDays: trainedDates.size,
  };
}
