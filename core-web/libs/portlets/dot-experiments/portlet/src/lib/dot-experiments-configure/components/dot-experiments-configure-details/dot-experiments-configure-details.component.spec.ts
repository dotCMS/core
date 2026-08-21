import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { Injector, signal, WritableSignal } from '@angular/core';
import { disabled, form, maxLength } from '@angular/forms/signals';

import { DotMessageService } from '@dotcms/data-access';
import { MAX_INPUT_DESCRIPTIVE_LENGTH, MAX_INPUT_TITLE_LENGTH } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsConfigureDetailsComponent } from './dot-experiments-configure-details.component';

import { ConfigureValidationRule } from '../../../shared/models';
import { DotExperimentsConfigureStore } from '../../../store/dot-experiments-configure.store';

const NAME_REQUIRED_COPY = 'Give the experiment a name';
const NAME_MAX_LENGTH_COPY = 'The name cannot be longer than {0} characters';
const DESCRIPTION_MAX_LENGTH_COPY = 'The description cannot be longer than {0} characters';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.details.name.required': NAME_REQUIRED_COPY,
    'experiments.configure.details.name.max-length': NAME_MAX_LENGTH_COPY,
    'experiments.configure.details.description.max-length': DESCRIPTION_MAX_LENGTH_COPY
});

/** The two leaves of the root form the card is handed. */
interface DetailsModel {
    name: string;
    description: string;
}

const EXPERIMENT_DETAILS: DetailsModel = {
    name: 'Summer landing test',
    description: 'Compares two hero images'
};

/** Only what the card still reads: the rest of its state now arrives through its two inputs. */
const createStoreMock = () => ({
    $validationErrors: signal<ConfigureValidationRule[]>([]),
    $isLocked: signal(false)
});

describe('DotExperimentsConfigureDetailsComponent', () => {
    let spectator: Spectator<DotExperimentsConfigureDetailsComponent>;
    let storeMock: ReturnType<typeof createStoreMock>;
    let model: WritableSignal<DetailsModel>;

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

    /**
     * Mounts the card on a real form tree carrying the rules the shell declares over these two
     * leaves — a stub tree would let the card claim a length error the form never raises.
     */
    const mountWith = (details: DetailsModel = { name: '', description: '' }) => {
        model = signal(details);

        const formTree = form(
            model,
            (path) => {
                maxLength(path.name, MAX_INPUT_TITLE_LENGTH, {
                    message: () =>
                        messageServiceMock.get(
                            'experiments.configure.details.name.max-length',
                            String(MAX_INPUT_TITLE_LENGTH)
                        )
                });
                disabled(path.name, { when: () => storeMock.$isLocked() });
                maxLength(path.description, MAX_INPUT_DESCRIPTIVE_LENGTH, {
                    message: () =>
                        messageServiceMock.get(
                            'experiments.configure.details.description.max-length',
                            String(MAX_INPUT_DESCRIPTIVE_LENGTH)
                        )
                });
                disabled(path.description, { when: () => storeMock.$isLocked() });
            },
            { injector: spectator.inject(Injector) }
        );

        // Both at once: the template reads them in the same pass, so setting one and rendering
        // would trip over the other still being unset.
        spectator.setInput({
            nameField: formTree.name,
            descriptionField: formTree.description
        });
        spectator.detectChanges();
    };

    const nameInput = () => spectator.query(byTestId('details-name-input')) as HTMLInputElement;

    const descriptionTextarea = () =>
        spectator.query(byTestId('details-description-textarea')) as HTMLTextAreaElement;

    beforeEach(() => {
        storeMock = createStoreMock();
        spectator = createComponent();
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    describe('rendering the fields it is handed', () => {
        it('should render the name and description the form holds', () => {
            mountWith(EXPERIMENT_DETAILS);

            expect(nameInput().value).toBe(EXPERIMENT_DETAILS.name);
            expect(descriptionTextarea().value).toBe(EXPERIMENT_DETAILS.description);
        });

        it('should render an empty form as empty fields', () => {
            mountWith();

            expect(nameInput().value).toBe('');
            expect(descriptionTextarea().value).toBe('');
        });
    });

    describe('editing', () => {
        it('should write the typed name into the model', () => {
            mountWith(EXPERIMENT_DETAILS);

            spectator.typeInElement('Winter landing test', nameInput());
            spectator.detectChanges();

            expect(model().name).toBe('Winter landing test');
        });

        it('should write the typed description into the model', () => {
            mountWith(EXPERIMENT_DETAILS);

            spectator.typeInElement('Now with a video hero', descriptionTextarea());
            spectator.detectChanges();

            expect(model().description).toBe('Now with a video hero');
        });
    });

    describe('a form nobody has touched yet', () => {
        it('should not mark the empty name as invalid', () => {
            // AC28: `required` in the schema reaches the DOM as the native attribute, which makes
            // an empty field `:invalid` — and painted red — before the user has done anything.
            mountWith();

            expect(nameInput().hasAttribute('required')).toBe(false);
            expect(nameInput().matches(':invalid')).toBe(false);
            expect(nameInput().getAttribute('aria-invalid')).toBeNull();
        });

        it('should not mark the empty description as invalid either', () => {
            mountWith();

            expect(descriptionTextarea().hasAttribute('required')).toBe(false);
            expect(descriptionTextarea().matches(':invalid')).toBe(false);
        });
    });

    describe('name error', () => {
        it('should stay hidden while the name is blank and Start has not been pressed', () => {
            // AC28: nothing is validated until Start/Schedule.
            mountWith();

            expect(spectator.query(byTestId('details-name-required-error'))).toBeNull();
        });

        it('should appear once the store reports the name rule as failing', () => {
            mountWith();

            storeMock.$validationErrors.set(['name']);
            spectator.detectChanges();

            expect(spectator.query(byTestId('details-name-required-error'))?.textContent).toContain(
                NAME_REQUIRED_COPY
            );
        });

        /**
         * The card renders what the store reports and nothing more: the store re-runs the rules
         * on every edit, so a typed name drops `name` there rather than being re-checked here.
         */
        it('should disappear once the store stops reporting the rule', () => {
            mountWith();
            storeMock.$validationErrors.set(['name']);
            spectator.detectChanges();
            expect(spectator.query(byTestId('details-name-required-error'))).not.toBeNull();

            storeMock.$validationErrors.set([]);
            spectator.detectChanges();

            expect(spectator.query(byTestId('details-name-required-error'))).toBeNull();
        });

        it('should not appear for a rule that belongs to another card', () => {
            mountWith();

            storeMock.$validationErrors.set(['goalType']);
            spectator.detectChanges();

            expect(spectator.query(byTestId('details-name-required-error'))).toBeNull();
        });
    });

    describe('name length error', () => {
        const overlongName = 'a'.repeat(MAX_INPUT_TITLE_LENGTH + 1);

        it('should stay hidden while the name fits', () => {
            mountWith(EXPERIMENT_DETAILS);

            spectator.typeInElement('a'.repeat(MAX_INPUT_TITLE_LENGTH), nameInput());
            spectator.detectChanges();

            expect(spectator.query(byTestId('details-name-error'))).toBeNull();
        });

        it('should say how long the name may be once the form reports it too long', () => {
            mountWith(EXPERIMENT_DETAILS);

            spectator.typeInElement(overlongName, nameInput());
            spectator.detectChanges();

            expect(spectator.query(byTestId('details-name-error'))?.textContent).toContain(
                `longer than ${MAX_INPUT_TITLE_LENGTH} characters`
            );
        });

        it('should replace the required error rather than stack with it', () => {
            // The two messages share one slot: a name that is too long is not a missing name.
            mountWith(EXPERIMENT_DETAILS);
            storeMock.$validationErrors.set(['name']);

            spectator.typeInElement(overlongName, nameInput());
            // What the store would report for a name that is now filled in, only too long.
            storeMock.$validationErrors.set([]);
            spectator.detectChanges();

            expect(spectator.query(byTestId('details-name-required-error'))).toBeNull();
            expect(spectator.query(byTestId('details-name-error'))).not.toBeNull();
        });
    });

    // The description had a length rule and nowhere to report it, so an overlong one used to
    // invalidate the form in silence.
    describe('description length error', () => {
        it('should stay hidden while the description fits', () => {
            mountWith(EXPERIMENT_DETAILS);

            spectator.typeInElement(
                'a'.repeat(MAX_INPUT_DESCRIPTIVE_LENGTH),
                descriptionTextarea()
            );
            spectator.detectChanges();

            expect(spectator.query(byTestId('details-description-error'))).toBeNull();
        });

        it('should say how long the description may be once the form reports it too long', () => {
            mountWith(EXPERIMENT_DETAILS);

            spectator.typeInElement(
                'a'.repeat(MAX_INPUT_DESCRIPTIVE_LENGTH + 1),
                descriptionTextarea()
            );
            spectator.detectChanges();

            expect(spectator.query(byTestId('details-description-error'))?.textContent).toContain(
                `longer than ${MAX_INPUT_DESCRIPTIVE_LENGTH} characters`
            );
        });
    });

    describe('locked experiment', () => {
        it('should disable both fields', () => {
            // AC34: every field is read-only once the experiment is no longer a draft. The rule
            // lives in the shell's schema, and reaches the card through the fields it is handed.
            storeMock.$isLocked.set(true);
            mountWith(EXPERIMENT_DETAILS);

            expect(nameInput().disabled).toBe(true);
            expect(descriptionTextarea().disabled).toBe(true);
        });

        it('should leave both fields editable while the experiment is a draft', () => {
            mountWith(EXPERIMENT_DETAILS);

            expect(nameInput().disabled).toBe(false);
            expect(descriptionTextarea().disabled).toBe(false);
        });
    });
});
