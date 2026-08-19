import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { A11yGroup } from '@dotcms/portlets/dot-ema/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotA11yPreviewComponent } from './a11y-preview.component';

import { StudioPageRow } from '../../models/accessibility-studio.models';
import { A11yMarkerService } from '../../services/a11y-marker.service';

const MOCK_PAGE: StudioPageRow = {
    identifier: 'id-1',
    title: 'About Us',
    path: '/about-us',
    type: 'htmlpageasset',
    languageId: 1,
    hostId: 'host-id-1',
    hostName: 'demo.dotcms.com',
    modDate: '04/09/2026',
    modUserName: 'Admin User',
    live: true
};

/**
 * The LIVE frame's findings, deliberately DIFFERENT from the preview's. The two frames
 * run separate scans, so a test that fed both the same array could not tell a correct
 * pairing from an inverted one.
 */
const MOCK_LIVE_GROUPS: A11yGroup[] = [
    {
        code: 'link-name',
        type: 'error',
        message: 'Links must have discernible text',
        impact: 'serious',
        helpUrl: 'https://example.com/link-name',
        items: [{ context: '<a>', selector: 'a.live-only' }],
        count: 1
    }
];

const MOCK_GROUPS: A11yGroup[] = [
    {
        code: 'image-alt',
        type: 'error',
        message: 'Images must have alternate text',
        impact: 'critical',
        helpUrl: 'https://example.com/image-alt',
        items: [{ context: '<img>', selector: 'img.a' }],
        count: 1
    }
];

describe('DotA11yPreviewComponent', () => {
    let spectator: Spectator<DotA11yPreviewComponent>;

    const createComponent = createComponentFactory({
        component: DotA11yPreviewComponent,
        componentProviders: [mockProvider(A11yMarkerService)],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'accessibility.studio.preview.mode.preview': 'Preview',
                    'accessibility.studio.preview.mode.live': 'Live'
                })
            }
        ]
    });

    /** Render with the page set and, by default, no findings and markers off. */
    function render(props: Partial<Record<string, unknown>> = {}) {
        spectator = createComponent({
            props: { page: MOCK_PAGE, ...props } as never
        });
        spectator.detectChanges();
    }

    describe('the two framed renders', () => {
        beforeEach(() => render());

        it('renders the PREVIEW (with-fixes) iframe on a /dot-page PREVIEW_MODE URL', () => {
            const iframe = spectator.query(byTestId('studio-preview-iframe'));
            expect(iframe).toBeTruthy();
            expect(iframe?.getAttribute('src')).toContain('/dot-page/about-us');
            expect(iframe?.getAttribute('src')).toContain('host_id=host-id-1');
            expect(iframe?.getAttribute('src')).toContain('mode=PREVIEW_MODE');
        });

        it('renders the LIVE (published) iframe on a /dot-page LIVE URL', () => {
            const iframe = spectator.query(byTestId('studio-live-iframe'));
            expect(iframe).toBeTruthy();
            expect(iframe?.getAttribute('src')).toContain('/dot-page/about-us');
            expect(iframe?.getAttribute('src')).toContain('host_id=host-id-1');
            expect(iframe?.getAttribute('src')).toContain('mode=LIVE');
            expect(iframe?.getAttribute('src')).not.toContain('PREVIEW_MODE');
        });

        it('shows both frames at once with their before/after labels', () => {
            expect(spectator.query(byTestId('studio-live-label'))).toBeTruthy();
            expect(spectator.query(byTestId('studio-preview-label'))).toBeTruthy();
            // No dropdown anymore — the two versions are shown simultaneously.
            expect(spectator.query(byTestId('studio-preview-mode-select'))).toBeFalsy();
        });

        it('renders no iframes at all without a page', () => {
            spectator.setInput('page', null);
            expect(spectator.query(byTestId('studio-preview-iframe'))).toBeFalsy();
            expect(spectator.query(byTestId('studio-live-iframe'))).toBeFalsy();
        });

        it('reloads the PREVIEW iframe when previewRevision advances (cache-buster)', () => {
            // Revision 0 → no cache-buster.
            expect(
                spectator.query(byTestId('studio-preview-iframe'))?.getAttribute('src')
            ).not.toContain('rev=');

            // A fix landing bumps the revision → src carries rev → iframe reloads.
            spectator.setInput('previewRevision', 3);
            expect(
                spectator.query(byTestId('studio-preview-iframe'))?.getAttribute('src')
            ).toContain('rev=3');
            // LIVE never reloads mid-run (published render is fixed).
            expect(
                spectator.query(byTestId('studio-live-iframe'))?.getAttribute('src')
            ).not.toContain('rev=');
        });
    });

    describe('what actually reaches the marker service', () => {
        // The `showMarkers` gate is only the input. The behaviour is the effect calling
        // render() with `show ? groups : []` for EACH frame, so an inverted ternary, or
        // the preview groups sent to the live frame, would otherwise pass silently.
        function renderCalls() {
            const marker = spectator.inject(A11yMarkerService, true);

            return (marker.render as jest.Mock).mock.calls;
        }

        it('draws each frame with its OWN scan findings', () => {
            render({
                previewGroups: MOCK_GROUPS,
                liveGroups: MOCK_LIVE_GROUPS,
                showMarkers: true
            });

            const groupsByFrame = new Map(renderCalls().map(([frame, groups]) => [frame, groups]));
            const preview = spectator.query('[data-testid="studio-preview-iframe"]');
            const live = spectator.query('[data-testid="studio-live-iframe"]');

            expect(groupsByFrame.get(preview)).toEqual(MOCK_GROUPS);
            expect(groupsByFrame.get(live)).toEqual(MOCK_LIVE_GROUPS);
        });

        it('clears BOTH frames rather than skipping the call when markers are off', () => {
            // Passing [] is what erases a previously drawn layer; not calling at all
            // would leave stale markers on screen.
            render({
                previewGroups: MOCK_GROUPS,
                liveGroups: MOCK_LIVE_GROUPS,
                showMarkers: false
            });

            const calls = renderCalls();
            expect(calls.length).toBeGreaterThanOrEqual(2);
            for (const [, groups] of calls) {
                expect(groups).toEqual([]);
            }
        });

        it('redraws a frame from its own scan when its document reloads', () => {
            // A load replaces the document, dropping the effect-drawn layer.
            render({
                previewGroups: MOCK_GROUPS,
                liveGroups: MOCK_LIVE_GROUPS,
                showMarkers: true
            });
            const marker = spectator.inject(A11yMarkerService, true);
            (marker.render as jest.Mock).mockClear();

            spectator.component.onLiveLoad();

            expect(marker.render).toHaveBeenCalledWith(
                spectator.query('[data-testid="studio-live-iframe"]'),
                MOCK_LIVE_GROUPS
            );
        });
    });

    describe('scroll sync', () => {
        // jsdom iframes don't lay out or scroll, so drive the same-origin
        // contentWindows directly and assert the mirror direction + guard.
        beforeEach(() => render());

        function fakeFrame(scrollX: number, scrollY: number) {
            const listeners: Array<() => void> = [];
            const win = {
                scrollX,
                scrollY,
                addEventListener: (_evt: string, cb: () => void) => listeners.push(cb),
                scrollTo: jest.fn((x: number, y: number) => {
                    win.scrollX = x;
                    win.scrollY = y;
                })
            };
            return {
                emitScroll: () => listeners.forEach((cb) => cb()),
                // Stands in for a DotA11yPreviewFrameComponent: the pane reads the
                // framed element through `element()`.
                element: () => ({ contentWindow: win }),
                win
            };
        }

        it('mirrors the live frame scroll onto the preview frame', () => {
            const live = fakeFrame(0, 0);
            const preview = fakeFrame(0, 0);
            jest.spyOn(spectator.component as never, '$liveFrame').mockReturnValue(live);
            jest.spyOn(spectator.component as never, '$previewFrame').mockReturnValue(preview);

            spectator.component.onLiveLoad();
            live.win.scrollX = 40;
            live.win.scrollY = 120;
            live.emitScroll();

            expect(preview.win.scrollTo).toHaveBeenCalledWith(40, 120);
        });

        it('does not bounce back (re-entrancy guard)', () => {
            const live = fakeFrame(0, 0);
            const preview = fakeFrame(0, 0);
            jest.spyOn(spectator.component as never, '$liveFrame').mockReturnValue(live);
            jest.spyOn(spectator.component as never, '$previewFrame').mockReturnValue(preview);

            // Wire BOTH directions, then scroll live once.
            spectator.component.onLiveLoad();
            spectator.component.onPreviewLoad();
            live.win.scrollY = 200;
            live.emitScroll();

            // preview mirrored live once; the echoed preview-scroll must NOT scroll
            // live back while the guard is set.
            expect(preview.win.scrollTo).toHaveBeenCalledTimes(1);
            preview.emitScroll();
            expect(live.win.scrollTo).not.toHaveBeenCalled();
        });
    });
});
