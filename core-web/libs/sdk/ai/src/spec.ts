// Build entry for `@dotcms/ai/spec`. Rollup names each output bundle after its entry
// file, so every subpath needs an entry with its OWN basename — three `index.ts` entries
// collapsed into one `index` bundle. The code lives in ./spec/.
export * from './spec/index';
