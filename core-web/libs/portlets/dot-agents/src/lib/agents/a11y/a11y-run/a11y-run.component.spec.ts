import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { Component, input, output, signal } from '@angular/core';
import { ActivatedRoute, Router, UrlSegment } from '@angular/router';

import { DotMessageService } from '@dotcms/data-access';
import { AgentHeartbeat, AgentRunStep } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotA11yRunComponent } from './a11y-run.component';

import { DotA11yDiffViewerComponent } from '../a11y-diff/a11y-diff-viewer.component';
import { DotA11yDiffComponent } from '../a11y-diff/a11y-diff.component';
import { A11yGroup } from '../models/a11y-groups';
import {
    FixReport,
    NEEDS_ATTENTION_STATUSES,
    RESEARCH_RULE_ID,
    StudioPageRow,
    StudioPhase
} from '../models/accessibility-studio.models';
import { MOCK_FIX_REPORT } from '../models/mock-fix-report';
import { PageDiffFile } from '../models/page-render-sources.models';
import { A11yMarkerService } from '../services/a11y-marker.service';
import { A11yRunStore } from '../store/a11y-run.store';

/**
 * Stubs for the diff pieces so the run spec doesn't pull in Monaco / HTTP. The list
 * emits the picked file; the viewer just records what it was handed.
 */
@Component({ selector: 'dot-a11y-diff', standalone: true, template: '' })
class DotA11yDiffStubComponent {
    readonly activeFileId = input<string | null>(null);
    readonly fileSelected = output<PageDiffFile | null>();
    readonly changedCount = output<number>();
}

@Component({ selector: 'dot-a11y-diff-viewer', standalone: true, template: '' })
class DotA11yDiffViewerStubComponent {
    readonly file = input<PageDiffFile | null>(null);
    readonly closed = output<void>();
}

const DIFF_FILE: PageDiffFile = {
    identifier: 'vtl-1',
    path: '//demo/application/containers/awazon/a.vtl',
    name: 'a.vtl',
    extension: 'vtl',
    origin: 'container',
    working: 'new',
    live: 'old',
    added: 1,
    removed: 1
};

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

describe('DotA11yRunComponent', () => {
    let spectator: Spectator<DotA11yRunComponent>;

    const runScan = jest.fn();
    const stopScan = jest.fn();
    const startFix = jest.fn();
    const stopAgent = jest.fn();
    const publish = jest.fn();
    const discard = jest.fn();
    const setSkipCss = jest.fn();
    const openPageByUri = jest.fn();
    const navigate = jest.fn();

    // Mutable per-test state read by the store mock's reactive getters.
    let phase: StudioPhase = 'ready';
    let report: FixReport | null = null;
    let steps: AgentRunStep[] = [];
    let fixError: string | null = null;
    let rehydrateStatus: 'idle' | 'loading' | 'not-found' = 'idle';
    let heartbeat: AgentHeartbeat | null = null;
    // Bumped when the working render changes → preview iframe cache-buster.
    let previewRevision = 0;
    // Whether a scan result is present (drives report vs. iframe in the pane).
    let hasScan = false;
    // The page path (as URL segments) the run component reads on init.
    let pathSegments = ['about-us'];
    // The current site — the run rehydrate effect waits for it before fetching.
    const currentSiteIdSignal = signal<string | null>('site-1');

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
        heartbeat: () => heartbeat,
        selected: () => MOCK_PAGE,
        skipCss: () => false,
        scanResult: () => (hasScan ? ({ standard: 'WCAG2AA' } as unknown) : null),
        liveScanResult: () => (hasScan ? ({ standard: 'WCAG2AA' } as unknown) : null),
        a11yGroups: () => (hasScan ? MOCK_GROUPS : []),
        liveA11yGroups: () => (hasScan ? MOCK_GROUPS : []),
        errorCount: () => (hasScan ? 5 : 0),
        warningCount: () => (hasScan ? 2 : 0),
        isWorking: () => phase === 'scanning' || phase === 'fixing',
        finished: () => ['done', 'published'].includes(phase),
        runStarted: () => ['fixing', 'done', 'published'].includes(phase),
        hasResults: () => ['scanned', 'fixing', 'done', 'published'].includes(phase),
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
        fixedResults: () =>
            report?.results.filter(
                (r) => r.status === 'fixed-to-working' && r.ruleId !== RESEARCH_RULE_ID
            ) ?? [],
        reportedResults: () =>
            report?.results.filter((r) => NEEDS_ATTENTION_STATUSES.includes(r.status)) ?? [],
        // Both counts come from the before/after rescan, not from row statuses — the
        // rows only log the deterministic pass. See the real store's computeds.
        fixedCount: () =>
            report ? Math.max(0, report.scan.before.violations - report.scan.after.violations) : 0,
        reportedCount: () => report?.scan.after.violations ?? 0,
        rehydrateStatus: () => rehydrateStatus,
        previewRevision: () => previewRevision,
        runScan,
        stopScan,
        startFix,
        stopAgent,
        publish,
        discard,
        setSkipCss,
        openPageByUri
    };

    const createComponent = createComponentFactory({
        component: DotA11yRunComponent,
        overrideComponents: [
            [
                DotA11yRunComponent,
                {
                    remove: {
                        imports: [DotA11yDiffComponent, DotA11yDiffViewerComponent]
                    },
                    add: {
                        imports: [DotA11yDiffStubComponent, DotA11yDiffViewerStubComponent]
                    }
                }
            ]
        ],
        componentProviders: [
            { provide: A11yRunStore, useValue: storeMock },
            mockProvider(A11yMarkerService)
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'accessibility.studio.working.thinking': 'Thinking…',
                    'accessibility.studio.working.analyzing': 'Analyzing the page…',
                    'accessibility.studio.working.reasoning': 'Working through the fix…',
                    'accessibility.studio.working.stillworking': 'Still working on it…',
                    'accessibility.studio.working.elapsed': '{0}s'
                })
            },
            { provide: Router, useValue: { navigate } },
            {
                provide: GlobalStore,
                useValue: {
                    get currentSiteId() {
                        return currentSiteIdSignal;
                    }
                }
            },
            {
                provide: ActivatedRoute,
                // useFactory so the segments are read at injection time (per test),
                // not when the component factory config is first evaluated.
                useFactory: () => ({
                    url: of(pathSegments.map((p) => new UrlSegment(p, {})))
                })
            }
        ]
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
        rehydrateStatus = 'idle';
        heartbeat = null;
        previewRevision = 0;
        hasScan = false;
        pathSegments = ['about-us'];
        currentSiteIdSignal.set('site-1');
        // Report reduced-motion so the score count-up snaps to its final value
        // synchronously (no requestAnimationFrame timing in the DOM assertions).
        window.matchMedia = jest
            .fn()
            .mockReturnValue({ matches: true }) as unknown as typeof matchMedia;
    });

    describe('side panel accordion', () => {
        beforeEach(() => render('ready'));

        /** Click a panel's PrimeNG accordion header. */
        const clickHeader = (panel: 'scanner' | 'files') => {
            const header = spectator
                .query(byTestId(`studio-panel-${panel}`))
                ?.querySelector('p-accordion-header') as HTMLElement;
            spectator.click(header);
            spectator.detectChanges();
        };

        it('opens with the scanner panel expanded and files collapsed', () => {
            expect(spectator.component.isPanelOpen('scanner')).toBe(true);
            expect(spectator.component.isPanelOpen('files')).toBe(false);
            // p-accordion-content keeps content mounted (hideStrategy="visibility"),
            // so the files list keeps resolving the delta while collapsed.
            expect(spectator.query(byTestId('studio-panel-files-body'))).toBeTruthy();
        });

        it('both panels can be open at once', () => {
            clickHeader('files');

            expect(spectator.component.isPanelOpen('files')).toBe(true);
            // Opening files must NOT collapse the scanner.
            expect(spectator.component.isPanelOpen('scanner')).toBe(true);
            expect(spectator.component.openPanels()).toEqual(['scanner', 'files']);
        });

        it('each panel collapses independently', () => {
            clickHeader('files');
            clickHeader('scanner');

            expect(spectator.component.isPanelOpen('scanner')).toBe(false);
            expect(spectator.component.isPanelOpen('files')).toBe(true);
        });

        it('both panels can be closed at once', () => {
            clickHeader('scanner');

            expect(spectator.component.isPanelOpen('scanner')).toBe(false);
            expect(spectator.component.isPanelOpen('files')).toBe(false);
            expect(spectator.component.openPanels()).toEqual([]);
        });

        it('tracks the changed-file count reported by the diff list', () => {
            expect(spectator.component.changedFileCount()).toBe(0);
            expect(spectator.component.hasChangedFiles()).toBe(false);

            const list = spectator.query(DotA11yDiffStubComponent) as DotA11yDiffStubComponent;
            list.changedCount.emit(2);
            spectator.detectChanges();

            expect(spectator.component.changedFileCount()).toBe(2);
            expect(spectator.component.hasChangedFiles()).toBe(true);
        });
    });

    describe('files panel actions', () => {
        beforeEach(() => render('done', MOCK_FIX_REPORT));

        /** Report N changed files from the stubbed list. */
        const reportFiles = (n: number) => {
            const list = spectator.query(DotA11yDiffStubComponent) as DotA11yDiffStubComponent;
            list.changedCount.emit(n);
            spectator.detectChanges();
        };

        it('shows no action bar until there are files to publish', () => {
            expect(spectator.query(byTestId('studio-publish-bar'))).toBeFalsy();
        });

        it('shows Discard next to Publish once files changed', () => {
            reportFiles(2);

            expect(spectator.query(byTestId('studio-publish-bar'))).toBeTruthy();
            expect(spectator.query(byTestId('studio-discard-btn'))).toBeTruthy();
            expect(spectator.query(byTestId('studio-apply-btn'))).toBeTruthy();
        });

        it('Publish publishes the page', () => {
            reportFiles(1);
            spectator.click(
                spectator
                    .query(byTestId('studio-apply-btn'))
                    ?.querySelector('button') as HTMLElement
            );
            expect(publish).toHaveBeenCalled();
        });

        it('Discard drops the working fixes', () => {
            reportFiles(1);
            spectator.click(
                spectator
                    .query(byTestId('studio-discard-btn'))
                    ?.querySelector('button') as HTMLElement
            );
            expect(discard).toHaveBeenCalled();
        });

        // The changed files may predate this run (an earlier run, a manual edit), so
        // the actions are gated on the files existing — not on the run's phase.
        it.each(['ready', 'scanned', 'published'] as StudioPhase[])(
            'shows both actions in the %s phase when files changed',
            (studioPhase) => {
                render(studioPhase, studioPhase === 'ready' ? null : MOCK_FIX_REPORT);
                reportFiles(2);

                expect(spectator.query(byTestId('studio-discard-btn'))).toBeTruthy();
                expect(spectator.query(byTestId('studio-apply-btn'))).toBeTruthy();
            }
        );
    });

    describe('ready phase', () => {
        beforeEach(() => render('ready'));

        it('shows the scan button', () => {
            expect(spectator.query(byTestId('studio-scan-btn'))).toBeTruthy();
        });

        it('does not show the skip-css toggle yet (it is a fix option)', () => {
            expect(spectator.query(byTestId('studio-skipcss-toggle'))).toBeFalsy();
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

        it('shows the changed-files list before a run completes', () => {
            // The list resolves the working-vs-live delta itself, so it's present
            // (and loading) from the moment the page opens — no scan required.
            expect(spectator.query(DotA11yDiffStubComponent)).toBeTruthy();
        });

        it('shows the preview, not a diff, until a file is picked', () => {
            expect(spectator.component.diffFile()).toBeNull();
            expect(spectator.query(DotA11yDiffViewerStubComponent)).toBeFalsy();
        });
    });

    describe('scanned phase', () => {
        beforeEach(() => render('scanned', MOCK_FIX_REPORT));

        it('shows the fix button', () => {
            expect(spectator.query(byTestId('studio-fix-btn'))).toBeTruthy();
        });

        it('shows the skip-css toggle (a fix option, offered before Fix)', () => {
            expect(spectator.query(byTestId('studio-skipcss-toggle'))).toBeTruthy();
        });

        it('shows the real open-count in the ring', () => {
            expect(spectator.query(byTestId('studio-score-count'))).toHaveText('5');
        });

        it('animates the score count up to the open-count (snaps under reduced motion)', () => {
            // reduced-motion is mocked on, so displayCount snaps to the target.
            expect(spectator.component.displayCount()).toBe(5);
        });

        it('crossfades the real issue-type list in (over the skeleton)', () => {
            expect(spectator.query(byTestId('studio-issue-type-list'))).toHaveClass(
                'studio-fade-in'
            );
            // The skeleton is only shown while scanning, not in the scanned state.
            expect(spectator.query(byTestId('studio-issue-type-skeleton'))).toBeFalsy();
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
        // Markers only ever go on the LIVE frame, which always still carries the
        // original scan's violations. The only gate is: a scan has produced findings.
        it('is off before a scan', () => {
            render('ready');
            expect(spectator.component.showMarkers()).toBe(false);
        });

        it('is on once scanned (pre-fix)', () => {
            render('scanned', MOCK_FIX_REPORT);
            expect(spectator.component.showMarkers()).toBe(true);
        });

        it('stays on after fixes exist (done) — the LIVE frame is still unfixed', () => {
            render('done', MOCK_FIX_REPORT);
            expect(spectator.component.showMarkers()).toBe(true);
        });
    });

    describe('scanning phase', () => {
        beforeEach(() => render('scanning'));

        it('renders the results skeleton (score + issue list) with the results footprint', () => {
            expect(spectator.query(byTestId('studio-score-skeleton'))).toBeTruthy();
            expect(spectator.query(byTestId('studio-issue-type-skeleton'))).toBeTruthy();
        });

        it('shows the Stop scan button and triggers stopScan', () => {
            const btn = spectator.query(byTestId('studio-stopscan-btn'))?.querySelector('button');
            expect(btn).toBeTruthy();
            spectator.click(btn as HTMLElement);
            expect(stopScan).toHaveBeenCalled();
        });

        it('shows the skeleton, not the real widget or issue list, while scanning', () => {
            expect(spectator.query(byTestId('studio-issue-type-list'))).toBeFalsy();
            expect(spectator.query(byTestId('studio-score-ring'))).toBeFalsy();
        });

        // Guards the `phase() !== 'ready'` negations: written as `!phase() === 'ready'`
        // they'd parse as `(!phase()) === 'ready'` — always false — silently hiding the
        // section header and showing the ready-state explainer mid-scan.
        it('shows the section header and hides the ready explainer once scanning', () => {
            expect(spectator.query(byTestId('studio-working-badge'))).toBeTruthy();
            expect(spectator.query(byTestId('studio-ready-card'))).toBeFalsy();
        });
    });

    describe('fixing phase (live stream)', () => {
        const LIVE_STEPS: AgentRunStep[] = [
            { message: 'Scanning live + working baseline', meta: { phase: 'scan' } },
            { message: 'Fixing color-contrast → .btn', meta: { phase: 'fix' } },
            // Leading "Agent:" role label the model sometimes prepends — the
            // presenter strips it so the log/banner show just the action.
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

        it('renders one settled bubble per streamed step, plus a separate thinking item', () => {
            // 3 streamed steps as settled message bubbles…
            expect(spectator.queryAll(byTestId('agent-message')).length).toBe(3);
            // …and the live state is its own thinking component, not a 4th message.
            expect(spectator.query(byTestId('agent-thinking'))).not.toBeNull();
        });

        it('shows generic thinking copy — never the last step text', () => {
            const thinking = spectator.query(byTestId('agent-thinking'));
            expect(thinking).not.toBeNull();
            // Always generic loading copy; must NOT echo the latest step.
            expect(thinking).not.toHaveText('reading activity.vtl');
            // No heartbeat yet → first cycling phrase.
            expect(thinking).toHaveText('Thinking…');
        });

        it('shows the elapsed seconds sub-line from the heartbeat', () => {
            heartbeat = { elapsedMs: 20000, sinceLastEventMs: 12000 };
            render('fixing', null, LIVE_STEPS);
            const thinking = spectator.query(byTestId('agent-thinking'));
            // Still generic copy, never the step text.
            expect(thinking).not.toHaveText('reading activity.vtl');
            // Elapsed seconds on the current action ride along as the sub-line.
            expect(thinking).toHaveText('12s');
        });

        it('keeps cycling reassurance copy on a very long step (loops, never freezes)', () => {
            // 5-minute step: index wraps (300000/5000 % 4 = 0 → "Thinking…"), so the
            // copy keeps moving rather than sticking on a "nearly done" phrase.
            heartbeat = { elapsedMs: 305000, sinceLastEventMs: 300000 };
            render('fixing', null, LIVE_STEPS);
            const thinking = spectator.query(byTestId('agent-thinking'));
            expect(thinking).toHaveText('Thinking…');
            expect(thinking).toHaveText('300s');
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

        it('offers a jump to the files panel — discard/publish live there', () => {
            expect(spectator.query(byTestId('studio-reviewfiles-btn'))).toBeTruthy();
            // Both actions belong to the files panel, and it has no files yet.
            expect(spectator.query(byTestId('studio-discard-btn'))).toBeFalsy();
            expect(spectator.query(byTestId('studio-apply-btn'))).toBeFalsy();
        });

        it('Review files opens the files panel, leaving the scanner open', () => {
            const btn = spectator
                .query(byTestId('studio-reviewfiles-btn'))
                ?.querySelector('button');
            spectator.click(btn as HTMLElement);
            spectator.detectChanges();

            expect(spectator.component.isPanelOpen('files')).toBe(true);
            expect(spectator.component.isPanelOpen('scanner')).toBe(true);
        });

        it('Review files is idempotent — it opens rather than toggles', () => {
            const btn = () =>
                spectator.query(byTestId('studio-reviewfiles-btn'))?.querySelector('button');
            spectator.click(btn() as HTMLElement);
            spectator.detectChanges();
            spectator.click(btn() as HTMLElement);
            spectator.detectChanges();

            // A second press must not close the panel it just opened.
            expect(spectator.component.isPanelOpen('files')).toBe(true);
        });

        it('renders an activity step per result plus scan/locate/rescan framing', () => {
            // 7 fixed + 2 skipped + 3 framing steps (scan, locate, rescan). The mock's 3
            // `reported` rows are deferrals to the agentic pass, not unresolved work, so
            // they get no bubble.
            expect(spectator.queryAll(byTestId('agent-message')).length).toBe(12);
        });

        it('shows the after-count in the ring', () => {
            expect(spectator.query(byTestId('studio-score-count'))).toHaveText('5');
        });

        it('still shows the needs-review section in the report', () => {
            expect(spectator.query(byTestId('studio-review-section'))).toBeTruthy();
        });

        it('picking a file in the list opens its diff in the right pane', () => {
            const list = spectator.query(DotA11yDiffStubComponent) as DotA11yDiffStubComponent;
            list.fileSelected.emit(DIFF_FILE);
            spectator.detectChanges();

            expect(spectator.component.diffFile()).toEqual(DIFF_FILE);
            expect(spectator.query(DotA11yDiffViewerStubComponent)?.file()).toEqual(DIFF_FILE);
            // Opening a diff is a view swap, not a navigation — run state is kept.
            expect(navigate).not.toHaveBeenCalled();
        });

        it("the viewer's close action returns to the preview and clears the list", () => {
            const list = spectator.query(DotA11yDiffStubComponent) as DotA11yDiffStubComponent;
            list.fileSelected.emit(DIFF_FILE);
            spectator.detectChanges();
            // The list's highlighted row tracks the pane via activeFileId.
            expect(list.activeFileId()).toBe(DIFF_FILE.identifier);

            const viewer = spectator.query(
                DotA11yDiffViewerStubComponent
            ) as DotA11yDiffViewerStubComponent;
            viewer.closed.emit();
            spectator.detectChanges();

            expect(spectator.component.diffFile()).toBeNull();
            expect(spectator.query(DotA11yDiffViewerStubComponent)).toBeFalsy();
            expect(list.activeFileId()).toBeNull();
        });

        it('the list can also clear the selection itself (back to preview)', () => {
            const list = spectator.query(DotA11yDiffStubComponent) as DotA11yDiffStubComponent;
            list.fileSelected.emit(DIFF_FILE);
            spectator.detectChanges();
            expect(spectator.component.diffFile()).toEqual(DIFF_FILE);

            list.fileSelected.emit(null);
            spectator.detectChanges();
            expect(spectator.component.diffFile()).toBeNull();
        });
    });

    describe('published phase', () => {
        beforeEach(() => render('published', MOCK_FIX_REPORT));

        it('shows the all-pages button', () => {
            expect(spectator.query(byTestId('studio-allpages-btn'))).toBeTruthy();
        });
    });

    describe('preview pane (side-by-side diff)', () => {
        beforeEach(() => render('ready'));

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

        it('reloads the PREVIEW iframe when previewRevision advances (cache-buster)', () => {
            // Revision 0 → no cache-buster.
            expect(
                spectator.query(byTestId('studio-preview-iframe'))?.getAttribute('src')
            ).not.toContain('rev=');

            // A fix landing bumps the revision → src carries rev → iframe reloads.
            previewRevision = 3;
            render('fixing', null, [{ message: 'working', meta: { phase: 'fix' } }]);
            expect(
                spectator.query(byTestId('studio-preview-iframe'))?.getAttribute('src')
            ).toContain('rev=3');
            // LIVE never reloads mid-run (published render is fixed).
            expect(
                spectator.query(byTestId('studio-live-iframe'))?.getAttribute('src')
            ).not.toContain('rev=');
        });
    });

    describe('scroll sync', () => {
        // jsdom iframes don't lay out or scroll, so drive the same-origin
        // contentWindows directly and assert the mirror direction + guard.
        beforeEach(() => render('ready'));

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
                nativeElement: { contentWindow: win },
                win
            };
        }

        it('mirrors the live frame scroll onto the preview frame', () => {
            const live = fakeFrame(0, 0);
            const preview = fakeFrame(0, 0);
            jest.spyOn(spectator.component as never, 'liveFrame').mockReturnValue(live);
            jest.spyOn(spectator.component as never, 'previewFrame').mockReturnValue(preview);

            spectator.component.onLiveLoad();
            live.win.scrollX = 40;
            live.win.scrollY = 120;
            live.emitScroll();

            expect(preview.win.scrollTo).toHaveBeenCalledWith(40, 120);
        });

        it('does not bounce back (re-entrancy guard)', () => {
            const live = fakeFrame(0, 0);
            const preview = fakeFrame(0, 0);
            jest.spyOn(spectator.component as never, 'liveFrame').mockReturnValue(live);
            jest.spyOn(spectator.component as never, 'previewFrame').mockReturnValue(preview);

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

    it('navigates up to the picker from the back button', () => {
        render('ready');
        const btn = spectator.query(byTestId('studio-back-btn'))?.querySelector('button');
        spectator.click(btn as HTMLElement);
        // No store reset — the per-route run store is destroyed on navigation.
        expect(navigate).toHaveBeenCalledWith(
            ['..'],
            expect.objectContaining({ relativeTo: expect.anything() })
        );
    });

    describe('deep link (page path)', () => {
        it('rehydrates the store from the route page path on init', () => {
            pathSegments = ['blog', 'post', 'hello'];
            render('ready');
            expect(openPageByUri).toHaveBeenCalledWith('/blog/post/hello');
        });

        it('does not rehydrate until the current site is known', () => {
            currentSiteIdSignal.set(null);
            render('ready');
            expect(openPageByUri).not.toHaveBeenCalled();
        });

        it('bounces back to the picker when the deep-linked page is not found', () => {
            rehydrateStatus = 'not-found';
            render('ready');
            expect(navigate).toHaveBeenCalledWith(
                ['..'],
                expect.objectContaining({ relativeTo: expect.anything() })
            );
        });
    });
});
