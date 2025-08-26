"use client";

import { ReactElement, useMemo } from 'react';

import { DotCMSAnalyticsConfig } from '../../dotAnalytics/shared/dot-content-analytics.model';
import { useRouterTracker } from '../hook/useRouterTracker';
import { initializeAnalytics } from '../internal/utils';

/**
 * Client bootstrapper for dotCMS Analytics in React/Next.
 * - No UI: initializes the analytics singleton from props or env config.
 * - If auto tracking is enabled, hooks into Next App Router to send page views.
 *
 * @param props.config - Optional analytics configuration. If provided, overrides env config.
 */
export interface DotContentAnalyticsProps {
    config: DotCMSAnalyticsConfig;
}

export function DotContentAnalytics({ config }: DotContentAnalyticsProps): ReactElement | null {
    const analytics = useMemo(() => initializeAnalytics(config), [config]);
    const debug = Boolean(config.debug);

    if (analytics) {
        useRouterTracker(analytics, debug);
    }

    return null;
}


