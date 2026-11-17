import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/vitest';
import { Mock, vi } from 'vitest';

import { Component, EnvironmentInjector, createEnvironmentInjector, signal } from '@angular/core';

import { DotMessageDisplayService, DotMessageService } from '@dotcms/data-access';
import { DotMessageSeverity, DotMessageType } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { DotSidePanelRef, DotSidePanelService } from './dot-side-panel.service';

@Component({ selector: 'dot-test-content', template: '' })
class TestContentComponent {}

const PREV_NAV_COLLAPSED_KEY = 'dot-edit-content-side-panel-prev-nav-collapsed';

const setViewportWidth = (width: number) =>
    Object.defineProperty(window, 'innerWidth', { value: width, configurable: true });

describe('DotSidePanelService', () => {
    let spectator: SpectatorService<DotSidePanelService>;
    let navigation: {
        isNavigationCollapsed: Mock;
        collapseNavigation: Mock;
        expandNavigation: Mock;
    };

    const createService = createServiceFactory({
        service: DotSidePanelService,
        providers: [
            mockProvider(GlobalStore, {
                isNavigationCollapsed: vi.fn().mockReturnValue(false),
                collapseNavigation: vi.fn(),
                expandNavigation: vi.fn()
            }),
            mockProvider(DotMessageDisplayService),
            mockProvider(DotMessageService, { get: (key: string) => key })
        ]
    });

    beforeEach(() => {
        // mockProvider's vi.fn()s are shared across tests: clear their calls and restore defaults.
        vi.clearAllMocks();
        sessionStorage.clear();
        // Narrow by default, so opening a panel collapses the navigation.
        setViewportWidth(800);
        spectator = createService();
        navigation = spectator.inject(GlobalStore) as unknown as typeof navigation;
        navigation.isNavigationCollapsed.mockReturnValue(false);
    });

    /** An opener's injector, which the test can destroy as if the opener were. */
    const openerInjector = () => {
        const injector = createEnvironmentInjector([], spectator.inject(EnvironmentInjector));

        return { injector, destroyOpener: () => injector.destroy() };
    };

    const open = (component: Parameters<DotSidePanelService['open']>[0] = TestContentComponent) =>
        spectator.service.open(component, {
            heading: 'header.vtl',
            injector: openerInjector().injector
        });

    describe('open', () => {
        it('adds a panel in front of the open ones, with its heading and content', () => {
            open();
            const second = open();

            const panels = spectator.service.panels();
            expect(panels).toHaveLength(2);
            expect(panels[1].ref).toBe(second);
            expect(panels[1].heading()).toBe('header.vtl');
            expect(panels[1].component()).toBe(TestContentComponent);
        });

        it('keeps a heading given as a signal live', () => {
            const heading = signal('first');
            spectator.service.open(TestContentComponent, {
                heading,
                injector: openerInjector().injector
            });

            heading.set('second');

            expect(spectator.service.panels()[0].heading()).toBe('second');
        });

        it('shows nothing until lazily loaded content arrives', async () => {
            let resolve: (type: typeof TestContentComponent) => void = () => undefined;
            open(new Promise((done) => (resolve = done)));

            expect(spectator.service.panels()[0].component()).toBeNull();

            resolve(TestContentComponent);
            await Promise.resolve();

            expect(spectator.service.panels()[0].component()).toBe(TestContentComponent);
        });

        it('tells the author and closes the panel when its content fails to load', async () => {
            const error = vi.spyOn(console, 'error').mockImplementation(() => undefined);
            const ref = open(Promise.reject(new Error('chunk failed')));

            await Promise.resolve();
            await Promise.resolve();

            expect(spectator.inject(DotMessageDisplayService).push).toHaveBeenCalledWith(
                expect.objectContaining({
                    message: 'edit.content.side-panel.load-error',
                    severity: DotMessageSeverity.ERROR,
                    type: DotMessageType.SIMPLE_MESSAGE
                })
            );
            expect(ref.isClosed).toBe(true);
            expect(spectator.service.panels()).toHaveLength(0);
            error.mockRestore();
        });

        it('closes the panel with the opener', () => {
            const { injector, destroyOpener } = openerInjector();
            const ref = spectator.service.open(TestContentComponent, {
                heading: 'header.vtl',
                injector
            });

            destroyOpener();

            expect(ref.isClosed).toBe(true);
        });
    });

    describe('DotSidePanelRef', () => {
        let ref: DotSidePanelRef;

        beforeEach(() => (ref = open()));

        it('closes once: takes the panel off and emits closed, then completes', () => {
            const closed = vi.fn();
            const complete = vi.fn();
            ref.closed.subscribe({ next: closed, complete });

            ref.close();
            ref.close();

            expect(spectator.service.panels()).toHaveLength(0);
            expect(closed).toHaveBeenCalledTimes(1);
            expect(complete).toHaveBeenCalled();
        });

        it("asks the content's guard on requestClose, and leaves the panel open", () => {
            const requestClose = vi.fn();
            ref.attach({ requestClose });

            ref.requestClose();

            expect(requestClose).toHaveBeenCalled();
            expect(ref.isClosed).toBe(false);
        });

        it('closes on requestClose when the content has no guard', () => {
            ref.attach(new TestContentComponent());

            ref.requestClose();

            expect(ref.isClosed).toBe(true);
        });

        it('lays its content out without padding until the content says otherwise', () => {
            expect(ref.contentStyleClass()).toBe('p-0!');
        });
    });

    describe('main navigation', () => {
        it('collapses an expanded nav when a panel opens and restores it when it closes', () => {
            const ref = open();

            expect(navigation.collapseNavigation).toHaveBeenCalledTimes(1);
            expect(sessionStorage.getItem(PREV_NAV_COLLAPSED_KEY)).toBe('false');

            ref.close();

            expect(navigation.expandNavigation).toHaveBeenCalledTimes(1);
            expect(sessionStorage.getItem(PREV_NAV_COLLAPSED_KEY)).toBeNull();
        });

        it('keeps an already-collapsed nav collapsed (no collapse, no restore)', () => {
            navigation.isNavigationCollapsed.mockReturnValue(true);

            const ref = open();
            expect(navigation.collapseNavigation).not.toHaveBeenCalled();
            expect(sessionStorage.getItem(PREV_NAV_COLLAPSED_KEY)).toBe('true');

            ref.close();
            expect(navigation.expandNavigation).not.toHaveBeenCalled();
        });

        it('collapses once for stacked panels and restores only when the last one closes', () => {
            const outer = open();
            const inner = open();

            expect(navigation.collapseNavigation).toHaveBeenCalledTimes(1);

            inner.close();
            expect(navigation.expandNavigation).not.toHaveBeenCalled();

            outer.close();
            expect(navigation.expandNavigation).toHaveBeenCalledTimes(1);
        });

        it('leaves the nav alone on a wide viewport', () => {
            // The threshold itself counts as wide: the comparison is exclusive (`<`).
            setViewportWidth(2560);

            const ref = open();
            expect(spectator.service.collapsesNavigation()).toBe(false);
            expect(navigation.collapseNavigation).not.toHaveBeenCalled();
            expect(sessionStorage.getItem(PREV_NAV_COLLAPSED_KEY)).toBeNull();

            ref.close();
            expect(navigation.expandNavigation).not.toHaveBeenCalled();
        });

        it('keeps the pre-panel state remembered before a refresh with a panel open', () => {
            // A refresh: storage already holds the original (expanded) state, and the nav is
            // collapsed from before the refresh.
            sessionStorage.setItem(PREV_NAV_COLLAPSED_KEY, 'false');
            navigation.isNavigationCollapsed.mockReturnValue(true);

            const ref = open();

            expect(sessionStorage.getItem(PREV_NAV_COLLAPSED_KEY)).toBe('false');
            expect(navigation.collapseNavigation).not.toHaveBeenCalled();

            ref.close();

            expect(navigation.expandNavigation).toHaveBeenCalledTimes(1);
            expect(sessionStorage.getItem(PREV_NAV_COLLAPSED_KEY)).toBeNull();
        });
    });
});
