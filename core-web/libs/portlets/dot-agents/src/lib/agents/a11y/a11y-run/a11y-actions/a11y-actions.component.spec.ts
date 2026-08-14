import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotA11yActionsComponent } from './a11y-actions.component';

import { StudioPhase } from '../../models/accessibility-studio.models';

describe('DotA11yActionsComponent', () => {
    let spectator: Spectator<DotA11yActionsComponent>;

    const createComponent = createComponentFactory({
        component: DotA11yActionsComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'accessibility.studio.footer.scanned.title': '{0} issues to fix',
                    'accessibility.studio.footer.done.title': '{0} fixed, {1} for you',
                    'accessibility.studio.footer.done.sub': 'on {0}'
                })
            }
        ]
    });

    function render(props: Record<string, unknown> = {}) {
        spectator = createComponent({
            props: { phase: 'ready' as StudioPhase, ...props } as never
        });
        spectator.detectChanges();
    }

    /** Click a PrimeNG button by its test id. */
    function click(testId: string) {
        const btn = spectator.query(byTestId(testId))?.querySelector('button');
        spectator.click(btn as HTMLElement);
    }

    describe('which controls each phase offers', () => {
        it.each([
            ['ready', ['studio-scan-btn']],
            ['scanning', ['studio-stopscan-btn']],
            ['scanned', ['studio-rescan-btn', 'studio-fix-btn', 'studio-skipcss-toggle']],
            ['fixing', ['studio-stopagent-btn']],
            ['done', ['studio-reviewfiles-btn']],
            ['published', ['studio-allpages-btn']]
        ])('offers %s the right controls', (phase, expected) => {
            render({ phase: phase as StudioPhase });

            for (const testId of expected) {
                expect(spectator.query(byTestId(testId))).toBeTruthy();
            }
        });

        it('offers the skip-css toggle only with Fix — it is a fix option, not a scan one', () => {
            render();
            expect(spectator.query(byTestId('studio-skipcss-toggle'))).toBeFalsy();

            spectator.setInput('phase', 'scanned' as StudioPhase);
            expect(spectator.query(byTestId('studio-skipcss-toggle'))).toBeTruthy();
        });

        it('keeps discard/publish out — those live with the files they act on', () => {
            render({ phase: 'done' as StudioPhase });

            expect(spectator.query(byTestId('studio-discard-btn'))).toBeFalsy();
            expect(spectator.query(byTestId('studio-apply-btn'))).toBeFalsy();
        });
    });

    describe('what each control emits', () => {
        it.each([
            ['ready', 'studio-scan-btn', (c: DotA11yActionsComponent) => c.scan],
            ['scanning', 'studio-stopscan-btn', (c: DotA11yActionsComponent) => c.stopScan],
            // Re-scan is the same action as the first scan, from a different phase.
            ['scanned', 'studio-rescan-btn', (c: DotA11yActionsComponent) => c.scan],
            ['scanned', 'studio-fix-btn', (c: DotA11yActionsComponent) => c.fix],
            ['fixing', 'studio-stopagent-btn', (c: DotA11yActionsComponent) => c.stopAgent],
            ['done', 'studio-reviewfiles-btn', (c: DotA11yActionsComponent) => c.reviewFiles],
            ['published', 'studio-allpages-btn', (c: DotA11yActionsComponent) => c.allPages]
        ])('%s: %s emits its action', (phase, testId, pickOutput) => {
            render({ phase: phase as StudioPhase });
            const emitted = jest.fn();
            pickOutput(spectator.component).subscribe(emitted);

            click(testId);

            expect(emitted).toHaveBeenCalled();
        });

        it('emits the skip-css choice rather than owning it', () => {
            render({ phase: 'scanned' as StudioPhase });
            const emitted = jest.fn();
            spectator.component.skipCssChange.subscribe(emitted);

            spectator.triggerEventHandler('p-toggleswitch', 'ngModelChange', true);

            expect(emitted).toHaveBeenCalledWith(true);
        });
    });

    describe('footer copy', () => {
        it('counts the open issues while scanned', () => {
            render({ phase: 'scanned' as StudioPhase, openCount: 7 });

            expect(spectator.element).toHaveText('7 issues to fix');
        });

        it('reports fixed vs left-for-a-human once done, on the page it ran against', () => {
            render({
                phase: 'done' as StudioPhase,
                fixedCount: 4,
                reportedCount: 2,
                pagePath: '/about-us'
            });

            expect(spectator.element).toHaveText('4 fixed, 2 for you');
            expect(spectator.element).toHaveText('on /about-us');
        });

        it.each([['ready'], ['scanning']])('shows no footer copy while %s', (phase) => {
            // Nothing has happened yet, so a status line would be noise above the
            // single button.
            render({ phase: phase as StudioPhase });

            expect(spectator.element).not.toHaveText('accessibility.studio.footer');
        });
    });
});
