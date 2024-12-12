import { ReactElement, ReactNode, useEffect, useMemo } from 'react';

import { DotContentAnalytics } from '../../dotAnalytics/dot-content-analytics';
import { DotContentAnalyticsConfig } from '../../dotAnalytics/shared/dot-content-analytics.model';
import DotContentAnalyticsContext from '../contexts/DotContentAnalyticsContext';
import { useRouteTracker } from '../hook/useRouterTracker';

interface DotContentAnalyticsProviderProps {
    children?: ReactNode;
    config: DotContentAnalyticsConfig;
}

/**
 * Provider component that initializes and manages DotContentAnalytics instance.
 * It makes the analytics functionality available to all child components through React Context.
 * This component is responsible for:
 * - Initializing the DotContentAnalytics singleton instance with the provided config
 * - Making the instance accessible via useContext hook to child components
 * - Managing the lifecycle of the analytics instance
 *
 * @param {DotContentAnalyticsProviderProps} props - Configuration and children to render
 * @returns {ReactElement} Provider component that enables analytics tracking
 */
export const DotContentAnalyticsProvider = ({
    children,
    config
}: DotContentAnalyticsProviderProps): ReactElement => {
    const instance = useMemo(() => DotContentAnalytics.getInstance(config), [config]);

    useEffect(() => {
        instance.ready().catch((err) => {
            console.error('Error initializing analytics:', err);
        });
    }, [instance]);

    useRouteTracker(instance);

    return (
        <DotContentAnalyticsContext.Provider value={instance}>
            {children}
        </DotContentAnalyticsContext.Provider>
    );
};
