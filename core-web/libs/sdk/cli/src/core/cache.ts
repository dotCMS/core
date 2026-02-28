/**
 * Schema and metadata caching for @dotcms/cli.
 * Caches content type schemas and language maps to `.dotcli/cache/`.
 */
import * as fs from 'node:fs';
import * as path from 'node:path';

import {
    CONTENT_TYPES_CACHE_DIR,
    DEFAULT_CACHE_TTL,
    type ContentTypeSchema,
    type LanguageMap
} from './types';

// ─── TTL Helpers ────────────────────────────────────────────────────────────

/**
 * Check if a cached entry is still fresh based on its `cachedAt` timestamp.
 */
function isFresh(cachedAt: string, ttl: number): boolean {
    const cachedTime = new Date(cachedAt).getTime();
    return Date.now() - cachedTime < ttl;
}

// ─── Content Type Cache ─────────────────────────────────────────────────────

function contentTypeCachePath(cacheDir: string, variable: string): string {
    return path.join(cacheDir, CONTENT_TYPES_CACHE_DIR, `${variable}.json`);
}

/**
 * Retrieve a cached content type schema, or null if missing/stale.
 */
export function getCachedContentType(
    cacheDir: string,
    variable: string,
    ttl: number = DEFAULT_CACHE_TTL
): ContentTypeSchema | null {
    const filePath = contentTypeCachePath(cacheDir, variable);

    if (!fs.existsSync(filePath)) {
        return null;
    }

    const raw = fs.readFileSync(filePath, 'utf-8');
    const schema = JSON.parse(raw) as ContentTypeSchema;

    if (!schema.cachedAt || !isFresh(schema.cachedAt, ttl)) {
        return null;
    }

    return schema;
}

/**
 * Write a content type schema to the cache, stamping it with `cachedAt`.
 */
export function cacheContentType(
    cacheDir: string,
    variable: string,
    schema: ContentTypeSchema
): void {
    const filePath = contentTypeCachePath(cacheDir, variable);
    const dir = path.dirname(filePath);

    if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
    }

    const stamped: ContentTypeSchema = {
        ...schema,
        cachedAt: new Date().toISOString()
    };

    fs.writeFileSync(filePath, JSON.stringify(stamped, null, 2), 'utf-8');
}

// ─── Language Cache ─────────────────────────────────────────────────────────

function languageCachePath(cacheDir: string): string {
    return path.join(cacheDir, 'languages.json');
}

interface CachedLanguages {
    cachedAt: string;
    languages: LanguageMap;
}

/**
 * Retrieve cached languages, or null if missing/stale.
 */
export function getCachedLanguages(
    cacheDir: string,
    ttl: number = DEFAULT_CACHE_TTL
): LanguageMap | null {
    const filePath = languageCachePath(cacheDir);

    if (!fs.existsSync(filePath)) {
        return null;
    }

    const raw = fs.readFileSync(filePath, 'utf-8');
    const cached = JSON.parse(raw) as CachedLanguages;

    if (!cached.cachedAt || !isFresh(cached.cachedAt, ttl)) {
        return null;
    }

    return cached.languages;
}

/**
 * Write languages to the cache.
 */
export function cacheLanguages(cacheDir: string, languages: LanguageMap): void {
    const filePath = languageCachePath(cacheDir);
    const dir = path.dirname(filePath);

    if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
    }

    const cached: CachedLanguages = {
        cachedAt: new Date().toISOString(),
        languages
    };

    fs.writeFileSync(filePath, JSON.stringify(cached, null, 2), 'utf-8');
}

// ─── Cache Clearing ─────────────────────────────────────────────────────────

/**
 * Clear cached data.
 * - If `type` is given, clears only that content type's cache file.
 * - Otherwise clears the entire cache directory.
 */
export function clearCache(cacheDir: string, type?: string): void {
    if (type) {
        const filePath = contentTypeCachePath(cacheDir, type);
        if (fs.existsSync(filePath)) {
            fs.unlinkSync(filePath);
        }
        return;
    }

    // Clear entire cache directory
    if (fs.existsSync(cacheDir)) {
        fs.rmSync(cacheDir, { recursive: true, force: true });
    }
}
