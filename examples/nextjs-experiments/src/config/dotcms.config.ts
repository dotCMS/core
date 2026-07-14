import type { DotCMSPageRendererMode } from "@dotcms/types";

/**
 * Centralized, typed access to the dotCMS environment variables.
 *
 * Every other module imports from here instead of reading `process.env`
 * directly, so there is a single place to validate and document configuration.
 * All variables are `NEXT_PUBLIC_*` because the example uses a read-only API
 * token that is safe to expose to the browser (required by the UVE bridge).
 */
export const dotCMSHost = process.env.NEXT_PUBLIC_DOTCMS_HOST ?? "";
export const dotCMSAuthToken = process.env.NEXT_PUBLIC_DOTCMS_AUTH_TOKEN ?? "";
export const dotCMSSiteId = process.env.NEXT_PUBLIC_DOTCMS_SITE_ID ?? "";

export const dotCMSMode = (process.env.NEXT_PUBLIC_DOTCMS_MODE ??
  "production") as DotCMSPageRendererMode;

/** Verbose client logging in development, quiet otherwise. */
export const dotCMSLogLevel =
  process.env.NODE_ENV === "development" ? "verbose" : "default";

/** Index used by the AI search dialog. Create it in dotCMS before searching. */
export const aiSearchIndexName = "example-travel-lux";


/**
 * Centralized configuration for dotCMS A/B Experiments.
 *
 * Consumed by `withExperiments()` (from `@dotcms/experiments`) in
 * `src/views/Page.tsx`, which wraps `DotCMSLayoutBody` to serve experiment
 * variants and report results. The `apiKey` acts as the enable switch: when it
 * is empty the page renders without experiments.
 *
 * Environment variables:
 * - NEXT_PUBLIC_DOTCMS_HOST            → server (required)
 * - NEXT_PUBLIC_DOTCMS_EXPERIMENTS_KEY → apiKey (required to enable experiments)
 * - NEXT_PUBLIC_EXPERIMENTS_DEBUG      → debug (optional; "true" enables verbose logging)
 */
export const experimentsConfig = {
    server: process.env.NEXT_PUBLIC_DOTCMS_HOST ?? "",
    apiKey: process.env.NEXT_PUBLIC_DOTCMS_EXPERIMENTS_KEY ?? "",
    debug: process.env.NEXT_PUBLIC_EXPERIMENTS_DEBUG === "true",
};
