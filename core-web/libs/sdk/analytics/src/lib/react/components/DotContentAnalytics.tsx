'use client';

import { ReactElement, Suspense, useMemo } from 'react';

import { DotCMSAnalyticsConfig } from '../../core/shared/dot-content-analytics.model';
import { useRouterTracker } from '../hook/useRouterTracker';
import { initializeAnalytics } from '../internal/utils';

/**
 * Internal component that uses Next.js hooks requiring Suspense boundary
 */
function DotContentAnalyticsTracker({
    config
}: {
    config: DotCMSAnalyticsConfig;
}): ReactElement | null {
    const analytics = useMemo(() => initializeAnalytics(config), [config]);
    const debug = Boolean(config.debug);

    if (analytics) {
        useRouterTracker(analytics, debug);
    }

    return null;
}

/**
 * Client bootstrapper for dotCMS Analytics in React/Next.
 * - No UI: initializes the analytics singleton from props or env config.
 * - If auto tracking is enabled, hooks into Next App Router to send page views.
 * - Automatically wraps in Suspense boundary for Next.js App Router compatibility.
 *
 * @param props.config - Optional analytics configuration. If provided, overrides env config.
 */
export interface DotContentAnalyticsProps {
    config: DotCMSAnalyticsConfig;
}

export function DotContentAnalytics({ config }: DotContentAnalyticsProps): ReactElement | null {
    return (
        <Suspense fallback={null}>
            <DotContentAnalyticsTracker config={config} />
        </Suspense>
    );
}
