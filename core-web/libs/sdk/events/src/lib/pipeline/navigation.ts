import onRouteChange from '@analytics/router-utils';

/** The part of the browser's Navigation API this module listens to. */
interface NavigationApi {
    addEventListener(type: 'currententrychange', listener: () => void): void;
}

/**
 * Calls back on every navigation inside the page, an SPA route change, that changes the path
 * or the query. It listens to the browser's Navigation API, which reports every History change,
 * back and forward included, without patching `history`. Where the browser has none, it falls
 * back to `@analytics/router-utils`, which reports path changes only.
 *
 * @param navigated - Called once the URL has changed
 */
export const onNavigation = (navigated: () => void): void => {
    const navigation = (window as unknown as { navigation?: NavigationApi }).navigation;

    if (!navigation) {
        onRouteChange(() => navigated());

        return;
    }

    const pageOf = (): string => `${window.location.pathname}${window.location.search}`;
    let current = pageOf();

    navigation.addEventListener('currententrychange', () => {
        const next = pageOf();

        // Routers also save their own state on the URL they are on
        if (next !== current) {
            current = next;
            navigated();
        }
    });
};
