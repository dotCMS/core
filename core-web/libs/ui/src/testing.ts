/**
 * Test-only entry point for `@dotcms/ui`.
 *
 * Kept out of `index.ts` on purpose: what lives here calls Jest globals (`describe`, `it`,
 * `expect`), and re-exporting it from the production barrel drags those into every consumer's
 * `tsconfig.lib.json` typecheck — which is exactly what happened the first time. Reachable as
 * `@dotcms/ui/testing` (see the path mapping in `tsconfig.base.json`) and excluded from this
 * library's production compilation.
 */
export * from './lib/components/dot-filter-bar/testing/filter-facade.conformance';
