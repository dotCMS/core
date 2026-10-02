/**
 * `dom-autoscroller` ships no type declarations, and there is no `@types/dom-autoscroller`.
 *
 * An ambient `declare module` rather than a `paths` entry in `tsconfig.base.json`: a `paths` entry
 * points the module id at this `.d.ts`, and Vite resolves the same mapping at runtime, so the
 * import would evaluate to an empty module under Vitest and in a Vite build. The triple-slash
 * reference in `content-type-fields-drop-zone.component.ts` pulls this file into the program of
 * every project that compiles those sources.
 *
 * Only what `ContentTypeFieldsDropZoneComponent.setUpDragulaScroll` passes is declared, and kept
 * deliberately narrow so a change in what we use from the package is still a compile error.
 */
declare module 'dom-autoscroller' {
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
    function autoScroll(
        elements: (Element | null)[],
        options?: AutoScrollOptions
    ): { destroy(): void };

    export default autoScroll;
}
