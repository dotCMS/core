import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { EMPTY } from 'rxjs';

import { computed, effect, inject, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';

import { filter, map } from 'rxjs/operators';

/**
 * A single step in the related-content navigation trail.
 */
export interface DotRelatedContentCrumb {
    /** Inode of the content this crumb points to. */
    inode: string;
    /** Content title shown as the breadcrumb label. */
    title: string;
}

/** Query param that carries the navigation trail (comma-separated inodes). */
export const RELATED_TRAIL_PARAM = 'rc';

interface RelatedContentNavigationState {
    /**
     * Best-effort cache of `inode -> title`, populated as contents are visited.
     * Persisted to `sessionStorage` so it survives a hard refresh (the trail lives
     * in the URL, but only the current content reloads, so without this the earlier
     * crumbs would fall back to a placeholder). Scoped to the tab/session: a fresh
     * tab or a shared link still starts empty until each content is visited. Kept as
     * a Record for JSON-serializability.
     */
    titleCache: Record<string, string>;
}

/** sessionStorage key for the persisted {@link RelatedContentNavigationState.titleCache}. */
const TITLE_CACHE_STORAGE_KEY = 'dot-related-content-title-cache';

/**
 * Reads the persisted title cache from sessionStorage. Returns an empty cache when
 * storage is unavailable (SSR/tests) or the payload is missing/corrupt — the cache
 * is best-effort, so a read failure must never break store construction.
 */
function readTitleCache(): Record<string, string> {
    try {
        const raw = sessionStorage.getItem(TITLE_CACHE_STORAGE_KEY);
        const parsed = raw ? JSON.parse(raw) : null;

        return parsed && typeof parsed === 'object' ? (parsed as Record<string, string>) : {};
    } catch {
        return {};
    }
}

/** Persists the title cache to sessionStorage; silently ignores storage failures. */
function writeTitleCache(cache: Record<string, string>): void {
    try {
        sessionStorage.setItem(TITLE_CACHE_STORAGE_KEY, JSON.stringify(cache));
    } catch {
        // best-effort: quota errors / disabled storage must not break navigation.
    }
}

const initialState: RelatedContentNavigationState = {
    titleCache: {}
};

/**
 * Placeholder label for a crumb whose title is not (yet) cached — happens for a
 * content that has never been visited in this tab/session (e.g. a shared link).
 */
const MISSING_TITLE = '…';

/**
 * Maps trail inodes to breadcrumb crumbs, labeling each from the title cache (or a
 * placeholder when uncached). Shared by the URL-driven trail (this store) and the
 * dialog host's per-instance in-memory trail.
 */
export function toRelatedContentCrumbs(
    inodes: string[],
    titleCache: Record<string, string>
): DotRelatedContentCrumb[] {
    return inodes.map((inode) => ({ inode, title: titleCache[inode] ?? MISSING_TITLE }));
}

/**
 * Holds the related-content navigation trail and exposes it as breadcrumb crumbs.
 * It does not navigate — that is the {@link EditContentHost}'s job; this store only
 * owns the trail data and title cache.
 *
 * **Trail ownership differs by presentation:**
 * - Full-screen ({@link RouterEditContentHost}): the URL is the source of truth. The
 *   trail lives in the `rc` query param (comma-separated inodes, current last) and is
 *   derived reactively from the router URL, so browser back/forward and refresh work
 *   for free. That URL-derived trail is what this store exposes.
 * - Dialog/overlay ({@link OverlayEditContentHost}): there is no URL, so each overlay
 *   keeps its own per-instance in-memory trail in the host — NOT in this store — so an
 *   open overlay never disturbs the trail of the full-screen editor behind it.
 *
 * Provided in **root** so the title cache is shared across both hosts (and survives a
 * hard refresh via sessionStorage), reachable regardless of whether the editor
 * component is reused or recreated.
 */
export const DotRelatedContentNavigationStore = signalStore(
    { providedIn: 'root' },
    withState<RelatedContentNavigationState>(initialState),
    withComputed((store) => {
        const router = inject(Router);

        // Reactive snapshot of the current URL (updates on every navigation).
        // Fall back to EMPTY when `events` is absent so partially-mocked routers
        // in tests do not break store construction.
        const url = toSignal(
            (router.events ?? EMPTY).pipe(
                filter((event) => event instanceof NavigationEnd),
                map(() => router.url)
            ),
            { initialValue: router.url ?? '' }
        );

        const trailInodes = computed(() => {
            // `parseUrl` always returns a UrlTree in production; the guards keep
            // the store resilient under partially-mocked routers in tests.
            const rc = router.parseUrl(url() ?? '')?.queryParams?.[RELATED_TRAIL_PARAM];

            // Angular's `Params` types `rc` as `any`; a repeated query param would
            // arrive as a string[]. Only split when it is actually a string.
            return typeof rc === 'string' ? rc.split(',').filter(Boolean) : [];
        });

        return {
            /** The trail as bare inodes, derived from the `rc` query param. */
            trailInodes,
            /** The trail as crumbs (inode + cached title) for the breadcrumb. */
            trail: computed<DotRelatedContentCrumb[]>(() =>
                toRelatedContentCrumbs(trailInodes(), store.titleCache())
            )
        };
    }),
    withMethods((store) => {
        /**
         * Caches a title so the breadcrumb can label a crumb by its inode.
         *
         * TODO: the cache has no cap/eviction, so it grows as more related contents
         * are visited. Bounded in practice — it lives in sessionStorage (per-tab,
         * cleared on tab close) and each entry is a short inode → title string, so
         * reaching the storage quota needs an unrealistic number of distinct contents
         * in one session. If a heavy-navigation use case ever appears, bound it here
         * (e.g. keep the last N entries on write).
         */
        const registerTitle = (inode: string | undefined, title: string | undefined): void => {
            if (!inode || !title) {
                return;
            }

            patchState(store, { titleCache: { ...store.titleCache(), [inode]: title } });
        };

        return {
            registerTitle,

            /**
             * Computes the trail after navigating into a related content, without
             * performing any navigation — the host decides how to move (router URL
             * vs in-place reload) and owns its own current trail (URL-derived for
             * full-screen, in-memory for the dialog), which it passes in as
             * `currentTrail`. Registers both titles and seeds the trail with the
             * current content as the first crumb when starting fresh, so the banner
             * shows where navigation began (`origin › target`).
             *
             * @returns The next trail as bare inodes.
             */
            appendToTrail(
                currentTrail: string[],
                current: DotRelatedContentCrumb,
                target: DotRelatedContentCrumb
            ): string[] {
                registerTitle(current.inode, current.title);
                registerTitle(target.inode, target.title);

                return currentTrail.length === 0
                    ? [current.inode, target.inode]
                    : [...currentTrail, target.inode];
            },

            /**
             * When the current content is saved it gets a NEW inode; the trail's
             * last crumb (the content being edited) then points at the stale
             * inode. This returns the `rc` value with that last crumb repointed to
             * `newInode`, or `null` when there is no active trail. The caller
             * (post-save navigation) uses it so the breadcrumb stays consistent
             * with the freshly-saved version.
             *
             * @param newInode The inode of the just-saved content version.
             */
            buildTrailForSavedInode(newInode: string): string | null {
                const trail = store.trailInodes();
                if (trail.length < 2) {
                    return null;
                }

                return [...trail.slice(0, -1), newInode].join(',');
            }
        };
    }),
    withHooks({
        onInit(store) {
            // Hydrate the cache from the previous page load so a hard refresh keeps
            // labeling the earlier crumbs (their inodes come back from the URL, but
            // only the current content reloads and re-registers its own title).
            patchState(store, { titleCache: readTitleCache() });

            // Persist on every change so the next refresh starts warm.
            effect(() => {
                const cache = store.titleCache();
                untracked(() => writeTitleCache(cache));
            });
        }
    })
);
