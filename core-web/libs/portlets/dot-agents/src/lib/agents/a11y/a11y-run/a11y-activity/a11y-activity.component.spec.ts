import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { AgentRunStep } from '@dotcms/dotcms-models';
import { A11yGroup } from '@dotcms/portlets/dot-ema/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotA11yActivityComponent } from './a11y-activity.component';

import { SEVERITY_COLOR } from '../../models/a11y-severity';
import { StudioPhase } from '../../models/accessibility-studio.models';
import { MOCK_FIX_REPORT } from '../../models/mock-fix-report';

const ERROR_GROUPS: A11yGroup[] = [
    {
        code: 'image-alt',
        type: 'error',
        message: 'Images must have alternate text',
        impact: 'critical',
        helpUrl: 'https://example.com/image-alt',
        items: [{ context: '<img>', selector: 'img.a' }],
        count: 3
    },
    {
        code: 'button-name',
        type: 'error',
        message: 'Buttons must have discernible text',
        impact: 'serious',
        helpUrl: 'https://example.com/button-name',
        items: [{ context: '<button>', selector: 'button.x' }],
        count: 2
    }
];

const REVIEW_GROUPS: A11yGroup[] = [
    {
        code: 'color-contrast',
        type: 'warning',
        message: 'Elements must have sufficient color contrast',
        impact: 'moderate',
        helpUrl: 'https://example.com/color-contrast',
        items: [{ context: '<a>', selector: 'a.l1' }],
        count: 1
    },
    {
        // Not in the per-rule map — must fall back to the generic reason.
        code: 'some-unmapped-rule',
        type: 'warning',
        message: 'Something axe could not confirm',
        impact: 'minor',
        helpUrl: 'https://example.com/unmapped',
        items: [{ context: '<div>', selector: 'div.x' }],
        count: 1
    }
];

const LIVE_STEPS: AgentRunStep[] = [
    { message: 'Scanning live + working baseline', meta: { phase: 'scan' } },
    { message: 'Fixing color-contrast → .btn', meta: { phase: 'fix' } },
    // Leading "Agent:" role label the model sometimes prepends — the presenter
    // strips it so the log shows just the action.
    { message: 'Agent: reading activity.vtl', meta: { phase: 'read' } }
];

/** jsdom reports style bindings as `rgb(...)`, so compare the palette hex the same way. */
function asRgb(hex: string): string {
    const [r, g, b] = [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16));

    return `rgb(${r}, ${g}, ${b})`;
}

/** The severity dot's resolved color for an issue-type row. */
function dotColor(row: Element): string {
    return (row.querySelector('span') as HTMLElement).style.backgroundColor;
}

describe('DotA11yActivityComponent', () => {
    let spectator: Spectator<DotA11yActivityComponent>;

    const createComponent = createComponentFactory({
        component: DotA11yActivityComponent,
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
            }
        ]
    });

    function render(props: Record<string, unknown> = {}) {
        spectator = createComponent({
            props: { phase: 'scanned' as StudioPhase, ...props } as never
        });
        spectator.detectChanges();
    }

    describe('section header', () => {
        it.each([
            ['scanning', 'accessibility.studio.loghdr.scan'],
            ['scanned', 'accessibility.studio.loghdr.issues'],
            ['fixing', 'accessibility.studio.loghdr.activity'],
            ['done', 'accessibility.studio.loghdr.activity']
        ])('labels the %s phase with the %s key', (phase, key) => {
            render({ phase: phase as StudioPhase, finished: phase === 'done' });

            expect(spectator.element).toHaveText(key);
        });

        it('has no header at all in the ready state', () => {
            render({ phase: 'ready' });

            expect(spectator.element).not.toHaveText('accessibility.studio.loghdr');
        });

        it.each([
            ['scanning', true],
            ['fixing', true],
            ['scanned', false],
            ['done', false]
        ])('shows the working badge while %s: %s', (phase, expected) => {
            render({ phase: phase as StudioPhase, finished: phase === 'done' });

            expect(!!spectator.query(byTestId('studio-working-badge'))).toBe(expected);
        });
    });

    describe('bodies by phase', () => {
        it('explains what to expect before a scan', () => {
            render({ phase: 'ready' });

            expect(spectator.query(byTestId('studio-ready-card'))).toBeTruthy();
            expect(spectator.query(byTestId('studio-issue-type-list'))).toBeFalsy();
        });

        it('shows the skeleton, not the list, while scanning', () => {
            render({ phase: 'scanning', issueTypeGroups: ERROR_GROUPS });

            expect(spectator.query(byTestId('studio-issue-type-skeleton'))).toBeTruthy();
            expect(spectator.query(byTestId('studio-issue-type-list'))).toBeFalsy();
            expect(spectator.query(byTestId('studio-ready-card'))).toBeFalsy();
        });

        it('crossfades the real issue-type list in once scanned', () => {
            render({ issueTypeGroups: ERROR_GROUPS });

            expect(spectator.query(byTestId('studio-issue-type-list'))).toHaveClass(
                'studio-fade-in'
            );
            expect(spectator.query(byTestId('studio-issue-type-skeleton'))).toBeFalsy();
        });

        it('renders one issue-type row per rule, dotted by its severity color', () => {
            render({ issueTypeGroups: ERROR_GROUPS });

            const rows = spectator.queryAll(byTestId('studio-issue-type-row'));
            expect(rows.length).toBe(2);
            // critical vs serious — a shared color would mean impact was ignored.
            expect(dotColor(rows[0])).toBe(asRgb(SEVERITY_COLOR.critical));
            expect(dotColor(rows[1])).toBe(asRgb(SEVERITY_COLOR.serious));
        });
    });

    describe('needs-review rows', () => {
        it('resolves a per-rule reason, falling back to the generic one', () => {
            render({ reviewGroups: REVIEW_GROUPS });

            const rows = spectator.queryAll(byTestId('studio-review-row'));
            expect(rows.length).toBe(2);
            expect(rows[0]).toHaveText('accessibility.studio.review.reason.colorcontrast');
            expect(rows[1]).toHaveText('accessibility.studio.review.reason.default');
        });

        it('shows the section in the final report too', () => {
            render({ phase: 'done', finished: true, reviewGroups: REVIEW_GROUPS });

            expect(spectator.query(byTestId('studio-review-section'))).toBeTruthy();
        });

        it.each([
            ['ready', REVIEW_GROUPS],
            ['scanning', REVIEW_GROUPS],
            // Nothing incomplete → no section, even in a phase that would show it.
            ['scanned', []]
        ])('renders no section while %s', (phase, groups) => {
            render({ phase: phase as StudioPhase, reviewGroups: groups });

            expect(spectator.query(byTestId('studio-review-section'))).toBeFalsy();
        });
    });

    describe('agent activity', () => {
        it('renders one settled bubble per streamed step, plus a separate thinking item', () => {
            render({ phase: 'fixing', steps: LIVE_STEPS });

            // 3 streamed steps as settled message bubbles…
            expect(spectator.queryAll(byTestId('agent-message')).length).toBe(3);
            // …and the live state is its own thinking component, not a 4th message.
            expect(spectator.query(byTestId('agent-thinking'))).not.toBeNull();
        });

        it('renders the final report as bubbles once finished', () => {
            render({ phase: 'done', finished: true, report: MOCK_FIX_REPORT });

            // 7 fixed + 2 skipped + 3 framing steps (scan, locate, rescan). The mock's 3
            // `reported` rows are deferrals to the agentic pass, not unresolved work, so
            // they get no bubble.
            expect(spectator.queryAll(byTestId('agent-message')).length).toBe(12);
            expect(spectator.query(byTestId('agent-thinking'))).toBeNull();
        });

        it('renders nothing before a run starts', () => {
            render({ steps: LIVE_STEPS, report: MOCK_FIX_REPORT });

            // Steps and a report can be present from a previous pass; only the phase
            // decides what the log shows.
            expect(spectator.queryAll(byTestId('agent-message')).length).toBe(0);
        });
    });

    describe('the live thinking copy', () => {
        it('shows generic thinking copy — never the last step text', () => {
            render({ phase: 'fixing', steps: LIVE_STEPS });

            const thinking = spectator.query(byTestId('agent-thinking'));
            expect(thinking).not.toBeNull();
            // Always generic loading copy; must NOT echo the latest step.
            expect(thinking).not.toHaveText('reading activity.vtl');
            // No heartbeat yet → first cycling phrase.
            expect(thinking).toHaveText('Thinking…');
        });

        it('shows the elapsed seconds sub-line from the heartbeat', () => {
            render({
                phase: 'fixing',
                steps: LIVE_STEPS,
                heartbeat: { elapsedMs: 20000, sinceLastEventMs: 12000 }
            });

            const thinking = spectator.query(byTestId('agent-thinking'));
            // Still generic copy, never the step text.
            expect(thinking).not.toHaveText('reading activity.vtl');
            // Elapsed seconds on the current action ride along as the sub-line.
            expect(thinking).toHaveText('12s');
        });

        it('cycles the phrase as the current action runs', () => {
            render({
                phase: 'fixing',
                steps: LIVE_STEPS,
                heartbeat: { elapsedMs: 8000, sinceLastEventMs: 7000 }
            });

            // 7000/5000 → index 1.
            expect(spectator.query(byTestId('agent-thinking'))).toHaveText('Analyzing the page…');
        });

        it('keeps cycling reassurance copy on a very long step (loops, never freezes)', () => {
            // 5-minute step: index wraps (300000/5000 % 4 = 0 → "Thinking…"), so the
            // copy keeps moving rather than sticking on a "nearly done" phrase.
            render({
                phase: 'fixing',
                steps: LIVE_STEPS,
                heartbeat: { elapsedMs: 305000, sinceLastEventMs: 300000 }
            });

            const thinking = spectator.query(byTestId('agent-thinking'));
            expect(thinking).toHaveText('Thinking…');
            expect(thinking).toHaveText('300s');
        });
    });
});
