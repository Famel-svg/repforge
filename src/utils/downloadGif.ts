import { Directory, File, Paths } from 'expo-file-system';

import { proxyWorkoutXUrl, workoutXProxyOrigin } from '@/config';

export async function downloadGif(
  url: string,
  apiKey?: string | null,
  installId?: string | null,
): Promise<string | null> {
  const key = apiKey?.trim();
  const proxiedUrl = proxyWorkoutXUrl(url);
  if (!url || !url.startsWith('http')) return null;
  if (!workoutXProxyOrigin && !key) return null;

  const rawName = url.split('/').pop() ?? `img_${Date.now()}`;
  const filename = rawName.endsWith('.gif') ? rawName : `${rawName}.gif`;
  const directory = new Directory(Paths.cache, 'gifs');
  const file = new File(directory, filename);

  if (file.exists) {
    return file.uri;
  }

  try {
    if (!directory.exists) {
      directory.create({ intermediates: true });
    }

    const response = await fetch(proxiedUrl, {
      headers: workoutXProxyOrigin
        ? { 'X-RepForge-Install': installId ?? 'unknown' }
        : { 'X-WorkoutX-Key': key ?? '' },
    });

    if (!response.ok) return null;

    file.write(new Uint8Array(await response.arrayBuffer()));

    return file.uri;
  } catch {
    return null;
  }
}
