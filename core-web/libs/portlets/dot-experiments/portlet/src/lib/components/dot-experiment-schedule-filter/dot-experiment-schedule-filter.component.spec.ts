import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentScheduleFilterComponent } from './dot-experiment-schedule-filter.component';

const ANY_SCHEDULE = 'Any schedule';

describe('DotExperimentScheduleFilterComponent', () => {
    let spectator: Spectator<DotExperimentScheduleFilterComponent>;

    const createComponent = createComponentFactory({
        component: DotExperimentScheduleFilterComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'experiments.list.filter.schedule': 'Schedule',
                    'experiments.list.filter.schedule.any': ANY_SCHEDULE,
                    'experiments.list.filter.schedule.range': 'Scheduled {0} to {1}',
                    'experiments.list.filter.schedule.from-only': 'Scheduled from {0}',
                    'experiments.list.filter.schedule.to-only': 'Scheduled until {0}',
                    'dot.common.remove': 'Remove'
                })
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        spectator.detectChanges();
    });

    const chipText = () =>
        spectator.query(byTestId('experiment-schedule-filter-chip'))?.textContent;

    const setPeriod = (from: string | null, to: string | null): void => {
        spectator.setInput('from', from);
        spectator.setInput('to', to);
        spectator.detectChanges();
    };

    describe('the chip label', () => {
        it('should read as unfiltered while no period is picked (FR-019)', () => {
            expect(chipText()).toContain(ANY_SCHEDULE);
        });

        it('should read both bounds once a whole period is in force', () => {
            setPeriod('2026-06-01', '2026-06-30');

            expect(chipText()).toContain('Scheduled 2026-06-01 to 2026-06-30');
            expect(chipText()).not.toContain(ANY_SCHEDULE);
        });

        /**
         * The calendar passes through this state on every pick — the first click sets one end —
         * so it is not an edge case. A chip reading "Scheduled 2026-06-01 to" would look broken,
         * which is why the open-ended period gets its own phrasing rather than a blank.
         */
        it('should phrase an open upper bound as "from", not as a blank second bound', () => {
            setPeriod('2026-06-01', null);

            expect(chipText()).toContain('Scheduled from 2026-06-01');
            expect(chipText()).not.toContain(' to ');
        });

        it('should phrase an open lower bound as "until"', () => {
            // Unreachable through the calendar, reachable through the address.
            setPeriod(null, '2026-06-30');

            expect(chipText()).toContain('Scheduled until 2026-06-30');
        });

        it('should read as unfiltered again once the period is cleared', () => {
            setPeriod('2026-06-01', '2026-06-30');
            setPeriod(null, null);

            expect(chipText()).toContain(ANY_SCHEDULE);
        });
    });

    /**
     * These assert PrimeNG's **contract**, not our convenience, and they exist because the first
     * version of this component got it wrong in a way no unit test could have caught: the model
     * was seeded `[null, null]`, PrimeNG's range branch tested `value.length`, passed, then read
     * `value[0].getTime()` and threw on the first click. The calendar selected nothing and the
     * filter silently did not work. Found in a browser, pinned here.
     */
    describe('the calendar model', () => {
        const range = () => spectator.component['$range']();

        it('should be null while no period is in force, never a pair of nulls', () => {
            expect(range()).toBeNull();
        });

        it('should be a pair when a whole period is in force', () => {
            setPeriod('2026-06-01', '2026-06-30');

            expect(range()?.map((date) => date.getDate())).toEqual([1, 30]);
        });

        it('should be a one-element array for a half-picked period', () => {
            // Which is what the picker itself produces after the first click, so feeding it back
            // anything else puts it in a state it cannot continue from.
            setPeriod('2026-06-01', null);

            expect(range()).toHaveLength(1);
            expect(range()?.[0].getDate()).toBe(1);
        });

        it('should be null when the lower bound is not a date', () => {
            // A hand-edited address. Nothing to select beats a wrong day.
            setPeriod('not-a-date', '2026-06-30');

            expect(range()).toBeNull();
        });

        it('should be null when only an upper bound is set', () => {
            // Range mode cannot draw "up to this day, from anywhere". The chip still names the
            // bound and the filter still applies; only the calendar cannot depict it.
            setPeriod(null, '2026-06-30');

            expect(range()).toBeNull();
        });

        it('should go back to null once the period is cleared', () => {
            setPeriod('2026-06-01', '2026-06-30');
            spectator.component.onRangeChange(null);

            expect(range()).toBeNull();
        });
    });

    describe('what it emits', () => {
        it('should emit both bounds as calendar dates, not as instants', () => {
            const emitted = vi.fn();
            spectator.output('selectionChange').subscribe(emitted);

            spectator.component.onRangeChange([new Date(2026, 5, 1), new Date(2026, 5, 30)]);

            expect(emitted).toHaveBeenCalledWith({ from: '2026-06-01', to: '2026-06-30' });
        });

        /**
         * FR-020. Emitted rather than withheld: "on or after this date" is a useful filter in its
         * own right, and withholding it would leave the table unchanged while the chip showed a
         * selection.
         */
        it('should emit a half-picked range with the upper bound open', () => {
            const emitted = vi.fn();
            spectator.output('selectionChange').subscribe(emitted);

            spectator.component.onRangeChange([new Date(2026, 5, 1)]);

            expect(emitted).toHaveBeenCalledWith({ from: '2026-06-01', to: null });
        });

        it('should emit an empty period when the calendar is cleared', () => {
            const emitted = vi.fn();
            setPeriod('2026-06-01', '2026-06-30');
            spectator.output('selectionChange').subscribe(emitted);

            spectator.component.onRangeChange(null);

            expect(emitted).toHaveBeenCalledWith({ from: null, to: null });
        });

        it('should replace the period rather than adding to it (FR-018)', () => {
            const emitted = vi.fn();
            setPeriod('2026-06-01', '2026-06-30');
            spectator.output('selectionChange').subscribe(emitted);

            spectator.component.onRangeChange([new Date(2026, 8, 1), new Date(2026, 8, 30)]);

            // One period, never two: the payload is a single range, so there is no shape in which
            // two can be in force at once.
            expect(emitted).toHaveBeenCalledTimes(1);
            expect(emitted).toHaveBeenCalledWith({ from: '2026-09-01', to: '2026-09-30' });
        });

        it('should clear the period from the chip remove control (FR-024)', () => {
            const emitted = vi.fn();
            setPeriod('2026-06-01', '2026-06-30');
            spectator.output('selectionChange').subscribe(emitted);

            spectator.component.onRemove();

            expect(emitted).toHaveBeenCalledWith({ from: null, to: null });
        });
    });
});
