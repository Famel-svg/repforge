export const workoutXOrigin = 'https://api.workoutxapp.com';
const defaultWorkoutXProxyOrigin =
  'https://repforge-workoutx-proxy.repforge-rafael.workers.dev';

export const workoutXProxyOrigin =
  process.env.EXPO_PUBLIC_WORKOUTX_PROXY_URL?.trim().replace(/\/+$/, '') ||
  (process.env.NODE_ENV === 'test' ? null : defaultWorkoutXProxyOrigin);

export const workoutXApiUrl = workoutXProxyOrigin
  ? `${workoutXProxyOrigin}/v1`
  : `${workoutXOrigin}/v1`;

export function proxyWorkoutXUrl(url: string): string {
  if (!workoutXProxyOrigin || !url.startsWith(workoutXOrigin)) {
    return url;
  }

  return `${workoutXProxyOrigin}${url.slice(workoutXOrigin.length)}`;
}
