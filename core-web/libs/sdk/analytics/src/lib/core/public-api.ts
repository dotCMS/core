/**
 * Framework-neutral public API for `@dotcms/analytics`.
 *
 * This is the package root (`@dotcms/analytics`). It contains no React and no Next.js: the
 * React bindings — `DotContentAnalytics` and `useContentAnalytics`, which depend on
 * `react` and `next/navigation` — live behind the explicit `@dotcms/analytics/react`
 * subpath so a Vue, Angular, Astro or plain-JS consumer never pulls them in.
 */
export { initializeContentAnalytics } from './dot-analytics.content';

export { getAnalyticsConfig } from './shared/utils/dot-analytics.utils';

export type {
    DotCMSAnalytics,
    DotCMSAnalyticsConfig,
    DotCMSAnalyticsParams,
    ContentletData,
    ImpressionConfig,
    QueueConfig
} from './shared/models';

export type {
    DotCMSAnalyticsEventContext,
    DotCMSEventPageData,
    DotCMSImpressionEventData
} from './shared/models';
