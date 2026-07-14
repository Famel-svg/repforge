export const DEFAULT_REST_SECONDS = 90;
export const MIN_REST_SECONDS = 15;
export const MAX_REST_SECONDS = 300;
export const REST_STEP_SECONDS = 15;

export function clampRestSeconds(seconds: number): number {
  if (!Number.isFinite(seconds)) return DEFAULT_REST_SECONDS;
  return Math.min(MAX_REST_SECONDS, Math.max(MIN_REST_SECONDS, Math.round(seconds)));
}

export function formatRestTime(seconds: number): string {
  const safeSeconds = Math.max(0, Math.floor(seconds));
  const minutes = Math.floor(safeSeconds / 60);
  const remainingSeconds = safeSeconds % 60;
  return `${minutes}:${String(remainingSeconds).padStart(2, '0')}`;
}
