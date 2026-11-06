import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/vitest';
import { Subject } from 'rxjs';
import { Mock, vi } from 'vitest';

import { Location, PopStateEvent } from '@angular/common';
import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import {
    Event as RouterEvent,
    NavigationCancel,
    NavigationCancellationCode,
    NavigationEnd,
    NavigationError,
    NavigationSkipped,
    NavigationStart,
    Router
} from '@angular/router';

import { DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotSidePanelRef } from '@dotcms/ui';

import { DotPanelUrlStateService } from './dot-panel-url-state.service';

/** The surface's own URL, before any panel opens. */
const SURFACE_URL = '/c/query-tool?q=%2Blive%3Atrue&limit=20';

/** The surface's own params, decoded. No panel write may change them (FR-044). */
const SURFACE_PARAMS = { q: '+live:true', limit: '20' };

const ID_1 = '3f0c8e5a-7b2d-4c1e-9a6f-0d4b8e2c1a57';
const ID_2 = '7c2e9b14-5a3d-4f6e-8b1c-2d9a0e4f6b38';
const NEW_ID = 'b4e1f0a2-9c3d-4e5f-a6b7-c8d9e0f1a2b3';

/**
 * Stands in for Angular's `Location`: keeps the URL the last write left, as the browser does, and
 * plays Back or Forward to everyone subscribed.
 */
const createLocationDouble = (initialUrl: string) => {
    let current = initialUrl;
    const listeners: Array<(event: PopStateEvent) => void> = [];
    const write = (path: string, query = ''): void => {
        current = query ? `${path}?${query.replace(/^\?/, '')}` : path;
    };

    return {
        path: vi.fn(() => current),
        go: vi.fn(write),
        replaceState: vi.fn(write),
        subscribe: vi.fn((onNext: (event: PopStateEvent) => void) => {
            listeners.push(onNext);

            return { unsubscribe: vi.fn() };
        }),
        /**
         * Browser Back or Forward, landing on `url`. Under hash routing the browser follows the
         * `popstate` with a `hashchange` for the same move, and `Location` reports both.
         */
        pop(url: string, type: 'popstate' | 'hashchange' = 'popstate'): void {
            current = url;
            listeners.forEach((listener) => listener({ url, pop: true, type, state: null }));
        }
    };
};

/**
 * Stands in for Angular's `Router`, which answers every Back with a navigation of its own once it
 * has navigated (`onSameUrlNavigation: 'reload'` in dotcms-ui), ending with a `replaceState` of
 * the URL Back landed on. `navigate` and `navigateByUrl` are spies the service must never call.
 */
const createRouterDouble = (navigated: boolean) => ({
    navigated,
    events: new Subject<RouterEvent>(),
    navigate: vi.fn(),
    navigateByUrl: vi.fn()
});

/**
 * The last URL written with `write`, as its path and its decoded params, so a test reads what the
 * address bar shows whatever the encoding or the order of the params.
 */
const lastWrite = (write: {
    mock: { calls: unknown[][] };
}): { path: string; params: Record<string, string> } => {
    const [path, query = ''] = write.mock.calls[write.mock.calls.length - 1] as [string, string?];
    const url = new URL(query ? `${path}?${query.replace(/^\?/, '')}` : path, 'http://dotcms.test');

    return { path: url.pathname, params: Object.fromEntries(url.searchParams) };
};

/** The surface's URL with `params` added, as the address bar would hold it. */
const surfaceUrlWith = (params: Record<string, string>): string =>
    `${SURFACE_URL}&${new URLSearchParams(params).toString()}`;

/** A panel ref as the outlet hands it out: `closed` and `isClosed` behave as the real one's. */
const newRef = (): DotSidePanelRef => new DotSidePanelRef(() => undefined);

/**
 * A ref whose `requestClose` does nothing, as an editor's unsaved-changes guard does while its
 * prompt is open.
 */
const guardedRef = (): { ref: DotSidePanelRef; requestClose: Mock } => {
    const ref = newRef();
    const requestClose = vi.fn();
    vi.spyOn(ref, 'requestClose').mockImplementation(requestClose);

    return { ref, requestClose };
};

/**
 * The open side panel, in the surface's URL (#38303, FR-044 to FR-048). Written with
 * `Location.go` and `Location.replaceState` only: the legacy iframe reloads on every router query
 * param change, and a router navigation recreates a `reuseRoute: false` portlet (FR-046).
 */
describe('DotPanelUrlStateService', () => {
    let spectator: SpectatorService<DotPanelUrlStateService>;
    let location: ReturnType<typeof createLocationDouble>;
    let router: ReturnType<typeof createRouterDouble>;

    const createService = createServiceFactory({
        service: DotPanelUrlStateService,
        providers: [
            { provide: Location, useFactory: () => location },
            { provide: Router, useFactory: () => router },
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    /**
     * Starts the service on a surface whose address bar holds `url`, in an app whose router has
     * navigated unless told otherwise.
     */
    const start = (url = SURFACE_URL, { routerNavigated = true } = {}): void => {
        location = createLocationDouble(url);
        router = createRouterDouble(routerNavigated);
        spectator = createService();
    };

    /**
     * The router's answer to a Back landing on `url`: its own navigation, which writes that URL
     * with `replaceState` before it ends.
     */
    const routerAnswersBack = (url: string, end: RouterEvent = new NavigationEnd(2, url, url)) => {
        router.events.next(new NavigationStart(2, url, 'popstate'));
        location.replaceState(url);
        router.events.next(end);
    };

    /** Browser Back or Forward landing on `url`, and the router's navigation for it. */
    const back = (url: string): void => {
        location.pop(url);
        routerAnswersBack(url);
    };

    /** How many times the service wrote with `replaceState`, the router's own writes left out. */
    const serviceReplaces = (routerWrites: number): number =>
        location.replaceState.mock.calls.length - routerWrites;

    describe('opening a panel', () => {
        it("pushes one history entry naming an edit, beside the surface's own params", () => {
            start();

            spectator.service.register(newRef(), { editContent: ID_1, editContentLang: '1' });

            expect(location.go).toHaveBeenCalledTimes(1);
            expect(location.replaceState).not.toHaveBeenCalled();
            expect(lastWrite(location.go)).toEqual({
                path: '/c/query-tool',
                params: { ...SURFACE_PARAMS, editContent: ID_1, editContentLang: '1' }
            });
        });

        it("pushes a create's content type and the caller's extra params", () => {
            start();

            spectator.service.register(newRef(), { createContent: 'Blog', folder: 'f-1' });

            expect(location.go).toHaveBeenCalledTimes(1);
            expect(lastWrite(location.go).params).toEqual({
                ...SURFACE_PARAMS,
                createContent: 'Blog',
                folder: 'f-1'
            });
        });

        // A link opened on load: a second entry for the same URL would make Back do nothing.
        it('writes nothing when the URL already names the panel', () => {
            start(surfaceUrlWith({ editContent: ID_1, editContentLang: '1' }));

            spectator.service.register(newRef(), { editContent: ID_1, editContentLang: '1' });

            expect(location.go).not.toHaveBeenCalled();
            expect(location.replaceState).not.toHaveBeenCalled();
        });

        // Content Drive's switch to the old editor closes the panel and reopens it at once: the
        // reopened panel takes the entry the closed one had, so Back still leaves in one step.
        it('rewrites the current entry instead of adding one for a panel registered in place', () => {
            start();
            const first = newRef();
            spectator.service.register(first, { editContent: ID_1, editContentLang: '1' });
            first.close();

            spectator.service.register(
                newRef(),
                { editContent: ID_2, editContentLang: '2' },
                { inPlace: true }
            );

            expect(location.go).toHaveBeenCalledTimes(1);
            expect(lastWrite(location.replaceState).params).toEqual({
                ...SURFACE_PARAMS,
                editContent: ID_2,
                editContentLang: '2'
            });
        });
    });

    describe('closing a panel', () => {
        it('clears its params in place, so Forward cannot reopen it', () => {
            start();
            const ref = newRef();
            spectator.service.register(ref, { editContent: ID_1, editContentLang: '1' });

            ref.close();

            expect(location.go).toHaveBeenCalledTimes(1);
            expect(location.replaceState).toHaveBeenCalledTimes(1);
            expect(lastWrite(location.replaceState).params).toEqual(SURFACE_PARAMS);
        });

        it("clears a create's extra params with its own", () => {
            start();
            const ref = newRef();
            spectator.service.register(ref, { createContent: 'Blog', folder: 'f-1' });

            ref.close();

            expect(location.replaceState).toHaveBeenCalledTimes(1);
            expect(lastWrite(location.replaceState).params).toEqual(SURFACE_PARAMS);
        });
    });

    describe('rewriting a panel in place', () => {
        it('rewrites the language when the editor switches it, without a history entry', () => {
            start();
            const ref = newRef();
            spectator.service.register(ref, { editContent: ID_1, editContentLang: '1' });

            spectator.service.update(ref, { editContent: ID_1, editContentLang: '2' });

            expect(location.go).toHaveBeenCalledTimes(1);
            expect(location.replaceState).toHaveBeenCalledTimes(1);
            expect(lastWrite(location.replaceState).params).toEqual({
                ...SURFACE_PARAMS,
                editContent: ID_1,
                editContentLang: '2'
            });
        });

        it("names the new content in place of the create on the create's first save", () => {
            start();
            const ref = newRef();
            spectator.service.register(ref, { createContent: 'Blog' });

            spectator.service.update(ref, { editContent: NEW_ID, editContentLang: '1' });

            expect(location.go).toHaveBeenCalledTimes(1);
            expect(location.replaceState).toHaveBeenCalledTimes(1);
            expect(lastWrite(location.replaceState).params).toEqual({
                ...SURFACE_PARAMS,
                editContent: NEW_ID,
                editContentLang: '1'
            });
        });
    });

    describe('stacked panels', () => {
        it('names only the front panel, and the one underneath again once the front one closes', () => {
            start();
            const underneath = newRef();
            const front = newRef();
            spectator.service.register(underneath, { editContent: ID_1, editContentLang: '1' });

            spectator.service.register(front, { createContent: 'Blog', folder: 'f-1' });

            expect(location.go).toHaveBeenCalledTimes(2);
            // One panel named at a time: a URL naming two would open none on load (FR-048, FR-049).
            expect(lastWrite(location.go).params).toEqual({
                ...SURFACE_PARAMS,
                createContent: 'Blog',
                folder: 'f-1'
            });

            front.close();

            expect(location.go).toHaveBeenCalledTimes(2);
            expect(location.replaceState).toHaveBeenCalledTimes(1);
            expect(lastWrite(location.replaceState).params).toEqual({
                ...SURFACE_PARAMS,
                editContent: ID_1,
                editContentLang: '1'
            });
            expect(underneath.isClosed).toBe(false);
        });
    });

    describe('browser Back and Forward', () => {
        it("restores the open panel's params while its guard decides, and asks it to close", () => {
            start();
            const { ref, requestClose } = guardedRef();
            spectator.service.register(ref, { editContent: ID_1, editContentLang: '1' });

            back(SURFACE_URL);

            expect(requestClose).toHaveBeenCalledTimes(1);
            expect(serviceReplaces(1)).toBe(1);
            expect(lastWrite(location.replaceState).params).toEqual({
                ...SURFACE_PARAMS,
                editContent: ID_1,
                editContentLang: '1'
            });
        });

        it('clears the params once the author discards and the panel closes', () => {
            start();
            const { ref } = guardedRef();
            spectator.service.register(ref, { editContent: ID_1, editContentLang: '1' });
            back(SURFACE_URL);

            ref.close();

            expect(location.go).toHaveBeenCalledTimes(1);
            expect(lastWrite(location.replaceState).params).toEqual(SURFACE_PARAMS);
        });

        it('asks only the front panel to close when panels are stacked', () => {
            start();
            const underneath = guardedRef();
            const front = guardedRef();
            spectator.service.register(underneath.ref, { editContent: ID_1, editContentLang: '1' });
            spectator.service.register(front.ref, { editContent: ID_2, editContentLang: '2' });

            // Back one step: the entry the first panel pushed.
            back(surfaceUrlWith({ editContent: ID_1, editContentLang: '1' }));

            expect(front.requestClose).toHaveBeenCalledTimes(1);
            expect(underneath.requestClose).not.toHaveBeenCalled();
            expect(serviceReplaces(1)).toBe(1);
            expect(lastWrite(location.replaceState).params).toEqual({
                ...SURFACE_PARAMS,
                editContent: ID_2,
                editContentLang: '2'
            });
        });

        it('does nothing when the URL still names the front panel', () => {
            start();
            const { ref, requestClose } = guardedRef();
            spectator.service.register(ref, { editContent: ID_1, editContentLang: '1' });

            back(surfaceUrlWith({ editContent: ID_1, editContentLang: '1' }));

            expect(requestClose).not.toHaveBeenCalled();
            expect(serviceReplaces(1)).toBe(0);
        });

        /**
         * The router navigates on every Back once it has navigated, even to a URL it already holds
         * (`onSameUrlNavigation: 'reload'`), and its navigation ends with a `replaceState` of the URL
         * Back landed on. A restore written before that is overwritten while the editor's prompt is
         * open, so the service restores once the navigation is over (T156, FR-047).
         */
        describe('while the router answers Back', () => {
            it("waits for the router's navigation to end, so the router's own write cannot undo the restore", () => {
                start();
                const { ref, requestClose } = guardedRef();
                spectator.service.register(ref, { editContent: ID_1, editContentLang: '1' });

                location.pop(SURFACE_URL);
                router.events.next(new NavigationStart(2, SURFACE_URL, 'popstate'));

                expect(requestClose).not.toHaveBeenCalled();
                expect(location.replaceState).not.toHaveBeenCalled();

                location.replaceState(SURFACE_URL);
                router.events.next(new NavigationEnd(2, SURFACE_URL, SURFACE_URL));

                expect(requestClose).toHaveBeenCalledTimes(1);
                expect(serviceReplaces(1)).toBe(1);
                expect(lastWrite(location.replaceState).params).toEqual({
                    ...SURFACE_PARAMS,
                    editContent: ID_1,
                    editContentLang: '1'
                });
            });

            it.each([
                ['cancelled', new NavigationCancel(2, SURFACE_URL, 'guard')],
                ['failed', new NavigationError(2, SURFACE_URL, new Error('boom'))],
                ['skipped', new NavigationSkipped(2, SURFACE_URL, 'same URL')]
            ])('restores once a %s navigation ends it', (_label, end) => {
                start();
                const { ref, requestClose } = guardedRef();
                spectator.service.register(ref, { editContent: ID_1, editContentLang: '1' });

                location.pop(SURFACE_URL);
                routerAnswersBack(SURFACE_URL, end);

                expect(requestClose).toHaveBeenCalledTimes(1);
                expect(lastWrite(location.replaceState).params).toEqual({
                    ...SURFACE_PARAMS,
                    editContent: ID_1,
                    editContentLang: '1'
                });
            });

            /**
             * A second Back before the first navigation ends cancels it as superseded while the
             * second navigation still runs, and that navigation's own `replaceState` would undo a
             * restore written now. So the restore waits for the navigation that replaced it.
             */
            it('keeps waiting through a navigation a newer one superseded', () => {
                start();
                const { ref, requestClose } = guardedRef();
                spectator.service.register(ref, { editContent: ID_1, editContentLang: '1' });

                location.pop(SURFACE_URL);
                router.events.next(new NavigationStart(2, SURFACE_URL, 'popstate'));
                router.events.next(
                    new NavigationCancel(
                        2,
                        SURFACE_URL,
                        'superseded',
                        NavigationCancellationCode.SupersededByNewNavigation
                    )
                );

                expect(requestClose).not.toHaveBeenCalled();
                expect(location.replaceState).not.toHaveBeenCalled();

                routerAnswersBack(SURFACE_URL, new NavigationEnd(3, SURFACE_URL, SURFACE_URL));

                expect(requestClose).toHaveBeenCalledTimes(1);
                expect(serviceReplaces(1)).toBe(1);
                expect(lastWrite(location.replaceState).params).toEqual({
                    ...SURFACE_PARAMS,
                    editContent: ID_1,
                    editContentLang: '1'
                });
            });

            // No navigation will answer this Back, so waiting would leave the panel unnamed.
            it('restores at once when the router has never navigated', () => {
                start(SURFACE_URL, { routerNavigated: false });
                const { ref, requestClose } = guardedRef();
                spectator.service.register(ref, { editContent: ID_1, editContentLang: '1' });

                location.pop(SURFACE_URL);

                expect(requestClose).toHaveBeenCalledTimes(1);
                expect(location.replaceState).toHaveBeenCalledTimes(1);
            });

            // Under hash routing `Location` reports a `hashchange` after the `popstate`.
            it('treats the hashchange that follows the popstate as the same Back', () => {
                start();
                const { ref, requestClose } = guardedRef();
                spectator.service.register(ref, { editContent: ID_1, editContentLang: '1' });

                location.pop(SURFACE_URL);
                location.pop(SURFACE_URL, 'hashchange');
                routerAnswersBack(SURFACE_URL);

                expect(requestClose).toHaveBeenCalledTimes(1);
                expect(serviceReplaces(1)).toBe(1);
            });

            it('restores nothing for a panel that closed before the navigation ended', () => {
                start();
                const { ref, requestClose } = guardedRef();
                spectator.service.register(ref, { editContent: ID_1, editContentLang: '1' });

                location.pop(SURFACE_URL);
                ref.close();
                routerAnswersBack(SURFACE_URL);

                expect(requestClose).not.toHaveBeenCalled();
                expect(lastWrite(location.replaceState).params).toEqual(SURFACE_PARAMS);
            });

            it('ignores router navigations that answer no Back', () => {
                start();
                const { ref, requestClose } = guardedRef();
                spectator.service.register(ref, { editContent: ID_1, editContentLang: '1' });

                router.events.next(new NavigationEnd(3, SURFACE_URL, SURFACE_URL));

                expect(requestClose).not.toHaveBeenCalled();
                expect(location.replaceState).not.toHaveBeenCalled();
            });
        });

        /**
         * Back that lands on another route has left the surface, and the URL Back landed on belongs
         * to another screen (T155). A panel a legacy (JSP) portlet opened lives under the side
         * panel host, not the surface, so nothing else would close it: the service closes it,
         * without the guard (the surface is gone, as it is for an Angular surface), and writes
         * nothing.
         */
        describe('when Back leaves the surface', () => {
            /** A page on another route, which happens to name a panel of its own. */
            const OTHER_ROUTE_URL = `/c/content-drive?path=%2Fblog&editContent=${ID_2}`;

            it.each([
                ['a panel that pushed its own entry', SURFACE_URL],
                [
                    'a panel a link opened, which has no entry of its own',
                    surfaceUrlWith({ editContent: ID_1, editContentLang: '1' })
                ]
            ])('closes it without its guard and restores nothing, for %s', (_label, url) => {
                start(url);
                const { ref, requestClose } = guardedRef();
                spectator.service.register(ref, { editContent: ID_1, editContentLang: '1' });
                location.go.mockClear();

                back(OTHER_ROUTE_URL);

                expect(ref.isClosed).toBe(true);
                expect(requestClose).not.toHaveBeenCalled();
                expect(serviceReplaces(1)).toBe(0);
                expect(location.go).not.toHaveBeenCalled();
            });
        });

        /**
         * Any navigation of the router that ends on another path has left the surface, a link in a
         * legacy portlet's frame as much as Back: every panel registered there closes, without its
         * guard, and nothing is written to the URL. A navigation that stays on the surface's path
         * (a Back that pops a param, a deep link's redirect) closes nothing.
         */
        describe('when a navigation ends on another path', () => {
            it('closes every panel registered on the surface, front first, and writes nothing', () => {
                start();
                const underneath = guardedRef();
                const front = guardedRef();
                spectator.service.register(underneath.ref, {
                    editContent: ID_1,
                    editContentLang: '1'
                });
                spectator.service.register(front.ref, { createContent: 'Blog' });
                const closedOrder: string[] = [];
                underneath.ref.closed.subscribe(() => closedOrder.push('underneath'));
                front.ref.closed.subscribe(() => closedOrder.push('front'));
                location.go.mockClear();
                location.replaceState.mockClear();
                // The router's own write for an imperative navigation, before it ends.
                location.go('/starter');

                router.events.next(new NavigationEnd(3, '/starter', '/starter'));

                expect(closedOrder).toEqual(['front', 'underneath']);
                expect(underneath.requestClose).not.toHaveBeenCalled();
                expect(front.requestClose).not.toHaveBeenCalled();
                // Only the router's write: the service wrote nothing.
                expect(location.go).toHaveBeenCalledTimes(1);
                expect(location.replaceState).not.toHaveBeenCalled();
            });

            it('closes nothing for a navigation that stays on the surface, whatever its params', () => {
                start();
                const { ref } = guardedRef();
                spectator.service.register(ref, { editContent: ID_1, editContentLang: '1' });

                router.events.next(
                    new NavigationEnd(3, `${SURFACE_URL}&offset=20`, `${SURFACE_URL}&offset=20`)
                );

                expect(ref.isClosed).toBe(false);
            });

            it('closes nothing before any panel is registered', () => {
                start();

                expect(() =>
                    router.events.next(new NavigationEnd(3, '/starter', '/starter'))
                ).not.toThrow();
            });
        });

        describe('after the surface is gone', () => {
            const OTHER_ROUTE_URL = `/c/content-drive?path=%2Fblog&editContent=${ID_2}`;

            it("leaves the other route's URL alone when the panel then closes with its surface", () => {
                start();
                const { ref } = guardedRef();
                spectator.service.register(ref, { editContent: ID_1, editContentLang: '1' });
                back(OTHER_ROUTE_URL);

                ref.close();

                expect(serviceReplaces(1)).toBe(0);
                expect(location.path()).toBe(OTHER_ROUTE_URL);
            });
        });
    });

    // A link naming two panels opens neither, and loses both (FR-049).
    it("clears every panel param and the surface's extra keys in place", () => {
        start(surfaceUrlWith({ editContent: ID_1, createContent: 'Blog', folder: 'f-1' }));

        spectator.service.clear(['folder']);

        expect(location.go).not.toHaveBeenCalled();
        expect(location.replaceState).toHaveBeenCalledTimes(1);
        expect(lastWrite(location.replaceState).params).toEqual(SURFACE_PARAMS);
    });

    /**
     * A link the author cannot have meant (a panel param of the wrong shape, or two panels) tells
     * them with the error a missing content shows, then loses the params, so every bad link reads
     * the same (T172).
     */
    it('refuses a bad link: the standard not-found error, then the params cleared in place', () => {
        start(surfaceUrlWith({ editContent: 'wrong-identifier', folder: 'f-1' }));
        const handle = spectator.inject(DotHttpErrorManagerService).handle as Mock;

        spectator.service.refuse(['folder']);

        expect(handle).toHaveBeenCalledTimes(1);
        expect((handle.mock.calls[0][0] as HttpErrorResponse).status).toBe(HttpStatusCode.NotFound);
        expect(location.go).not.toHaveBeenCalled();
        expect(location.replaceState).toHaveBeenCalledTimes(1);
        expect(lastWrite(location.replaceState).params).toEqual(SURFACE_PARAMS);
    });

    it('never navigates through the router', () => {
        start();
        const underneath = newRef();
        const { ref: front } = guardedRef();
        spectator.service.register(underneath, { editContent: ID_1, editContentLang: '1' });
        spectator.service.register(front, { createContent: 'Blog' });
        spectator.service.update(front, { editContent: NEW_ID, editContentLang: '1' });
        location.pop(SURFACE_URL);
        front.close();
        underneath.close();

        expect(router.navigate).not.toHaveBeenCalled();
        expect(router.navigateByUrl).not.toHaveBeenCalled();
    });
});
