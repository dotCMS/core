import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { AgentRunStep } from '@dotcms/dotcms-models';
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
    const stopScan = jest.fn();
    const startFix = jest.fn();
    const stopAgent = jest.fn();
    const publish = jest.fn();
    const discard = jest.fn();
    const backToPicker = jest.fn();
    const setSkipCss = jest.fn();

    // Mutable per-test state read by the store mock's reactive getters.
    let phase: StudioPhase = 'ready';
    let report: FixReport | null = null;
    let steps: AgentRunStep[] = [];
    let fixError: string | null = null;
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
        steps: () => steps,
        fixError: () => fixError,
        latestStep: () => (steps.length ? steps[steps.length - 1] : null),
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
        openCount: () => report?.scan.after.violations ?? (hasScan ? 5 : 0),
        // 3 critical (image-alt) + 2 serious (button-name) + 0 moderate/minor errors.
        severityCounts: () => ({
            critical: hasScan ? 3 : 0,
            serious: hasScan ? 2 : 0,
            moderate: 0,
            minor: 0
        }),
        issueTypeRows: () => (hasScan ? MOCK_GROUPS.filter((g) => g.type === 'error') : []),
        reviewGroups: () => (hasScan ? MOCK_GROUPS.filter((g) => g.type === 'warning') : []),
        fixedResults: () => report?.results.filter((r) => r.status === 'fixed-to-working') ?? [],
        reportedResults: () =>
            report?.results.filter((r) => r.status !== 'fixed-to-working') ?? [],
        fixedCount: () =>
            report?.results.filter((r) => r.status === 'fixed-to-working').length ?? 0,
        reportedCount: () =>
            report?.results.filter((r) => r.status !== 'fixed-to-working').length ?? 0,
        runScan,
        stopScan,
        startFix,
        stopAgent,
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

    function render(
        nextPhase: StudioPhase,
        nextReport: FixReport | null = null,
        nextSteps: AgentRunStep[] = [],
        nextFixError: string | null = null
    ) {
        phase = nextPhase;
        report = nextReport;
        steps = nextSteps;
        fixError = nextFixError;
        // A scan result exists once the page has been scanned.
        hasScan = ['scanned', 'fixing', 'done', 'published'].includes(nextPhase);
        spectator = createComponent();
        spectator.detectChanges();
    }

    beforeEach(() => {
        jest.clearAllMocks();
        phase = 'ready';
        report = null;
        steps = [];
        fixError = null;
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

        it('hides the score widget in the ready state (before scanning)', () => {
            expect(spectator.query(byTestId('studio-score-ring'))).toBeFalsy();
            expect(spectator.query(byTestId('studio-score-count'))).toBeFalsy();
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

        it('renders the BY ISSUE TYPE list — one row per error rule', () => {
            // MOCK_GROUPS has 2 error groups (image-alt, button-name) + 1 warning.
            expect(spectator.queryAll(byTestId('studio-issue-type-row')).length).toBe(2);
        });

        it('renders the severity legend (non-empty buckets)', () => {
            const legend = spectator.query(byTestId('studio-severity-legend'));
            expect(legend).toBeTruthy();
            // critical + serious have counts; moderate/minor are 0 → hidden when scanned.
            expect(legend).toHaveText('Critical');
            expect(legend).toHaveText('Serious');
        });

        it('shows the re-scan icon button', () => {
            expect(spectator.query(byTestId('studio-rescan-btn'))).toBeTruthy();
        });

        it('surfaces needs-review items separately (not in the fix count)', () => {
            // mock warningCount = 2 → the note renders (the mock message service returns
            // the key verbatim); the donut count stays 5 (confirmed errors only).
            expect(spectator.query(byTestId('studio-needsreview-note'))).toBeTruthy();
            expect(spectator.query(byTestId('studio-score-count'))).toHaveText('5');
        });

        it('renders the needs-review section with a row per incomplete rule', () => {
            // MOCK_GROUPS has 1 warning group (color-contrast).
            expect(spectator.query(byTestId('studio-review-section'))).toBeTruthy();
            expect(spectator.queryAll(byTestId('studio-review-row')).length).toBe(1);
        });
    });

    describe('marker visibility (showMarkers)', () => {
        it('is off before a scan', () => {
            render('ready');
            expect(spectator.component.showMarkers()).toBe(false);
        });

        it('is on in BOTH preview modes while scanned (pre-fix)', () => {
            render('scanned', MOCK_FIX_REPORT);
            spectator.component.previewMode.set('PREVIEW_MODE');
            expect(spectator.component.showMarkers()).toBe(true);
            spectator.component.previewMode.set('LIVE');
            expect(spectator.component.showMarkers()).toBe(true);
        });

        it('is LIVE-only once fixes exist (done) — PREVIEW would be stale', () => {
            render('done', MOCK_FIX_REPORT);
            spectator.component.previewMode.set('PREVIEW_MODE');
            expect(spectator.component.showMarkers()).toBe(false);
            spectator.component.previewMode.set('LIVE');
            expect(spectator.component.showMarkers()).toBe(true);
        });
    });

    describe('scanning phase', () => {
        beforeEach(() => render('scanning'));

        it('renders the scanning mini-log', () => {
            expect(spectator.query(byTestId('studio-scanning-log'))).toBeTruthy();
        });

        it('shows the Stop scan button and triggers stopScan', () => {
            const btn = spectator
                .query(byTestId('studio-stopscan-btn'))
                ?.querySelector('button');
            expect(btn).toBeTruthy();
            spectator.click(btn as HTMLElement);
            expect(stopScan).toHaveBeenCalled();
        });

        it('does not show the issue-type list while scanning', () => {
            expect(spectator.query(byTestId('studio-issue-type-list'))).toBeFalsy();
        });
    });

    describe('fixing phase (live stream)', () => {
        const LIVE_STEPS: AgentRunStep[] = [
            { message: 'Scanning live + working baseline', meta: { phase: 'scan' } },
            { message: 'Fixing color-contrast → .btn', meta: { phase: 'fix' } },
            { message: 'Agent: reading activity.vtl', meta: { phase: 'read' } }
        ];

        beforeEach(() => render('fixing', null, LIVE_STEPS));

        it('shows the Stop agent button', () => {
            expect(spectator.query(byTestId('studio-stopagent-btn'))).toBeTruthy();
        });

        it('triggers stopAgent on click', () => {
            const btn = spectator.query(byTestId('studio-stopagent-btn'))?.querySelector('button');
            spectator.click(btn as HTMLElement);
            expect(stopAgent).toHaveBeenCalled();
        });

        it('renders one live activity step per streamed event', () => {
            expect(spectator.queryAll(byTestId('agent-message')).length).toBe(3);
        });

        it('shows the latest step in the now-doing banner', () => {
            const banner = spectator.query(byTestId('agent-now-doing'));
            expect(banner).toHaveText('Agent: reading activity.vtl');
        });
    });

    describe('fix error state', () => {
        beforeEach(() => render('scanned', MOCK_FIX_REPORT, [], 'render unreliable'));

        it('surfaces the agent error inline', () => {
            const error = spectator.query(byTestId('studio-fix-error'));
            expect(error).toHaveText('render unreliable');
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

        it('still shows the needs-review section in the report', () => {
            expect(spectator.query(byTestId('studio-review-section'))).toBeTruthy();
        });

        it('renders an activity step per result plus scan/locate/rescan framing', () => {
            // 7 fixed + 5 reported + 3 framing steps (scan, locate, rescan)
            expect(spectator.queryAll(byTestId('agent-message')).length).toBe(15);
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

        it('renders the preview iframe with a /dot-page PREVIEW_MODE URL by default', () => {
            const iframe = spectator.query(byTestId('studio-preview-iframe'));
            expect(iframe).toBeTruthy();
            expect(iframe?.getAttribute('src')).toContain('/dot-page/about-us');
            expect(iframe?.getAttribute('src')).toContain('host_id=host-id-1');
            expect(iframe?.getAttribute('src')).toContain('mode=PREVIEW_MODE');
        });

        it('shows the preview/live mode select', () => {
            expect(spectator.query(byTestId('studio-preview-mode-select'))).toBeTruthy();
        });

        it('switches the iframe to LIVE when previewMode is set to LIVE', () => {
            spectator.component.previewMode.set('LIVE');
            spectator.detectChanges();
            const iframe = spectator.query(byTestId('studio-preview-iframe'));
            expect(iframe?.getAttribute('src')).toContain('mode=LIVE');
            expect(iframe?.getAttribute('src')).not.toContain('PREVIEW_MODE');
        });
    });

    it('triggers backToPicker from the back button', () => {
        render('ready');
        const btn = spectator.query(byTestId('studio-back-btn'))?.querySelector('button');
        spectator.click(btn as HTMLElement);
        expect(backToPicker).toHaveBeenCalled();
    });
});
