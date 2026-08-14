import { Dispatcher } from '@ngrx/signals/events';
import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { signal } from '@angular/core';
import { FieldTree } from '@angular/forms/signals';

import { DotMessageService } from '@dotcms/data-access';
import {
    DotExperiment,
    GOAL_OPERATORS,
    GOAL_TYPES,
    MAX_INPUT_DESCRIPTIVE_LENGTH
} from '@dotcms/dotcms-models';
import { getExperimentMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsConfigureGoalComponent } from './dot-experiments-configure-goal.component';

import { ConfigureValidationRule, GoalConditionFormModel } from '../../../shared/models';
import { dotExperimentsConfigurePageEvents } from '../../../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../../../store/dot-experiments-configure.store';

const DEFAULT_GOAL_NAMES = {
    [GOAL_TYPES.BOUNCE_RATE]: 'Minimize bounce rate',
    [GOAL_TYPES.EXIT_RATE]: 'Detect exit rate',
    [GOAL_TYPES.REACH_PAGE]: 'Maximize reach page',
    [GOAL_TYPES.URL_PARAMETER]: 'Detect query param in URL'
} as const;

const OPERATOR_LABELS = {
    [GOAL_OPERATORS.CONTAINS]: 'Contains',
    [GOAL_OPERATORS.EQUALS]: 'Equals',
    [GOAL_OPERATORS.EXISTS]: 'Exists'
} as const;

const GOAL_NAME_REQUIRED_COPY = 'Name the goal';
const GOAL_TYPE_REQUIRED_COPY = 'Pick a goal';
const CONDITION_VALUE_REQUIRED_COPY = 'The condition needs a value';
const PARAMETER_NAME_REQUIRED_COPY = 'The condition needs a parameter name';

const messageServiceMock = new MockDotMessageService({
    'experiments.goal.conditions.minimize.bounce.rate': DEFAULT_GOAL_NAMES[GOAL_TYPES.BOUNCE_RATE],
    'experiments.goal.conditions.detect.exit.rate': DEFAULT_GOAL_NAMES[GOAL_TYPES.EXIT_RATE],
    'experiments.goal.conditions.maximize.reach.page': DEFAULT_GOAL_NAMES[GOAL_TYPES.REACH_PAGE],
    'experiments.goal.conditions.detect.queryparam.in.url':
        DEFAULT_GOAL_NAMES[GOAL_TYPES.URL_PARAMETER],
    'experiments.goal.conditions.operators.contains.label':
        OPERATOR_LABELS[GOAL_OPERATORS.CONTAINS],
    'experiments.goal.conditions.operators.equals.label': OPERATOR_LABELS[GOAL_OPERATORS.EQUALS],
    'experiments.goal.conditions.operators.exists.label': OPERATOR_LABELS[GOAL_OPERATORS.EXISTS],
    'experiments.goal.conditions.params.url.label': 'URL',
    'experiments.configure.goal.name.required': GOAL_NAME_REQUIRED_COPY,
    'experiments.configure.goal.type.required': GOAL_TYPE_REQUIRED_COPY,
    'experiments.configure.goal.condition-value.required': CONDITION_VALUE_REQUIRED_COPY,
    'experiments.configure.goal.parameter-name.required': PARAMETER_NAME_REQUIRED_COPY
});

const EXPERIMENT: DotExperiment = { ...getExperimentMock(0), goals: null };

/** The shell provides the store; real signals keep the card's effects reactive. */
const createStoreMock = () => ({
    experiment: signal<DotExperiment | null>(EXPERIMENT),
    validationErrors: signal<ConfigureValidationRule[]>([]),
    $isLocked: signal(false)
});

interface GoalFormModel extends GoalConditionFormModel {
    type: GOAL_TYPES | '';
    name: string;
}

/**
 * The form tree is `protected`, and the two condition selects are PrimeNG overlays. Reading the
 * tree is the supported escape hatch for driving them, the same one the signal-forms specs use.
 */
const formTreeOf = (component: DotExperimentsConfigureGoalComponent): FieldTree<GoalFormModel> =>
    Reflect.get(component, 'formTree') as FieldTree<GoalFormModel>;

describe('DotExperimentsConfigureGoalComponent', () => {
    let spectator: Spectator<DotExperimentsConfigureGoalComponent>;
    let storeMock: ReturnType<typeof createStoreMock>;
    let dispatch: jest.SpyInstance;

    // The select's overlay queries `matchMedia`, which jsdom does not implement.
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
        component: DotExperimentsConfigureGoalComponent,
        providers: [
            { provide: DotExperimentsConfigureStore, useFactory: () => storeMock },
            { provide: DotMessageService, useValue: messageServiceMock }
        ],
        detectChanges: false
    });

    /** `injectDispatch` appends a scope argument, so only the event itself is compared. */
    const dispatchedEvents = () => dispatch.mock.calls.map(([event]) => event);

    const goalChangedPayloads = () =>
        dispatchedEvents()
            .filter(({ type }) => type === dotExperimentsConfigurePageEvents.goalChanged.type)
            .map(({ payload }) => payload);

    const nameInput = () => spectator.query(byTestId('goal-name-input')) as HTMLInputElement;

    const selectGoalType = (type: GOAL_TYPES) => {
        spectator.click(byTestId(`goal-type-${type}`));
        spectator.detectChanges();
    };

    const setOperator = (operator: GOAL_OPERATORS) => {
        formTreeOf(spectator.component).operator().value.set(operator);
        spectator.detectChanges();
    };

    /** Labels the operator select offers, read from the overlay it opens. */
    const openOperatorOptions = (): string[] => {
        spectator.click(byTestId('goal-operator-select'));
        spectator.detectChanges();

        // The panel is appended to the body, so it is outside the fixture's DOM.
        return Array.from(document.querySelectorAll('.p-select-option')).map((option) =>
            (option.textContent ?? '').trim()
        );
    };

    beforeEach(() => {
        storeMock = createStoreMock();
        spectator = createComponent();
        dispatch = jest.spyOn(spectator.inject(Dispatcher), 'dispatch');
        spectator.detectChanges();
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    describe('goal types', () => {
        it.each([
            GOAL_TYPES.BOUNCE_RATE,
            GOAL_TYPES.EXIT_RATE,
            GOAL_TYPES.REACH_PAGE,
            GOAL_TYPES.URL_PARAMETER
        ])('should offer %s', (type) => {
            expect(spectator.query(byTestId(`goal-type-${type}`))).not.toBeNull();
        });

        it('should offer exactly the four supported types', () => {
            const options = spectator.query(byTestId('goal-type-options'))?.children ?? [];

            expect(options.length).toBe(4);
            expect(
                spectator.query(byTestId(`goal-type-${GOAL_TYPES.CLICK_ON_ELEMENT}`))
            ).toBeNull();
        });

        it('should mark the picked type as checked', () => {
            selectGoalType(GOAL_TYPES.EXIT_RATE);

            expect(
                spectator
                    .query(byTestId(`goal-type-${GOAL_TYPES.EXIT_RATE}`))
                    ?.getAttribute('aria-checked')
            ).toBe('true');
        });
    });

    describe('goal name', () => {
        it('should seed the default name of the picked type', () => {
            selectGoalType(GOAL_TYPES.BOUNCE_RATE);

            expect(nameInput().value).toBe(DEFAULT_GOAL_NAMES[GOAL_TYPES.BOUNCE_RATE]);
        });

        it('should follow the type while the name is still a default', () => {
            selectGoalType(GOAL_TYPES.BOUNCE_RATE);
            selectGoalType(GOAL_TYPES.EXIT_RATE);

            expect(nameInput().value).toBe(DEFAULT_GOAL_NAMES[GOAL_TYPES.EXIT_RATE]);
        });

        it('should keep a name the user typed when the type changes', () => {
            selectGoalType(GOAL_TYPES.BOUNCE_RATE);

            spectator.typeInElement('Newsletter signups', nameInput());
            spectator.detectChanges();
            selectGoalType(GOAL_TYPES.EXIT_RATE);

            expect(nameInput().value).toBe('Newsletter signups');
        });
    });

    /**
     * The card renders no message for it — unlike the Details card — so the length rule is
     * asserted where it lives: on the field the form tree validates.
     */
    describe('goal name length', () => {
        const nameErrorKinds = (): string[] =>
            formTreeOf(spectator.component)
                .name()
                .errors()
                .map(({ kind }) => kind);

        const typeName = (name: string) => {
            selectGoalType(GOAL_TYPES.BOUNCE_RATE);
            spectator.typeInElement(name, nameInput());
            spectator.detectChanges();
        };

        it('should accept a name of exactly the maximum length', () => {
            typeName('a'.repeat(MAX_INPUT_DESCRIPTIVE_LENGTH));

            expect(nameErrorKinds()).toEqual([]);
        });

        it('should flag a name past the maximum length', () => {
            typeName('a'.repeat(MAX_INPUT_DESCRIPTIVE_LENGTH + 1));

            expect(nameErrorKinds()).toContain('maxLength');
        });
    });

    describe('goals without conditions', () => {
        it.each([GOAL_TYPES.BOUNCE_RATE, GOAL_TYPES.EXIT_RATE])(
            'should dispatch %s as soon as it is picked',
            (type) => {
                selectGoalType(type);

                expect(goalChangedPayloads()).toContainEqual({
                    primary: {
                        name: DEFAULT_GOAL_NAMES[type as keyof typeof DEFAULT_GOAL_NAMES],
                        type,
                        conditions: []
                    }
                });
            }
        );

        it('should render no condition panel for them', () => {
            selectGoalType(GOAL_TYPES.BOUNCE_RATE);

            expect(spectator.query(byTestId('goal-conditions-panel'))).toBeNull();
        });
    });

    describe('reach page goal', () => {
        beforeEach(() => selectGoalType(GOAL_TYPES.REACH_PAGE));

        it('should render the parameter, operator and value fields', () => {
            expect(spectator.query(byTestId('goal-conditions-panel'))).not.toBeNull();
            expect(spectator.query(byTestId('goal-parameter-select'))).not.toBeNull();
            expect(spectator.query(byTestId('goal-operator-select'))).not.toBeNull();
            expect(spectator.query(byTestId('goal-condition-value-input'))).not.toBeNull();
            expect(spectator.query(byTestId('goal-parameter-name-input'))).toBeNull();
        });

        it('should only offer the operators the backend validates', () => {
            expect(openOperatorOptions()).toEqual([
                OPERATOR_LABELS[GOAL_OPERATORS.CONTAINS],
                OPERATOR_LABELS[GOAL_OPERATORS.EQUALS]
            ]);
        });

        it('should not dispatch a goal while the condition has no value', () => {
            expect(goalChangedPayloads()).toEqual([]);
        });

        it('should dispatch the goal once the condition is complete', () => {
            spectator.typeInElement(
                '/pricing',
                spectator.query(byTestId('goal-condition-value-input')) as HTMLInputElement
            );
            spectator.detectChanges();

            expect(goalChangedPayloads()).toContainEqual({
                primary: {
                    name: DEFAULT_GOAL_NAMES[GOAL_TYPES.REACH_PAGE],
                    type: GOAL_TYPES.REACH_PAGE,
                    conditions: [
                        {
                            parameter: 'url',
                            operator: GOAL_OPERATORS.CONTAINS,
                            value: '/pricing'
                        }
                    ]
                }
            });
        });
    });

    describe('url parameter goal', () => {
        beforeEach(() => selectGoalType(GOAL_TYPES.URL_PARAMETER));

        const typeParameterName = (name: string) => {
            spectator.typeInElement(
                name,
                spectator.query(byTestId('goal-parameter-name-input')) as HTMLInputElement
            );
            spectator.detectChanges();
        };

        it('should ask for a parameter name instead of a parameter', () => {
            expect(spectator.query(byTestId('goal-parameter-name-input'))).not.toBeNull();
            expect(spectator.query(byTestId('goal-parameter-select'))).toBeNull();
        });

        it('should also offer the exists operator', () => {
            expect(openOperatorOptions()).toEqual([
                OPERATOR_LABELS[GOAL_OPERATORS.CONTAINS],
                OPERATOR_LABELS[GOAL_OPERATORS.EQUALS],
                OPERATOR_LABELS[GOAL_OPERATORS.EXISTS]
            ]);
        });

        it('should hide the value field for the exists operator', () => {
            // AC18: EXISTS only asks whether the parameter is there.
            expect(spectator.query(byTestId('goal-condition-value-input'))).not.toBeNull();

            setOperator(GOAL_OPERATORS.EXISTS);

            expect(spectator.query(byTestId('goal-condition-value-input'))).toBeNull();
        });

        it('should not dispatch a goal while the parameter name is missing', () => {
            setOperator(GOAL_OPERATORS.EXISTS);

            expect(goalChangedPayloads()).toEqual([]);
        });

        it('should dispatch the goal without a value once exists is picked', () => {
            typeParameterName('utm_source');
            setOperator(GOAL_OPERATORS.EXISTS);

            expect(goalChangedPayloads()).toContainEqual({
                primary: {
                    name: DEFAULT_GOAL_NAMES[GOAL_TYPES.URL_PARAMETER],
                    type: GOAL_TYPES.URL_PARAMETER,
                    conditions: [
                        {
                            parameter: 'queryParameter',
                            operator: GOAL_OPERATORS.EXISTS,
                            value: { name: 'utm_source', value: '' }
                        }
                    ]
                }
            });
        });

        it('should require a value for every other operator', () => {
            typeParameterName('utm_source');

            expect(goalChangedPayloads()).toEqual([]);

            spectator.typeInElement(
                'newsletter',
                spectator.query(byTestId('goal-condition-value-input')) as HTMLInputElement
            );
            spectator.detectChanges();

            expect(goalChangedPayloads()).toContainEqual({
                primary: {
                    name: DEFAULT_GOAL_NAMES[GOAL_TYPES.URL_PARAMETER],
                    type: GOAL_TYPES.URL_PARAMETER,
                    conditions: [
                        {
                            parameter: 'queryParameter',
                            operator: GOAL_OPERATORS.EQUALS,
                            value: { name: 'utm_source', value: 'newsletter' }
                        }
                    ]
                }
            });
        });
    });

    describe('validation errors', () => {
        it('should show none before Start is pressed', () => {
            // AC28: an untouched card never accuses the user of anything.
            expect(spectator.query(byTestId('goal-type-error'))).toBeNull();
            expect(spectator.query(byTestId('goal-name-error'))).toBeNull();
        });

        it('should reveal the missing type once the store reports it', () => {
            storeMock.validationErrors.set(['goalType']);
            spectator.detectChanges();

            expect(spectator.query(byTestId('goal-type-error'))?.textContent).toContain(
                GOAL_TYPE_REQUIRED_COPY
            );
        });

        it('should hide the type error again once a type is picked', () => {
            storeMock.validationErrors.set(['goalType']);
            spectator.detectChanges();

            selectGoalType(GOAL_TYPES.BOUNCE_RATE);

            expect(spectator.query(byTestId('goal-type-error'))).toBeNull();
        });

        it('should reveal the missing name once the store reports it', () => {
            storeMock.validationErrors.set(['goalName']);
            spectator.detectChanges();

            expect(spectator.query(byTestId('goal-name-error'))?.textContent).toContain(
                GOAL_NAME_REQUIRED_COPY
            );
        });

        it('should reveal the missing condition value once the store reports it', () => {
            storeMock.validationErrors.set(['goalConditionValue']);
            selectGoalType(GOAL_TYPES.REACH_PAGE);

            expect(spectator.query(byTestId('goal-condition-value-error'))?.textContent).toContain(
                CONDITION_VALUE_REQUIRED_COPY
            );
        });

        it('should keep the condition value error out of sight for the exists operator', () => {
            storeMock.validationErrors.set(['goalConditionValue']);
            selectGoalType(GOAL_TYPES.URL_PARAMETER);

            setOperator(GOAL_OPERATORS.EXISTS);

            expect(spectator.query(byTestId('goal-condition-value-error'))).toBeNull();
        });

        it('should reveal the missing parameter name once the store reports it', () => {
            storeMock.validationErrors.set(['goalParameterName']);
            selectGoalType(GOAL_TYPES.URL_PARAMETER);

            expect(spectator.query(byTestId('goal-parameter-name-error'))?.textContent).toContain(
                PARAMETER_NAME_REQUIRED_COPY
            );
        });
    });

    describe('locked experiment', () => {
        beforeEach(() => {
            storeMock.$isLocked.set(true);
            spectator.detectChanges();
        });

        it('should disable every goal type', () => {
            const options = Array.from(spectator.queryAll('[role="radio"]')) as HTMLButtonElement[];

            expect(options.length).toBe(4);
            expect(options.every(({ disabled }) => disabled)).toBe(true);
        });

        it('should disable the goal name', () => {
            expect(nameInput().disabled).toBe(true);
        });

        it('should not change the goal when a type is clicked', () => {
            // AC34: a locked experiment is read-only, whatever the DOM allows.
            spectator.click(byTestId(`goal-type-${GOAL_TYPES.BOUNCE_RATE}`));
            spectator.detectChanges();

            expect(goalChangedPayloads()).toEqual([]);
            expect(nameInput().value).toBe('');
        });
    });
});
