import type { DotCMSEventsConfig } from "@dotcms/events";
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
 * Configuration for `dotEvents.init()` from `@dotcms/events`, called once in
 * `src/instrumentation-client.ts`. Experiments are on by default: the SDK asks dotCMS
 * for the visitor's variant and redirects to it. Impressions and clicks are opt-in; this
 * example turns both on.
 *
 * Environment variables:
 * - NEXT_PUBLIC_DOTCMS_HOST      → dotcmsUrl (required)
 * - NEXT_PUBLIC_DOTCMS_SITE_AUTH → siteAuth, from the Content Analytics app (required)
 * - NEXT_PUBLIC_EVENTS_DEBUG     → debug (optional; "true" logs what the SDK does)
 */
export const eventsConfig: DotCMSEventsConfig = {
    dotcmsUrl: process.env.NEXT_PUBLIC_DOTCMS_HOST ?? "",
    siteAuth: process.env.NEXT_PUBLIC_DOTCMS_SITE_AUTH ?? "",
    impressions: true,
    clicks: true,
    debug: process.env.NEXT_PUBLIC_EVENTS_DEBUG === "true",
};
