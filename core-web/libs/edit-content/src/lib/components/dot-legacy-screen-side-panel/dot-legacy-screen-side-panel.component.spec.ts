import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { Subject } from 'rxjs';
import { Mock, vi } from 'vitest';

import { signal } from '@angular/core';

import { ZIndexUtils } from 'primeng/utils';

import { DotUiColorsService } from '@dotcms/data-access';
import { DotSidePanelEntry, DotSidePanelRef, DotSidePanelService } from '@dotcms/ui';

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
        requestClose: Mock;
        contentStyleClass: ReturnType<typeof signal<string>>;
        closed: Subject<void>;
    };

    const createComponent = createComponentFactory({
        component: DotLegacyScreenSidePanelComponent,
        providers: [mockProvider(DotUiColorsService, { setColors: vi.fn() })],
        detectChanges: false
    });

    /** The panels the side-panel service has open, in open order: the last is in front. */
    const openPanels = signal<DotSidePanelEntry[]>([]);
    const entry = (ref: unknown): DotSidePanelEntry => ({ ref }) as unknown as DotSidePanelEntry;

    beforeEach(() => {
        panel = {
            close: vi.fn(),
            requestClose: vi.fn(),
            contentStyleClass: signal('p-0!'),
            closed: new Subject<void>()
        };
        openPanels.set([entry(panel)]);
        spectator = createComponent({
            providers: [
                { provide: DotSidePanelRef, useValue: panel },
                { provide: DotSidePanelService, useValue: { panels: openPanels } }
            ]
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

        const pressEscape = (doc: Document, handled = false): void => {
            const event = new KeyboardEvent('keydown', {
                key: 'Escape',
                bubbles: true,
                cancelable: true
            });
            if (handled) {
                event.preventDefault();
            }
            doc.dispatchEvent(event);
        };

        afterEach(() => vi.restoreAllMocks());

        it('asks this panel to close when Escape is pressed inside the screen', () => {
            open();
            const { doc } = loadFrame();

            pressEscape(doc);

            expect(panel.requestClose).toHaveBeenCalledTimes(1);
        });

        it('closes this panel, not the one in front, when another panel is stacked above', () => {
            open();
            const { doc } = loadFrame();
            const front = { requestClose: vi.fn() };
            openPanels.set([entry(panel), entry(front)]);
            const keysOnAdminPage = vi.fn();
            document.addEventListener('keydown', keysOnAdminPage);

            pressEscape(doc);

            document.removeEventListener('keydown', keysOnAdminPage);
            expect(panel.requestClose).not.toHaveBeenCalled();
            expect(front.requestClose).not.toHaveBeenCalled();
            expect(keysOnAdminPage).not.toHaveBeenCalled();
        });

        it('leaves the key to an overlay open above the panel', () => {
            open();
            const { doc } = loadFrame();
            vi.spyOn(ZIndexUtils, 'getCurrent').mockReturnValue(1200);

            pressEscape(doc);

            expect(panel.requestClose).not.toHaveBeenCalled();
        });

        it('keeps an Escape the screen already handled to itself', () => {
            open();
            const { doc } = loadFrame();

            pressEscape(doc, true);

            expect(panel.requestClose).not.toHaveBeenCalled();
        });
    });

    describe('layout', () => {
        it('asks the drawer for no padding and a flex row, so the iframe fills it', () => {
            open();

            expect(panel.contentStyleClass()).toBe('p-0! flex');
        });
    });
});
