import { ReactElement, ReactNode, useMemo } from 'react';

import { initializeContentAnalytics } from '../../dotAnalytics/dot-content-analytics';
import {
    DotCMSAnalytics,
    DotCMSAnalyticsConfig
} from '../../dotAnalytics/shared/dot-content-analytics.model';
import DotContentAnalyticsContext from '../contexts/DotContentAnalyticsContext';
import { useRouterTracker } from '../hook/useRouterTracker';

interface DotContentAnalyticsProviderProps {
    children?: ReactNode;
    config: DotCMSAnalyticsConfig;
}

/**
 * Provider component that initializes and manages DotCMSAnalytics instance.
 * It makes the analytics functionality available to all child components through React Context.
 * This component is responsible for:
 * - Initializing the DotCMSAnalytics singleton instance with the provided config
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
    const analytics: DotCMSAnalytics | null = useMemo(
        () => initializeContentAnalytics(config),
        [config]
    );

    if (config.autoPageView !== false && analytics) {
        useRouterTracker(analytics);
    }

    return (
        <DotContentAnalyticsContext.Provider value={analytics}>
            {children}
        </DotContentAnalyticsContext.Provider>
    );
};
