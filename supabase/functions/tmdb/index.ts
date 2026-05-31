import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const TMDB_API_KEY = Deno.env.get('TMDB_API_KEY')!
const TMDB_BASE = 'https://api.themoviedb.org/3'

const supabase = createClient(
  Deno.env.get('SUPABASE_URL')!,
  Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
)

function getTtlMs(path: string): number {
  if (path.startsWith('search/'))    return 60 * 60 * 1000        // 1h
  if (path.startsWith('trending/'))  return 6 * 60 * 60 * 1000   // 6h
  if (path.startsWith('movie/now_playing')) return 60 * 60 * 1000 // 1h
  return 24 * 60 * 60 * 1000                                      // 24h: details, credits
}

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response(null, {
      headers: {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Headers': 'authorization, content-type',
      },
    })
  }

  const url = new URL(req.url)
  const pathMatch = url.pathname.match(/\/tmdb\/(.+)/)
  if (!pathMatch) {
    return json({ error: 'Invalid path' }, 400)
  }

  const tmdbPath = pathMatch[1]
  const params = url.searchParams
  const cacheKey = `${tmdbPath}?${params.toString()}`
  const now = Date.now()

  // Cache lookup
  const { data: cached } = await supabase
    .from('tmdb_cache')
    .select('payload, cached_at')
    .eq('cache_key', cacheKey)
    .single()

  if (cached && now - cached.cached_at < getTtlMs(tmdbPath)) {
    return json(cached.payload, 200, 'HIT')
  }

  // Proxy to TMDB
  const tmdbParams = new URLSearchParams(params)
  tmdbParams.set('api_key', TMDB_API_KEY)

  try {
    const res = await fetch(`${TMDB_BASE}/${tmdbPath}?${tmdbParams}`)
    if (!res.ok) return json({ error: 'TMDB error' }, res.status)

    const data = await res.json()

    await supabase.from('tmdb_cache').upsert({
      cache_key: cacheKey,
      payload: data,
      cached_at: now,
    })

    return json(data, 200, 'MISS')
  } catch (e) {
    return json({ error: 'Proxy error', message: (e as Error).message }, 500)
  }
})

function json(data: unknown, status: number, cache?: string): Response {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (cache) headers['X-Cache'] = cache
  return new Response(JSON.stringify(data), { status, headers })
}
