const WORKOUTX_ORIGIN = 'https://api.workoutxapp.com';
const ALLOWED_PREFIXES = ['/v1/exercises', '/v1/gifs'];

type Env = {
  WORKOUTX_API_KEY: string;
  USAGE: KVNamespace;
  BURST_LIMITER: RateLimit;
  GLOBAL_DAILY_LIMIT?: string;
  PER_INSTALL_DAILY_LIMIT?: string;
  PER_IP_DAILY_LIMIT?: string;
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

type Quota = { key: string; limit: number };

// KV has no atomic increment, so concurrent misses can overshoot a limit by a
// few requests. Every quota is checked before any is spent, so a caller that is
// already blocked never consumes the shared global budget.
async function spendDailyQuotas(env: Env, quotas: Quota[]): Promise<boolean> {
  const today = new Date().toISOString().slice(0, 10);
  const usage = await Promise.all(
    quotas.map(async ({ key, limit }) => {
      const quotaKey = `${today}:${key}`;
      const used = Number((await env.USAGE.get(quotaKey)) ?? '0');
      return { quotaKey, used, limit };
    }),
  );
  if (usage.some(({ used, limit }) => used >= limit)) return false;

  const expirationTtl = secondsUntilTomorrow();
  await Promise.all(
    usage.map(({ quotaKey, used }) =>
      env.USAGE.put(quotaKey, String(used + 1), { expirationTtl }),
    ),
  );
  return true;
}

// The install id is chosen by the client, so it is only a fairness hint; the
// caller IP is what actually bounds a single abuser.
function sanitizeInstallId(value: string | null): string {
  const cleaned = (value ?? '').replace(/[^A-Za-z0-9_-]/g, '').slice(0, 64);
  return cleaned || 'unknown';
}

function isAllowedPath(pathname: string): boolean {
  return ALLOWED_PREFIXES.some(
    (prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`),
  );
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    const installId = sanitizeInstallId(
      request.headers.get('X-RepForge-Install'),
    );
    const ip = request.headers.get('CF-Connecting-IP') ?? 'unknown';

    if (request.method !== 'GET') {
      return json({ error: 'method_not_allowed' }, 405);
    }

    if (!isAllowedPath(url.pathname)) {
      return json({ error: 'not_found' }, 404);
    }

    const [burstByIp, burstByInstall] = await Promise.all([
      env.BURST_LIMITER.limit({ key: `ip:${ip}` }),
      env.BURST_LIMITER.limit({ key: `install:${installId}` }),
    ]);
    if (!burstByIp.success || !burstByInstall.success) {
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

    const installLimit = Number(env.PER_INSTALL_DAILY_LIMIT ?? 60);
    const allowed = await spendDailyQuotas(env, [
      { key: `install:${installId}`, limit: installLimit },
      // Several installs can share one address (NAT, mobile carriers).
      { key: `ip:${ip}`, limit: Number(env.PER_IP_DAILY_LIMIT ?? installLimit * 3) },
      { key: 'global', limit: Number(env.GLOBAL_DAILY_LIMIT ?? 15) },
    ]);

    if (!allowed) {
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
