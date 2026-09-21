import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentScheduleFilterComponent } from './dot-experiment-schedule-filter.component';

const ANY_SCHEDULE = 'Any schedule';
const LAST_MONTH = 'Scheduled in the last month';
const LAST_3_MONTHS = 'Scheduled in the last 3 months';

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
                    'experiments.list.filter.schedule.1m': LAST_MONTH,
                    'experiments.list.filter.schedule.3m': LAST_3_MONTHS,
                    'experiments.list.filter.schedule.6m': 'Scheduled in the last 6 months',
                    'experiments.list.filter.schedule.12m': 'Scheduled in the last 12 months',
                    'dot.common.remove': 'Remove'
                })
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent({ props: { selected: null } as never });
        spectator.detectChanges();
    });

    const chipText = () => spectator.query(byTestId('experiment-schedule-filter-chip'))?.textContent;

    describe('the options', () => {
        it('should offer exactly the four windows, and no "any" row among them', () => {
            // FR-025. "Any schedule" is the chip's empty label, not a fifth option: an `ANY` entry
            // in the list would make the shared chip read as active while applying no filter, and
            // would allow "Any schedule" ticked beside a specific window.
            expect(spectator.component.$options()).toHaveLength(4);
            expect(spectator.component.$options().map(({ value }) => value)).toEqual([
                '1m',
                '3m',
                '6m',
                '12m'
            ]);
        });

        it('should take every label from the catalogue (FR-026)', () => {
            expect(spectator.component.$options().map(({ label }) => label)).toEqual([
                LAST_MONTH,
                LAST_3_MONTHS,
                'Scheduled in the last 6 months',
                'Scheduled in the last 12 months'
            ]);
        });

        it('should show no counts beside the options', () => {
            // Unlike Status and Goal: a count per window would have to be recomputed against a
            // moving "now", and the windows overlap, so the numbers would not add up to anything.
            expect(spectator.component.$options().every(({ count }) => !count)).toBe(true);
        });
    });

    describe('the selection', () => {
        it('should read as unfiltered while no window is chosen (FR-019)', () => {
            expect(chipText()).toContain(ANY_SCHEDULE);
        });

        it('should show the chosen window on the chip', () => {
            spectator.setInput('selected', '3m');
            spectator.detectChanges();

            expect(chipText()).toContain(LAST_3_MONTHS);
            expect(chipText()).not.toContain(ANY_SCHEDULE);
        });

        it('should emit the window token, not its label', () => {
            const emitted = vi.fn();
            spectator.output('selectionChange').subscribe(emitted);

            spectator.component.onChange('6m');

            expect(emitted).toHaveBeenCalledWith('6m');
        });

        it('should replace the previous choice rather than adding to it (FR-018)', () => {
            const emitted = vi.fn();
            spectator.setInput('selected', '1m');
            spectator.detectChanges();
            spectator.output('selectionChange').subscribe(emitted);

            spectator.component.onChange('12m');

            // One window, never two: the payload is a single token, so there is no shape in which
            // two can be in force at once.
            expect(emitted).toHaveBeenCalledWith('12m');
        });

        it('should return to no constraint when the chip is cleared (FR-024)', () => {
            const emitted = vi.fn();
            spectator.setInput('selected', '6m');
            spectator.detectChanges();
            spectator.output('selectionChange').subscribe(emitted);

            spectator.component.onRemove();

            expect(emitted).toHaveBeenCalledWith(null);
        });

        it('should read the chip as unfiltered again once cleared', () => {
            spectator.setInput('selected', '6m');
            spectator.detectChanges();
            spectator.setInput('selected', null);
            spectator.detectChanges();

            expect(chipText()).toContain(ANY_SCHEDULE);
        });

        it('should treat deselecting the current window as clearing it', () => {
            // PrimeNG's single-select listbox emits `null` when the selected row is clicked
            // again. That has to mean "no constraint", not "keep what you had".
            const emitted = vi.fn();
            spectator.setInput('selected', '3m');
            spectator.detectChanges();
            spectator.output('selectionChange').subscribe(emitted);

            spectator.component.onChange(null);

            expect(emitted).toHaveBeenCalledWith(null);
        });
    });
});
