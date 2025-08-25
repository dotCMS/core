import { usePathname, useSearchParams, type ReadonlyURLSearchParams } from 'next/navigation';
import { useEffect, useRef } from 'react';

import { DotCMSAnalytics } from '../../dotAnalytics/shared/dot-content-analytics.model';
import { isInsideUVE } from '../internal/uve.utils';

// Using Next.js App Router hooks directly (library assumes Next environment)

// Helpers
const computeNextKey = (
    pathname: string,
    searchParams?: URLSearchParams | ReadonlyURLSearchParams | null
): string =>
    `${pathname}${searchParams?.toString() ? '?' + searchParams.toString() : ''}`;

/**
 * Tracks page views on route changes using Next.js App Router signals.
 * - Fires a single pageView per unique path+search.
 * - Disabled inside UVE editor.
 * - Requires Next.js App Router; no SPA fallback.
 *
 * @param analytics Analytics singleton instance; if null, does nothing
 * @param debug When true, logs which tracking path is used
 */
export function useRouterTracker(analytics: DotCMSAnalytics | null, debug = false) {
    const lastKeyRef = useRef<string | null>(null);

    const pathname = usePathname();
    const searchParams = useSearchParams();

    useEffect(() => {
        if (!analytics) return;

        const fireIfChanged = (key: string) => {
            if (isInsideUVE()) return;
            if (key === lastKeyRef.current) return;
            lastKeyRef.current = key;
            analytics.pageView();
        };

        if (debug) {
            // eslint-disable-next-line no-console
            console.info('DotContentAnalytics: using Next.js App Router tracking');
        }
        fireIfChanged(computeNextKey(pathname, searchParams));
    }, [analytics, pathname, searchParams, debug]);
}
