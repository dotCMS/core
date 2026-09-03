import { InjectionToken, Signal } from '@angular/core';

/**
 * A filter value as every shared chip sees it.
 *
 * Deliberately narrow: a chip either holds one string or a list of them. `undefined` is not part of
 * this type — it is what {@link DotFilterFacade.getFilterValue} returns for a key that is not set,
 * and it is a different state from an empty array (see O2 in the facade contract).
 */
export type DotFilterValue = string | string[];

/**
 * What a chip reports when the request behind its options fails.
 *
 * A translation key rather than an `HttpErrorResponse`, and a small object rather than a bare
 * string, for the same reason the AssetPicker store reports errors this way: the chip's job is to
 * say *that* its options could not load, and the surface's job is to decide how to say it. Two
 * identical failures in a row stay two distinct values, so a surface re-reports the second.
 */
export interface DotFilterChipError {
    /** Translation key for the message the surface shows. */
    messageKey: string;
}

/**
 * The seam between a shared filter chip and whatever surface is rendering it.
 *
 * Chips read and write filters through this and never touch a store, which is what lets one chip
 * serve Content Drive (whose filters round-trip through the URL) and the AssetPicker dialog (which
 * has no URL at all) without knowing which it is on.
 *
 * **Values crossing this boundary are normalized.** Base types, for instance, are always base-type
 * *names* here; Content Drive's implementation translates to and from the numeric keys its URL
 * uses, and the picker's passes names straight through. Each surface absorbs its own encoding so no
 * chip has to know one exists.
 *
 * **Caller restrictions are not filters and never appear here** — a media-type restriction or a
 * pinned version state is part of what the surface *is*, not something the editor can change. A
 * chip whose options such a restriction narrows receives that bound as an input instead.
 *
 * Provide it at the component that owns the surface's store, never in `root`: each open picker
 * needs its own, the same way `DotAssetPickerStore` is provided per dialog.
 *
 * The behavioral obligations every implementation must satisfy (O1–O9) are specified in
 * `specs/37174-shared-picker-toolbar/contracts/filter-facade.contract.md` and are executable as a
 * shared conformance suite — see `./testing/filter-facade.conformance.ts`.
 */
export interface DotFilterFacade {
    /**
     * The normalized value of one filter, or `undefined` when it is not set.
     *
     * `undefined` and `[]` mean different things and both are load-bearing: the first is "no
     * filter", the second is "filtered to nothing selected".
     */
    getFilterValue(key: string): DotFilterValue | undefined;

    /**
     * Merges values into the filter set. Every write returns the result list to its first page and
     * discards any cursor bookmarks, so a stale cursor can never be applied to a narrower result.
     */
    patchFilters(patch: Record<string, DotFilterValue>): void;

    /**
     * Drops one filter entirely. The key is removed rather than set to `undefined`, so the filter
     * set never carries empty entries — and, on a surface that serializes them, no empty parameter
     * appears in the URL.
     */
    removeFilter(key: string): void;

    /**
     * Returns every filter to this surface's defaults — **not** to an empty set.
     *
     * Content Drive lands on its seeded language plus shared-assets-on; the picker lands on
     * whatever the caller seeded plus the same shared-assets default. Clearing to empty would
     * strand an editor in an unfiltered library.
     */
    clearFilters(): void;

    /**
     * Whether anything differs from this surface's defaults, which is what decides if clearing is
     * worth offering.
     *
     * Not the same question as "are there filters at all": the defaults are always present, so
     * counting keys would answer yes on a surface nobody has filtered.
     */
    readonly $hasNonDefaultFilters: Signal<boolean>;
}

export const DOT_FILTER_FACADE = new InjectionToken<DotFilterFacade>('DOT_FILTER_FACADE');
