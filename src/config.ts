export const workoutXOrigin = 'https://api.workoutxapp.com';
export const workoutXProxyOrigin =
  process.env.EXPO_PUBLIC_WORKOUTX_PROXY_URL?.trim().replace(/\/+$/, '') ||
  null;

export const workoutXApiUrl = workoutXProxyOrigin
  ? `${workoutXProxyOrigin}/v1`
  : `${workoutXOrigin}/v1`;

export function proxyWorkoutXUrl(url: string): string {
  if (!workoutXProxyOrigin || !url.startsWith(workoutXOrigin)) {
    return url;
  }

  return `${workoutXProxyOrigin}${url.slice(workoutXOrigin.length)}`;
}
