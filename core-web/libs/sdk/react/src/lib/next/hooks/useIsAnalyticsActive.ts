'use client';

import { useEffect, useState } from 'react';

import { ANALYTICS_READY_EVENT, isDotAnalyticsActive } from '@dotcms/uve/internal';

/**
 * @internal
 * A React hook that determines whether DotCMS Analytics is active on the page.
 *
 * It reads the analytics active flag on mount and subscribes to the
 * `dotcms:analytics:ready` event so contentlets re-render with the attributes
 * Analytics needs, regardless of initialization order.
 *
 * @returns {boolean} - `true` when analytics is active; otherwise, `false`.
 */
export const useIsAnalyticsActive = (): boolean => {
    const [isAnalyticsActive, setIsAnalyticsActive] = useState(false);

    useEffect(() => {
        const updateState = () => setIsAnalyticsActive(isDotAnalyticsActive());

        updateState();
        window.addEventListener(ANALYTICS_READY_EVENT, updateState);

        return () => window.removeEventListener(ANALYTICS_READY_EVENT, updateState);
    }, []);

    return isAnalyticsActive;
};
