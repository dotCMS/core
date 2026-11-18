import { Dispatcher } from '@ngrx/signals/events';
import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { of } from 'rxjs';
import { MockInstance, vi } from 'vitest';

import { provideLocationMocks } from '@angular/common/testing';
import { provideRouter } from '@angular/router';

import { ConfirmationService } from 'primeng/api';

import { DotExperimentsService, DotMessageService } from '@dotcms/data-access';
import { DotExperimentStatus } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import {
    DotExperimentsConfigureFooterComponent,
    LIMITED_MODE_START_CONFIRM_KEY
} from './dot-experiments-configure-footer.component';

import { dotExperimentsConfigurePageEvents } from '../../../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../../../store/dot-experiments-configure.store';

const SAVE_HINT_COPY = 'Save Draft stores this form';
const UNSAVED_COPY = 'You have unsaved changes';
const SAVE_DRAFT_COPY = 'Save Draft';
const SAVING_COPY = 'Saving…';
const LOCKED_COPY = 'This experiment can no longer be edited';
const VALIDATION_ONE_COPY = '1 field needs your attention';
const VALIDATION_MANY_COPY = '{0} fields need your attention';
const START_COPY = 'Start Experiment';
const SCHEDULE_COPY = 'Schedule Experiment';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.footer.save-hint': SAVE_HINT_COPY,
    'experiments.configure.footer.unsaved': UNSAVED_COPY,
    'experiments.configure.action.save-draft': SAVE_DRAFT_COPY,
    'experiments.configure.footer.saving': SAVING_COPY,
    'experiments.configure.footer.locked': LOCKED_COPY,
    'experiments.configure.footer.validation.one': VALIDATION_ONE_COPY,
    'experiments.configure.footer.validation.many': VALIDATION_MANY_COPY,
    'experiments.action.start-experiment': START_COPY,
    'experiments.action.schedule-experiment': SCHEDULE_COPY,
    'experiments.limited-mode.start.confirm.header': 'Start your free experiment',
    'experiments.limited-mode.start.confirm.message':
        'You are about to use your one free experiment.',
    'experiments.limited-mode.start.confirm.accept': 'Continue',
    'dot.common.dialog.reject': 'Cancel'
});

const createStoreMock = () => ({
    $isLocked: vi.fn().mockReturnValue(false),
    $isSaving: vi.fn().mockReturnValue(false),
    $hasUnsavedChanges: vi.fn().mockReturnValue(false),
    $canSave: vi.fn().mockReturnValue(false),
    $isScheduledStart: vi.fn().mockReturnValue(false),
    $validationErrorCount: vi.fn().mockReturnValue(0),
    $status: vi.fn().mockReturnValue(DotExperimentStatus.DRAFT)
});

describe('DotExperimentsConfigureFooterComponent', () => {
    let spectator: Spectator<DotExperimentsConfigureFooterComponent>;
    let storeMock: ReturnType<typeof createStoreMock>;
    let dispatch: MockInstance;

    const createComponent = createComponentFactory({
        component: DotExperimentsConfigureFooterComponent,
        providers: [
            provideRouter([{ path: 'experiments', children: [] }]),
            provideLocationMocks(),
            { provide: DotExperimentsConfigureStore, useFactory: () => storeMock },
            { provide: DotMessageService, useValue: messageServiceMock },
            mockProvider(DotExperimentsService, {
                healthCheck: vi.fn().mockReturnValue(of({ health: 'OK', tier: 'full' }))
            }),
            mockProvider(ConfirmationService)
        ],
        detectChanges: false
    });

    /** `injectDispatch` appends a scope argument, so only the event itself is compared. */
    const dispatchedEvents = () => dispatch.mock.calls.map(([event]) => event);

    const hint = () => spectator.query(byTestId('experiments-configure-footer-hint'));

    const startButton = () =>
        spectator
            .query(byTestId('experiments-configure-start-btn'))
            ?.querySelector('button') as HTMLButtonElement;

    const clickButton = (testId: string) => {
        const host = spectator.query(byTestId(testId));
        spectator.click(host?.querySelector('button') as HTMLElement);
        spectator.detectChanges();
    };

    afterEach(() => {
        vi.restoreAllMocks();
    });

    describe('in full mode', () => {
        beforeEach(() => {
            storeMock = createStoreMock();
            spectator = createComponent();
            dispatch = vi.spyOn(spectator.inject(Dispatcher), 'dispatch');
        });

        describe('hint', () => {
            it('should name what Save Draft covers while there is nothing to save', () => {
                spectator.detectChanges();

                expect(hint()?.textContent).toContain(SAVE_HINT_COPY);
                expect(hint()?.getAttribute('role')).toBeNull();
            });

            it('should say there are unsaved changes once the form is dirty', () => {
                storeMock.$hasUnsavedChanges.mockReturnValue(true);
                spectator.detectChanges();

                expect(hint()?.textContent).toContain(UNSAVED_COPY);
            });

            it('should say it is saving while the write is in flight', () => {
                storeMock.$hasUnsavedChanges.mockReturnValue(true);
                storeMock.$isSaving.mockReturnValue(true);
                spectator.detectChanges();

                expect(hint()?.textContent).toContain(SAVING_COPY);
            });

            it('should say the experiment is read-only instead of offering to save', () => {
                storeMock.$isLocked.mockReturnValue(true);
                storeMock.$hasUnsavedChanges.mockReturnValue(true);
                spectator.detectChanges();

                expect(hint()?.textContent).toContain(LOCKED_COPY);
            });

            it('should count a single failing field once Start has been pressed', () => {
                storeMock.$validationErrorCount.mockReturnValue(1);
                spectator.detectChanges();

                expect(hint()?.textContent).toContain(VALIDATION_ONE_COPY);
                expect(hint()?.getAttribute('role')).toBe('alert');
            });

            it('should count the failing fields once Start has been pressed', () => {
                storeMock.$validationErrorCount.mockReturnValue(3);
                spectator.detectChanges();

                expect(hint()?.textContent).toContain('3 fields need your attention');
                expect(hint()?.getAttribute('role')).toBe('alert');
            });

            it('should report the failed Start over a save still in flight', () => {
                storeMock.$validationErrorCount.mockReturnValue(2);
                storeMock.$isSaving.mockReturnValue(true);
                spectator.detectChanges();

                expect(hint()?.textContent).toContain('2 fields need your attention');
            });
        });

        describe('start', () => {
            it('should read as Start while the experiment runs immediately', () => {
                spectator.detectChanges();

                expect(startButton().textContent).toContain(START_COPY);
            });

            it('should read as Schedule while the start date is in the future', () => {
                storeMock.$isScheduledStart.mockReturnValue(true);
                spectator.detectChanges();

                expect(startButton().textContent).toContain(SCHEDULE_COPY);
            });

            it('should stay enabled while the configuration is incomplete', () => {
                // Pressing it is the only thing that reveals what is missing (AC28), so disabling it
                // would leave the user with no way to find out.
                storeMock.$validationErrorCount.mockReturnValue(4);
                spectator.detectChanges();

                expect(startButton().disabled).toBe(false);
            });

            it('should dispatch startRequested when pressed', () => {
                spectator.detectChanges();

                clickButton('experiments-configure-start-btn');

                expect(dispatchedEvents()).toContainEqual(
                    dotExperimentsConfigurePageEvents.startRequested()
                );
            });

            it.each([
                DotExperimentStatus.RUNNING,
                DotExperimentStatus.SCHEDULED,
                DotExperimentStatus.ENDED,
                DotExperimentStatus.ARCHIVED
            ])('should not offer Start for %s', (status) => {
                storeMock.$status.mockReturnValue(status);
                spectator.detectChanges();

                expect(spectator.query(byTestId('experiments-configure-start-btn'))).toBeNull();
            });
        });
    }); // end 'in full mode'

    /**
     * Limited-mode paths for the Start button. The outer describe uses tier:'full' throughout,
     * so $startDisabled and the slot-free direct-dispatch path never ran in a test — reverting
     * them would leave the suite green. These are the regression guards.
     */
    describe('in limited mode', () => {
        /**
         * Creates a factory with the given limited-tier health response. The store mock is
         * provided via closure so it picks up the instance created in each beforeEach.
         */
        const makeComponent = (freeExperimentUsed: boolean) =>
            createComponentFactory({
                component: DotExperimentsConfigureFooterComponent,
                providers: [
                    provideRouter([{ path: 'experiments', children: [] }]),
                    provideLocationMocks(),
                    { provide: DotExperimentsConfigureStore, useFactory: () => storeMock },
                    { provide: DotMessageService, useValue: messageServiceMock },
                    mockProvider(DotExperimentsService, {
                        healthCheck: vi
                            .fn()
                            .mockReturnValue(
                                of({ health: 'OK', tier: 'limited', freeExperimentUsed })
                            )
                    }),
                    mockProvider(ConfirmationService)
                ],
                detectChanges: false
            });

        describe('slot free (freeExperimentUsed=false)', () => {
            const createLimited = makeComponent(false);

            beforeEach(() => {
                storeMock = createStoreMock();
                spectator = createLimited();
                dispatch = vi.spyOn(spectator.inject(Dispatcher), 'dispatch');
                spectator.detectChanges();
            });

            it('should keep Start enabled', () => {
                expect(startButton().disabled).toBe(false);
            });

            it('should show a confirmation dialog before dispatching startRequested', () => {
                const confirm = vi.spyOn(spectator.inject(ConfirmationService), 'confirm');

                clickButton('experiments-configure-start-btn');

                expect(confirm).toHaveBeenCalledWith(
                    expect.objectContaining({ key: LIMITED_MODE_START_CONFIRM_KEY })
                );
                expect(dispatchedEvents()).not.toContainEqual(
                    dotExperimentsConfigurePageEvents.startRequested()
                );
            });
        });

        describe('slot used (freeExperimentUsed=true)', () => {
            const createLimited = makeComponent(true);

            beforeEach(() => {
                storeMock = createStoreMock();
                spectator = createLimited();
                dispatch = vi.spyOn(spectator.inject(Dispatcher), 'dispatch');
                spectator.detectChanges();
            });

            it('should disable Start so the operator cannot exceed the one-experiment limit', () => {
                expect(startButton().disabled).toBe(true);
            });
        });
    });
});
