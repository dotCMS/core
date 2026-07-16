import { usePathname, useSearchParams, type ReadonlyURLSearchParams } from 'next/navigation';
import { useEffect, useRef } from 'react';

import { DotCMSAnalytics } from '../../core/shared/models';

// TODO: Make this work no tightly coupled to Next.js App Router https://github.com/dotCMS/core/issues/33100

// Helpers
const computeNextKey = (
    pathname: string,
    searchParams?: URLSearchParams | ReadonlyURLSearchParams | null
): string => `${pathname}${searchParams?.toString() ? '?' + searchParams.toString() : ''}`;

/**
 * Tracks page views on route changes using Next.js App Router signals.
 * - Fires a single pageView per unique path+search.
 * - Automatically disabled inside UVE editor (analytics instance is null).
 * - Requires Next.js App Router; no SPA fallback.
 *
 * @param analytics Analytics singleton instance; if null, does nothing
 * @param debug When true, logs which tracking path is used
 * @param enabled When false, tracking is disabled even if analytics is present (defaults to true)
 */
export function useRouterTracker(analytics: DotCMSAnalytics | null, debug = false, enabled = true) {
    const lastKeyRef = useRef<string | null>(null);

    const pathname = usePathname();
    const searchParams = useSearchParams();

    useEffect(() => {
        if (!enabled) return;
        if (!analytics) return;

        const fireIfChanged = (key: string) => {
            if (key === lastKeyRef.current) return;
            lastKeyRef.current = key;
            analytics.pageView();
        };

        if (debug) {
            // eslint-disable-next-line no-console
            console.info('DotCMS Analytics [React]: using Next.js App Router tracking');
        }
        fireIfChanged(computeNextKey(pathname, searchParams));
    }, [analytics, pathname, searchParams, debug, enabled]);
}
