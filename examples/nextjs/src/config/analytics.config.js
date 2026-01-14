/**
 * Centralized analytics configuration for dotCMS Content Analytics
 *
 * This configuration is used by:
 * - DotContentAnalytics provider in layout.js
 * - useContentAnalytics() hook when used standalone (optional)
 *
 * Environment variables required:
 * - NEXT_PUBLIC_DOTCMS_ANALYTICS_SITE_KEY
 * - NEXT_PUBLIC_DOTCMS_ANALYTICS_HOST
 * - NEXT_PUBLIC_DOTCMS_ANALYTICS_DEBUG (optional)
 */
export const analyticsConfig = {
    siteAuth: process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_SITE_KEY,
    server: process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_HOST,
    autoPageView: true, // Automatically track page views on route changes
    impressions: true,
    clicks: true,
    debug: process.env.NEXT_PUBLIC_DOTCMS_ANALYTICS_DEBUG === "true",
    queue: {
        eventBatchSize: 15, // Send when 15 events are queued
        flushInterval: 5000, // Or send every 5 seconds (ms)
    },
};
