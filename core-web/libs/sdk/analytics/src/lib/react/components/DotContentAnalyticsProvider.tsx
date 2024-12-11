import { ReactElement, ReactNode, useEffect, useState } from 'react';

import { DotContentAnalytics } from '../../dot-content-analytics';
import { DotContentAnalyticsConfig } from '../../shared/dot-content-analytics.model';
import DotContentAnalyticsContext from '../contexts/DotContentAnalyticsContext';
import { useContentAnalytics } from '../hook/useContentAnalytics';

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
    const [instance, setInstance] = useState<DotContentAnalytics | null>(null);

    useContentAnalytics(instance);

    useEffect(() => {
        const dotContentAnalyticsInstance = DotContentAnalytics.getInstance(config);

        dotContentAnalyticsInstance.ready().then(() => {
            setInstance(dotContentAnalyticsInstance);
        });
    }, []);

    return (
        <DotContentAnalyticsContext.Provider value={instance}>
            {children}
        </DotContentAnalyticsContext.Provider>
    );
};
