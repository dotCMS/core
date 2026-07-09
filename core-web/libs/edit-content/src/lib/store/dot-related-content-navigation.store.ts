import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { EMPTY } from 'rxjs';

import { computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';

import { filter, map } from 'rxjs/operators';

/**
 * A single step in the related-content navigation trail.
 */
export interface DotRelatedContentCrumb {
    /** Inode used as the `:id` route param to load this content. */
    inode: string;
    /** Content title shown as the breadcrumb label. */
    title: string;
}

/** Query param that carries the navigation trail (comma-separated inodes). */
export const RELATED_TRAIL_PARAM = 'rc';

interface RelatedContentNavigationState {
    /**
     * Best-effort cache of `inode -> title`, populated as contents are visited.
     * Survives in-session navigation (back/forward) since this store is a root
     * singleton; lost on a hard refresh (crumbs then fall back to a placeholder
     * until revisited). Kept as a Record for JSON-serializability.
     */
    titleCache: Record<string, string>;

    /**
     * In-memory trail used when the editor cannot rely on the URL (dialog/overlay
     * presentation). `null` means "derive the trail from the URL" — the full-screen
     * default, so nothing changes there. A non-null array (even empty) makes the
     * in-memory trail the source of truth. Set by the DialogEditContentHost while a
     * dialog is open and cleared when it closes.
     */
    inMemoryTrail: string[] | null;
}

const initialState: RelatedContentNavigationState = {
    titleCache: {},
    inMemoryTrail: null
};

/**
 * Placeholder label for a crumb whose title is not (yet) cached — only happens
 * after a hard refresh, until that content is revisited.
 */
const MISSING_TITLE = '…';

/**
 * Drives related-content navigation for the full-screen edit-content editor and
 * exposes the breadcrumb trail.
 *
 * **The URL is the source of truth.** The trail lives in the `rc` query param
 * (comma-separated inodes, current last); this store derives `trail`/`trailInodes`
 * reactively from the router URL. That gives browser back/forward and refresh for
 * free — there is no in-memory copy of the trail to keep in sync with the route.
 *
 * Provided in **root** so it survives the destroy/recreate the editor goes
 * through on every `:id → :id` navigation (the `content` route opts out of route
 * reuse).
 *
 * Navigation always goes through the Angular router, so the existing
 * `unsavedChangesGuard` prompts on unsaved changes for free.
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

        const urlTrailInodes = computed(() => {
            // `parseUrl` always returns a UrlTree in production; the guards keep
            // the store resilient under partially-mocked routers in tests.
            const rc = router.parseUrl(url() ?? '')?.queryParams?.[RELATED_TRAIL_PARAM];

            return rc ? rc.split(',').filter(Boolean) : [];
        });

        // The in-memory trail wins when set (dialog/overlay); otherwise the URL is
        // the source of truth (full-screen — unchanged behavior).
        const trailInodes = computed(() => store.inMemoryTrail() ?? urlTrailInodes());

        return {
            /** The trail as bare inodes, from the in-memory trail or the `rc` param. */
            trailInodes,
            /** The trail as crumbs (inode + cached title) for the breadcrumb. */
            trail: computed<DotRelatedContentCrumb[]>(() => {
                const cache = store.titleCache();

                return trailInodes().map((inode) => ({
                    inode,
                    title: cache[inode] ?? MISSING_TITLE
                }));
            })
        };
    }),
    withMethods((store) => {
        /** Caches a title so the breadcrumb can label a crumb by its inode. */
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
             * vs in-place reload). Registers both titles and seeds the trail with
             * the current content as the first crumb when starting fresh, so the
             * banner shows where navigation began (`origin › target`).
             *
             * @returns The next trail as bare inodes.
             */
            appendToTrail(
                current: DotRelatedContentCrumb,
                target: DotRelatedContentCrumb
            ): string[] {
                registerTitle(current.inode, current.title);
                registerTitle(target.inode, target.title);

                const currentTrail = store.trailInodes();

                return currentTrail.length === 0
                    ? [current.inode, target.inode]
                    : [...currentTrail, target.inode];
            },

            /**
             * Sets (or clears) the in-memory trail. Passing `null` reverts to the
             * URL-driven trail (full-screen). Used by the dialog host to keep the
             * trail without touching the URL, and to reset it on close.
             */
            setInMemoryTrail(trail: string[] | null): void {
                patchState(store, { inMemoryTrail: trail });
            },

            /**
             * When the current content is saved it gets a NEW inode; the trail's
             * last crumb (the content being edited) then points at the stale
             * inode. This returns the `rc` value with that last crumb repointed to
             * `newInode`, or `null` when there is no active trail. The caller
             * (post-save navigation) uses it so the breadcrumb stays consistent
             * with the freshly-saved version.
             *
             * Note: navigation to earlier crumbs is driven declaratively by
             * `routerLink` on the breadcrumb items (built in the layout), so there
             * is no imperative "navigate to crumb" method here.
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
    })
);
