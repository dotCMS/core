import { ReactElement, ReactNode, useEffect, useState } from 'react';


import { DotContentAnalytics } from '../../dot-content-analytics';
import { DotContentAnalyticsConfig } from '../../shared/dot-content-analytics.model';
import DotContentAnalyticsContext from '../contexts/DotContentAnalyticsContext';
import { useContentAnalytics } from '../hook/useContentAnalytics';

interface DotContentAnalyticsProviderProps {
    children?: ReactNode;
    config: DotContentAnalyticsConfig;
}


export const DotContentAnalyticsProvider = ({
    children,
    config
}: DotContentAnalyticsProviderProps): ReactElement => {
    const [instance, setInstance] = useState<DotContentAnalytics | null>(null);


    useContentAnalytics(instance);

    useEffect(() => {
        // const insideEditor = isInsideEditor();
        const insideEditor = false;

        if (!insideEditor) {
            const dotContentAnalyticsInstance = DotContentAnalytics.getInstance(config);

            dotContentAnalyticsInstance.ready().then(() => {
                console.log('DotContentAnalyticsProvider: DotContentAnalytics instance initialized');
                setInstance(dotContentAnalyticsInstance);
                dotContentAnalyticsInstance.trackPageView();
            });
        } else {
            if (config.debug) {
                console.warn(
                    'DotContentAnalyticsProvider: DotContentAnalytics instance not initialized because it is inside the editor.'
                );
            }
        }
    }, [config]);

    return (
        <DotContentAnalyticsContext.Provider value={instance}>
            {children}
        </DotContentAnalyticsContext.Provider>
    );
};
