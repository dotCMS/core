import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { UIChart } from 'primeng/chart';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotA11yScoreComponent } from './a11y-score.component';

import { SeverityCounts } from '../../models/a11y-severity';
import { StudioPhase } from '../../models/accessibility-studio.models';

/** 3 critical + 2 serious, nothing moderate/minor — two populated buckets, two empty. */
const COUNTS: SeverityCounts = { critical: 3, serious: 2, moderate: 0, minor: 0 };
const NO_COUNTS: SeverityCounts = { critical: 0, serious: 0, moderate: 0, minor: 0 };

describe('DotA11yScoreComponent', () => {
    let spectator: Spectator<DotA11yScoreComponent>;

    const createComponent = createComponentFactory({
        component: DotA11yScoreComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'accessibility.studio.score.open': 'OPEN',
                    'accessibility.studio.score.now': 'NOW',
                    'accessibility.studio.score.before': 'BEFORE',
                    'accessibility.studio.score.found': 'ISSUES FOUND',
                    'accessibility.studio.score.fixing': 'FIXING ISSUES',
                    'accessibility.studio.score.remaining': 'ISSUES REMAINING'
                })
            }
        ]
    });

    /** A scanned widget with 5 open issues unless overridden. */
    function render(props: Record<string, unknown> = {}) {
        spectator = createComponent({
            props: {
                phase: 'scanned' as StudioPhase,
                hasResults: true,
                openCount: 5,
                severityCounts: COUNTS,
                ...props
            } as never
        });
        spectator.detectChanges();
    }

    beforeEach(() => {
        // Report reduced-motion so the count-up snaps to its final value synchronously
        // (no requestAnimationFrame timing in the DOM assertions).
        window.matchMedia = jest
            .fn()
            .mockReturnValue({ matches: true }) as unknown as typeof matchMedia;
    });

    describe('skeleton / results swap', () => {
        it('shows only the skeleton while scanning', () => {
            render({ phase: 'scanning', hasResults: false });

            expect(spectator.query(byTestId('studio-score-skeleton'))).toBeTruthy();
            expect(spectator.query(byTestId('studio-score-ring'))).toBeFalsy();
        });

        it('shows neither before a scan has run', () => {
            render({ phase: 'ready', hasResults: false });

            expect(spectator.query(byTestId('studio-score-skeleton'))).toBeFalsy();
            expect(spectator.query(byTestId('studio-score-ring'))).toBeFalsy();
        });

        it('crossfades the widget in once there are results', () => {
            render();

            expect(spectator.query(byTestId('studio-score-skeleton'))).toBeFalsy();
            expect(spectator.query(byTestId('studio-score-ring'))).toBeTruthy();
        });
    });

    describe('the rolling count', () => {
        it('snaps to the open count under reduced motion', () => {
            render();

            expect(spectator.query(byTestId('studio-score-count'))).toHaveText('5');
        });

        it('stays parked at 0 until a scan has results', () => {
            // Otherwise a rescan would start rolling from the previous run's number.
            render({ phase: 'scanning', hasResults: false, openCount: 5 });
            spectator.setInput('hasResults', true);

            expect(spectator.query(byTestId('studio-score-count'))).toHaveText('5');
        });

        it('rolls to the new target when the count changes mid-fix', () => {
            render({ phase: 'fixing', runStarted: true, openCount: 5 });
            spectator.setInput('openCount', 2);

            expect(spectator.query(byTestId('studio-score-count'))).toHaveText('2');
        });

        it('eases over rAF frames when motion is allowed, landing on the target', () => {
            // Without reduced motion the number is animated across frames, so it must
            // still END on the open count — an easing that never reaches 1 would park
            // the ring one short forever.
            window.matchMedia = jest
                .fn()
                .mockReturnValue({ matches: false }) as unknown as typeof matchMedia;
            jest.useFakeTimers();

            render();
            expect(spectator.query(byTestId('studio-score-count'))).toHaveText('0');

            // Past the 600ms duration → the final frame clamps t to 1.
            jest.advanceTimersByTime(700);
            spectator.detectChanges();

            expect(spectator.query(byTestId('studio-score-count'))).toHaveText('5');
            jest.useRealTimers();
        });
    });

    describe('severity legend', () => {
        it('hides empty buckets while scanned', () => {
            render();

            const legend = spectator.query(byTestId('studio-severity-legend'));
            expect(legend).toHaveText('Critical');
            expect(legend).toHaveText('Serious');
            expect(legend).not.toHaveText('Moderate');
            expect(legend).not.toHaveText('Minor');
        });

        it('keeps empty buckets once a run started, so a bucket can be seen reaching 0', () => {
            render({ phase: 'fixing', runStarted: true });

            const legend = spectator.query(byTestId('studio-severity-legend'));
            expect(legend).toHaveText('Moderate');
            expect(legend).toHaveText('Minor');
        });
    });

    describe('donut data', () => {
        function chartData() {
            return (spectator.query(UIChart) as UIChart).data as {
                labels: string[];
                datasets: { data: number[] }[];
            };
        }

        it('draws one arc per severity, in severity order', () => {
            render();

            expect(chartData().labels).toEqual(['Critical', 'Serious', 'Moderate', 'Minor']);
            expect(chartData().datasets[0].data).toEqual([3, 2, 0, 0]);
        });

        it('draws a single full "clear" ring when nothing is open', () => {
            // A zero-value doughnut renders nothing at all, so the ring would vanish
            // exactly when the run succeeded.
            render({ phase: 'done', runStarted: true, openCount: 0, severityCounts: NO_COUNTS });

            expect(chartData().labels).toEqual(['Clear']);
            expect(chartData().datasets[0].data).toEqual([1]);
        });
    });

    describe('phase-dependent copy', () => {
        it.each([
            ['scanned', 'ISSUES FOUND'],
            ['fixing', 'FIXING ISSUES'],
            ['done', 'ISSUES REMAINING'],
            ['published', 'ISSUES REMAINING']
        ])('headlines %s with "%s"', (phase, expected) => {
            render({ phase: phase as StudioPhase, runStarted: phase !== 'scanned' });

            expect(spectator.query(byTestId('studio-score-ring'))?.parentElement).toHaveText(
                expected
            );
        });

        it('shows the before → now delta only once a run started', () => {
            render({ beforeCount: 9 });
            expect(spectator.element).not.toHaveText('BEFORE');

            spectator.setInput('runStarted', true);
            expect(spectator.element).toHaveText('BEFORE');
            expect(spectator.element).toHaveText('9');
        });
    });

    it('surfaces needs-review items as a note, outside the counted total', () => {
        render({ warningCount: 2 });

        expect(spectator.query(byTestId('studio-needsreview-note'))).toBeTruthy();
        // The ring still counts confirmed errors only.
        expect(spectator.query(byTestId('studio-score-count'))).toHaveText('5');
    });

    it('renders no note when nothing needs review', () => {
        render({ warningCount: 0 });

        expect(spectator.query(byTestId('studio-needsreview-note'))).toBeFalsy();
    });
});
