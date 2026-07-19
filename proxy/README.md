# RepForge WorkoutX Proxy

Cloudflare Worker that keeps `WORKOUTX_API_KEY` off the APK.

## Free limits

Cloudflare Workers Free includes 100,000 requests/day. KV Free includes
100,000 reads/day and 1,000 writes/day.

This proxy protects quota in three ways:

- caches WorkoutX responses for 24h;
- allows only `/v1/exercises*` and `/v1/gifs*`;
- limits cache misses per install id and globally per UTC day.

Defaults:

- `PER_INSTALL_DAILY_LIMIT=60`
- `GLOBAL_DAILY_LIMIT=450`
- burst limit: `10` requests/minute per install id

For a WorkoutX free key with 500 requests/month, set
`GLOBAL_DAILY_LIMIT=15`.

## Setup

```bash
cd proxy
npm install
npx wrangler login
npx wrangler kv namespace create USAGE
```

Copy the returned KV `id` into `wrangler.jsonc`.

```bash
npx wrangler secret put WORKOUTX_API_KEY
npm run deploy
```

Copy the deployed URL, then add this to the app `.env.local` before building
the APK:

```text
EXPO_PUBLIC_WORKOUTX_PROXY_URL=https://repforge-workoutx-proxy.YOUR.workers.dev
```

This URL is public and safe to embed. The WorkoutX key stays in Cloudflare.
