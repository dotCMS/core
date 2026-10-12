import { createServiceFactory, SpectatorService } from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { Component, EnvironmentInjector, createEnvironmentInjector, signal } from '@angular/core';

import { DotSidePanelRef, DotSidePanelService } from './dot-side-panel.service';

@Component({ selector: 'dot-test-content', template: '' })
class TestContentComponent {}

describe('DotSidePanelService', () => {
    let spectator: SpectatorService<DotSidePanelService>;

    const createService = createServiceFactory(DotSidePanelService);

    beforeEach(() => (spectator = createService()));

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

        it('closes the panel when its content fails to load', async () => {
            const error = vi.spyOn(console, 'error').mockImplementation(() => undefined);
            const ref = open(Promise.reject(new Error('chunk failed')));

            await Promise.resolve();
            await Promise.resolve();

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
});
