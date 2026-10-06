/**
 * The order every surface renders its filter chips in.
 *
 * A shared constant rather than a per-toolbar decision, because two toolbars that offer the same
 * chips in different orders cost an editor a moment of re-orientation on every switch between
 * them. A surface may **omit** chips; it may not reorder them.
 *
 * `dot-filter-bar` does not read this to lay anything out — chips arrive by content projection, so
 * the rendered order is the surface's template order. What enforces the rule is a test per
 * toolbar asserting that the rendered `data-filter-chip` sequence is a *subsequence* of this list.
 * Machinery that could enforce it at compile time (a registry plus dynamic component outlets) would
 * also stop Content Drive projecting its portlet-local Workflow chip, which the shared library
 * cannot import.
 *
 * Why this order: `sharedAssets` leads because it scopes *which* assets are in play at all, so it
 * reads as a precondition for the filters that narrow within them. `workflow` and `status` sit
 * adjacent because both ask where content is in its lifecycle, and `workflow` derives its scheme
 * list from the content-type selection so it must follow `contentType`. `fieldFilters` trails
 * because its chips are dynamic and come and go.
 *
 * `goal`, `schedule` and `createdBy` join for the Experiments listing (#37307), which is the first
 * surface here that is not browsing content. They sit after `status` — which that listing reuses
 * for an experiment's own lifecycle, the same question the slot already names — and before
 * `language`, reading as what the experiment measures, when it runs, and who made it. Their
 * arrival is what turns this list from "the order for browsing content" into "the order for every
 * surface"; the rule is a subsequence, so no existing toolbar changes.
 */
export const DOT_CANONICAL_FILTER_ORDER = [
    'sharedAssets',
    'contentType',
    'workflow',
    'status',
    'goal',
    'schedule',
    'createdBy',
    'language',
    'fieldFilters'
] as const;

/**
 * Identifies one filter chip. Each chip carries its id as `data-filter-chip`, which is what the
 * per-toolbar ordering test reads.
 */
export type DotFilterChipId = (typeof DOT_CANONICAL_FILTER_ORDER)[number];

/**
 * Whether `chips` appear in canonical order, allowing omissions.
 *
 * Exported for the toolbar specs of every consuming surface — the rule is one line, and the point
 * of sharing the assertion is that two surfaces cannot disagree about what "in order" means.
 *
 * @param chips The rendered chip ids, in the order they appear.
 * @return True when `chips` is a subsequence of {@link DOT_CANONICAL_FILTER_ORDER}.
 */
export function isCanonicalChipOrder(chips: readonly string[]): boolean {
    let cursor = 0;

    for (const chip of chips) {
        const found = DOT_CANONICAL_FILTER_ORDER.indexOf(chip as DotFilterChipId, cursor);

        if (found === -1) {
            return false;
        }

        cursor = found + 1;
    }

    return true;
}
