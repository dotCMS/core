import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { Subject } from 'rxjs';
import { Mock, vi } from 'vitest';

import { signal } from '@angular/core';

import { DotUiColorsService } from '@dotcms/data-access';
import { DotSidePanelRef } from '@dotcms/ui';

import { DotLegacyScreenSidePanelComponent } from './dot-legacy-screen-side-panel.component';

import { DotLegacyScreenRequest } from '../../models/dot-legacy-screen.model';

const REQUEST: DotLegacyScreenRequest = {
    url: '/html/ng-contentlet-selector.jsp?ng=true&container_id=c-1',
    title: 'Add content'
};

/** A stand-in for the legacy screen's window; the test DOM never loads the JSP. */
interface StubFrame {
    doc: Document;
    win: Window;
}

/** Keeps the test DOM from fetching the JSP for every rendered iframe. */
const disableIframePageLoading = (): void => {
    const happyDOM = (window as unknown as { happyDOM?: { settings: Record<string, unknown> } })
        .happyDOM;

    if (happyDOM) {
        happyDOM.settings['disableIframePageLoading'] = true;
    }
};

describe('DotLegacyScreenSidePanelComponent', () => {
    beforeAll(disableIframePageLoading);

    let spectator: Spectator<DotLegacyScreenSidePanelComponent>;
    let panel: {
        close: Mock;
        contentStyleClass: ReturnType<typeof signal<string>>;
        closed: Subject<void>;
    };

    const createComponent = createComponentFactory({
        component: DotLegacyScreenSidePanelComponent,
        providers: [mockProvider(DotUiColorsService, { setColors: vi.fn() })],
        detectChanges: false
    });

    beforeEach(() => {
        panel = { close: vi.fn(), contentStyleClass: signal('p-0!'), closed: new Subject<void>() };
        spectator = createComponent({
            providers: [{ provide: DotSidePanelRef, useValue: panel }]
        });
        vi.clearAllMocks();
    });

    const getIframe = (): HTMLIFrameElement | null =>
        spectator.query<HTMLIFrameElement>(byTestId('legacy-screen-iframe'), { root: true });

    const open = (request: DotLegacyScreenRequest | null = REQUEST): void => {
        spectator.setInput('request', request);
        spectator.detectChanges();
    };

    /** Replaces the iframe's window with a stand-in and fires `load`, as the browser does. */
    const loadFrame = (search = ''): StubFrame => {
        const iframe = getIframe() as HTMLIFrameElement;
        const doc = document.implementation.createHTMLDocument('legacy screen');
        const win = Object.assign(new EventTarget(), {
            document: doc,
            location: { search }
        }) as unknown as Window;

        Object.defineProperty(iframe, 'contentWindow', { configurable: true, get: () => win });
        iframe.dispatchEvent(new Event('load'));

        return { doc, win };
    };

    describe('URL', () => {
        it('loads a same-origin admin URL in the iframe', () => {
            open();

            expect(getIframe()?.getAttribute('src')).toBe(REQUEST.url);
            expect(getIframe()?.getAttribute('title')).toBe('Add content');
        });

        it('renders nothing for a URL that is not on this server', () => {
            open({ ...REQUEST, url: 'https://elsewhere.example/html/x.jsp' });

            expect(getIframe()).toBeNull();
        });

        it('renders nothing before it has a request', () => {
            open(null);

            expect(getIframe()).toBeNull();
        });
    });

    describe('events', () => {
        it('reports the screen as loaded, with its window', () => {
            open();
            const loaded = vi.fn();
            spectator.output('loaded').subscribe(loaded);

            const { win } = loadFrame();

            expect(loaded).toHaveBeenCalledWith(win);
        });

        it('passes every ng-event the screen dispatches through, unchanged', () => {
            open();
            const { doc } = loadFrame();
            const legacyEvent = vi.fn();
            spectator.output('legacyEvent').subscribe(legacyEvent);
            const detail = { name: 'select-contentlet', data: { inode: 'x', identifier: 'y' } };

            doc.dispatchEvent(new CustomEvent('ng-event', { detail }));

            expect(legacyEvent).toHaveBeenCalledWith(detail);
        });

        it('listens to the new document after every reload, and only once', () => {
            open();
            const first = loadFrame();
            const second = loadFrame();
            const legacyEvent = vi.fn();
            spectator.output('legacyEvent').subscribe(legacyEvent);

            first.doc.dispatchEvent(new CustomEvent('ng-event', { detail: { name: 'a' } }));
            second.doc.dispatchEvent(new CustomEvent('ng-event', { detail: { name: 'b' } }));

            expect(legacyEvent).toHaveBeenCalledTimes(1);
            expect(legacyEvent).toHaveBeenCalledWith({ name: 'b' });
        });

        it('applies the admin UI colors to the screen once it loads', () => {
            open();
            const { doc } = loadFrame();

            expect(spectator.inject(DotUiColorsService).setColors).toHaveBeenCalledWith(
                doc.querySelector('html')
            );
        });
    });

    describe('loading', () => {
        const spinner = () => spectator.query(byTestId('legacy-screen-loading'), { root: true });

        it('shows a spinner until the screen has loaded', () => {
            open();

            expect(spinner()).toBeTruthy();

            loadFrame();
            spectator.detectChanges();

            expect(spinner()).toBeNull();
        });

        it('shows it again while the screen reloads itself', () => {
            open();
            const { win } = loadFrame();
            spectator.detectChanges();

            win.dispatchEvent(new Event('pagehide'));
            spectator.detectChanges();

            expect(spinner()).toBeTruthy();
        });
    });

    describe('closing', () => {
        it('closes the panel from requestClose', () => {
            open();

            spectator.component.requestClose();

            expect(panel.close).toHaveBeenCalledTimes(1);
        });

        /** Every keydown on the admin page while `run` runs. */
        const keysOnAdminPage = (run: () => void): KeyboardEvent[] => {
            const keys: KeyboardEvent[] = [];
            const listener = (event: Event) => keys.push(event as KeyboardEvent);
            document.addEventListener('keydown', listener);
            run();
            document.removeEventListener('keydown', listener);

            return keys;
        };

        it('presses Escape again on the admin page when it is pressed inside the screen', () => {
            open();
            const { doc } = loadFrame();

            const keys = keysOnAdminPage(() =>
                doc.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
            );

            expect(keys).toEqual([expect.objectContaining({ key: 'Escape', cancelable: true })]);
        });

        it('keeps an Escape the screen already handled to itself', () => {
            open();
            const { doc } = loadFrame();
            const handled = new KeyboardEvent('keydown', {
                key: 'Escape',
                bubbles: true,
                cancelable: true
            });
            handled.preventDefault();

            const keys = keysOnAdminPage(() => doc.dispatchEvent(handled));

            expect(keys).toEqual([]);
        });
    });

    describe('layout', () => {
        it('asks the drawer for no padding and a flex row, so the iframe fills it', () => {
            open();

            expect(panel.contentStyleClass()).toBe('p-0! flex');
        });
    });
});
