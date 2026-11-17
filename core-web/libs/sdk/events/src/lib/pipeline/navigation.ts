import onRouteChange from '@analytics/router-utils';

/** The part of the browser's Navigation API this module listens to. */
interface NavigationApi {
    addEventListener(type: 'currententrychange', listener: () => void): void;
    removeEventListener(type: 'currententrychange', listener: () => void): void;
}

/**
 * Calls back on every navigation inside the page, an SPA route change, that changes the path
 * or the query. It listens to the browser's Navigation API, which reports every History change,
 * back and forward included, without patching `history`. Where the browser has none, it falls
 * back to `@analytics/router-utils`, which reports path changes only.
 *
 * @param navigated - Called once the URL has changed
 * @returns Stops the calls. router-utils has no way to detach, so in the fallback its listener
 * stays and only stops calling back
 */
export const onNavigation = (navigated: () => void): (() => void) => {
    const navigation = (window as unknown as { navigation?: NavigationApi }).navigation;

    if (!navigation) {
        let active = true;

        onRouteChange(() => {
            if (active) {
                navigated();
            }
        });

        return () => {
            active = false;
        };
    }

    const pageOf = (): string => `${window.location.pathname}${window.location.search}`;
    let current = pageOf();

    const listener = (): void => {
        const next = pageOf();

        // Routers also save their own state on the URL they are on
        if (next !== current) {
            current = next;
            navigated();
        }
    };

    navigation.addEventListener('currententrychange', listener);

    return () => navigation.removeEventListener('currententrychange', listener);
};

/**
 * Calls back each time the browser restores the page from its back/forward cache (`pageshow`
 * with `persisted`). The page comes back as the visitor left it, its scripts still running, so
 * nothing that runs on a load runs again, and onNavigation reports nothing: the visitor is
 * back on the same page, not on another route.
 *
 * @param restored - Called on each restore
 * @returns Stops the calls
 */
export const onPageRestore = (restored: () => void): (() => void) => {
    const listener = (event: PageTransitionEvent): void => {
        if (event.persisted) {
            restored();
        }
    };

    window.addEventListener('pageshow', listener);

    return () => window.removeEventListener('pageshow', listener);
};
