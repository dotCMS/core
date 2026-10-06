import { afterEach, describe, expect, it, vi } from 'vitest';

const onRouteChange = vi.hoisted(() => vi.fn());

vi.mock('@analytics/router-utils', () => ({ default: onRouteChange }));

import { onNavigation, onPageRestore } from './navigation';

describe('onNavigation', () => {
    const globalScope = window as unknown as Record<string, unknown>;

    afterEach(() => {
        delete globalScope['navigation'];
        onRouteChange.mockReset();
        window.history.replaceState(null, '', '/');
    });

    it('calls back on every navigation that changes the path or the query, through the Navigation API', () => {
        const navigation = new EventTarget();
        globalScope['navigation'] = navigation;
        const navigated = vi.fn();
        onNavigation(navigated);

        const go = (url: string) => {
            window.history.pushState(null, '', url);
            navigation.dispatchEvent(new Event('currententrychange'));
        };

        go('/blog');
        // A query-only navigation, such as a paginated list
        go('/blog?page=2');
        // The router saves its own state on the same URL
        go('/blog?page=2');

        expect(navigated).toHaveBeenCalledTimes(2);
        expect(onRouteChange).not.toHaveBeenCalled();
    });

    it('falls back to History changes, by path, where the browser has no Navigation API', () => {
        const navigated = vi.fn();
        onNavigation(navigated);

        expect(onRouteChange).toHaveBeenCalledTimes(1);

        onRouteChange.mock.calls[0]?.[0]('/blog');
        expect(navigated).toHaveBeenCalledTimes(1);
    });

    it('stops calling back once unsubscribed, through the Navigation API', () => {
        const navigation = new EventTarget();
        globalScope['navigation'] = navigation;
        const navigated = vi.fn();
        const stop = onNavigation(navigated);

        stop();
        window.history.pushState(null, '', '/blog');
        navigation.dispatchEvent(new Event('currententrychange'));

        expect(navigated).not.toHaveBeenCalled();
    });

    it('stops calling back once unsubscribed, in the fallback, which cannot detach from router-utils', () => {
        const navigated = vi.fn();
        const stop = onNavigation(navigated);

        stop();
        onRouteChange.mock.calls[0]?.[0]('/blog');

        expect(navigated).not.toHaveBeenCalled();
    });
});

describe('onPageRestore', () => {
    const pageshow = (persisted: boolean) =>
        window.dispatchEvent(Object.assign(new Event('pageshow'), { persisted }));

    it('calls back each time the browser restores the page from its back/forward cache', () => {
        const restored = vi.fn();
        const stop = onPageRestore(restored);

        pageshow(true);
        pageshow(true);

        expect(restored).toHaveBeenCalledTimes(2);
        stop();
    });

    it('does not call back on the show of a page that loads', () => {
        const restored = vi.fn();
        const stop = onPageRestore(restored);

        pageshow(false);

        expect(restored).not.toHaveBeenCalled();
        stop();
    });

    it('stops calling back once unsubscribed', () => {
        const restored = vi.fn();
        const stop = onPageRestore(restored);

        stop();
        pageshow(true);

        expect(restored).not.toHaveBeenCalled();
    });
});
