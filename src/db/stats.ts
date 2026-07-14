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

type StatsDatabase = Pick<SQLiteDatabase, 'getAllAsync' | 'getFirstAsync'>;

const DAY_MS = 24 * 60 * 60 * 1000;

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
      date: date.toISOString().slice(0, 10),
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
