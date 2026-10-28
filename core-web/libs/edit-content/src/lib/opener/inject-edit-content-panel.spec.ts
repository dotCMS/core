import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/vitest';
import { Mock, vi } from 'vitest';

import { Component, isSignal, signal } from '@angular/core';

import { DotSidePanelRef } from '@dotcms/ui';

import { DotEditContentOpenerService } from './dot-edit-content-opener.service';
import { DotEditContentPanelBinding, injectEditContentPanel } from './inject-edit-content-panel';

import { DotEditContentRequest } from '../models/dot-edit-content-request.model';

const EDIT_REQUEST: DotEditContentRequest = {
    mode: 'edit',
    inode: 'abc',
    contentType: 'Blog',
    title: 'My post'
};

/** An opener that keeps a panel open while its request is set, as the portlets do. */
@Component({ selector: 'dot-test-opener', template: '' })
class TestOpenerComponent {
    readonly request = signal<DotEditContentRequest | null>(null);
    readonly closed = vi.fn();
    readonly saved = vi.fn();
    readonly panel = injectEditContentPanel(this.request, {
        saved: (contentlet) => this.saved(contentlet),
        closed: () => this.closed()
    });
}

describe('injectEditContentPanel', () => {
    let spectator: Spectator<TestOpenerComponent>;
    let open: Mock;
    /** The refs the opener handed out, in order. */
    let refs: DotSidePanelRef[];

    const createComponent = createComponentFactory({
        component: TestOpenerComponent,
        providers: [mockProvider(DotEditContentOpenerService, { open: vi.fn() })]
    });

    beforeEach(() => {
        refs = [];
        spectator = createComponent();
        open = spectator.inject(DotEditContentOpenerService).open as Mock;
        open.mockReset();
        open.mockImplementation((_request, options) => {
            const ref = new DotSidePanelRef(() => undefined);
            // The real opener reports `closed` from the ref; the mock does the same.
            ref.closed.subscribe(() => options.closed?.());
            refs.push(ref);

            return ref;
        });
    });

    const request = (value: DotEditContentRequest | null) => {
        spectator.component.request.set(value);
        spectator.detectChanges();
    };

    it('opens nothing while there is no request', () => {
        expect(open).not.toHaveBeenCalled();
        expect(spectator.component.panel.isOpen()).toBe(false);
    });

    it('opens the panel through the opener when a request is set, passing the handlers', () => {
        request(EDIT_REQUEST);

        expect(open).toHaveBeenCalledTimes(1);
        const [passedRequest, options] = open.mock.calls[0];
        expect(isSignal(passedRequest) ? passedRequest() : passedRequest).toEqual(EDIT_REQUEST);
        expect(options.injector).toBeDefined();
        options.saved?.({ inode: 'abc2' });
        expect(spectator.component.saved).toHaveBeenCalledWith({ inode: 'abc2' });
        expect(spectator.component.panel.isOpen()).toBe(true);
    });

    it('keeps the same panel open while the request changes, following it', () => {
        request(EDIT_REQUEST);

        request({ ...EDIT_REQUEST, title: 'Renamed' });

        expect(open).toHaveBeenCalledTimes(1);
        const [passedRequest] = open.mock.calls[0];
        expect(isSignal(passedRequest)).toBe(true);
        expect(passedRequest().title).toBe('Renamed');
    });

    it('closes the panel when the request is cleared, without reporting a close', () => {
        request(EDIT_REQUEST);

        request(null);

        expect(refs[0].isClosed).toBe(true);
        expect(spectator.component.closed).not.toHaveBeenCalled();
        expect(spectator.component.panel.isOpen()).toBe(false);
    });

    it('reports a close the panel made itself', () => {
        request(EDIT_REQUEST);

        refs[0].close();

        expect(spectator.component.closed).toHaveBeenCalledTimes(1);
        expect(spectator.component.panel.isOpen()).toBe(false);
    });

    it('opens a fresh panel when a request is set again after a self-close', () => {
        request(EDIT_REQUEST);
        refs[0].close();
        request(null);

        request({ ...EDIT_REQUEST, inode: 'other' });

        expect(open).toHaveBeenCalledTimes(2);
        expect(spectator.component.panel.isOpen()).toBe(true);
    });

    it("routes the opener's close through the panel's guard", () => {
        request(EDIT_REQUEST);
        const requestClose = vi.fn();
        refs[0].attach({ requestClose });

        spectator.component.panel.requestClose();

        expect(requestClose).toHaveBeenCalled();
    });
});

/** An opener whose binding decides the editor per open, as Content Drive's "switch" does. */
@Component({ selector: 'dot-test-forced-opener', template: '' })
class TestForcedOpenerComponent {
    readonly request = signal<DotEditContentRequest | null>(null);
    readonly forceLegacy = signal(false);
    readonly panel = injectEditContentPanel(this.request, this.#binding());

    /** A binding whose `editor` is a getter, read when the panel opens. */
    #binding(): DotEditContentPanelBinding {
        const forceLegacy = this.forceLegacy;

        return {
            get editor() {
                return forceLegacy() ? 'legacy' : undefined;
            }
        };
    }
}

describe('injectEditContentPanel, forcing the legacy editor', () => {
    let spectator: Spectator<TestForcedOpenerComponent>;
    let open: Mock;

    const createComponent = createComponentFactory({
        component: TestForcedOpenerComponent,
        providers: [mockProvider(DotEditContentOpenerService, { open: vi.fn() })]
    });

    beforeEach(() => {
        spectator = createComponent();
        open = spectator.inject(DotEditContentOpenerService).open as Mock;
        open.mockReset();
        open.mockImplementation(() => new DotSidePanelRef(() => undefined));
    });

    it('forwards the editor the binding names, read each time the panel opens', () => {
        spectator.component.request.set(EDIT_REQUEST);
        spectator.detectChanges();
        spectator.component.request.set(null);
        spectator.detectChanges();

        spectator.component.forceLegacy.set(true);
        spectator.component.request.set({ ...EDIT_REQUEST, inode: 'other' });
        spectator.detectChanges();

        expect(open).toHaveBeenCalledTimes(2);
        expect(open.mock.calls[0][1].editor).toBeUndefined();
        expect(open.mock.calls[1][1].editor).toBe('legacy');
    });
});
