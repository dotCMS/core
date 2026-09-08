/**
 * `dom-autoscroller` ships no type declarations, and there is no `@types/dom-autoscroller`.
 *
 * Wired up through `paths` in `tsconfig.base.json` rather than as an ambient `declare module` in
 * the one component that imports it, for the same reason as `jstat`: an ambient declaration is only
 * visible to the project whose `include` reaches it, so a strict consumer compiling those sources
 * through a path mapping would still see the import as an implicit `any`. `paths` affects type
 * resolution only; the bundler still resolves the real package at runtime.
 *
 * Only what `ContentTypeFieldsDropZoneComponent.setUpDragulaScroll` passes is declared, and kept
 * deliberately narrow so a change in what we use from the package is still a compile error.
 */

interface AutoScrollOptions {
    /** Distance from the container edge, in pixels, at which scrolling starts. */
    margin?: number;
    /** Pixels per frame at the fastest point. */
    maxSpeed?: number;
    /** Whether to keep scrolling while the pointer is outside the container. */
    scrollWhenOutside?: boolean;
    /**
     * Called each frame to decide whether to scroll. `this` carries the scroller, whose `down`
     * says whether a pointer is held.
     */
    autoScroll?: (this: { down: boolean }) => boolean;
}

/** Attaches an auto-scroller to the given containers. */
declare function autoScroll(
    elements: (Element | null)[],
    options?: AutoScrollOptions
): { destroy(): void };

export default autoScroll;
