/**
 * Filter key the shared-assets toggle writes.
 *
 * Shared verbatim with Content Drive, which also encodes it into the URL — the encode/decode pair,
 * the back/forward guard and the legacy-editor `CD_` round-trip all key off this exact string, so
 * it is defined once here and re-exported by the portlet rather than spelled twice.
 */
export const SHARED_ASSETS_FILTER_KEY = 'sharedAssets';

/** Shows SYSTEM_HOST (shared) assets. The default on both surfaces. */
export const SHARED_ASSETS_ENABLED_VALUE = 'true';

/** Hides SYSTEM_HOST (shared) assets. The only value that turns the filter off. */
export const SHARED_ASSETS_DISABLED_VALUE = 'false';
