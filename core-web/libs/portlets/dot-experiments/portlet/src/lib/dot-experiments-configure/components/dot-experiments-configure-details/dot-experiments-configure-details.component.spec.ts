import { Dispatcher } from '@ngrx/signals/events';
import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { DotExperiment, MAX_INPUT_TITLE_LENGTH } from '@dotcms/dotcms-models';
import { getExperimentMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsConfigureDetailsComponent } from './dot-experiments-configure-details.component';

import { ConfigureValidationRule } from '../../../shared/models';
import { dotExperimentsConfigurePageEvents } from '../../../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../../../store/dot-experiments-configure.store';

const NAME_REQUIRED_COPY = 'Give the experiment a name';
const NAME_MAX_LENGTH_COPY = 'The name cannot be longer than {0} characters';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.details.name.required': NAME_REQUIRED_COPY,
    'experiments.configure.details.name.max-length': NAME_MAX_LENGTH_COPY
});

const EXPERIMENT: DotExperiment = {
    ...getExperimentMock(0),
    name: 'Summer landing test',
    description: 'Compares two hero images'
};

/**
 * The shell provides the store, so the card only injects it. Real signals rather than
 * `jest.fn()`s: the card is effect-driven, and a plain function would never re-run them.
 */
const createStoreMock = () => ({
    experiment: signal<DotExperiment | null>(null),
    draftName: signal(''),
    draftDescription: signal(''),
    validationErrors: signal<ConfigureValidationRule[]>([]),
    $isLocked: signal(false)
});

describe('DotExperimentsConfigureDetailsComponent', () => {
    let spectator: Spectator<DotExperimentsConfigureDetailsComponent>;
    let storeMock: ReturnType<typeof createStoreMock>;
    let dispatch: jest.SpyInstance;

    const createComponent = createComponentFactory({
        component: DotExperimentsConfigureDetailsComponent,
        providers: [
            {
                provide: DotExperimentsConfigureStore,
                useFactory: () => storeMock
            },
            { provide: DotMessageService, useValue: messageServiceMock }
        ],
        detectChanges: false
    });

    /** `injectDispatch` appends a scope argument, so only the event itself is compared. */
    const dispatchedEvents = () => dispatch.mock.calls.map(([event]) => event);

    const nameInput = () => spectator.query(byTestId('details-name-input')) as HTMLInputElement;

    const descriptionTextarea = () =>
        spectator.query(byTestId('details-description-textarea')) as HTMLTextAreaElement;

    /** Puts a loaded experiment on the card, which is what hydrates the form. */
    const loadExperiment = (experiment: DotExperiment = EXPERIMENT) => {
        storeMock.experiment.set(experiment);
        storeMock.draftName.set(experiment.name);
        storeMock.draftDescription.set(experiment.description ?? '');
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

    describe('hydration', () => {
        it('should render the name and description of the loaded experiment', () => {
            loadExperiment();

            expect(nameInput().value).toBe(EXPERIMENT.name);
            expect(descriptionTextarea().value).toBe(EXPERIMENT.description);
        });

        it('should start empty while no experiment exists yet', () => {
            spectator.detectChanges();

            expect(nameInput().value).toBe('');
            expect(descriptionTextarea().value).toBe('');
        });

        it('should not dispatch anything just for hydrating', () => {
            loadExperiment();

            expect(dispatchedEvents()).toEqual([]);
        });

        it('should keep what is being typed when the autosave response replaces the experiment', () => {
            // Every PATCH answers with a whole new experiment object; re-reading it would drop
            // the characters typed while the call was travelling.
            loadExperiment();

            spectator.typeInElement('Winter landing test', nameInput());
            storeMock.experiment.set({ ...EXPERIMENT, name: 'Summer landing test' });
            spectator.detectChanges();

            expect(nameInput().value).toBe('Winter landing test');
        });
    });

    describe('reporting changes', () => {
        it('should dispatch nameChanged with what was typed', () => {
            loadExperiment();

            spectator.typeInElement('Winter landing test', nameInput());
            spectator.detectChanges();

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsConfigurePageEvents.nameChanged('Winter landing test')
            );
        });

        it('should dispatch descriptionChanged with what was typed', () => {
            loadExperiment();

            spectator.typeInElement('Now with a video hero', descriptionTextarea());
            spectator.detectChanges();

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsConfigurePageEvents.descriptionChanged('Now with a video hero')
            );
        });

        it('should never dispatch a blank name', () => {
            // The backend rejects it, so letting it through would only queue a doomed PATCH.
            loadExperiment();

            spectator.typeInElement('', nameInput());
            spectator.detectChanges();

            expect(dispatchedEvents()).toEqual([]);
        });

        it('should still dispatch an emptied description', () => {
            loadExperiment();

            spectator.typeInElement('', descriptionTextarea());
            spectator.detectChanges();

            expect(dispatchedEvents()).toContainEqual(
                dotExperimentsConfigurePageEvents.descriptionChanged('')
            );
        });
    });

    describe('name error', () => {
        it('should stay hidden while the name is blank and Start has not been pressed', () => {
            // AC28: nothing is validated until Start/Schedule.
            spectator.detectChanges();

            expect(spectator.query(byTestId('details-name-required-error'))).toBeNull();
        });

        it('should appear once the store reports the name rule as failing', () => {
            storeMock.validationErrors.set(['name']);
            spectator.detectChanges();

            expect(spectator.query(byTestId('details-name-required-error'))?.textContent).toContain(
                NAME_REQUIRED_COPY
            );
        });

        it('should disappear as soon as a name is typed', () => {
            storeMock.validationErrors.set(['name']);
            spectator.detectChanges();

            spectator.typeInElement('Summer landing test', nameInput());
            spectator.detectChanges();

            expect(spectator.query(byTestId('details-name-required-error'))).toBeNull();
        });

        it('should not appear for a rule that belongs to another card', () => {
            storeMock.validationErrors.set(['goalType']);
            spectator.detectChanges();

            expect(spectator.query(byTestId('details-name-required-error'))).toBeNull();
        });
    });

    describe('name length error', () => {
        const overlongName = 'a'.repeat(MAX_INPUT_TITLE_LENGTH + 1);

        it('should stay hidden while the name fits', () => {
            loadExperiment();

            spectator.typeInElement('a'.repeat(MAX_INPUT_TITLE_LENGTH), nameInput());
            spectator.detectChanges();

            expect(spectator.query(byTestId('details-name-max-length-error'))).toBeNull();
        });

        it('should say how long the name may be once it is too long', () => {
            loadExperiment();

            spectator.typeInElement(overlongName, nameInput());
            spectator.detectChanges();

            expect(
                spectator.query(byTestId('details-name-max-length-error'))?.textContent
            ).toContain(`longer than ${MAX_INPUT_TITLE_LENGTH} characters`);
        });

        it('should replace the required error rather than stack with it', () => {
            // The two messages share one slot: a name that is too long is not a missing name.
            storeMock.validationErrors.set(['name']);
            loadExperiment();

            spectator.typeInElement(overlongName, nameInput());
            spectator.detectChanges();

            expect(spectator.query(byTestId('details-name-required-error'))).toBeNull();
            expect(spectator.query(byTestId('details-name-max-length-error'))).not.toBeNull();
        });
    });

    describe('locked experiment', () => {
        it('should disable both fields', () => {
            // AC34: every field is read-only once the experiment is no longer a draft.
            storeMock.$isLocked.set(true);
            loadExperiment();

            expect(nameInput().disabled).toBe(true);
            expect(descriptionTextarea().disabled).toBe(true);
        });

        it('should leave both fields editable while the experiment is a draft', () => {
            loadExperiment();

            expect(nameInput().disabled).toBe(false);
            expect(descriptionTextarea().disabled).toBe(false);
        });
    });
});
