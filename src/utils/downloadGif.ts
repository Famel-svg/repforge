import { Directory, File, Paths } from 'expo-file-system';

export async function downloadGif(
  url: string,
  apiKey?: string | null,
): Promise<string | null> {
  const key = apiKey?.trim();
  if (!url || !key) return null;

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
      headers: { 'X-WorkoutX-Key': key },
    });

    if (!response.ok) return null;

    file.write(new Uint8Array(await response.arrayBuffer()));

    return file.uri;
  } catch {
    return null;
  }
}
