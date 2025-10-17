import { useEffect, useState } from 'react';

import { isAnalyticsActive } from '@dotcms/uve/internal';

/**
 * @internal
 * A React hook that checks whether DotCMS Analytics is active.
 *
 * The hook reads the global window flag set by the @dotcms/analytics SDK
 * on component mount. This assumes analytics is initialized in the layout
 * before child components render.
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
    const [isActive, setIsActive] = useState<boolean>(() => isAnalyticsActive());

    useEffect(() => {
        // Update state if it wasn't correctly initialized
        setIsActive(isAnalyticsActive());
    }, []);

    return isActive;
};
