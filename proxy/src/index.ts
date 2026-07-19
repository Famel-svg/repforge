const WORKOUTX_ORIGIN = 'https://api.workoutxapp.com';
const ALLOWED_PREFIXES = ['/v1/exercises', '/v1/gifs'];

type Env = {
  WORKOUTX_API_KEY: string;
  USAGE: KVNamespace;
  BURST_LIMITER: RateLimit;
  GLOBAL_DAILY_LIMIT?: string;
  PER_INSTALL_DAILY_LIMIT?: string;
};

type RateLimit = {
  limit(input: { key: string }): Promise<{ success: boolean }>;
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

function secondsUntilTomorrow(): number {
  const now = new Date();
  const tomorrow = Date.UTC(
    now.getUTCFullYear(),
    now.getUTCMonth(),
    now.getUTCDate() + 1,
  );
  return Math.max(60, Math.ceil((tomorrow - now.getTime()) / 1000));
}

async function spendDailyQuota(
  env: Env,
  key: string,
  limit: number,
): Promise<boolean> {
  const today = new Date().toISOString().slice(0, 10);
  const quotaKey = `${today}:${key}`;
  const used = Number((await env.USAGE.get(quotaKey)) ?? '0');
  if (used >= limit) return false;

  await env.USAGE.put(quotaKey, String(used + 1), {
    expirationTtl: secondsUntilTomorrow(),
  });
  return true;
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    const installId = request.headers.get('X-RepForge-Install') ?? 'unknown';

    if (request.method !== 'GET') {
      return json({ error: 'method_not_allowed' }, 405);
    }

    if (!ALLOWED_PREFIXES.some((prefix) => url.pathname.startsWith(prefix))) {
      return json({ error: 'not_found' }, 404);
    }

    const burst = await env.BURST_LIMITER.limit({ key: installId });
    if (!burst.success) {
      return json({ error: 'too_many_requests' }, 429);
    }

    const upstream = new URL(`${WORKOUTX_ORIGIN}${url.pathname}`);
    upstream.search = url.search;

    const cache = (caches as unknown as { default: Cache }).default;
    const cacheKey = new Request(upstream.toString(), request);
    const cached = await cache.match(cacheKey);
    if (cached) {
      return new Response(cached.body, {
        status: cached.status,
        headers: {
          ...Object.fromEntries(cached.headers),
          'X-RepForge-Cache': 'HIT',
        },
      });
    }

    const globalLimit = Number(env.GLOBAL_DAILY_LIMIT ?? 450);
    const installLimit = Number(env.PER_INSTALL_DAILY_LIMIT ?? 60);
    const globalAllowed = await spendDailyQuota(env, 'global', globalLimit);
    const installAllowed = await spendDailyQuota(
      env,
      `install:${installId}`,
      installLimit,
    );

    if (!globalAllowed || !installAllowed) {
      return json({ error: 'daily_limit_reached' }, 429);
    }

    const response = await fetch(upstream, {
      headers: {
        Accept: 'application/json,image/*,*/*',
        'X-WorkoutX-Key': env.WORKOUTX_API_KEY,
      },
    });

    const headers = new Headers(response.headers);
    headers.set('Cache-Control', 'public, max-age=86400');
    headers.set('X-RepForge-Cache', 'MISS');
    headers.delete('Set-Cookie');

    const proxied = new Response(response.body, {
      status: response.status,
      headers,
    });

    if (response.ok) {
      await cache.put(cacheKey, proxied.clone());
    }

    return proxied;
  },
};
