import { Dispatcher } from '@ngrx/signals/events';
import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { signal } from '@angular/core';
import { FieldTree } from '@angular/forms/signals';
import { ActivatedRoute } from '@angular/router';

import { DotMessageService } from '@dotcms/data-access';
import { DotExperiment, ExperimentsConfigProperties, PROP_NOT_FOUND } from '@dotcms/dotcms-models';
import { getExperimentMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsConfigureSchedulingComponent } from './dot-experiments-configure-scheduling.component';

import { dotExperimentsConfigurePageEvents } from '../../../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../../../store/dot-experiments-configure.store';

const CLEAR_COPY = 'Clear Schedule';
const OUT_OF_BOUNDS_COPY = 'The end date must fall between {0} and {1}';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.scheduling.action.clear': CLEAR_COPY,
    'experiments.configure.scheduling.end.error.out-of-bounds': OUT_OF_BOUNDS_COPY
});

const MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000;

/** The backend reports its limits in days. Both are well inside the 7/90-day defaults. */
const CONFIGURED_MIN_DURATION_DAYS = 3;
const CONFIGURED_MAX_DURATION_DAYS = 30;

const EXPERIMENT: DotExperiment = { ...getExperimentMock(1), scheduling: null };

const daysFromNow = (days: number): Date => new Date(Date.now() + days * MILLISECONDS_PER_DAY);

/** The shell provides the store; real signals keep the card's effects reactive. */
const createStoreMock = () => ({
    experiment: signal<DotExperiment | null>(EXPERIMENT),
    $isLocked: signal(false)
});

interface SchedulingFormModel {
    startDate: Date | null;
    endDate: Date | null;
}

/**
 * The form tree is `protected`, and both dates live behind a PrimeNG overlay calendar. Reading the
 * tree is the supported escape hatch for driving them, the same one the signal-forms specs use.
 */
const formTreeOf = (
    component: DotExperimentsConfigureSchedulingComponent
): FieldTree<SchedulingFormModel> =>
    Reflect.get(component, 'formTree') as FieldTree<SchedulingFormModel>;

describe('DotExperimentsConfigureSchedulingComponent', () => {
    let spectator: Spectator<DotExperimentsConfigureSchedulingComponent>;
    let storeMock: ReturnType<typeof createStoreMock>;
    let dispatch: jest.SpyInstance;

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

    /** A card mounted on a route whose resolver published `configProps`, or nothing at all. */
    const createComponentWith = (configProps?: Record<string, string>) =>
        createComponentFactory({
            component: DotExperimentsConfigureSchedulingComponent,
            providers: [
                { provide: DotExperimentsConfigureStore, useFactory: () => storeMock },
                { provide: DotMessageService, useValue: messageServiceMock },
                {
                    provide: ActivatedRoute,
                    useValue: { snapshot: { data: configProps ? { config: configProps } : {} } }
                }
            ],
            detectChanges: false
        });

    /** `injectDispatch` appends a scope argument, so only the event itself is compared. */
    const dispatchedEvents = () => dispatch.mock.calls.map(([event]) => event);

    const schedulingPayloads = () =>
        dispatchedEvents()
            .filter(({ type }) => type === dotExperimentsConfigurePageEvents.schedulingChanged.type)
            .map(({ payload }) => payload);

    const setDates = (dates: Partial<SchedulingFormModel>) => {
        const formTree = formTreeOf(spectator.component);

        if ('startDate' in dates) {
            formTree.startDate().value.set(dates.startDate ?? null);
        }

        if ('endDate' in dates) {
            formTree.endDate().value.set(dates.endDate ?? null);
        }

        spectator.detectChanges();
    };

    const endDateError = () =>
        spectator.query(byTestId('experiments-configure-scheduling-end-error'));

    const clearButton = () =>
        spectator.query(byTestId('experiments-configure-scheduling-clear-btn'));

    describe('with the durations the backend configured', () => {
        const createComponent = createComponentWith({
            [ExperimentsConfigProperties.EXPERIMENTS_MIN_DURATION]: String(
                CONFIGURED_MIN_DURATION_DAYS
            ),
            [ExperimentsConfigProperties.EXPERIMENTS_MAX_DURATION]: String(
                CONFIGURED_MAX_DURATION_DAYS
            )
        });

        beforeEach(() => {
            storeMock = createStoreMock();
            spectator = createComponent();
            dispatch = jest.spyOn(spectator.inject(Dispatcher), 'dispatch');
            spectator.detectChanges();
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

            it.each([
                'experiments-configure-scheduling-start',
                'experiments-configure-scheduling-end'
            ])('should let %s pick a time as well as a day', (testId) => {
                expect(
                    openPicker(testId)?.querySelector('.p-datepicker-time-picker')
                ).not.toBeNull();
            });
        });

        describe('bounds', () => {
            it('should accept an end date inside the configured window', () => {
                setDates({ endDate: daysFromNow(CONFIGURED_MIN_DURATION_DAYS + 1) });

                expect(endDateError()).toBeNull();
            });

            it('should reject an end date before the minimum duration', () => {
                setDates({ endDate: daysFromNow(CONFIGURED_MIN_DURATION_DAYS - 1) });

                expect(endDateError()).not.toBeNull();
            });

            it('should reject an end date beyond the maximum duration', () => {
                setDates({ endDate: daysFromNow(CONFIGURED_MAX_DURATION_DAYS + 10) });

                expect(endDateError()).not.toBeNull();
            });

            it('should measure the window from the start date, not from now', () => {
                const startDate = daysFromNow(10);

                setDates({
                    startDate,
                    endDate: new Date(
                        startDate.getTime() + CONFIGURED_MIN_DURATION_DAYS * MILLISECONDS_PER_DAY
                    )
                });

                expect(endDateError()).toBeNull();
            });
        });

        describe('reporting the schedule', () => {
            it('should report a valid range', () => {
                const startDate = daysFromNow(1);
                const endDate = daysFromNow(CONFIGURED_MIN_DURATION_DAYS + 2);

                setDates({ startDate, endDate });

                expect(schedulingPayloads()).toContainEqual({
                    startDate: startDate.getTime(),
                    endDate: endDate.getTime()
                });
            });

            it('should not report an out-of-bounds range', () => {
                setDates({
                    startDate: daysFromNow(1),
                    endDate: daysFromNow(CONFIGURED_MAX_DURATION_DAYS + 10)
                });

                expect(schedulingPayloads()).toEqual([]);
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

            it('should drop the whole schedule when pressed', () => {
                // `null` is what the PATCH endpoint reads as "starts when Start is pressed".
                setDates({ startDate: daysFromNow(1) });

                spectator.click(clearButton()?.querySelector('button') as HTMLElement);
                spectator.detectChanges();

                expect(schedulingPayloads()).toContainEqual(null);
                expect(clearButton()).toBeNull();
            });

            it('should not offer it while the experiment is locked', () => {
                // AC34: a locked experiment is read-only, schedule included.
                setDates({ startDate: daysFromNow(1) });
                storeMock.$isLocked.set(true);
                spectator.detectChanges();

                expect(clearButton()).toBeNull();
            });
        });
    });

    describe('without the durations resolved', () => {
        const createComponent = createComponentWith();

        beforeEach(() => {
            storeMock = createStoreMock();
            spectator = createComponent();
            dispatch = jest.spyOn(spectator.inject(Dispatcher), 'dispatch');
            spectator.detectChanges();
        });

        afterEach(() => jest.restoreAllMocks());

        it('should fall back to a seven day minimum', () => {
            setDates({ endDate: daysFromNow(5) });

            expect(endDateError()).not.toBeNull();
        });

        it('should fall back to a ninety day maximum', () => {
            setDates({ endDate: daysFromNow(40) });

            expect(endDateError()).toBeNull();
        });
    });

    describe('with a duration the backend does not have', () => {
        const createComponent = createComponentWith({
            [ExperimentsConfigProperties.EXPERIMENTS_MIN_DURATION]: PROP_NOT_FOUND,
            [ExperimentsConfigProperties.EXPERIMENTS_MAX_DURATION]: PROP_NOT_FOUND
        });

        beforeEach(() => {
            storeMock = createStoreMock();
            spectator = createComponent();
            dispatch = jest.spyOn(spectator.inject(Dispatcher), 'dispatch');
            spectator.detectChanges();
        });

        afterEach(() => jest.restoreAllMocks());

        it('should treat an unset property as no property at all', () => {
            setDates({ endDate: daysFromNow(5) });

            expect(endDateError()).not.toBeNull();
        });
    });
});
