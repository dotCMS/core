import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { Component, signal } from '@angular/core';

import { GlobalStore } from '@dotcms/store';

import { DotSidePanelService } from './dot-side-panel.service';
import { injectSidePanel } from './inject-side-panel';

interface TestRequest {
    title: string;
    id: string;
}

@Component({ selector: 'dot-test-content', template: '' })
class TestContentComponent {}

/** An opener that keeps a panel open while its request is set, as the portlets do. */
@Component({ selector: 'dot-test-opener', template: '' })
class TestOpenerComponent {
    readonly request = signal<TestRequest | null>(null);
    readonly closed = vi.fn();
    readonly saved = vi.fn();
    readonly panel = injectSidePanel(this.request, {
        component: () => TestContentComponent,
        heading: (request) => request.title,
        inputs: (request) => ({ id: request.id }),
        outputs: { saved: (value: string) => this.saved(value) },
        closed: () => this.closed()
    });
}

describe('injectSidePanel', () => {
    let spectator: Spectator<TestOpenerComponent>;

    const createComponent = createComponentFactory({
        component: TestOpenerComponent,
        // Opening a panel collapses the main navigation.
        providers: [
            mockProvider(GlobalStore, {
                isNavigationCollapsed: vi.fn().mockReturnValue(false),
                collapseNavigation: vi.fn(),
                expandNavigation: vi.fn()
            })
        ]
    });

    beforeEach(() => (spectator = createComponent()));

    const panels = () => spectator.inject(DotSidePanelService).panels();

    const request = (value: TestRequest | null) => {
        spectator.component.request.set(value);
        spectator.detectChanges();
    };

    it('opens nothing while there is no request', () => {
        expect(panels()).toHaveLength(0);
        expect(spectator.component.panel.isOpen()).toBe(false);
    });

    it('opens the panel when a request is set, with its heading, inputs and outputs', () => {
        request({ title: 'header.vtl', id: 'a' });

        const [panel] = panels();
        expect(panels()).toHaveLength(1);
        expect(panel.heading()).toBe('header.vtl');
        expect(panel.component()).toBe(TestContentComponent);
        expect((panel.config.inputs?.['id'] as () => string)()).toBe('a');
        panel.config.outputs?.['saved']('done' as never);
        expect(spectator.component.saved).toHaveBeenCalledWith('done');
        expect(spectator.component.panel.isOpen()).toBe(true);
    });

    it('keeps the same panel open while the request changes, following it', () => {
        request({ title: 'header.vtl', id: 'a' });
        const [first] = panels();

        request({ title: 'footer.vtl', id: 'b' });

        expect(panels()).toEqual([first]);
        expect(first.heading()).toBe('footer.vtl');
        expect((first.config.inputs?.['id'] as () => string)()).toBe('b');
    });

    it('closes the panel when the request is cleared, without reporting a close', () => {
        request({ title: 'header.vtl', id: 'a' });

        request(null);

        expect(panels()).toHaveLength(0);
        expect(spectator.component.closed).not.toHaveBeenCalled();
    });

    it('reports a close the panel made itself', () => {
        request({ title: 'header.vtl', id: 'a' });

        panels()[0].ref.close();

        expect(spectator.component.closed).toHaveBeenCalledTimes(1);
        expect(spectator.component.panel.isOpen()).toBe(false);
    });

    it("routes the opener's close through the panel's guard", () => {
        request({ title: 'header.vtl', id: 'a' });
        const requestClose = vi.fn();
        panels()[0].ref.attach({ requestClose });

        spectator.component.panel.requestClose();

        expect(requestClose).toHaveBeenCalled();
    });

    it('closes the panel with the opener, without reporting a close', () => {
        request({ title: 'header.vtl', id: 'a' });
        const [panel] = panels();

        spectator.fixture.destroy();

        expect(panel.ref.isClosed).toBe(true);
        // The opener is going away: its handler would act on a screen that no longer exists.
        expect(spectator.component.closed).not.toHaveBeenCalled();
    });
});
