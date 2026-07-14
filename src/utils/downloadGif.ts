import { Directory, File, Paths } from 'expo-file-system';

const API_KEY = process.env.EXPO_PUBLIC_WORKOUTX_KEY ?? '';

export async function downloadGif(url: string): Promise<string | null> {
  if (!url || !API_KEY) return null;

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

    const response = await fetch(url, {
      headers: { 'X-WorkoutX-Key': API_KEY },
    });

    if (!response.ok) return null;

    file.write(new Uint8Array(await response.arrayBuffer()));

    return file.uri;
  } catch {
    return null;
  }
}
