import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { computed, Injector, signal, WritableSignal } from '@angular/core';
import { disabled, FieldTree, form, maxDate, minDate } from '@angular/forms/signals';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsConfigureSchedulingComponent } from './dot-experiments-configure-scheduling.component';

import { SchedulingDateBounds, SchedulingFormSlice } from '../../../shared/models';

const CLEAR_COPY = 'Clear Schedule';
const OUT_OF_BOUNDS_COPY = 'The end date must fall between {0} and {1}';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.scheduling.action.clear': CLEAR_COPY,
    'experiments.configure.scheduling.end.error.out-of-bounds': OUT_OF_BOUNDS_COPY
});

const MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000;

/** The window the shell hands over. Both ends are well inside any configured duration. */
const MIN_END_DAYS = 3;
const MAX_END_DAYS = 30;

const daysFromNow = (days: number): Date => new Date(Date.now() + days * MILLISECONDS_PER_DAY);

const EMPTY_SCHEDULE: SchedulingFormSlice = { startDate: null, endDate: null };

describe('DotExperimentsConfigureSchedulingComponent', () => {
    let spectator: Spectator<DotExperimentsConfigureSchedulingComponent>;
    let $isLocked: WritableSignal<boolean>;
    let scheduling: WritableSignal<SchedulingFormSlice>;
    let field: FieldTree<SchedulingFormSlice>;

    // The date picker's overlay queries `matchMedia`, which jsdom does not implement.
    beforeAll(() => {
        Object.defineProperty(window, 'matchMedia', {
            writable: true,
            value: jest.fn().mockImplementation((query: string) => ({
                matches: false,
                media: query,
                onchange: null,
                addListener: jest.fn(),
                removeListener: jest.fn(),
                addEventListener: jest.fn(),
                removeEventListener: jest.fn(),
                dispatchEvent: jest.fn()
            }))
        });
    });

    const createComponent = createComponentFactory({
        component: DotExperimentsConfigureSchedulingComponent,
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }],
        detectChanges: false
    });

    /**
     * Mounts the card on a real slice carrying the card's own schema — the same rules the shell
     * applies to `path.scheduling` — with the window measured from the start date being edited,
     * as the shell measures it.
     */
    const mountWith = (initialScheduling: SchedulingFormSlice = EMPTY_SCHEDULE) => {
        scheduling = signal(initialScheduling);

        const $bounds = computed<SchedulingDateBounds>(() => {
            const from = scheduling().startDate?.getTime() ?? Date.now();

            return {
                initialStartDate: new Date(),
                minEndDate: new Date(from + MIN_END_DAYS * MILLISECONDS_PER_DAY),
                maxEndDate: new Date(from + MAX_END_DAYS * MILLISECONDS_PER_DAY)
            };
        });

        // The shell's rules for this slice, restated: the card is only meaningful on a slice that
        // carries them, and the shell is where they actually live now.
        field = form(
            scheduling,
            (path) => {
                minDate(path.startDate, new Date());
                minDate(path.endDate, () => $bounds().minEndDate);
                maxDate(path.endDate, () => $bounds().maxEndDate);
                disabled(path, { when: () => $isLocked() });
            },
            { injector: spectator.inject(Injector) }
        );

        spectator.setInput({ field, bounds: $bounds() });
        spectator.detectChanges();
    };

    const setDates = (dates: Partial<SchedulingFormSlice>) => {
        if ('startDate' in dates) {
            field.startDate().value.set(dates.startDate ?? null);
        }

        if ('endDate' in dates) {
            field.endDate().value.set(dates.endDate ?? null);
        }

        // The shell recomputes the window from the new start date and passes it back down.
        spectator.setInput('bounds', {
            initialStartDate: new Date(),
            minEndDate: new Date(
                (scheduling().startDate?.getTime() ?? Date.now()) +
                    MIN_END_DAYS * MILLISECONDS_PER_DAY
            ),
            maxEndDate: new Date(
                (scheduling().startDate?.getTime() ?? Date.now()) +
                    MAX_END_DAYS * MILLISECONDS_PER_DAY
            )
        });
        spectator.detectChanges();
    };

    const endDateError = () =>
        spectator.query(byTestId('experiments-configure-scheduling-end-error'));

    const clearButton = () =>
        spectator.query(byTestId('experiments-configure-scheduling-clear-btn'));

    beforeEach(() => {
        $isLocked = signal(false);
        spectator = createComponent();
        mountWith();
    });

    afterEach(() => jest.restoreAllMocks());

    describe('the pickers', () => {
        const openPicker = (testId: string) => {
            spectator.click(
                spectator.query(byTestId(testId))?.querySelector('input') as HTMLElement
            );
            spectator.detectChanges();

            return spectator.query(byTestId(testId));
        };

        it.each(['experiments-configure-scheduling-start', 'experiments-configure-scheduling-end'])(
            'should let %s pick a time as well as a day',
            (testId) => {
                expect(
                    openPicker(testId)?.querySelector('.p-datepicker-time-picker')
                ).not.toBeNull();
            }
        );

        it('should render the schedule it was handed', () => {
            const startDate = daysFromNow(1);

            mountWith({ startDate, endDate: null });

            expect(
                spectator
                    .query(byTestId('experiments-configure-scheduling-start'))
                    ?.querySelector('input')?.value
            ).not.toBe('');
        });
    });

    describe('the window handed down by the shell', () => {
        it('should accept an end date inside it', () => {
            setDates({ endDate: daysFromNow(MIN_END_DAYS + 1) });

            expect(endDateError()).toBeNull();
        });

        it('should reject an end date before it opens', () => {
            setDates({ endDate: daysFromNow(MIN_END_DAYS - 1) });

            expect(endDateError()?.textContent).toContain('The end date must fall between');
        });

        it('should reject an end date beyond it', () => {
            setDates({ endDate: daysFromNow(MAX_END_DAYS + 10) });

            expect(endDateError()).not.toBeNull();
        });

        it('should measure it from the start date, not from now', () => {
            const startDate = daysFromNow(10);

            setDates({
                startDate,
                endDate: new Date(startDate.getTime() + MIN_END_DAYS * MILLISECONDS_PER_DAY)
            });

            expect(endDateError()).toBeNull();
        });
    });

    describe('editing the schedule', () => {
        it('should write both dates into the slice', () => {
            const startDate = daysFromNow(1);
            const endDate = daysFromNow(MIN_END_DAYS + 2);

            setDates({ startDate, endDate });

            expect(scheduling()).toEqual({ startDate, endDate });
        });

        it('should say the experiment starts immediately while no start date is set', () => {
            expect(
                spectator.query(byTestId('experiments-configure-scheduling-note'))?.textContent
            ).toContain('experiments.configure.scheduling.note.immediate');
        });
    });

    describe('clearing the schedule', () => {
        it('should not offer a way to clear a schedule that was never set', () => {
            expect(clearButton()).toBeNull();
        });

        it('should offer it once a start date is chosen', () => {
            setDates({ startDate: daysFromNow(1) });

            expect(clearButton()?.textContent).toContain(CLEAR_COPY);
        });

        it('should empty both dates when pressed', () => {
            // Emptying the slice is what the shell turns into `scheduling: null`.
            setDates({ startDate: daysFromNow(1), endDate: daysFromNow(MIN_END_DAYS + 2) });

            spectator.click(clearButton()?.querySelector('button') as HTMLElement);
            spectator.detectChanges();

            expect(scheduling()).toEqual(EMPTY_SCHEDULE);
            expect(clearButton()).toBeNull();
        });

        it('should not offer it while the experiment is locked', () => {
            // AC34: a locked experiment is read-only, schedule included.
            setDates({ startDate: daysFromNow(1) });
            $isLocked.set(true);
            spectator.detectChanges();

            expect(clearButton()).toBeNull();
        });
    });

    describe('locked experiment', () => {
        it('should disable both pickers', () => {
            $isLocked.set(true);
            spectator.detectChanges();

            expect(
                spectator
                    .query(byTestId('experiments-configure-scheduling-start'))
                    ?.querySelector('input')?.disabled
            ).toBe(true);
            expect(
                spectator
                    .query(byTestId('experiments-configure-scheduling-end'))
                    ?.querySelector('input')?.disabled
            ).toBe(true);
        });
    });
});
