/**
 * `@dotcms/ai/spec` — the (committed, filtered) dotCMS OpenAPI spec, behind its own subpath
 * so importing the adapter doesn't drag in the ~hundreds-of-KB JSON. Opt-in for the search
 * use case. The spec is generated from a specific dotCMS instance; see the support matrix in
 * the README for the spec ↔ server-version coupling.
 */
export { getSpec } from './spec';
