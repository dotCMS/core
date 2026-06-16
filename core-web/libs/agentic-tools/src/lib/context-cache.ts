import { type DotCMSContext, loadDotCMSContext } from './context';

import type { Adapter } from './types';

const DEFAULT_TTL_MS = 5 * 60 * 1000;

interface CacheEntry {
    context: DotCMSContext;
    expiresAt: number;
}

interface CacheOptions {
    ttlMs?: number;
    onError?: (label: string, error: unknown) => void;
    now?: () => number;
}

/**
 * Session-scoped cache for dotCMS instance context.
 * - TTL-based expiry (default 5 minutes)
 * - In-flight dedup so concurrent loads for the same session run once
 * - In-memory only; no persistence
 */
export class ContextCache {
    private cache = new Map<string, CacheEntry>();
    private inFlight = new Map<string, Promise<DotCMSContext>>();
    private ttlMs: number;
    private onError?: (label: string, error: unknown) => void;
    private now: () => number;

    constructor(options: CacheOptions = {}) {
        this.ttlMs = options.ttlMs ?? DEFAULT_TTL_MS;
        this.onError = options.onError;
        this.now = options.now ?? (() => Date.now());
    }

    async get(sessionId: string, apiAdapter: Adapter): Promise<DotCMSContext> {
        const cached = this.cache.get(sessionId);
        if (cached && cached.expiresAt > this.now()) {
            return cached.context;
        }

        const existing = this.inFlight.get(sessionId);
        if (existing) return existing;

        const startedAt = this.now();
        const promise = loadDotCMSContext(apiAdapter, this.onError)
            .then((context) => {
                this.cache.set(sessionId, {
                    context,
                    expiresAt: this.now() + this.ttlMs
                });
                if (process.env.DEBUG) {
                    console.error(
                        `[context] loaded session=${sessionId} took=${this.now() - startedAt}ms ` +
                            `contentTypes=${context.contentTypes.length} ` +
                            `sites=${context.sites.length} ` +
                            `languages=${context.languages.length} ` +
                            `currentUser=${context.currentUser?.email ?? 'null'}`
                    );
                }
                return context;
            })
            .finally(() => {
                this.inFlight.delete(sessionId);
            });

        this.inFlight.set(sessionId, promise);
        return promise;
    }

    invalidate(sessionId: string): void {
        this.cache.delete(sessionId);
    }

    clear(): void {
        this.cache.clear();
        this.inFlight.clear();
    }
}

let sharedCache: ContextCache | null = null;

export function getSharedContextCache(options?: CacheOptions): ContextCache {
    if (!sharedCache) {
        sharedCache = new ContextCache(options);
    }
    return sharedCache;
}
