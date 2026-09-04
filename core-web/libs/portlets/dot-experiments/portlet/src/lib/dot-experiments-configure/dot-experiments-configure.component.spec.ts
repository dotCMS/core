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
    DotExperimentStatus,
    CONFIGURE_SECTION_VARIANTS,
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

import {
    LOCKED_BANNER_KEY_READ_ONLY,
    LOCKED_BANNER_KEY_RUNNING,
    MIN_PROGRESS_BAR_VISIBLE_MS
} from '../shared/constants';
import {
    ConfigureFormModel,
    ConfigureValidationRule,
    VariantWeightFormRow
} from '../shared/models';
import { dotExperimentsConfigureApiEvents } from '../store/dot-experiments-configure-api.events';
import { dotExperimentsConfigurePageEvents } from '../store/dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from '../store/dot-experiments-configure.store';
import { EMPTY_GOAL_SLICE, emptyConfigureForm } from '../util/dot-experiments-configure-form.util';

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
    // Projects: the Page fields live inside this card now, so a stub that swallowed its content
    // would report the Page section as missing from the screen.
    template: '<span data-error data-testid="details-error-marker"></span><ng-content />'
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
    readonly gated = input<boolean>();
    readonly field = input<unknown>();
}

@Component({ selector: 'dot-experiments-configure-page', template: '' })
class PageStubComponent {
    readonly field = input<FieldTree<number>>();
}

@Component({ selector: 'dot-experiments-configure-variants', template: '' })
class VariantsStubComponent {
    readonly gated = input<boolean>();
    readonly field = input<FieldTree<VariantWeightFormRow[]>>();
}

@Component({ selector: 'dot-experiments-configure-scheduling', template: '' })
class SchedulingStubComponent {
    readonly gated = input<boolean>();
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
    $validationErrors: signal<ConfigureValidationRule[]>([]),
    $lockedBannerKey: signal<string | null>(null),
    $isLocked: signal(false),
    $isAutosaving: signal(false),
    $isSaving: signal(false),
    experiment: signal<DotExperiment | null>(null),
    // What the save gate reads: true until a draft exists, so it follows `experiment`.
    isNew: signal(true),
    // The weights slice is seeded from these, so they move with `experiment` (see `loadExperiment`).
    $variants: signal<Variant[]>([]),
    $disabledTooltipKey: signal<string | null>(null),
    /** A confirmed page change in flight; gates the Variants card on its own. */
    pageChanging: signal(false),
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
        configProps,
        section
    }: {
        experimentId?: string;
        configProps?: Record<string, string>;
        /** The `section` query param, set by the return leg of the variant round-trip. */
        section?: string;
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
                            queryParamMap: convertToParamMap(section ? { section } : {}),
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
        storeMock.isNew.set(false);
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

    /** Every form value the screen mirrored into the store, in order. */
    const mirroredValues = (): ConfigureFormModel[] =>
        dispatchedEvents()
            .filter(({ type }) => type === dotExperimentsConfigurePageEvents.formChanged.type)
            .map(({ payload }) => (payload as { value: ConfigureFormModel }).value);

    /** The last value the screen mirrored, which is what a save would send. */
    const lastMirrored = (): ConfigureFormModel | undefined => mirroredValues().at(-1);

    /**
     * Whether the scheduling slice was last reported as sendable.
     *
     * The value is always mirrored — the pickers show what was chosen either way — so what the
     * bounds decide is not whether it travels but whether the save includes it.
     */
    const schedulingReportedValid = (): boolean | undefined => {
        const last = dispatchedEvents()
            .filter(({ type }) => type === dotExperimentsConfigurePageEvents.formChanged.type)
            .at(-1);

        return (last?.payload as { validity: { scheduling: boolean } } | undefined)?.validity
            .scheduling;
    };

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
            // The route names an experiment, so the store is past the save gate before the first
            // render — the tests here are about the screen of an experiment that exists.
            storeMock.isNew.set(false);
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

            /**
             * The bar outlives the request on purpose. A local PATCH can answer in a few
             * milliseconds, and a bar that appears and vanishes inside a frame reads as a glitch
             * rather than as feedback, so it is held for a legible beat.
             */
            describe('the minimum it stays up', () => {
                const bar = () => spectator.query(byTestId('experiments-configure-progress-bar'));

                const settleSaveAfter = (ms: number) => {
                    storeMock.$isSaving.set(true);
                    spectator.detectChanges();
                    jest.advanceTimersByTime(ms);
                    storeMock.$isSaving.set(false);
                    spectator.detectChanges();
                };

                beforeEach(() => jest.useFakeTimers());
                afterEach(() => jest.useRealTimers());

                it('should stay up for the rest of the window when the save beats it', () => {
                    settleSaveAfter(20);

                    expect(bar()).not.toBeNull();

                    jest.advanceTimersByTime(MIN_PROGRESS_BAR_VISIBLE_MS - 20 - 1);
                    spectator.detectChanges();
                    expect(bar()).not.toBeNull();

                    jest.advanceTimersByTime(1);
                    spectator.detectChanges();
                    expect(bar()).toBeNull();
                });

                it('should leave at once when the save already outlasted the window', () => {
                    settleSaveAfter(MIN_PROGRESS_BAR_VISIBLE_MS + 100);

                    expect(bar()).toBeNull();
                });

                it('should stay up for as long as a slow save takes', () => {
                    storeMock.$isSaving.set(true);
                    spectator.detectChanges();

                    jest.advanceTimersByTime(MIN_PROGRESS_BAR_VISIBLE_MS * 5);
                    spectator.detectChanges();

                    expect(bar()).not.toBeNull();
                });

                /** Back-to-back saves are one bar, not a blink between them. */
                it('should not blink when another save starts inside the window', () => {
                    settleSaveAfter(20);

                    storeMock.$isSaving.set(true);
                    spectator.detectChanges();
                    jest.advanceTimersByTime(MIN_PROGRESS_BAR_VISIBLE_MS);
                    spectator.detectChanges();

                    // The first hide was cancelled, so the still-running save keeps it up.
                    expect(bar()).not.toBeNull();
                });
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
                    variantWeights: [{ id: CONTROL_VARIANT.id, weight: CONTROL_VARIANT.weight }],
                    trafficProportionType: EXPERIMENT.trafficProportion.type
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
                    variantWeights: [],
                    trafficProportionType: TrafficProportionTypes.SPLIT_EVENLY
                });
            });

            it('should mirror the experiment it was filled from, unchanged', () => {
                loadExperiment();

                // Hydration is not an edit, but it is still what is on screen — and the store
                // snapshots it, so mirroring it is what keeps a freshly loaded screen clean.
                expect(lastMirrored()?.name).toBe(EXPERIMENT.name);
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
         * The screen's whole contribution to persistence: it mirrors the form and nothing else.
         *
         * What is *sendable* is no longer decided here — a blank name, a half-typed goal and a
         * weight split that does not total 100 are all mirrored, and `toConfigurePatch` is what
         * leaves them out of the body. Those rules are tested against that function directly,
         * which is where they now live.
         */
        describe('mirroring the form', () => {
            beforeEach(() => loadExperiment(withVariants(TWO_VARIANTS)));

            it('should mirror the whole form, not the field that changed', () => {
                editForm({ name: 'Winter landing test' });

                expect(lastMirrored()).toEqual(
                    expect.objectContaining({
                        name: 'Winter landing test',
                        description: expect.any(String),
                        trafficAllocation: expect.any(Number)
                    })
                );
            });

            it('should mirror several fields edited together as one value', () => {
                editForm({ name: 'Winter landing test', trafficAllocation: 40 });

                expect(lastMirrored()).toEqual(
                    expect.objectContaining({
                        name: 'Winter landing test',
                        trafficAllocation: 40
                    })
                );
            });

            /**
             * The mirror has to carry what the backend would refuse, or the rows on screen, the
             * running total and the Start rules would each be reading something different from
             * what the user is looking at.
             */
            it('should mirror weights that do not add up', () => {
                editForm({
                    variantWeights: [
                        { id: CONTROL_VARIANT.id, weight: 70 },
                        { id: SECOND_VARIANT.id, weight: 20 }
                    ]
                });

                expect(
                    lastMirrored()?.variantWeights.map(({ id, weight }) => ({ id, weight }))
                ).toEqual([
                    { id: CONTROL_VARIANT.id, weight: 70 },
                    { id: SECOND_VARIANT.id, weight: 20 }
                ]);
            });

            it('should mirror a blank name rather than swallow it', () => {
                editForm({ name: '   ' });

                expect(lastMirrored()?.name).toBe('   ');
            });

            it('should report the validity of the bounded slices alongside the value', () => {
                editForm({ name: 'Winter landing test' });

                const last = dispatchedEvents()
                    .filter(
                        ({ type }) => type === dotExperimentsConfigurePageEvents.formChanged.type
                    )
                    .at(-1);

                expect(last?.payload).toEqual(
                    expect.objectContaining({
                        validity: {
                            trafficAllocation: expect.any(Boolean),
                            scheduling: expect.any(Boolean)
                        }
                    })
                );
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

                expect(schedulingReportedValid()).toBe(false);
            });

            it('should accept an end date inside the configured window', () => {
                editForm({
                    scheduling: {
                        startDate: null,
                        endDate: daysFromNow(CONFIGURED_MIN_DURATION_DAYS + 1)
                    }
                });

                expect(schedulingReportedValid()).toBe(true);
            });

            it('should reject an end date beyond the configured maximum duration', () => {
                editForm({
                    scheduling: {
                        startDate: null,
                        endDate: daysFromNow(CONFIGURED_MAX_DURATION_DAYS + 10)
                    }
                });

                expect(schedulingReportedValid()).toBe(false);
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
                spectator.inject(Dispatcher).dispatch(
                    dotExperimentsConfigureApiEvents.saveSucceeded({
                        experiment,
                        form: emptyConfigureForm()
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

            /** A Start press the store rejected: the errors are revealed and nothing was sent. */
            const pressStartWith = (errors: ConfigureValidationRule[]) => {
                storeMock.$validationErrors.set(errors);
                spectator
                    .inject(Dispatcher)
                    .dispatch(dotExperimentsConfigurePageEvents.startRequested());
                flush();
            };

            it('should bring the first failing field into view once Start reveals the errors', () => {
                flush();

                pressStartWith(['name', 'goalType']);

                expect(scrollIntoView).toHaveBeenCalledWith({
                    behavior: 'smooth',
                    block: 'center'
                });
            });

            it('should scroll to the first marker on the screen, not to any later one', () => {
                flush();

                pressStartWith(['goalType']);

                expect(scrollIntoView.mock.instances[0]).toBe(
                    spectator.query(byTestId('details-error-marker'))
                );
            });

            it('should not scroll when a Start press got through', () => {
                flush();

                pressStartWith([]);

                expect(scrollIntoView).not.toHaveBeenCalled();
            });

            it('should scroll again when the footer re-runs it on a second press', () => {
                flush();
                pressStartWith(['name']);
                scrollIntoView.mockClear();

                spectator.component.scrollToFirstValidationError();

                expect(scrollIntoView).toHaveBeenCalledTimes(1);
            });
        });
    });

    // #37005. Variants are copies of the page, and the server takes `pageId` only while the
    // variants are the control alone — so one created before a page change lands is created under
    // the old page, and from then on the change can never be written.
    describe('while a page change is in flight', () => {
        const createComponent = createComponentOn({
            experimentId: EXPERIMENT.id,
            configProps: CONFIGURED_DURATIONS
        });

        beforeEach(() => {
            spectator = createComponent();
            storeMock.isNew.set(false);
        });

        /**
         * Asserted on the screen's own decision rather than on the rendered card: this spec
         * replaces the card components, so their `gated` input never reaches the DOM. Which
         * expression feeds which card is fixed by the template; what varies, and what this covers,
         * is that the two differ.
         */
        it('should gate the Variants card', () => {
            spectator.detectChanges();

            expect(spectator.component.$isVariantsGated()).toBe(false);

            storeMock.pageChanging.set(true);
            spectator.detectChanges();

            expect(spectator.component.$isVariantsGated()).toBe(true);
        });

        // The other cards do not depend on the page, so they stay live.
        it('should leave the rest of the form live', () => {
            storeMock.pageChanging.set(true);
            spectator.detectChanges();

            expect(spectator.component.$isGated()).toBe(false);
        });
    });

    // #37005. Configure is four stacked cards tall, and the variant round-trip starts and ends at
    // the Variants card — so coming back to the top of the form loses the reader's place.
    describe('returning from a variant', () => {
        const createComponent = createComponentOn({
            experimentId: EXPERIMENT.id,
            configProps: CONFIGURED_DURATIONS,
            section: CONFIGURE_SECTION_VARIANTS
        });

        beforeEach(() => {
            spectator = createComponent();
            storeMock.isNew.set(false);
        });

        it('should bring the Variants card into view once the experiment is loaded', () => {
            spectator.detectChanges();

            const variants = spectator.query(byTestId('configure-section-variants')) as HTMLElement;
            const scrollToVariants = jest.spyOn(variants, 'scrollIntoView');

            spectator
                .inject(Dispatcher)
                .dispatch(dotExperimentsConfigureApiEvents.loadSucceeded(EXPERIMENT));
            flush();

            expect(scrollToVariants).toHaveBeenCalled();
        });
    });

    // Entering from the list, or creating one, still lands at the top of the form.
    describe('entering Configure any other way', () => {
        const createComponent = createComponentOn({
            experimentId: EXPERIMENT.id,
            configProps: CONFIGURED_DURATIONS
        });

        beforeEach(() => {
            spectator = createComponent();
            storeMock.isNew.set(false);
        });

        it('should not scroll to any section', () => {
            spectator.detectChanges();
            spectator
                .inject(Dispatcher)
                .dispatch(dotExperimentsConfigureApiEvents.loadSucceeded(EXPERIMENT));
            flush();

            expect(scrollIntoView).not.toHaveBeenCalled();
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

        /**
         * The save gate: Goal, Variants and Scheduling hold nothing the creation POST can carry,
         * so they stay masked until the draft exists. The mask itself is each card's own business
         * — asserted in their specs; what the shell owns is who is told to close.
         */
        describe('the save gate', () => {
            const gatedCards = () => [
                spectator.query(GoalStubComponent),
                spectator.query(VariantsStubComponent),
                spectator.query(SchedulingStubComponent)
            ];

            it('should close over the three cards the POST cannot write', () => {
                expect(gatedCards().map((card) => card?.gated())).toEqual([true, true, true]);
            });

            it('should lift as soon as the draft exists', () => {
                createExperiment();
                spectator.detectChanges();

                expect(gatedCards().map((card) => card?.gated())).toEqual([false, false, false]);
            });

            it('should keep the footer bar out of the screen while the gate is closed', () => {
                // The bar's whole job is acting on an experiment that exists. Details carries the
                // one action there is before that.
                expect(spectator.query('dot-experiments-configure-footer')).toBeNull();

                createExperiment();
                spectator.detectChanges();

                expect(spectator.query('dot-experiments-configure-footer')).not.toBeNull();
            });
        });

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

        it('should mirror the form before the experiment exists', () => {
            // Nothing can be PATCHed yet, but the store still needs the value: it is what the
            // creation POST is built from, and what the Start rules are checked against.
            editForm({ name: 'Summer landing test', trafficAllocation: 40 });

            expect(lastMirrored()).toEqual(
                expect.objectContaining({ name: 'Summer landing test', trafficAllocation: 40 })
            );
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

            expect(schedulingReportedValid()).toBe(false);
        });

        it('should fall back to a ninety day maximum', () => {
            editForm({ scheduling: { startDate: null, endDate: daysFromNow(40) } });

            expect(schedulingReportedValid()).toBe(true);
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

            expect(schedulingReportedValid()).toBe(false);
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
                            // Present and empty, as `ActivatedRoute` always is: the screen reads
                            // the `section` param from here on init.
                            queryParamMap: convertToParamMap({}),
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

            expect(lastMirrored()?.goal).toEqual(
                expect.objectContaining({
                    type: GOAL_TYPES.BOUNCE_RATE,
                    name: 'experiments.goal.conditions.minimize.bounce.rate'
                })
            );
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
