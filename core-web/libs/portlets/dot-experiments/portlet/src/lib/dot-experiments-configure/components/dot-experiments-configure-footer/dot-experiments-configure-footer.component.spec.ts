import { Dispatcher } from '@ngrx/signals/events';
import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { provideLocationMocks } from '@angular/common/testing';
import { provideRouter, Router } from '@angular/router';

import { DotMessageService } from '@dotcms/data-access';
import { DotExperimentStatus } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsConfigureFooterComponent } from './dot-experiments-configure-footer.component';

import { dotExperimentsConfigurePageEvents } from '../../../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../../../store/dot-experiments-configure.store';

const AUTOSAVE_HINT_COPY = 'Changes are saved automatically';
const SAVING_COPY = 'Saving…';
const LOCKED_COPY = 'This experiment can no longer be edited';
const VALIDATION_ONE_COPY = '1 field needs your attention';
const VALIDATION_MANY_COPY = '{0} fields need your attention';
const START_COPY = 'Start Experiment';
const SCHEDULE_COPY = 'Schedule Experiment';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.footer.autosave-hint': AUTOSAVE_HINT_COPY,
    'experiments.configure.footer.saving': SAVING_COPY,
    'experiments.configure.footer.locked': LOCKED_COPY,
    'experiments.configure.footer.validation.one': VALIDATION_ONE_COPY,
    'experiments.configure.footer.validation.many': VALIDATION_MANY_COPY,
    'experiments.action.start-experiment': START_COPY,
    'experiments.action.schedule-experiment': SCHEDULE_COPY,
    'experiments.configure.action.back-to-list': 'Back To Experiments'
});

const createStoreMock = () => ({
    $isLocked: jest.fn().mockReturnValue(false),
    $isAutosaving: jest.fn().mockReturnValue(false),
    $isScheduledStart: jest.fn().mockReturnValue(false),
    $validationErrorCount: jest.fn().mockReturnValue(0),
    $status: jest.fn().mockReturnValue(DotExperimentStatus.DRAFT)
});

describe('DotExperimentsConfigureFooterComponent', () => {
    let spectator: Spectator<DotExperimentsConfigureFooterComponent>;
    let storeMock: ReturnType<typeof createStoreMock>;
    let dispatch: jest.SpyInstance;

    const createComponent = createComponentFactory({
        component: DotExperimentsConfigureFooterComponent,
        providers: [
            provideRouter([{ path: 'experiments', children: [] }]),
            provideLocationMocks(),
            { provide: DotExperimentsConfigureStore, useFactory: () => storeMock },
            { provide: DotMessageService, useValue: messageServiceMock }
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

    beforeEach(() => {
        storeMock = createStoreMock();
        spectator = createComponent();
        dispatch = jest.spyOn(spectator.inject(Dispatcher), 'dispatch');
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    describe('hint', () => {
        it('should explain that there is no Save button', () => {
            spectator.detectChanges();

            expect(hint()?.textContent).toContain(AUTOSAVE_HINT_COPY);
            expect(hint()?.getAttribute('role')).toBeNull();
        });

        it('should say it is saving while a field group is being persisted', () => {
            storeMock.$isAutosaving.mockReturnValue(true);
            spectator.detectChanges();

            expect(hint()?.textContent).toContain(SAVING_COPY);
        });

        it('should say the experiment is read-only instead of claiming to autosave', () => {
            storeMock.$isLocked.mockReturnValue(true);
            storeMock.$isAutosaving.mockReturnValue(true);
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

        it('should report the failed Start over an autosave still in flight', () => {
            storeMock.$validationErrorCount.mockReturnValue(2);
            storeMock.$isAutosaving.mockReturnValue(true);
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

    describe('back', () => {
        it('should leave for the experiments list', () => {
            const navigate = jest.spyOn(spectator.inject(Router), 'navigate');
            spectator.detectChanges();

            clickButton('experiments-configure-footer-back-btn');

            expect(navigate).toHaveBeenCalledWith(['/experiments']);
        });
    });
});
