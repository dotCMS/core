import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAccessibilityStudioRunComponent } from './dot-accessibility-studio-run.component';

import { A11yGroup } from '../models/a11y-groups';
import { FixReport, StudioPageRow, StudioPhase } from '../models/accessibility-studio.models';
import { MOCK_FIX_REPORT } from '../models/mock-fix-report';
import { A11yMarkerService } from '../services/a11y-marker.service';
import { AccessibilityStudioStore } from '../store/accessibility-studio.store';

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

describe('DotAccessibilityStudioRunComponent', () => {
    let spectator: Spectator<DotAccessibilityStudioRunComponent>;

    const runScan = jest.fn();
    const startFix = jest.fn();
    const publish = jest.fn();
    const discard = jest.fn();
    const backToPicker = jest.fn();
    const setSkipCss = jest.fn();

    // Mutable per-test state read by the store mock's reactive getters.
    let phase: StudioPhase = 'ready';
    let report: FixReport | null = null;
    // Whether a scan result is present (drives report vs. iframe in the pane).
    let hasScan = false;

    // Two error groups (5 elements) + one warning group (2 elements).
    const MOCK_GROUPS: A11yGroup[] = [
        {
            code: 'image-alt',
            type: 'error',
            message: 'Images must have alternate text',
            impact: 'critical',
            helpUrl: 'https://example.com/image-alt',
            items: [
                { context: '<img>', selector: 'img.a' },
                { context: '<img>', selector: 'img.b' },
                { context: '<img>', selector: 'img.c' }
            ],
            count: 3
        },
        {
            code: 'button-name',
            type: 'error',
            message: 'Buttons must have discernible text',
            impact: 'serious',
            helpUrl: 'https://example.com/button-name',
            items: [
                { context: '<button>', selector: 'button.x' },
                { context: '<button>', selector: 'button.y' }
            ],
            count: 2
        },
        {
            code: 'color-contrast',
            type: 'warning',
            message: 'Elements must have sufficient color contrast',
            impact: 'moderate',
            helpUrl: 'https://example.com/color-contrast',
            items: [{ context: '<a>', selector: 'a.l1' }],
            count: 1
        }
    ];

    const storeMock = {
        phase: () => phase,
        report: () => report,
        selected: () => MOCK_PAGE,
        skipCss: () => false,
        scanResult: () => (hasScan ? ({ standard: 'WCAG2AA' } as unknown) : null),
        a11yGroups: () => (hasScan ? MOCK_GROUPS : []),
        errorCount: () => (hasScan ? 5 : 0),
        warningCount: () => (hasScan ? 2 : 0),
        isReady: () => phase === 'ready',
        isScanning: () => phase === 'scanning',
        isScanned: () => phase === 'scanned',
        isFixing: () => phase === 'fixing',
        isDone: () => phase === 'done',
        isPublished: () => phase === 'published',
        isWorking: () => phase === 'scanning' || phase === 'fixing',
        scanned: () => ['scanned', 'fixing', 'done', 'published'].includes(phase),
        beforeCount: () => (hasScan ? 5 : 0),
        afterCount: () => report?.scan.after.violations ?? 0,
        fixedResults: () => report?.results.filter((r) => r.status === 'fixed-to-working') ?? [],
        reportedResults: () =>
            report?.results.filter((r) => r.status !== 'fixed-to-working') ?? [],
        fixedCount: () =>
            report?.results.filter((r) => r.status === 'fixed-to-working').length ?? 0,
        reportedCount: () =>
            report?.results.filter((r) => r.status !== 'fixed-to-working').length ?? 0,
        runScan,
        startFix,
        publish,
        discard,
        backToPicker,
        setSkipCss
    };

    const createComponent = createComponentFactory({
        component: DotAccessibilityStudioRunComponent,
        componentProviders: [
            { provide: AccessibilityStudioStore, useValue: storeMock },
            mockProvider(A11yMarkerService)
        ],
        providers: [{ provide: DotMessageService, useValue: new MockDotMessageService({}) }]
    });

    function render(nextPhase: StudioPhase, nextReport: FixReport | null = null) {
        phase = nextPhase;
        report = nextReport;
        // A scan result exists once the page has been scanned.
        hasScan = ['scanned', 'fixing', 'done', 'published'].includes(nextPhase);
        spectator = createComponent();
        spectator.detectChanges();
    }

    beforeEach(() => {
        jest.clearAllMocks();
        phase = 'ready';
        report = null;
        hasScan = false;
    });

    describe('ready phase', () => {
        beforeEach(() => render('ready'));

        it('shows the scan button', () => {
            expect(spectator.query(byTestId('studio-scan-btn'))).toBeTruthy();
        });

        it('shows the skip-css toggle', () => {
            expect(spectator.query(byTestId('studio-skipcss-toggle'))).toBeTruthy();
        });

        it('shows a dash in the score ring before scanning', () => {
            expect(spectator.query(byTestId('studio-score-count'))).toHaveText('–');
        });

        it('triggers runScan on click', () => {
            const btn = spectator.query(byTestId('studio-scan-btn'))?.querySelector('button');
            spectator.click(btn as HTMLElement);
            expect(runScan).toHaveBeenCalled();
        });
    });

    describe('scanned phase', () => {
        beforeEach(() => render('scanned', MOCK_FIX_REPORT));

        it('shows the fix button', () => {
            expect(spectator.query(byTestId('studio-fix-btn'))).toBeTruthy();
        });

        it('shows the real open-count in the ring', () => {
            expect(spectator.query(byTestId('studio-score-count'))).toHaveText('5');
        });

        it('keeps the preview iframe visible after scanning', () => {
            expect(spectator.query(byTestId('studio-preview-iframe'))).toBeTruthy();
        });

        it('triggers startFix on click', () => {
            const btn = spectator.query(byTestId('studio-fix-btn'))?.querySelector('button');
            spectator.click(btn as HTMLElement);
            expect(startFix).toHaveBeenCalled();
        });
    });

    describe('done phase', () => {
        beforeEach(() => render('done', MOCK_FIX_REPORT));

        it('shows publish + discard buttons', () => {
            expect(spectator.query(byTestId('studio-publish-btn'))).toBeTruthy();
            expect(spectator.query(byTestId('studio-discard-btn'))).toBeTruthy();
        });

        it('shows the after-count in the ring', () => {
            expect(spectator.query(byTestId('studio-score-count'))).toHaveText('5');
        });

        it('renders a recipe step per result plus scan/locate/rescan framing', () => {
            // 7 fixed + 5 reported + 3 framing steps (scan, locate, rescan)
            expect(spectator.queryAll(byTestId('studio-recipe-step')).length).toBe(15);
        });

        it('triggers publish on click', () => {
            const btn = spectator
                .query(byTestId('studio-publish-btn'))
                ?.querySelector('button');
            spectator.click(btn as HTMLElement);
            expect(publish).toHaveBeenCalled();
        });

        it('triggers discard on click', () => {
            const btn = spectator
                .query(byTestId('studio-discard-btn'))
                ?.querySelector('button');
            spectator.click(btn as HTMLElement);
            expect(discard).toHaveBeenCalled();
        });
    });

    describe('published phase', () => {
        beforeEach(() => render('published', MOCK_FIX_REPORT));

        it('shows the all-pages button', () => {
            expect(spectator.query(byTestId('studio-allpages-btn'))).toBeTruthy();
        });
    });

    describe('preview pane', () => {
        beforeEach(() => render('ready'));

        it('renders the preview iframe with a /dot-page edit-mode URL', () => {
            const iframe = spectator.query(byTestId('studio-preview-iframe'));
            expect(iframe).toBeTruthy();
            expect(iframe?.getAttribute('src')).toContain('/dot-page/about-us');
            expect(iframe?.getAttribute('src')).toContain('host_id=host-id-1');
            expect(iframe?.getAttribute('src')).toContain('mode=EDIT_MODE');
        });
    });

    it('triggers backToPicker from the back button', () => {
        render('ready');
        const btn = spectator.query(byTestId('studio-back-btn'))?.querySelector('button');
        spectator.click(btn as HTMLElement);
        expect(backToPicker).toHaveBeenCalled();
    });
});
