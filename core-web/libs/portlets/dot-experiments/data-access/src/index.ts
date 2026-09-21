export * from './lib/resolvers/dot-experiment-experiment.resolver';

export * from './lib/resolvers/dot-experiments-config-resolver';

/**
 * The Experiments panel's view state, and the signal that tells the portlet's screens they are
 * rendering inside the Universal Visual Editor (#37478).
 *
 * It lives in **this** lib rather than beside the screens it serves because the UVE shell
 * provides it, and `@dotcms/portlets/dot-experiments/portlet` is lazy-loaded — `edit-ema`'s own
 * routes reach it only through a dynamic `import()`, so a static import of it is forbidden. This
 * lib is the boundary the two already share statically, which makes it the store's home. It also
 * keeps the portlet lib fully lazy, which is what FR-037 is about.
 */
export * from './lib/dot-experiments-panel.store';
