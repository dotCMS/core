// Build entry for `@dotcms/ai/adapter`. Rollup names each output bundle after its entry
// file, so every subpath needs an entry with its OWN basename — three `index.ts` entries
// collapsed into one `index` bundle. The code lives in ./adapter/.
export * from './adapter/index';
