import { HttpError, type DotCMSRuntime } from '@dotcms/ai/runtime';

import { errorMessage } from './runtime';

/**
 * Resolution of the instance references a caller can name: sites and languages.
 *
 * ONE RULE runs through this file: **the session context cache is a fast path, never a
 * gate.** `loadContext()` loads sites/languages/content-types once per session, and every
 * loader inside it catches its own failure and returns an empty array (see
 * `sdk/ai/src/adapter/context.ts`). So a transient 500, an expired token, or a permission
 * quirk during that one load leaves the cache permanently empty for the session — and the
 * tools that gated on it then rejected every call with "Available sites: (none found)",
 * including for the default site and for a caller passing a correct site IDENTIFIER.
 *
 * That failure mode is worse than the problem the cache was solving. A cached list is a
 * useful accelerator and a good source of candidate names for an error message; it is not
 * evidence that something does not exist. Every resolver here therefore tries the cache
 * first, falls back to asking dotCMS directly, and only fails when the instance itself says
 * no — at which point the message distinguishes "this does not exist" from "the session
 * context never loaded".
 */

/** A dotCMS identifier is a 36-char UUID; anything else a caller passes is a name. */
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/** A resolved site: the identifier the API needs, plus the hostname for manifests. */
export interface ResolvedSite {
    identifier: string;
    hostname: string;
}

function looksLikeIdentifier(value: string): boolean {
    return UUID_PATTERN.test(value.trim());
}

function asRecord(value: unknown): Record<string, unknown> | undefined {
    return value && typeof value === 'object' ? (value as Record<string, unknown>) : undefined;
}

function entityOf(response: unknown): unknown {
    const record = asRecord(response);

    return record && 'entity' in record ? record['entity'] : response;
}

function readString(record: Record<string, unknown> | undefined, key: string): string | undefined {
    const value = record?.[key];

    return typeof value === 'string' && value ? value : undefined;
}

/** Project a raw site payload onto {@link ResolvedSite}, or undefined if it has no identifier. */
function toResolvedSite(raw: unknown): ResolvedSite | undefined {
    const record = asRecord(raw);
    const identifier = readString(record, 'identifier');
    if (!identifier) {
        return undefined;
    }

    return {
        identifier,
        hostname:
            readString(record, 'hostname') ??
            readString(record, 'hostName') ??
            readString(record, 'siteName') ??
            identifier
    };
}

/**
 * Resolve a site (hostname OR identifier) to its identifier + hostname.
 *
 * Order is deliberate — cheapest and least fallible first:
 *   1. the session cache, when it actually has sites;
 *   2. a direct `GET /api/v1/site/{id}` when the caller gave something UUID-shaped;
 *   3. a filtered `GET /api/v1/site` lookup by hostname.
 *
 * Step 2 matters most for the reported failure: a caller who already knows the identifier
 * should never be blocked by a cache, because the identifier is precisely what the
 * downstream call needs and no lookup is required to use it.
 */
export async function resolveSite(dotcms: DotCMSRuntime, site: string): Promise<ResolvedSite> {
    const wanted = site.trim();
    if (!wanted) {
        throw new Error('Site must be a non-empty hostname or identifier.');
    }

    const cached = await siteFromCache(dotcms, wanted);
    if (cached) {
        return cached;
    }

    const live = await siteFromApi(dotcms, wanted);
    if (live) {
        return live;
    }

    throw new Error(await siteNotFoundMessage(dotcms, wanted));
}

async function siteFromCache(
    dotcms: DotCMSRuntime,
    wanted: string
): Promise<ResolvedSite | undefined> {
    const { sites } = await safeContext(dotcms);
    const match = sites.find(
        (entry) =>
            entry.identifier === wanted || entry.hostname?.toLowerCase() === wanted.toLowerCase()
    );

    return match ? { identifier: match.identifier, hostname: match.hostname } : undefined;
}

async function siteFromApi(
    dotcms: DotCMSRuntime,
    wanted: string
): Promise<ResolvedSite | undefined> {
    if (looksLikeIdentifier(wanted)) {
        try {
            const resolved = toResolvedSite(
                entityOf(
                    await dotcms.request({ path: `/api/v1/site/${encodeURIComponent(wanted)}` })
                )
            );
            if (resolved) {
                return resolved;
            }
        } catch (error) {
            // A 404 means this identifier genuinely does not exist — fall through to the
            // hostname search, which will also miss, and report not-found properly. Any other
            // failure is an instance problem and must not be reported as "site not found".
            if (!(error instanceof HttpError) || error.status !== 404) {
                throw error;
            }
        }
    }

    try {
        const raw = entityOf(
            await dotcms.request({
                path: '/api/v1/site',
                query: { filter: wanted, per_page: 50, page: 1 }
            })
        );
        const candidates = Array.isArray(raw) ? raw : [];

        // The endpoint filters by substring, so `demo` also returns `demo-backup`. Only an
        // exact hostname (or identifier) match may be accepted — silently picking a
        // near-miss would write content to the wrong site.
        for (const candidate of candidates) {
            const resolved = toResolvedSite(candidate);
            if (
                resolved &&
                (resolved.identifier === wanted ||
                    resolved.hostname.toLowerCase() === wanted.toLowerCase())
            ) {
                return resolved;
            }
        }
    } catch {
        // Fall through to the not-found message, which explains what was tried.
    }

    return undefined;
}

/** Explain a miss WITHOUT implying the instance has no sites when the cache simply failed. */
async function siteNotFoundMessage(dotcms: DotCMSRuntime, wanted: string): Promise<string> {
    const { sites } = await safeContext(dotcms);

    if (sites.length === 0) {
        return (
            `Site "${wanted}" could not be resolved, and this session's site list is empty — ` +
            `which usually means the one-time context load failed (a transient error or a ` +
            `permissions problem), NOT that the instance has no sites. A direct lookup was ` +
            `also tried and did not find it. Verify the site exists and that the configured ` +
            `token can read it; if the site is correct, reconnecting the MCP server reloads ` +
            `the context.`
        );
    }

    const available = sites.map((entry) => entry.hostname).join(', ');

    return (
        `Site "${wanted}" was not found (neither a known hostname nor a site identifier), ` +
        `and a direct lookup did not find it either. Available sites: ${available}.`
    );
}

/**
 * Resolve a language id against the instance.
 *
 * dotCMS silently falls back to its default language for an unknown id rather than
 * rejecting it, so an unrecognised id does not fail — it quietly writes to a DIFFERENT
 * language than the caller named while the manifest echoes the id they asked for. That is
 * worth catching, but ONLY when the language list actually loaded: with an empty list the
 * caller's explicit id is better evidence than our missing cache, so it is passed through.
 */
export async function resolveLanguageId(
    dotcms: DotCMSRuntime,
    languageId?: number
): Promise<number> {
    const languages = await loadLanguages(dotcms);

    if (languageId === undefined) {
        // The instance's own default, not a hardcoded 1 — on some instances it is not 1.
        return languages[0]?.id ?? 1;
    }

    if (languages.length === 0 || languages.some((language) => language.id === languageId)) {
        return languageId;
    }

    const available = languages
        .map((language) => `${language.id} (${language.isoCode})`)
        .join(', ');
    throw new Error(
        `languageId ${languageId} does not exist on this instance. dotCMS would silently fall ` +
            `back to the default language and write to the WRONG language rather than reject ` +
            `it, so this is refused up front. Available languages: ${available}.`
    );
}

/** Languages from the cache, falling back to a direct read when the cache is empty. */
async function loadLanguages(
    dotcms: DotCMSRuntime
): Promise<Array<{ id: number; isoCode: string }>> {
    const { languages } = await safeContext(dotcms);
    if (languages.length > 0) {
        return languages.map((language) => ({ id: language.id, isoCode: language.isoCode }));
    }

    try {
        const raw = entityOf(await dotcms.request({ path: '/api/v2/languages' }));

        return (Array.isArray(raw) ? raw : [])
            .map((item) => {
                const record = asRecord(item);
                const id = record?.['id'];

                return {
                    id: typeof id === 'number' ? id : Number(id) || 0,
                    isoCode: readString(record, 'isoCode') ?? ''
                };
            })
            .filter((language) => language.id > 0);
    } catch {
        // Unknown rather than empty — the caller's id is then taken at face value.
        return [];
    }
}

/**
 * `loadContext()` with its own failure absorbed.
 *
 * A context load failing must never be the reason a tool call dies: the context is an
 * accelerator, and every resolver here has a direct-lookup path that does not need it.
 */
async function safeContext(dotcms: DotCMSRuntime) {
    try {
        return await dotcms.loadContext();
    } catch (error) {
        console.error(`[context] load failed during resolution: ${errorMessage(error)}`);

        return { contentTypes: [], sites: [], languages: [], currentUser: null };
    }
}
