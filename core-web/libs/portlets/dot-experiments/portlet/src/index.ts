export * from './lib/old/lib.routes';
export * from './lib/lib.routes';

/**
 * The Experiments panel, rendered inside the Universal Visual Editor (#37478).
 *
 * Exported so the UVE shell can reach it through a dynamic `import()`: one lazily imported symbol
 * keeps this whole lib — the three screens, their stores and chart.js — out of what the editor
 * loads before the panel is first opened (FR-037, SC-006).
 *
 * `@defer` is not what does that, and cannot be. Nx marks this lib lazy-loaded, so
 * `@nx/enforce-module-boundaries` rejects any static import of it from `edit-ema`, and a `@defer`
 * block still needs the component in `imports:` — which is a static import, so the rule fires on
 * it too. The shell calls `import()` and `ViewContainerRef.createComponent` instead; that is what
 * `@defer` compiles to anyway, so the chunk boundary is the same one.
 *
 * The panel's *store* is not here for the same reason: the shell provides it, a provider is a
 * static import, and this lib is only ever reached lazily. It lives in the sibling data-access lib.
 */
export * from './lib/dot-experiments-panel/dot-experiments-panel.component';
