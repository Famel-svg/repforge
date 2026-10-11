# RepForge WorkoutX Proxy

Cloudflare Worker that keeps `WORKOUTX_API_KEY` off the APK.

## Free limits

Cloudflare Workers Free includes 100,000 requests/day. KV Free includes
100,000 reads/day and 1,000 writes/day.

This proxy protects quota in three ways:

- caches WorkoutX responses for 24h;
- allows only `GET` on `/v1/exercises` and `/v1/gifs` (and their sub-paths);
- limits cache misses per install id, per caller IP and globally per UTC day.

Defaults:

- `GLOBAL_DAILY_LIMIT=15` — sized for the WorkoutX free key (500
  requests/month). Raise it in `wrangler.jsonc` if your plan allows more.
- `PER_INSTALL_DAILY_LIMIT=60`
- `PER_IP_DAILY_LIMIT` — three times the install limit unless set.
- burst limit: `10` requests/minute, applied per IP and per install id.

Known limits of this design:

- The install id is sent by the client and can be forged, so it is only a
  fairness hint. The IP and global limits are what bound abuse.
- KV has no atomic increment. Concurrent cache misses can exceed a daily
  limit by a few requests. Use a Durable Object counter if you need a hard
  cap.

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

Changes to `src/index.ts` or `wrangler.jsonc` only take effect after
`npm run deploy`.

The app already defaults to:

```text
https://repforge-workoutx-proxy.repforge-rafael.workers.dev
```

This URL is public and safe to embed. The WorkoutX key stays in Cloudflare.
The app reads it from `WorkoutXClient.DEFAULT_PROXY`.
