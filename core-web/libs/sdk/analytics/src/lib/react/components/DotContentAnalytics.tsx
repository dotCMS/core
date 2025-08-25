"use client";

import { ReactElement, useMemo } from 'react';

import { useRouterTracker } from '../hook/useRouterTracker';
import { getAnalyticsInstance, getCachedAnalyticsConfig } from '../internal/utils';

/**
 * Client bootstrapper for dotCMS Analytics in React/Next.
 * - No UI: reads env config and initializes the analytics singleton.
 * - If auto tracking is enabled via env, hooks into Next App Router to send page views.
 */
export function DotContentAnalytics(): ReactElement | null {
    const analytics = useMemo(() => getAnalyticsInstance(), []);
    const debug = Boolean(getCachedAnalyticsConfig()?.debug);

    if (analytics) {
        useRouterTracker(analytics, debug);
    }

    return null;
}


