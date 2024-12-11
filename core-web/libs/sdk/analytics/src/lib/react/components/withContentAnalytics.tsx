import React, { ReactNode, useCallback } from 'react';

import { DotcmsPageProps } from '@dotcms/react';

import { DotContentAnalyticsProvider } from './DotContentAnalyticsProvider';

import { DotContentAnalyticsConfig } from '../../shared/dot-content-analytics.model';

export interface PageProviderProps {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    readonly entity: any;
    readonly children: ReactNode;
}

/**
 * Wraps a given component with content analytics capabilities.
 * This HOC adds analytics tracking functionality to the wrapped component,
 * allowing it to automatically track page views and other analytics events.
 *
 * @param {React.ComponentType<DotcmsPageProps>} WrappedComponent - The component to be enhanced with analytics.
 * @param {DotContentAnalyticsConfig} config - Configuration for analytics, including API key, server URL and debug settings.
 * @returns {React.FunctionComponent<DotcmsPageProps>} A component that wraps the original component,
 *          adding analytics tracking based on the specified configuration.
 */
export const withContentAnalytics = (
    WrappedComponent: React.ComponentType<DotcmsPageProps>,
    config: DotContentAnalyticsConfig
) => {
    return useCallback(
        (props: DotcmsPageProps) => {
            return (
                <DotContentAnalyticsProvider config={config}>
                    <WrappedComponent {...props} />
                </DotContentAnalyticsProvider>
            );
        },
        [WrappedComponent]
    );
};
