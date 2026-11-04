import {
    Spectator,
    byTestId,
    createComponentFactory,
    mockProvider
} from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { Clipboard } from '@angular/cdk/clipboard';
import { WritableSignal, reflectComponentType, signal } from '@angular/core';

import { MessageService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';
import { DotSite } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { UVE_MODE } from '@dotcms/types';
import { WINDOW } from '@dotcms/utils';
import { MockDotMessageService, createFakeSite } from '@dotcms/utils-testing';

import { DotUveBrowserToolbarComponent } from './dot-uve-browser-toolbar.component';

import { DotPageApiParams } from '../../../services/dot-page-api/dot-page-api.service';
import { DEFAULT_DEVICE, DEFAULT_DEVICES, PERSONA_KEY } from '../../../shared/consts';
import { MOCK_RESPONSE_HEADLESS } from '../../../shared/mocks';
import { UVEStore } from '../../../store/dot-uve.store';
import { DeviceSelectorState } from '../../../store/features/editor/models';
import { Orientation } from '../../../store/models';
import { DotUveDeviceControlsComponent } from '../dot-uve-device-controls/dot-uve-device-controls.component';

const PAGE_PARAMS: DotPageApiParams = {
    url: '/my-page',
    clientHost: 'https://example.com',
    language_id: '1',
    [PERSONA_KEY]: 'dot:persona'
};

const TABLET = DEFAULT_DEVICES[1];

const MESSAGES = {
    Copied: 'Copied',
    'editpage.header.copy': 'Copy URL',
    'uve.toolbar.page.live.url': 'Live URL',
    'uve.toolbar.page.current.view.url': 'Current View URL',
    'uve.toolbar.page.current.site.url': 'Site URL',
    'uve.palette.toggle.open': 'Open palette',
    'uve.palette.toggle.close': 'Close palette',
    'uve.edit-panel.toggle.open': 'Open panel',
    'uve.edit-panel.toggle.close': 'Close panel'
};

/**
 * The part of `UVEStore` the toolbar and the controls it places read and write. The panel setters
 * write back, as the real store's do, so a test can follow a click through to the toggle's name.
 */
function createUVEStoreMock() {
    const editorPaletteOpen = signal(true);
    const editorEditPanelOpen = signal(true);

    return {
        viewMode: signal(UVE_MODE.EDIT),
        pageParams: signal<DotPageApiParams>(PAGE_PARAMS),
        pageAsset: signal(MOCK_RESPONSE_HEADLESS),
        editorPaletteOpen,
        editorEditPanelOpen,
        setPaletteOpen: vi.fn((open: boolean) => editorPaletteOpen.set(open)),
        setEditPanelOpen: vi.fn((open: boolean) => editorEditPanelOpen.set(open)),
        $viewDeviceSelectorState: signal<DeviceSelectorState>({
            device: DEFAULT_DEVICE,
            socialMedia: null,
            orientation: Orientation.PORTRAIT
        }),
        viewApplyDeviceSelectorChange: vi.fn(),
        viewZoomReset: vi.fn(),
        // Read and written by the size input and the zoom controls, which talk to the store
        // themselves.
        viewIframeWidth: signal(1200),
        viewIframeHeight: signal(800),
        viewResizeIframe: vi.fn(),
        viewZoomLevel: signal(100),
        viewZoomSetLevel: vi.fn()
    };
}

describe('DotUveBrowserToolbarComponent', () => {
    let spectator: Spectator<DotUveBrowserToolbarComponent>;
    let store: ReturnType<typeof createUVEStoreMock>;
    let siteDetails: WritableSignal<DotSite | null>;

    const createComponent = createComponentFactory({
        component: DotUveBrowserToolbarComponent,
        providers: [
            { provide: UVEStore, useFactory: () => store },
            { provide: GlobalStore, useFactory: () => ({ siteDetails }) },
            { provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) },
            { provide: WINDOW, useValue: window },
            mockProvider(MessageService),
            mockProvider(Clipboard)
        ],
        detectChanges: false
    });

    /** PrimeNG renders the clickable, named `<button>` inside the `p-button` host. */
    const buttonIn = (host: Element | null): HTMLButtonElement => {
        const button = host?.querySelector('button');

        if (!button) {
            throw new Error('Expected a p-button with a <button> inside');
        }

        return button;
    };

    const queryButton = (testId: string) => buttonIn(spectator.query(byTestId(testId)));

    const openCopyUrlPopover = () => {
        spectator.click(queryButton('uve-toolbar-copy-url'));
        spectator.detectChanges();
    };

    beforeEach(() => {
        store = createUVEStoreMock();
        siteDetails = signal(createFakeSite({ hostname: 'demo.dotcms.com' }));
        spectator = createComponent();
    });

    describe('layout', () => {
        it('should show the controls in order: palette toggle, copy URL, page URL, device controls, zoom reset, size input, zoom controls, edit-panel toggle', () => {
            const order = [
                'uve-toolbar-palette-toggle',
                'uve-toolbar-copy-url',
                'uve-toolbar-page-url',
                'uve-device-controls',
                'zoom-reset',
                'uve-iframe-size-input',
                'uve-zoom-controls',
                'right-sidebar-toggle'
            ];

            spectator.detectChanges();

            const rendered = spectator
                .queryAll('[data-testid]')
                .map((element) => element.getAttribute('data-testid'))
                .filter((testId): testId is string => testId !== null && order.includes(testId));

            expect(rendered).toEqual(order);
        });

        it.each([UVE_MODE.PREVIEW, UVE_MODE.LIVE])(
            'should hide the palette and edit-panel toggles in %s mode',
            (mode) => {
                store.viewMode.set(mode);
                spectator.detectChanges();

                expect(spectator.query(byTestId('uve-toolbar-palette-toggle'))).toBeNull();
                expect(spectator.query(byTestId('right-sidebar-toggle'))).toBeNull();
                expect(spectator.query(byTestId('uve-toolbar-copy-url'))).toBeTruthy();
                expect(spectator.query(byTestId('uve-device-controls'))).toBeTruthy();
            }
        );

        it('should have no inputs or outputs', () => {
            const mirror = reflectComponentType(DotUveBrowserToolbarComponent);

            expect(mirror?.inputs).toEqual([]);
            expect(mirror?.outputs).toEqual([]);
        });
    });

    describe('page URL', () => {
        it.each([
            {
                case: 'joins the client host and the page path',
                url: '/my-page',
                clientHost: 'https://example.com',
                expected: 'https://example.com/my-page'
            },
            {
                case: 'drops a trailing /index',
                url: '/my-page/index',
                clientHost: 'https://example.com',
                expected: 'https://example.com/my-page'
            },
            {
                case: 'drops a trailing /index.html',
                url: '/my-page/index.html',
                clientHost: 'https://example.com',
                expected: 'https://example.com/my-page'
            },
            {
                case: 'falls back to the window origin without a client host',
                url: '/my-page',
                clientHost: undefined,
                expected: `${window.location.origin}/my-page`
            },
            {
                case: 'falls back to the root path without a url',
                url: '',
                clientHost: 'https://example.com',
                expected: 'https://example.com/'
            }
        ])('should show the live URL in the URL bar: $case', ({ url, clientHost, expected }) => {
            store.pageParams.set({ ...PAGE_PARAMS, url, clientHost });
            spectator.detectChanges();

            expect(spectator.query(byTestId('uve-toolbar-page-url'))).toHaveText(expected);
        });
    });

    describe('copy-URL popover', () => {
        it('should list the live URL, the current view URL and the current site URL', () => {
            const siteId = MOCK_RESPONSE_HEADLESS.site.identifier;

            spectator.detectChanges();
            openCopyUrlPopover();

            const labels = spectator
                .queryAll(byTestId('copy-url-label'), { root: true })
                .map((label) => label.textContent.trim());
            const links = spectator
                .queryAll(byTestId('copy-url-link'), { root: true })
                .map((link) => link.getAttribute('href'));

            expect(labels).toEqual(['Live URL', 'Current View URL', 'Site URL']);
            expect(links).toEqual([
                'https://example.com/my-page',
                `https://example.com/my-page?language_id=1&com.dotmarketing.persona.id=dot%3Apersona&host_id=${siteId}`,
                'https://demo.dotcms.com/my-page'
            ]);
        });

        it('should leave the site URL out when no site is selected', () => {
            siteDetails.set(null);
            spectator.detectChanges();
            openCopyUrlPopover();

            const labels = spectator
                .queryAll(byTestId('copy-url-label'), { root: true })
                .map((label) => label.textContent.trim());

            expect(labels).toEqual(['Live URL', 'Current View URL']);
        });

        it('should copy the chosen URL and show the "Copied" toast', () => {
            spectator.detectChanges();
            openCopyUrlPopover();

            const [liveUrlCopyButton] = spectator.queryAll(byTestId('copy-url-button'), {
                root: true
            });
            spectator.click(buttonIn(liveUrlCopyButton));

            expect(spectator.inject(Clipboard).copy).toHaveBeenCalledWith(
                'https://example.com/my-page'
            );
            expect(spectator.inject(MessageService).add).toHaveBeenCalledWith({
                severity: 'success',
                summary: 'Copied',
                life: 3000
            });
        });
    });

    describe('palette toggle', () => {
        it('should close the palette when it is open', () => {
            store.editorPaletteOpen.set(true);
            spectator.detectChanges();

            spectator.click(queryButton('uve-toolbar-palette-toggle'));

            expect(store.setPaletteOpen).toHaveBeenCalledWith(false);
        });

        it('should open the palette when it is closed', () => {
            store.editorPaletteOpen.set(false);
            spectator.detectChanges();

            spectator.click(queryButton('uve-toolbar-palette-toggle'));

            expect(store.setPaletteOpen).toHaveBeenCalledWith(true);
        });

        it('should name the toggle after the palette state', () => {
            store.editorPaletteOpen.set(true);
            spectator.detectChanges();

            expect(queryButton('uve-toolbar-palette-toggle')).toHaveAttribute(
                'aria-label',
                'Close palette'
            );

            spectator.click(queryButton('uve-toolbar-palette-toggle'));
            spectator.detectChanges();

            expect(queryButton('uve-toolbar-palette-toggle')).toHaveAttribute(
                'aria-label',
                'Open palette'
            );
        });
    });

    describe('edit-panel toggle', () => {
        it('should close the edit panel when it is open', () => {
            store.editorEditPanelOpen.set(true);
            spectator.detectChanges();

            spectator.click(queryButton('right-sidebar-toggle'));

            expect(store.setEditPanelOpen).toHaveBeenCalledWith(false);
        });

        it('should open the edit panel when it is closed', () => {
            store.editorEditPanelOpen.set(false);
            spectator.detectChanges();

            spectator.click(queryButton('right-sidebar-toggle'));

            expect(store.setEditPanelOpen).toHaveBeenCalledWith(true);
        });

        it('should name the toggle after the edit-panel state', () => {
            store.editorEditPanelOpen.set(true);
            spectator.detectChanges();

            expect(queryButton('right-sidebar-toggle')).toHaveAttribute(
                'aria-label',
                'Close panel'
            );

            spectator.click(queryButton('right-sidebar-toggle'));
            spectator.detectChanges();

            expect(queryButton('right-sidebar-toggle')).toHaveAttribute('aria-label', 'Open panel');
        });
    });

    describe('device controls', () => {
        it("should pass the store's device selector state to the device controls", () => {
            const state: DeviceSelectorState = {
                device: TABLET,
                socialMedia: 'facebook',
                orientation: Orientation.LANDSCAPE
            };
            store.$viewDeviceSelectorState.set(state);
            spectator.detectChanges();

            expect(spectator.query(DotUveDeviceControlsComponent)?.$state()).toEqual(state);
        });

        it('should apply a chosen device through the store', () => {
            spectator.detectChanges();

            spectator.click(byTestId('tablet'));

            expect(store.viewApplyDeviceSelectorChange).toHaveBeenCalledWith({
                type: 'device',
                device: TABLET
            });
        });

        it('should apply an orientation change through the store', () => {
            store.$viewDeviceSelectorState.set({
                device: TABLET,
                socialMedia: null,
                orientation: Orientation.PORTRAIT
            });
            spectator.detectChanges();

            spectator.click(byTestId('orientation'));

            expect(store.viewApplyDeviceSelectorChange).toHaveBeenCalledWith({
                type: 'orientation',
                orientation: Orientation.LANDSCAPE
            });
        });
    });

    describe('zoom and size', () => {
        it('should reset zoom and canvas size through the store', () => {
            spectator.detectChanges();

            spectator.click(queryButton('zoom-reset'));

            expect(store.viewZoomReset).toHaveBeenCalled();
        });

        it('should place the size input, showing the iframe size from the store', () => {
            spectator.detectChanges();

            expect(spectator.query(byTestId('iframe-size-width'))).toHaveValue('1200');
            expect(spectator.query(byTestId('iframe-size-height'))).toHaveValue('800');
        });

        it('should place the zoom controls', () => {
            spectator.detectChanges();

            expect(spectator.query(byTestId('uve-zoom-controls'))).toBeTruthy();
        });
    });
});
