import type { DotCMSAnalyticsConfig } from "@dotcms/analytics";
import type { DotCMSPageRendererMode } from "@dotcms/types";

export const dotCMSHost = process.env.NEXT_PUBLIC_DOTCMS_HOST ?? "";
export const dotCMSMode = (process.env.NEXT_PUBLIC_DOTCMS_MODE ??
  "production") as DotCMSPageRendererMode;
export const debug = process.env.NEXT_PUBLIC_DOTCMS_DEBUG === "true";

/** Shared jsKey — used as siteAuth (analytics) and apiKey (experiments). */
const analyticsKey = process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_KEY ?? "";

export const analyticsConfig: DotCMSAnalyticsConfig = {
  siteAuth: analyticsKey,
  server: dotCMSHost,
  autoPageView: true,
  debug,
  impressions: true,
  clicks: true,
};

export const experimentsConfig = {
  apiKey: analyticsKey,
  server: dotCMSHost,
  debug,
} as const;
