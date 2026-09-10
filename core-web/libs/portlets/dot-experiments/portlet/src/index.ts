export * from './lib/old/lib.routes';
export * from './lib/lib.routes';

/**
 * The Experiments panel, rendered inside the Universal Visual Editor (#37478).
 *
 * Exported so the UVE shell can `@defer` it: one deferred symbol keeps this whole lib — the three
 * screens, their stores and chart.js — out of what the editor loads before the panel is first
 * opened (FR-037, SC-006). The panel's *store* is not here; it lives in the sibling data-access
 * lib, because a static import of this one is forbidden (it is only ever reached lazily).
 */
export * from './lib/dot-experiments-panel/dot-experiments-panel.component';
