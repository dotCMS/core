import { Dispatcher, EventCreator } from '@ngrx/signals/events';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { provideLocationMocks } from '@angular/common/testing';
import { ApplicationRef, Component, input, signal, WritableSignal } from '@angular/core';
import { FieldTree } from '@angular/forms/signals';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';

import { ConfirmationService } from 'primeng/api';

import { DotMessageDisplayService, DotMessageService } from '@dotcms/data-access';
import {
    ComponentStatus,
    DotExperiment,
    DotExperimentPatchBody,
    DotExperimentStatus,
    DotMessageSeverity,
    EXP_CONFIG_ERROR_LABEL_CANT_EDIT,
    ExperimentsConfigProperties,
    GOAL_OPERATORS,
    GOAL_TYPES,
    PROP_NOT_FOUND,
    TrafficProportionTypes,
    Variant
} from '@dotcms/dotcms-models';
import { getExperimentMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsConfigureDetailsComponent } from './components/dot-experiments-configure-details/dot-experiments-configure-details.component';
import { DotExperimentsConfigureFooterComponent } from './components/dot-experiments-configure-footer/dot-experiments-configure-footer.component';
import { DotExperimentsConfigureGoalComponent } from './components/dot-experiments-configure-goal/dot-experiments-configure-goal.component';
import { DotExperimentsConfigureHeaderComponent } from './components/dot-experiments-configure-header/dot-experiments-configure-header.component';
import { DotExperimentsConfigurePageComponent } from './components/dot-experiments-configure-page/dot-experiments-configure-page.component';
import { DotExperimentsConfigureSchedulingComponent } from './components/dot-experiments-configure-scheduling/dot-experiments-configure-scheduling.component';
import { DotExperimentsConfigureVariantsComponent } from './components/dot-experiments-configure-variants/dot-experiments-configure-variants.component';
import { DotExperimentsConfigureComponent } from './dot-experiments-configure.component';

import { LOCKED_BANNER_KEY_READ_ONLY, LOCKED_BANNER_KEY_RUNNING } from '../shared/constants';
import {
    ConfigureFormModel,
    ConfigureValidationRule,
    VariantWeightFormRow
} from '../shared/models';
import { dotExperimentsConfigureApiEvents } from '../store/dot-experiments-configure-api.events';
import { dotExperimentsConfigurePageEvents } from '../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../store/dot-experiments-configure.store';
import { EMPTY_GOAL_SLICE } from '../util/dot-experiments-configure-form.util';

const ERROR_COPY = {
    title: 'Could not load the experiment',
    subtitle: 'Failed to retrieve experiments data'
};

const LOCKED_COPY = {
    running: 'This experiment is running and cannot be edited',
    readOnly: 'This experiment can no longer be edited'
};

const messageServiceMock = new MockDotMessageService({
    'experiments.list.error.title': ERROR_COPY.title,
    'experiments.error.fetching.data': ERROR_COPY.subtitle,
    'experiments.configure.action.back-to-list': 'Back to experiments',
    [LOCKED_BANNER_KEY_RUNNING]: LOCKED_COPY.running,
    [LOCKED_BANNER_KEY_READ_ONLY]: LOCKED_COPY.readOnly,
    'experiments.configure.notification.created': 'Experiment {0} created',
    'experiments.action.scheduled.confirm-message': 'Experiment {0} scheduled',
    'experiments.action.start.confirm-message': 'Experiment {0} started',
    'experiments.action.stop.confirm-message': 'Experiment {0} ended',
    'experiments.notification.cancel.schedule': 'Experiment {0} unscheduled',
    'experiments.notification.abort': 'Experiment {0} aborted'
});

const MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000;

/** The backend reports its limits in days. Both are well inside the 7/90-day defaults. */
const CONFIGURED_MIN_DURATION_DAYS = 3;
const CONFIGURED_MAX_DURATION_DAYS = 30;

const CONFIGURED_DURATIONS = {
    [ExperimentsConfigProperties.EXPERIMENTS_MIN_DURATION]: String(CONFIGURED_MIN_DURATION_DAYS),
    [ExperimentsConfigProperties.EXPERIMENTS_MAX_DURATION]: String(CONFIGURED_MAX_DURATION_DAYS)
};

const daysFromNow = (days: number): Date => new Date(Date.now() + days * MILLISECONDS_PER_DAY);

const EXPERIMENT: DotExperiment = {
    ...getExperimentMock(0),
    id: 'exp-1',
    name: 'Summer landing test',
    description: 'Compares two hero images',
    goals: null,
    scheduling: null,
    trafficAllocation: 100
};

/** The control of the single-variant mock the screen loads by default. */
const [CONTROL_VARIANT] = EXPERIMENT.trafficProportion.variants;

const SECOND_VARIANT: Variant = { id: 'variant-b', name: 'Variant B', weight: 50 };

/** The same experiment once a second variant exists, weights adding up. */
const withVariants = (variants: Variant[]): DotExperiment => ({
    ...EXPERIMENT,
    trafficProportion: { ...EXPERIMENT.trafficProportion, variants }
});

const TWO_VARIANTS: Variant[] = [{ ...CONTROL_VARIANT, weight: 50 }, SECOND_VARIANT];

/**
 * The cards are shallow-rendered: this screen owns the layout, the banner, the toasts, the scroll to
 * the first failing field — and the form the cards render slices of. What each card does with the
 * slice it is handed belongs to that card's spec, so the stubs only declare the inputs the shell
 * binds.
 *
 * Two of them carry a `[data-error]` marker, standing in for a card revealing a failed rule, so
 * the scroll has something to find and the *first* one can be told apart from the second.
 */
@Component({ selector: 'dot-experiments-configure-header', template: '' })
class HeaderStubComponent {}

@Component({
    selector: 'dot-experiments-configure-details',
    template: '<span data-error data-testid="details-error-marker"></span>'
})
class DetailsStubComponent {
    readonly nameField = input<FieldTree<string>>();
    readonly descriptionField = input<FieldTree<string>>();
}

@Component({
    selector: 'dot-experiments-configure-goal',
    template: '<span data-error data-testid="goal-error-marker"></span>'
})
class GoalStubComponent {
    readonly field = input<unknown>();
}

@Component({ selector: 'dot-experiments-configure-page', template: '' })
class PageStubComponent {
    readonly field = input<FieldTree<number>>();
}

@Component({ selector: 'dot-experiments-configure-variants', template: '' })
class VariantsStubComponent {
    readonly field = input<FieldTree<VariantWeightFormRow[]>>();
}

@Component({ selector: 'dot-experiments-configure-scheduling', template: '' })
class SchedulingStubComponent {
    readonly field = input<unknown>();
    readonly bounds = input<unknown>();
}

@Component({ selector: 'dot-experiments-configure-footer', template: '' })
class FooterStubComponent {}

/**
 * The store is provided by the component itself, so it is replaced through `componentProviders`.
 * Real signals rather than `jest.fn()`s: the shell derives `$isLoading` in a `computed`, fills the
 * form in an `effect` and watches it in another, none of which would ever re-run over a plain
 * function.
 */
const createStoreMock = () => ({
    status: signal<ComponentStatus>(ComponentStatus.LOADED),
    validationErrors: signal<ConfigureValidationRule[]>([]),
    $lockedBannerKey: signal<string | null>(null),
    $isLocked: signal(false),
    $isAutosaving: signal(false),
    $isSaving: signal(false),
    experiment: signal<DotExperiment | null>(null),
    // The weights slice is seeded from these, so they move with `experiment` (see `loadExperiment`).
    $variants: signal<Variant[]>([]),
    $disabledTooltipKey: signal<string | null>(null),
    draftName: signal(''),
    draftDescription: signal('')
});

describe('DotExperimentsConfigureComponent', () => {
    let spectator: Spectator<DotExperimentsConfigureComponent>;
    let storeMock: ReturnType<typeof createStoreMock>;
    let scrollIntoView: jest.Mock;
    let dispatch: jest.SpyInstance;

    /**
     * A screen mounted on one of the two URLs it answers on, with whatever the config resolver
     * published. Both are read once, on init, exactly as the store reads the route.
     */
    const createComponentOn = ({
        experimentId,
        configProps
    }: {
        experimentId?: string;
        configProps?: Record<string, string>;
    }) =>
        createComponentFactory({
            component: DotExperimentsConfigureComponent,
            // `componentProviders` replaces the component's own `providers`, so the real
            // `ConfirmationService` has to be re-declared here (`p-confirmDialog` needs it).
            componentProviders: [
                { provide: DotExperimentsConfigureStore, useFactory: () => storeMock },
                ConfirmationService
            ],
            providers: [
                provideRouter([{ path: 'experiments', children: [] }]),
                provideLocationMocks(),
                { provide: DotMessageService, useValue: messageServiceMock },
                mockProvider(DotMessageDisplayService),
                {
                    provide: ActivatedRoute,
                    useValue: {
                        snapshot: {
                            paramMap: convertToParamMap(experimentId ? { experimentId } : {}),
                            data: configProps ? { config: configProps } : {}
                        }
                    }
                }
            ],
            overrideComponents: [
                [
                    DotExperimentsConfigureComponent,
                    {
                        remove: {
                            imports: [
                                DotExperimentsConfigureHeaderComponent,
                                DotExperimentsConfigureDetailsComponent,
                                DotExperimentsConfigureGoalComponent,
                                DotExperimentsConfigurePageComponent,
                                DotExperimentsConfigureVariantsComponent,
                                DotExperimentsConfigureSchedulingComponent,
                                DotExperimentsConfigureFooterComponent
                            ]
                        },
                        add: {
                            imports: [
                                HeaderStubComponent,
                                DetailsStubComponent,
                                GoalStubComponent,
                                PageStubComponent,
                                VariantsStubComponent,
                                SchedulingStubComponent,
                                FooterStubComponent
                            ]
                        }
                    }
                ]
            ],
            detectChanges: false
        });

    /** The form model is `protected`, so the spec reads it the way the card specs read a tree. */
    const modelOf = (): WritableSignal<ConfigureFormModel> =>
        Reflect.get(spectator.component, '$model') as WritableSignal<ConfigureFormModel>;

    /** Edits the form as a card would, and lets the autosave binding see it. */
    const editForm = (change: Partial<ConfigureFormModel>) => {
        modelOf().update((model) => ({ ...model, ...change }));
        spectator.detectChanges();
    };

    /**
     * Publishes a loaded experiment, which is what fills the form. `$variants` follows it the way
     * the real store's computed does — the weights slice is seeded from it.
     */
    const loadExperiment = (experiment: DotExperiment = EXPERIMENT) => {
        storeMock.experiment.set(experiment);
        storeMock.$variants.set(experiment.trafficProportion?.variants ?? []);
        storeMock.draftName.set(experiment.name);
        storeMock.draftDescription.set(experiment.description ?? '');
        spectator.detectChanges();
    };

    /**
     * The weights slice as plain rows. Signal forms brand the items it writes back with a tracking
     * symbol, which a deep comparison would otherwise report as a difference.
     */
    const weightsOf = (): VariantWeightFormRow[] =>
        modelOf()().variantWeights.map(({ id, weight }) => ({ id, weight }));

    /** `injectDispatch` appends a scope argument, so only the event itself is compared. */
    const dispatchedEvents = () => dispatch.mock.calls.map(([event]) => event);

    /** The bodies of every edit the screen reported. */
    const reportedPatches = (): DotExperimentPatchBody[] =>
        dispatchedEvents()
            .filter(({ type }) => type === dotExperimentsConfigurePageEvents.formEdited.type)
            .map(({ payload }) => payload as DotExperimentPatchBody);

    /** Dispatches an outcome the store would have raised once a call settled. */
    const emitSucceeded = (
        event: EventCreator<string, DotExperiment>,
        experiment: DotExperiment
    ) => {
        spectator.inject(Dispatcher).dispatch(event(experiment));
        spectator.detectChanges();
    };

    /** Renders, then flushes the render hooks the scroll-to-error effect schedules. */
    const flush = () => {
        spectator.detectChanges();
        spectator.inject(ApplicationRef).tick();
    };

    beforeEach(() => {
        storeMock = createStoreMock();
        // jsdom does not implement scrollIntoView, so there is nothing to spy on.
        scrollIntoView = jest.fn();
        Element.prototype.scrollIntoView = scrollIntoView;
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('on an existing experiment', () => {
        const createComponent = createComponentOn({
            experimentId: EXPERIMENT.id,
            configProps: CONFIGURED_DURATIONS
        });

        beforeEach(() => {
            spectator = createComponent();
            dispatch = jest.spyOn(spectator.inject(Dispatcher), 'dispatch');
        });

        describe('loaded screen', () => {
            it.each([
                'dot-experiments-configure-header',
                'dot-experiments-configure-details',
                'dot-experiments-configure-goal',
                'dot-experiments-configure-page',
                'dot-experiments-configure-variants',
                'dot-experiments-configure-scheduling',
                'dot-experiments-configure-footer'
            ])('should render %s', (selector) => {
                spectator.detectChanges();

                expect(spectator.query(selector)).not.toBeNull();
            });

            it('should render the cards inside the scrolling body', () => {
                spectator.detectChanges();

                const body = spectator.query(byTestId('experiments-configure-body'));

                expect(body?.querySelector('dot-experiments-configure-details')).not.toBeNull();
                expect(body?.querySelector('dot-experiments-configure-scheduling')).not.toBeNull();
            });

            it('should keep the header and the footer out of the scrolling body', () => {
                // They are pinned: the body is the only region that scrolls.
                spectator.detectChanges();

                const body = spectator.query(byTestId('experiments-configure-body'));

                expect(body?.querySelector('dot-experiments-configure-header')).toBeNull();
                expect(body?.querySelector('dot-experiments-configure-footer')).toBeNull();
            });

            it('should not render the loading or the error state', () => {
                spectator.detectChanges();

                expect(spectator.query(byTestId('experiments-configure-loading'))).toBeNull();
                expect(spectator.query(byTestId('experiments-configure-error'))).toBeNull();
            });
        });

        describe('loading', () => {
            it.each([ComponentStatus.INIT, ComponentStatus.LOADING])(
                'should render the skeleton on %s',
                (status) => {
                    // INIT counts: the route is read in the store's `onInit`, so an existing
                    // experiment spends a tick there before its load starts.
                    storeMock.status.set(status);
                    spectator.detectChanges();

                    expect(
                        spectator.query(byTestId('experiments-configure-loading'))
                    ).not.toBeNull();
                    expect(spectator.query(byTestId('experiments-configure-body'))).toBeNull();
                }
            );

            it('should not render any card while loading', () => {
                storeMock.status.set(ComponentStatus.LOADING);
                spectator.detectChanges();

                expect(spectator.query('dot-experiments-configure-header')).toBeNull();
                expect(spectator.query('dot-experiments-configure-footer')).toBeNull();
            });
        });

        describe('load error', () => {
            const renderError = () => {
                storeMock.status.set(ComponentStatus.ERROR);
                spectator.detectChanges();
            };

            it('should replace the screen with the error state', () => {
                renderError();

                const error = spectator.query(byTestId('experiments-configure-error'));

                expect(error?.textContent).toContain(ERROR_COPY.title);
                expect(error?.textContent).toContain(ERROR_COPY.subtitle);
                expect(spectator.query(byTestId('experiments-configure-body'))).toBeNull();
                expect(spectator.query(byTestId('experiments-configure-loading'))).toBeNull();
            });

            it('should offer a way back to the list', () => {
                renderError();
                const router = spectator.inject(Router);
                const navigate = jest.spyOn(router, 'navigate').mockResolvedValue(true);

                spectator.click(
                    spectator
                        .query(byTestId('experiments-configure-error'))
                        ?.querySelector('[data-testid="message-button"]') as HTMLElement
                );

                expect(navigate).toHaveBeenCalledWith(['/experiments']);
            });
        });

        describe('read-only banner', () => {
            it('should not render a banner while the experiment is editable', () => {
                spectator.detectChanges();

                expect(spectator.query(byTestId('experiments-configure-locked-banner'))).toBeNull();
            });

            it('should say the experiment is running when it is', () => {
                storeMock.$lockedBannerKey.set(LOCKED_BANNER_KEY_RUNNING);
                spectator.detectChanges();

                expect(
                    spectator.query(byTestId('experiments-configure-locked-banner'))?.textContent
                ).toContain(LOCKED_COPY.running);
            });

            it('should fall back to the generic copy for every other locked status', () => {
                storeMock.$lockedBannerKey.set(LOCKED_BANNER_KEY_READ_ONLY);
                spectator.detectChanges();

                expect(
                    spectator.query(byTestId('experiments-configure-locked-banner'))?.textContent
                ).toContain(LOCKED_COPY.readOnly);
            });

            it('should keep the cards on screen: the fields are frozen, not hidden', () => {
                storeMock.$lockedBannerKey.set(LOCKED_BANNER_KEY_RUNNING);
                spectator.detectChanges();

                expect(spectator.query(byTestId('experiments-configure-body'))).not.toBeNull();
                expect(spectator.query('dot-experiments-configure-footer')).not.toBeNull();
            });
        });

        describe('saving progress bar', () => {
            it('should stay hidden while nothing is being persisted', () => {
                spectator.detectChanges();

                expect(spectator.query(byTestId('experiments-configure-progress-bar'))).toBeNull();
            });

            it('should not run for the debounce window: typing alone is the footer copy, not a bar', () => {
                storeMock.$isAutosaving.set(true);
                spectator.detectChanges();

                expect(spectator.query(byTestId('experiments-configure-progress-bar'))).toBeNull();
            });

            it('should run under the header while a request is on the wire', () => {
                storeMock.$isSaving.set(true);
                spectator.detectChanges();

                expect(
                    spectator.query(byTestId('experiments-configure-progress-bar'))
                ).not.toBeNull();
            });

            it('should leave once the save settles', () => {
                storeMock.$isSaving.set(true);
                spectator.detectChanges();
                storeMock.$isSaving.set(false);
                spectator.detectChanges();

                expect(spectator.query(byTestId('experiments-configure-progress-bar'))).toBeNull();
            });
        });

        describe('filling the form', () => {
            it('should fill every slice from the loaded experiment', () => {
                loadExperiment({
                    ...EXPERIMENT,
                    trafficAllocation: 40,
                    scheduling: { startDate: 1893456000000, endDate: 1893542400000 },
                    goals: {
                        primary: {
                            name: 'Reach pricing',
                            type: GOAL_TYPES.REACH_PAGE,
                            conditions: [
                                {
                                    parameter: 'url',
                                    operator: GOAL_OPERATORS.CONTAINS,
                                    value: '/pricing'
                                }
                            ]
                        }
                    }
                });

                expect({ ...modelOf()(), variantWeights: weightsOf() }).toEqual({
                    name: EXPERIMENT.name,
                    description: EXPERIMENT.description,
                    goal: {
                        type: GOAL_TYPES.REACH_PAGE,
                        name: 'Reach pricing',
                        parameter: 'url',
                        operator: GOAL_OPERATORS.CONTAINS,
                        parameterName: '',
                        value: '/pricing'
                    },
                    trafficAllocation: 40,
                    scheduling: {
                        startDate: new Date(1893456000000),
                        endDate: new Date(1893542400000)
                    },
                    variantWeights: [{ id: CONTROL_VARIANT.id, weight: CONTROL_VARIANT.weight }]
                });
            });

            it('should start empty while the experiment is still loading', () => {
                spectator.detectChanges();

                expect(modelOf()()).toEqual({
                    name: '',
                    description: '',
                    goal: EMPTY_GOAL_SLICE,
                    trafficAllocation: 100,
                    scheduling: { startDate: null, endDate: null },
                    variantWeights: []
                });
            });

            it('should not report an edit just for being filled in', () => {
                loadExperiment();

                expect(reportedPatches()).toEqual([]);
            });

            it('should keep what is on screen when an autosave response replaces the experiment', () => {
                // Every PATCH answers with a whole new experiment object; re-reading it would drop
                // what was typed while the call was travelling.
                loadExperiment();
                editForm({ trafficAllocation: 40 });

                storeMock.experiment.set({ ...EXPERIMENT, trafficAllocation: 100 });
                spectator.detectChanges();

                expect(modelOf()().trafficAllocation).toBe(40);
            });
        });

        /**
         * The weights slice is the one part of the form that is filled from the *store's* copy of the
         * experiment rather than once per screen: variants come and go through their own endpoints,
         * and each of those answers with a new list the rows have to follow.
         */
        describe('the weights slice', () => {
            it('should hold one row per variant, in the order they are drawn', () => {
                loadExperiment(withVariants(TWO_VARIANTS));

                expect(weightsOf()).toEqual([
                    { id: CONTROL_VARIANT.id, weight: 50 },
                    { id: SECOND_VARIANT.id, weight: 50 }
                ]);
            });

            it('should re-seed when a variant is added', () => {
                loadExperiment();

                expect(weightsOf().length).toBe(1);

                loadExperiment(withVariants(TWO_VARIANTS));

                expect(weightsOf()).toEqual([
                    { id: CONTROL_VARIANT.id, weight: 50 },
                    { id: SECOND_VARIANT.id, weight: 50 }
                ]);
            });

            it('should re-seed when a variant is deleted, weights the backend recomputed included', () => {
                loadExperiment(withVariants(TWO_VARIANTS));

                loadExperiment(withVariants([{ ...CONTROL_VARIANT, weight: 100 }]));

                expect(weightsOf()).toEqual([{ id: CONTROL_VARIANT.id, weight: 100 }]);
            });

            it('should not clobber a weight being typed when a save response echoes the old one', () => {
                // The defect this guards: every PATCH answers with a whole experiment, and re-seeding
                // from one that knows nothing about the weight just typed would snap the row back.
                loadExperiment(withVariants(TWO_VARIANTS));

                editForm({
                    variantWeights: [
                        { id: CONTROL_VARIANT.id, weight: 70 },
                        { id: SECOND_VARIANT.id, weight: 50 }
                    ]
                });

                storeMock.experiment.set(withVariants(TWO_VARIANTS));
                spectator.detectChanges();

                expect(weightsOf()).toEqual([
                    { id: CONTROL_VARIANT.id, weight: 70 },
                    { id: SECOND_VARIANT.id, weight: 50 }
                ]);
            });

            it('should hold no rows before the experiment exists', () => {
                spectator.detectChanges();

                expect(weightsOf()).toEqual([]);
            });
        });

        /**
         * Weights are the one key reported even while they are invalid: the rows, the total and the
         * Start validation all read the store's copy, and the gate that keeps an intermediate total
         * off the wire is the store's own (`toOutgoingPatch`).
         */
        describe('reporting the weights', () => {
            beforeEach(() => loadExperiment(withVariants(TWO_VARIANTS)));

            const reportWeights = (control: number, variantB: number | null) =>
                editForm({
                    variantWeights: [
                        { id: CONTROL_VARIANT.id, weight: control },
                        { id: SECOND_VARIANT.id, weight: variantB }
                    ]
                });

            it('should report the whole proportion, not the row that changed', () => {
                reportWeights(30, 70);

                expect(reportedPatches()).toContainEqual({
                    trafficProportion: {
                        type: TrafficProportionTypes.CUSTOM_PERCENTAGES,
                        variants: [
                            { ...CONTROL_VARIANT, weight: 30 },
                            { ...SECOND_VARIANT, weight: 70 }
                        ]
                    }
                });
            });

            it('should report weights that do not add up as well', () => {
                reportWeights(70, 50);

                expect(reportedPatches()).toContainEqual({
                    trafficProportion: {
                        type: TrafficProportionTypes.CUSTOM_PERCENTAGES,
                        variants: [
                            { ...CONTROL_VARIANT, weight: 70 },
                            { ...SECOND_VARIANT, weight: 50 }
                        ]
                    }
                });
            });

            it('should report a cleared weight as the zero it counts as', () => {
                reportWeights(70, null);

                expect(reportedPatches()).toContainEqual({
                    trafficProportion: {
                        type: TrafficProportionTypes.CUSTOM_PERCENTAGES,
                        variants: [
                            { ...CONTROL_VARIANT, weight: 70 },
                            { ...SECOND_VARIANT, weight: 0 }
                        ]
                    }
                });
            });

            it('should not report the weights it was just seeded with', () => {
                expect(reportedPatches()).toEqual([]);
            });

            it('should not report an edit again when the store echoes it back', () => {
                reportWeights(30, 70);
                const reportedOnce = reportedPatches().length;

                storeMock.experiment.set(
                    withVariants([
                        { ...CONTROL_VARIANT, weight: 30 },
                        { ...SECOND_VARIANT, weight: 70 }
                    ])
                );
                storeMock.$variants.set([
                    { ...CONTROL_VARIANT, weight: 30 },
                    { ...SECOND_VARIANT, weight: 70 }
                ]);
                spectator.detectChanges();

                expect(reportedPatches().length).toBe(reportedOnce);
            });
        });

        describe('reporting what changed', () => {
            beforeEach(() => loadExperiment());

            it('should report a typed name on its own', () => {
                editForm({ name: 'Winter landing test' });

                expect(reportedPatches()).toContainEqual({ name: 'Winter landing test' });
            });

            it('should report an emptied description, which is a change like any other', () => {
                editForm({ description: '' });

                expect(reportedPatches()).toContainEqual({ description: '' });
            });

            it('should never report a blank name, which the backend rejects', () => {
                editForm({ name: '   ' });

                expect(reportedPatches()).toEqual([]);
            });

            it('should report a complete goal in the shape the endpoint persists', () => {
                editForm({
                    goal: {
                        type: GOAL_TYPES.REACH_PAGE,
                        name: 'Reach pricing',
                        parameter: 'url',
                        operator: GOAL_OPERATORS.CONTAINS,
                        parameterName: '',
                        value: '/pricing'
                    }
                });

                expect(reportedPatches()).toContainEqual({
                    goals: {
                        primary: {
                            name: 'Reach pricing',
                            type: GOAL_TYPES.REACH_PAGE,
                            conditions: [
                                {
                                    parameter: 'url',
                                    operator: GOAL_OPERATORS.CONTAINS,
                                    value: '/pricing'
                                }
                            ]
                        }
                    }
                });
            });

            it('should not report a goal whose condition is still half typed', () => {
                editForm({
                    goal: {
                        type: GOAL_TYPES.REACH_PAGE,
                        name: 'Reach pricing',
                        parameter: 'url',
                        operator: GOAL_OPERATORS.CONTAINS,
                        parameterName: '',
                        value: ''
                    }
                });

                expect(reportedPatches()).toEqual([]);
            });

            it('should report a new allocation', () => {
                editForm({ trafficAllocation: 40 });

                expect(reportedPatches()).toContainEqual({ trafficAllocation: 40 });
            });

            it('should not report an allocation outside 1-100', () => {
                editForm({ trafficAllocation: 120 });

                expect(reportedPatches()).toEqual([]);
            });

            it('should report a schedule as instants', () => {
                const startDate = daysFromNow(1);
                const endDate = daysFromNow(CONFIGURED_MIN_DURATION_DAYS + 2);

                editForm({ scheduling: { startDate, endDate } });

                expect(reportedPatches()).toContainEqual({
                    scheduling: { startDate: startDate.getTime(), endDate: endDate.getTime() }
                });
            });

            it('should report an emptied schedule as no schedule at all', () => {
                // `null` is what the PATCH endpoint reads as "starts when Start is pressed".
                loadExperiment({
                    ...EXPERIMENT,
                    scheduling: { startDate: daysFromNow(1).getTime(), endDate: null }
                });

                editForm({ scheduling: { startDate: null, endDate: null } });

                expect(reportedPatches()).toContainEqual({ scheduling: null });
            });

            it('should carry both keys in one body when two cards changed together', () => {
                // `PATCH /api/v1/experiments/{id}` applies every key of its body at once (AC6).
                editForm({ name: 'Winter landing test', trafficAllocation: 40 });

                expect(reportedPatches()).toContainEqual({
                    name: 'Winter landing test',
                    trafficAllocation: 40
                });
            });
        });

        describe('the rules over the form', () => {
            beforeEach(() => loadExperiment());

            it('should not require anything, so nothing is invalid before Start is pressed', () => {
                // AC28: `required` reaches the DOM as the native attribute, which would paint an
                // untouched Name red. The store checks it when Start/Schedule is pressed.
                editForm({ name: '', goal: EMPTY_GOAL_SLICE });

                expect(spectator.query('[required]')).toBeNull();
            });

            it('should reject an end date before the configured minimum duration', () => {
                editForm({
                    scheduling: {
                        startDate: null,
                        endDate: daysFromNow(CONFIGURED_MIN_DURATION_DAYS - 1)
                    }
                });

                expect(reportedPatches()).toEqual([]);
            });

            it('should accept an end date inside the configured window', () => {
                editForm({
                    scheduling: {
                        startDate: null,
                        endDate: daysFromNow(CONFIGURED_MIN_DURATION_DAYS + 1)
                    }
                });

                expect(reportedPatches().length).toBe(1);
            });

            it('should reject an end date beyond the configured maximum duration', () => {
                editForm({
                    scheduling: {
                        startDate: null,
                        endDate: daysFromNow(CONFIGURED_MAX_DURATION_DAYS + 10)
                    }
                });

                expect(reportedPatches()).toEqual([]);
            });
        });

        describe('success toasts', () => {
            const experiment = getExperimentMock(0);

            const pushedMessage = (): string =>
                (spectator.inject(DotMessageDisplayService, true).push as jest.Mock).mock.calls.at(
                    -1
                )?.[0].message;

            beforeEach(() => spectator.detectChanges());

            it.each([
                ['created', dotExperimentsConfigureApiEvents.createSucceeded],
                ['ended', dotExperimentsConfigureApiEvents.stopSucceeded],
                ['unscheduled', dotExperimentsConfigureApiEvents.cancelScheduleSucceeded],
                ['aborted', dotExperimentsConfigureApiEvents.abortSucceeded]
            ])('should push a success toast once the experiment is %s', (expectedVerb, event) => {
                emitSucceeded(event as EventCreator<string, DotExperiment>, experiment);

                expect(spectator.inject(DotMessageDisplayService, true).push).toHaveBeenCalledWith(
                    expect.objectContaining({
                        severity: DotMessageSeverity.SUCCESS,
                        message: `Experiment ${experiment.name} ${expectedVerb}`
                    })
                );
            });

            it('should say the experiment started when the server reports it running', () => {
                emitSucceeded(dotExperimentsConfigureApiEvents.startSucceeded, {
                    ...experiment,
                    status: DotExperimentStatus.RUNNING
                });

                expect(pushedMessage()).toBe(`Experiment ${experiment.name} started`);
            });

            it('should say the experiment was scheduled when the server reports it scheduled', () => {
                // A start dated in the future schedules the experiment instead of running it, and
                // the server's answer is what says which of the two happened.
                emitSucceeded(dotExperimentsConfigureApiEvents.startSucceeded, {
                    ...experiment,
                    status: DotExperimentStatus.SCHEDULED
                });

                expect(pushedMessage()).toBe(`Experiment ${experiment.name} scheduled`);
            });

            it('should stay silent on an autosave, which is deliberately unannounced', () => {
                // `saveSucceeded` carries the body it wrote beside the experiment (#37003), so it
                // is dispatched here rather than through the shared helper.
                spectator.inject(Dispatcher).dispatch(
                    dotExperimentsConfigureApiEvents.saveSucceeded({
                        experiment,
                        sent: { name: experiment.name }
                    })
                );
                spectator.detectChanges();

                expect(
                    spectator.inject(DotMessageDisplayService, true).push
                ).not.toHaveBeenCalled();
            });

            it('should stay silent on a failed transition, which the error manager already reported', () => {
                spectator
                    .inject(Dispatcher)
                    .dispatch(dotExperimentsConfigureApiEvents.startFailed(new Error('boom')));
                spectator.detectChanges();

                expect(
                    spectator.inject(DotMessageDisplayService, true).push
                ).not.toHaveBeenCalled();
            });
        });

        describe('scroll to the first failing field', () => {
            it('should not scroll while nothing has failed validation', () => {
                flush();

                expect(scrollIntoView).not.toHaveBeenCalled();
            });

            it('should bring the first failing field into view once Start reveals the errors', () => {
                flush();

                storeMock.validationErrors.set(['name', 'goalType']);
                flush();

                expect(scrollIntoView).toHaveBeenCalledWith({
                    behavior: 'smooth',
                    block: 'center'
                });
            });

            it('should scroll to the first marker on the screen, not to any later one', () => {
                flush();

                storeMock.validationErrors.set(['goalType']);
                flush();

                expect(scrollIntoView.mock.instances[0]).toBe(
                    spectator.query(byTestId('details-error-marker'))
                );
            });

            it('should scroll again when the footer re-runs it on a second press', () => {
                // The errors do not change on a re-press, so the effect alone would never fire
                // twice.
                flush();
                storeMock.validationErrors.set(['name']);
                flush();
                scrollIntoView.mockClear();

                spectator.component.scrollToFirstValidationError();

                expect(scrollIntoView).toHaveBeenCalledTimes(1);
            });
        });
    });

    describe('on the creation screen', () => {
        const createComponent = createComponentOn({ configProps: CONFIGURED_DURATIONS });

        beforeEach(() => {
            spectator = createComponent();
            dispatch = jest.spyOn(spectator.inject(Dispatcher), 'dispatch');
            spectator.detectChanges();
        });

        /** What a POST answering does: the API event fires and the store gets the created draft. */
        const createExperiment = (experiment: DotExperiment = EXPERIMENT) => {
            spectator
                .inject(Dispatcher)
                .dispatch(dotExperimentsConfigureApiEvents.createSucceeded(experiment));
            loadExperiment(experiment);
        };

        it('should not read the created experiment back into the form', () => {
            // The form is what created the draft, so re-reading it would drop a goal or a schedule
            // entered before the name that created it — and those are still on their way out.
            editForm({ trafficAllocation: 40, name: 'Summer landing test' });

            createExperiment({ ...EXPERIMENT, trafficAllocation: 100 });

            expect(modelOf()().trafficAllocation).toBe(40);
        });

        /**
         * One route serves `/experiments/new` and `/experiments/:experimentId/configuration` and the
         * component is reused across them, so the store follows the URL — and the form follows the
         * store, whichever experiment that turns out to be.
         */
        it('should fill the form when a different experiment arrives', () => {
            createExperiment({ ...EXPERIMENT, trafficAllocation: 40 });

            loadExperiment({ ...EXPERIMENT, id: 'another-experiment', trafficAllocation: 100 });

            expect(modelOf()().trafficAllocation).toBe(100);
        });

        it('should empty the form when the store goes back to a creation form', () => {
            createExperiment();
            editForm({ name: 'Summer landing test' });

            storeMock.experiment.set(null);
            spectator.detectChanges();

            expect(modelOf()().name).toBe('');
        });

        it('should not report an allocation while there is no experiment to patch', () => {
            // The creation POST does not carry it, and there is nothing to PATCH yet.
            editForm({ trafficAllocation: 40 });

            expect(reportedPatches()).toEqual([]);
        });

        it('should still report the name that creates the draft', () => {
            editForm({ name: 'Summer landing test' });

            expect(reportedPatches()).toContainEqual({ name: 'Summer landing test' });
        });
    });

    describe('without the durations resolved', () => {
        const createComponent = createComponentOn({ experimentId: EXPERIMENT.id });

        beforeEach(() => {
            spectator = createComponent();
            dispatch = jest.spyOn(spectator.inject(Dispatcher), 'dispatch');
            loadExperiment();
        });

        it('should fall back to a seven day minimum', () => {
            editForm({ scheduling: { startDate: null, endDate: daysFromNow(5) } });

            expect(reportedPatches()).toEqual([]);
        });

        it('should fall back to a ninety day maximum', () => {
            editForm({ scheduling: { startDate: null, endDate: daysFromNow(40) } });

            expect(reportedPatches().length).toBe(1);
        });
    });

    describe('with a duration the backend does not have', () => {
        const createComponent = createComponentOn({
            experimentId: EXPERIMENT.id,
            configProps: {
                [ExperimentsConfigProperties.EXPERIMENTS_MIN_DURATION]: PROP_NOT_FOUND,
                [ExperimentsConfigProperties.EXPERIMENTS_MAX_DURATION]: PROP_NOT_FOUND
            }
        });

        beforeEach(() => {
            spectator = createComponent();
            dispatch = jest.spyOn(spectator.inject(Dispatcher), 'dispatch');
            loadExperiment();
        });

        it('should treat an unset property as no property at all', () => {
            editForm({ scheduling: { startDate: null, endDate: daysFromNow(5) } });

            expect(reportedPatches()).toEqual([]);
        });
    });

    describe('a card bound to a real slice of the form', () => {
        /** Everything stubbed but the Goal card, which is handed `formTree.goal`. */
        const createComponent = createComponentFactory({
            component: DotExperimentsConfigureComponent,
            componentProviders: [
                { provide: DotExperimentsConfigureStore, useFactory: () => storeMock },
                ConfirmationService
            ],
            providers: [
                provideRouter([{ path: 'experiments', children: [] }]),
                provideLocationMocks(),
                { provide: DotMessageService, useValue: messageServiceMock },
                mockProvider(DotMessageDisplayService),
                {
                    provide: ActivatedRoute,
                    useValue: {
                        snapshot: {
                            paramMap: convertToParamMap({ experimentId: EXPERIMENT.id }),
                            data: { config: CONFIGURED_DURATIONS }
                        }
                    }
                }
            ],
            overrideComponents: [
                [
                    DotExperimentsConfigureComponent,
                    {
                        remove: {
                            imports: [
                                DotExperimentsConfigureHeaderComponent,
                                DotExperimentsConfigureDetailsComponent,
                                DotExperimentsConfigurePageComponent,
                                DotExperimentsConfigureVariantsComponent,
                                DotExperimentsConfigureSchedulingComponent,
                                DotExperimentsConfigureFooterComponent
                            ]
                        },
                        add: {
                            imports: [
                                HeaderStubComponent,
                                DetailsStubComponent,
                                PageStubComponent,
                                VariantsStubComponent,
                                SchedulingStubComponent,
                                FooterStubComponent
                            ]
                        }
                    }
                ]
            ],
            detectChanges: false
        });

        beforeEach(() => {
            spectator = createComponent();
            dispatch = jest.spyOn(spectator.inject(Dispatcher), 'dispatch');
            loadExperiment();
        });

        it('should write what is typed in the card into that slice of the model', () => {
            spectator.typeInElement(
                'Newsletter signups',
                spectator.query(byTestId('goal-name-input')) as HTMLInputElement
            );
            spectator.detectChanges();

            expect(modelOf()().goal.name).toBe('Newsletter signups');
        });

        it('should report the goal the card completed as one edit of the whole form', () => {
            // The card is a `<label>` around a real radio, so a pick is a click on that radio.
            const radio = spectator
                .query(byTestId(`goal-type-${GOAL_TYPES.BOUNCE_RATE}`))
                ?.querySelector('input') as HTMLInputElement;
            radio.click();
            spectator.detectChanges();

            expect(reportedPatches()).toContainEqual({
                goals: {
                    primary: {
                        name: 'experiments.goal.conditions.minimize.bounce.rate',
                        type: GOAL_TYPES.BOUNCE_RATE,
                        conditions: []
                    }
                }
            });
        });
    });

    describe('a locked experiment', () => {
        const createComponent = createComponentOn({ experimentId: EXPERIMENT.id });

        beforeEach(() => {
            spectator = createComponent();
            dispatch = jest.spyOn(spectator.inject(Dispatcher), 'dispatch');
            storeMock.$isLocked.set(true);
            // A locked status is also what the store explains a frozen variant row with.
            storeMock.$disabledTooltipKey.set(EXP_CONFIG_ERROR_LABEL_CANT_EDIT);
            loadExperiment(withVariants(TWO_VARIANTS));
        });

        it('should disable every field of the form', () => {
            // AC34: one rule per leaf and per slice, so no card can forget it.
            const formTree = Reflect.get(
                spectator.component,
                'formTree'
            ) as FieldTree<ConfigureFormModel>;

            expect(formTree.name().disabled()).toBe(true);
            expect(formTree.description().disabled()).toBe(true);
            expect(formTree.trafficAllocation().disabled()).toBe(true);
            expect(formTree.goal.name().disabled()).toBe(true);
            expect(formTree.scheduling.startDate().disabled()).toBe(true);
            expect(formTree.scheduling.endDate().disabled()).toBe(true);
            expect(formTree.variantWeights[0].weight().disabled()).toBe(true);
        });
    });
});
