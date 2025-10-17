import { useEffect, useState } from 'react';

import { isAnalyticsActive } from '@dotcms/uve/internal';

/**
 * @internal
 * A React hook that monitors whether DotCMS Analytics is active.
 *
 * The hook checks the global window flag set by the @dotcms/analytics SDK
 * to determine if analytics tracking is enabled.
 *
 * @returns {boolean} True if analytics is active, false otherwise
 *
 * @example
 * ```tsx
 * const isAnalyticsActive = useIsAnalyticsActive()
 *
 * if (isAnalyticsActive) {
 *   // Analytics is active
 * }
 * ```
 */
export const useIsAnalyticsActive = (): boolean => {
    const [isActive, setIsActive] = useState<boolean>(false);

    useEffect(() => {
        const checkAnalyticsStatus = () => {
            setIsActive(isAnalyticsActive());
        };

        // Check analytics state on mount
        checkAnalyticsStatus();

        // Poll for analytics state changes
        // This allows the component to react if analytics is initialized/destroyed after mount
        const checkAnalyticsInterval = setInterval(checkAnalyticsStatus, 1000);

        return () => {
            clearInterval(checkAnalyticsInterval);
        };
    }, []);

    return isActive;
};
