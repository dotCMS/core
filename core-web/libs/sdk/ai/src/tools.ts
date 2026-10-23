// Build entry for `@dotcms/ai/tools`. Rollup names each output bundle after its entry
// file, so every subpath needs an entry with its OWN basename — three `index.ts` entries
// collapsed into one `index` bundle. The code lives in ./tools/.
export * from './tools/index';
