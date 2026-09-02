import { Dispatcher, Events, provideDispatcher } from '@ngrx/signals/events';
import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { concat, of, Subject, throwError } from 'rxjs';

import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ActivatedRoute, convertToParamMap, ParamMap, Params, Router } from '@angular/router';

import {
    DotContentSearchService,
    DotExperimentsService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotPageBrowserPage,
    DotPageBrowserState,
    DotPagesBrowserService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    DotCMSContentlet,
    DotCurrentUser,
    DotExperiment,
    DotExperimentStatus,
    DotSite,
    EXP_CONFIG_ERROR_LABEL_CANT_EDIT,
    EXP_CONFIG_ERROR_LABEL_PAGE_BLOCKED,
    GOAL_OPERATORS,
    GOAL_PARAMETERS,
    GOAL_TYPES,
    Goal,
    Goals,
    TrafficProportionTypes,
    Variant
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { dotExperimentsConfigureApiEvents } from './dot-experiments-configure-api.events';
import { dotExperimentsConfigurePageEvents } from './dot-experiments-configure-page.events';
import { DotExperimentsConfigureStore } from './dot-experiments-configure.store';

import {
    DEFAULT_TRAFFIC_ALLOCATION,
    LOCKED_BANNER_KEY_READ_ONLY,
    LOCKED_BANNER_KEY_RUNNING,
    PAGE_PREFILL_ERROR_KEY,
    PAGE_PREFILL_LOOKUP_ERROR_KEY
} from '../shared/constants';
import {
    ConfigureFormModel,
    ConfigureFormValidity,
    ConfigureValidationRule,
    DotExperimentConfigurePage,
    ExperimentListAction
} from '../shared/models';
import {
    emptyConfigureForm,
    toConfigureFormModel,
    toGoalSlice
} from '../util/dot-experiments-configure-form.util';

const pageEvents = dotExperimentsConfigurePageEvents;
const apiEvents = dotExperimentsConfigureApiEvents;

const SITE_HOSTNAME = 'demo.dotcms.com';
const CURRENT_USER_ID = 'user-me';
const OTHER_USER_ID = 'user-someone-else';
const EXPERIMENT_ID = 'exp-1';

/** The page every fixture runs on, as the Page card renders it. */
const PAGE: DotExperimentConfigurePage = {
    pageId: '2e2e5f6a-1e17-4b21-9c1a-7d3f5b90ac41',
    title: 'Home',
    path: '/home',
    languageId: 1
};

const buildVariant = (id: string, weight: number): Variant => ({ id, name: id, weight });

const buildGoals = (goal: Partial<Goal> = {}): Goals => ({
    primary: {
        name: 'Bounce rate',
        type: GOAL_TYPES.BOUNCE_RATE,
        conditions: [],
        ...goal
    }
});

/** A complete draft: every one of the eight Start rules passes on it. */
const buildExperiment = (experiment: Partial<DotExperiment> = {}): DotExperiment => ({
    id: EXPERIMENT_ID,
    pageId: PAGE.pageId,
    name: 'Alpha campaign',
    description: 'Checkout funnel rework',
    status: DotExperimentStatus.DRAFT,
    readyToStart: false,
    archived: false,
    trafficProportion: {
        type: TrafficProportionTypes.SPLIT_EVENLY,
        variants: [buildVariant('DEFAULT', 50), buildVariant('variant-b', 50)]
    },
    trafficAllocation: 100,
    scheduling: null,
    creationDate: new Date('2026-01-01T00:00:00.000Z'),
    modDate: 0,
    goals: buildGoals(),
    ...experiment
});

const VALID_DRAFT = buildExperiment();

/**
 * What the creation POST actually answers with: the three fields it carried, plus the control
 * variant the backend makes. No goal, no schedule — `VALID_DRAFT` is a fully configured experiment
 * and standing in for a creation response with it describes a server that invented a goal.
 */
const CREATED_DRAFT: DotExperiment = buildExperiment({
    goals: null,
    scheduling: null,
    trafficProportion: {
        type: TrafficProportionTypes.SPLIT_EVENLY,
        variants: [buildVariant('DEFAULT', 100)]
    }
});

const buildPageContentlet = (contentlet: Partial<DotCMSContentlet> = {}): DotCMSContentlet =>
    ({
        identifier: PAGE.pageId,
        title: PAGE.title,
        url: PAGE.path,
        // Real contentlets always carry a language, and `toConfigurePage` copies it through so the
        // variant deep link can send it as `language_id` (#37005).
        languageId: PAGE.languageId,
        ...contentlet
    }) as DotCMSContentlet;

const PAGE_LOOKUP_RESULT = { jsonObjectView: { contentlets: [buildPageContentlet()] } };

const buildBrowserPage = (page: Partial<DotPageBrowserPage> = {}): DotPageBrowserPage => ({
    identifier: PAGE.pageId,
    inode: 'page-1-inode',
    title: PAGE.title,
    url: PAGE.path,
    path: PAGE.path,
    hostname: SITE_HOSTNAME,
    hostId: 'site-1',
    templateId: 'template-1',
    modDate: '',
    languageId: 1,
    state: DotPageBrowserState.DRAFT,
    ...page
});

describe('DotExperimentsConfigureStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotExperimentsConfigureStore>>;
    let store: InstanceType<typeof DotExperimentsConfigureStore>;
    let dispatcher: Dispatcher;
    let events: Events;
    let httpErrorManager: jest.Mocked<DotHttpErrorManagerService>;

    const getById = jest.fn();
    const add = jest.fn();
    const patchExperiment = jest.fn();
    const addVariant = jest.fn();
    const editVariant = jest.fn();
    const removeVariant = jest.fn();
    const start = jest.fn();
    const stop = jest.fn();
    const cancelSchedule = jest.fn();
    const contentSearchGet = jest.fn();
    const searchPages = jest.fn();
    const getPageLockState = jest.fn();
    const messageGet = jest.fn();
    const navigate = jest.fn();

    /** `GlobalStore` is root-provided; only the two signals this store reads are stubbed. */
    const globalStoreMock = {
        loggedUser: signal<DotCurrentUser | null>(null),
        siteDetails: signal<DotSite | null>(null)
    };

    let routeParams: Params;
    let routeQueryParams: Params;

    /**
     * The store follows `paramMap` for as long as it lives and reads the query params off the
     * snapshot when it lands on `/new`, so the stub offers both — and the subject lets a test push a
     * later URL at a store that is already up.
     */
    let laterUrls$: Subject<ParamMap>;

    const activatedRouteStub = {
        get paramMap() {
            return concat(of(convertToParamMap(routeParams)), laterUrls$);
        },
        snapshot: {
            get paramMap() {
                return convertToParamMap(routeParams);
            },
            get queryParamMap() {
                return convertToParamMap(routeQueryParams);
            }
        }
    };

    /** Everything but the experiments API, which one factory mocks and the other keeps real. */
    const surroundingProviders = () => [
        // `Dispatcher`/`Events` are `providedIn: 'platform'`, so they outlive TestBed resets and
        // a store from a previous test would keep reacting to this test's events.
        provideDispatcher(),
        mockProvider(DotPagesBrowserService, { searchPages, getPageLockState }),
        mockProvider(DotContentSearchService, { get: contentSearchGet }),
        mockProvider(DotHttpErrorManagerService),
        mockProvider(DotMessageService, { get: messageGet }),
        { provide: GlobalStore, useValue: globalStoreMock },
        { provide: Router, useValue: { navigate } },
        { provide: ActivatedRoute, useValue: activatedRouteStub }
    ];

    const createService = createServiceFactory({
        service: DotExperimentsConfigureStore,
        providers: [
            mockProvider(DotExperimentsService, {
                getById,
                add,
                patch: patchExperiment,
                addVariant,
                editVariant,
                removeVariant,
                start,
                stop,
                cancelSchedule
            }),
            ...surroundingProviders()
        ]
    });

    /**
     * Creates the store. Called from the tests rather than from a global `beforeEach` because
     * the route is read and the load flow runs in `onInit`, so every arrangement has to be in
     * place first.
     */
    const initStore = () => {
        spectator = createService();
        store = spectator.service;
        dispatcher = spectator.inject(Dispatcher);
        events = spectator.inject(Events);
        httpErrorManager = spectator.inject(
            DotHttpErrorManagerService
        ) as jest.Mocked<DotHttpErrorManagerService>;
        spectator.flushEffects();
    };

    /** `/experiments/new`, optionally with the prefill query params. */
    const initNew = (queryParams: Params = {}) => {
        routeParams = {};
        routeQueryParams = queryParams;
        initStore();
    };

    /** `/experiments/:experimentId/configuration`, with the load left to the test. */
    const initExistingRoute = (experimentId = EXPERIMENT_ID) => {
        routeParams = { experimentId };
        initStore();
    };

    /** The form as the shell would hold it, rebuilt for every test by `resetFormModel`. */
    let formModel: ConfigureFormModel;

    /**
     * What the shell does the moment it is hydrated: mirror the whole form into the store.
     *
     * Every loaded screen has been through this, so a test that skips it is testing a state the
     * app never reaches — the Start rules would read a form that does not exist and report every
     * field as missing, and the store would have no clean baseline to call the screen saved
     * against.
     */
    const mirrorForm = (experiment: DotExperiment) => {
        formModel = toConfigureFormModel({
            experiment,
            draftName: experiment.name,
            draftDescription: experiment.description ?? ''
        });
        dispatcher.dispatch(
            pageEvents.formChanged({
                value: formModel,
                validity: { trafficAllocation: true, scheduling: true }
            })
        );
    };

    const initExisting = (experiment: DotExperiment = VALID_DRAFT) => {
        getById.mockReturnValue(of(experiment));
        initExistingRoute(experiment.id);
        mirrorForm(experiment);
    };

    /**
     * The Name + Page combination, then the press that actually creates the draft.
     *
     * The description is typed too, because the POST carries it and `add` answers with
     * `VALID_DRAFT`: a fixture whose description differs from the one that was sent describes a
     * server that rewrote it, which would leave the screen legitimately dirty afterwards.
     */
    const createDraft = (name = VALID_DRAFT.name, answersWith = CREATED_DRAFT) => {
        // Whatever follows the POST answers with the same draft it made: a fully configured
        // experiment there would describe a server that added a goal nobody asked for.
        patchExperiment.mockReturnValue(of(answersWith));
        edit({ name, description: CREATED_DRAFT.description ?? '' });
        dispatcher.dispatch(pageEvents.pageSelected(PAGE));
        dispatcher.dispatch(pageEvents.saveDraftRequested());
    };

    /**
     * An edit as the shell reports it: the whole form, every time.
     *
     * The running model is what makes the call sites read as edits rather than as full payloads —
     * the screen mirrors the entire value on every keystroke, and this mirrors that.
     */
    const edit = (patch: Partial<ConfigureFormModel>, validity?: ConfigureFormValidity) => {
        formModel = { ...formModel, ...patch };
        dispatcher.dispatch(
            pageEvents.formChanged({
                value: formModel,
                validity: validity ?? { trafficAllocation: true, scheduling: true }
            })
        );
    };

    /** Save Draft: the only thing that writes the form. */
    const saveDraft = () => dispatcher.dispatch(pageEvents.saveDraftRequested());

    /**
     * Makes a call hang until the test answers it, which is the only way to observe the state
     * the store is in *while* a request is in flight.
     */
    const pendingCall = (call: jest.Mock): Subject<DotExperiment> => {
        const settled = new Subject<DotExperiment>();
        call.mockReturnValue(settled);

        return settled;
    };

    const httpError = (status: number, error: unknown = {}) =>
        new HttpErrorResponse({ status, error });

    beforeEach(() => {
        jest.resetAllMocks();
        jest.useFakeTimers();

        routeParams = {};
        routeQueryParams = {};
        laterUrls$ = new Subject<ParamMap>();
        formModel = emptyConfigureForm();

        getById.mockReturnValue(of(VALID_DRAFT));
        add.mockReturnValue(of(CREATED_DRAFT));
        patchExperiment.mockReturnValue(of(VALID_DRAFT));
        addVariant.mockReturnValue(of(VALID_DRAFT));
        editVariant.mockReturnValue(of(VALID_DRAFT));
        removeVariant.mockReturnValue(of(VALID_DRAFT));
        start.mockReturnValue(of(VALID_DRAFT));
        stop.mockReturnValue(of(VALID_DRAFT));
        cancelSchedule.mockReturnValue(of(VALID_DRAFT));
        contentSearchGet.mockReturnValue(of(PAGE_LOOKUP_RESULT));
        searchPages.mockReturnValue(of([buildBrowserPage()]));
        getPageLockState.mockReturnValue(of({ locked: false }));
        messageGet.mockImplementation((key: string) => key);

        globalStoreMock.loggedUser.set({ userId: CURRENT_USER_ID } as DotCurrentUser);
        globalStoreMock.siteDetails.set({ hostname: SITE_HOSTNAME } as DotSite);
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    describe('loading an existing experiment', () => {
        it('should seed the drafts from what the server answered', () => {
            initExisting();

            expect(getById).toHaveBeenCalledWith(EXPERIMENT_ID);
            expect(store.experiment()).toEqual(VALID_DRAFT);
            expect(store.draftName()).toBe(VALID_DRAFT.name);
            expect(store.draftDescription()).toBe(VALID_DRAFT.description);
            expect(store.isNew()).toBe(false);
            expect(store.status()).toBe(ComponentStatus.LOADED);
        });

        it('should resolve the page of the loaded experiment', () => {
            initExisting();

            expect(store.selectedPage()).toEqual(PAGE);
        });

        it('should end in error when the experiment cannot be found', () => {
            getById.mockReturnValue(of(undefined));

            initExistingRoute();

            expect(store.status()).toBe(ComponentStatus.ERROR);
            expect(store.experiment()).toBeNull();
        });

        it('should report the failure and end in error when the load fails', () => {
            const error = httpError(500);
            getById.mockReturnValue(throwError(() => error));

            initExistingRoute();

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(store.status()).toBe(ComponentStatus.ERROR);
        });
    });

    /**
     * One route serves `/experiments/new` and `/experiments/:experimentId/configuration`, and the
     * component is reused across them, so the store follows the URL instead of reading it once.
     */
    describe('a URL arriving while the screen is up', () => {
        const OTHER_ID = 'another-experiment-id';

        it('should load the experiment it points at', () => {
            initExisting();
            getById.mockClear();
            getById.mockReturnValue(of({ ...VALID_DRAFT, id: OTHER_ID }));

            laterUrls$.next(convertToParamMap({ experimentId: OTHER_ID }));

            expect(getById).toHaveBeenCalledWith(OTHER_ID);
            expect(store.experiment()?.id).toBe(OTHER_ID);
        });

        it('should not carry a pending diff over to it', () => {
            initExisting();
            edit({ name: 'Typed but not yet sent' });
            expect(store.$hasUnsavedChanges()).toBe(true);

            laterUrls$.next(convertToParamMap({ experimentId: OTHER_ID }));

            expect(store.$hasUnsavedChanges()).toBe(false);
        });

        /**
         * The reveal-on-Start guarantee is per experiment (AC28): errors named by a Start on the
         * one being left behind must not greet the one arriving.
         */
        it('should not carry validation errors over to it', () => {
            initExisting({ ...VALID_DRAFT, name: '' });
            dispatcher.dispatch(pageEvents.startRequested());
            expect(store.$validationErrors().length).toBeGreaterThan(0);

            laterUrls$.next(convertToParamMap({ experimentId: OTHER_ID }));

            expect(store.$validationErrors()).toEqual([]);
        });

        it('should ignore the swap to the experiment it just created', () => {
            initNew();
            createDraft();
            getById.mockClear();

            // What the store's own `createSucceeded` navigation produces.
            laterUrls$.next(convertToParamMap({ experimentId: CREATED_DRAFT.id }));

            expect(getById).not.toHaveBeenCalled();
            expect(store.experiment()).toEqual(CREATED_DRAFT);
        });

        it('should go back to a creation form when the URL drops the id', () => {
            initExisting();

            laterUrls$.next(convertToParamMap({}));

            expect(store.isNew()).toBe(true);
            expect(store.experiment()).toBeNull();
            expect(store.status()).toBe(ComponentStatus.LOADED);
        });
    });

    describe('creation (AC2/AC3/AC4)', () => {
        it('should not create anything until Save Draft is pressed', () => {
            initNew();

            edit({ name: 'Alpha campaign' });
            dispatcher.dispatch(pageEvents.pageSelected(PAGE));

            // Both halves are there, but nothing has been pressed: the draft does not exist yet.
            expect(add).not.toHaveBeenCalled();

            saveDraft();

            expect(add).toHaveBeenCalledTimes(1);
            expect(add).toHaveBeenCalledWith({
                pageId: PAGE.pageId,
                name: 'Alpha campaign',
                description: '',
                trafficAllocation: DEFAULT_TRAFFIC_ALLOCATION
            });
        });

        /**
         * Regression: the POST writes name, description and page, but they were left in the diff,
         * and the follow-up PATCH drops them for matching what the server holds — so nothing ever
         * settled them. The screen reported itself dirty the instant it was saved, and the leave
         * prompt fired on the `/new` → `/:id/configuration` swap that creation performs.
         */
        it('should be clean once the created draft is on screen', () => {
            // The POST makes the control variant and the card seeds a weight row from it — a
            // change the user never made. Counted as unsaved work it makes leaving prompt for
            // nothing, which is what it did (#37003).
            initNew();

            createDraft();
            mirrorForm(CREATED_DRAFT);

            expect(add).toHaveBeenCalledTimes(1);
            expect(store.$hasUnsavedChanges()).toBe(false);
            expect(store.$canSave()).toBe(false);
        });

        it('should carry an allocation set before the press in the POST itself', () => {
            // `ExperimentForm` takes it, so there is nothing for a second call to deliver — and a
            // creation with nothing left over cannot leave the screen dirty behind it.
            const created = buildExperiment({ ...CREATED_DRAFT, trafficAllocation: 89 });
            add.mockReturnValue(of(created));
            initNew();

            edit({
                name: VALID_DRAFT.name,
                description: CREATED_DRAFT.description ?? '',
                trafficAllocation: 89
            });
            dispatcher.dispatch(pageEvents.pageSelected(PAGE));
            saveDraft();
            mirrorForm(created);

            expect(add).toHaveBeenCalledWith(expect.objectContaining({ trafficAllocation: 89 }));
            expect(patchExperiment).not.toHaveBeenCalled();
            expect(store.$hasUnsavedChanges()).toBe(false);
        });

        it('should never create a draft from a blank name', () => {
            initNew();

            edit({ name: '   ' });
            dispatcher.dispatch(pageEvents.pageSelected(PAGE));

            expect(add).not.toHaveBeenCalled();
        });

        it('should not report autosaving while the draft cannot be created yet', () => {
            initNew();

            // A name alone cannot go anywhere — no experiment to PATCH, no page for the POST —
            // so the footer must keep the plain hint instead of a "Saving…" that never settles.
            edit({ name: 'Alpha campaign' });
            saveDraft();

            expect(store.$canSave()).toBe(false);
        });

        it('should not fire a second POST while the first one is in flight', () => {
            const created = new Subject<DotExperiment>();
            add.mockReturnValue(created);
            initNew();

            createDraft();
            edit({ name: 'Alpha campaign renamed' });

            expect(add).toHaveBeenCalledTimes(1);

            created.next(VALID_DRAFT);

            expect(store.creating()).toBe(false);
        });

        it('should never create a second experiment once one exists', () => {
            initNew();

            createDraft();
            edit({ name: 'Alpha campaign renamed' });
            dispatcher.dispatch(pageEvents.pageSelected({ ...PAGE, pageId: 'page-2' }));

            expect(add).toHaveBeenCalledTimes(1);
        });

        it('should replace /new with the created experiment url', () => {
            const created = buildExperiment({ id: 'exp-created' });
            // Both calls answer with the same draft: the follow-up PATCH replacing it with some
            // other experiment is the fixture talking, not the store.
            add.mockReturnValue(of(created));
            patchExperiment.mockReturnValue(of(created));
            initNew();

            createDraft(VALID_DRAFT.name, created);

            expect(store.experiment()).toEqual(created);
            expect(store.isNew()).toBe(false);
            expect(navigate).toHaveBeenCalledWith(['..', created.id, 'configuration'], {
                relativeTo: activatedRouteStub,
                replaceUrl: true
            });
        });

        it('should keep the typed draft on the screen when the creation fails', () => {
            const error = httpError(500);
            add.mockReturnValue(throwError(() => error));
            initNew();

            edit({ name: 'Alpha campaign' });
            edit({ description: 'Checkout funnel rework' });
            dispatcher.dispatch(pageEvents.pageSelected(PAGE));
            saveDraft();

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(navigate).not.toHaveBeenCalled();
            expect(store.experiment()).toBeNull();
            expect(store.draftName()).toBe('Alpha campaign');
            expect(store.draftDescription()).toBe('Checkout funnel rework');
            expect(store.selectedPage()).toEqual(PAGE);
            expect(store.isNew()).toBe(true);
            expect(store.creating()).toBe(false);
        });

        it('should ignore a page selection once the experiment exists', () => {
            initExisting();

            dispatcher.dispatch(
                pageEvents.pageSelected({
                    pageId: 'page-2',
                    title: 'Pricing',
                    path: '/pricing',
                    languageId: 1
                })
            );

            expect(store.selectedPage()).toEqual(PAGE);
        });
    });

    /**
     * The screen mirrors the rule `ExperimentsAPIImpl.save()` enforces, so a page the server would
     * refuse never leaves. See `specs/37176-draft-experiment-page-change`.
     */
    describe('changing the page of a draft', () => {
        const OTHER_PAGE = { pageId: 'page-2', title: 'Pricing', path: '/pricing', languageId: 1 };

        /** A draft in the only shape that may change page: the control and nothing else. */
        const controlOnlyDraft = () =>
            buildExperiment({
                trafficProportion: {
                    type: TrafficProportionTypes.SPLIT_EVENLY,
                    variants: [buildVariant('DEFAULT', 100)]
                }
            });

        it('should carry the new page in the diff and send it on Save Draft', () => {
            initExisting(controlOnlyDraft());

            dispatcher.dispatch(pageEvents.pageSelected(OTHER_PAGE));

            expect(store.selectedPage()).toEqual(OTHER_PAGE);
            expect(store.$hasUnsavedChanges()).toBe(true);

            saveDraft();

            expect(patchExperiment).toHaveBeenCalledWith(
                EXPERIMENT_ID,
                expect.objectContaining({ pageId: OTHER_PAGE.pageId })
            );
        });

        it('should ignore a pick once the draft has a variant of its own', () => {
            // The default fixture already carries `variant-b` beside the control.
            initExisting();

            dispatcher.dispatch(pageEvents.pageSelected(OTHER_PAGE));
            saveDraft();

            expect(store.selectedPage()).not.toEqual(OTHER_PAGE);
            expect(patchExperiment).not.toHaveBeenCalledWith(
                EXPERIMENT_ID,
                expect.objectContaining({ pageId: OTHER_PAGE.pageId })
            );
        });

        it('should ignore a pick once the experiment is past draft', () => {
            initExisting(
                buildExperiment({
                    status: DotExperimentStatus.RUNNING,
                    trafficProportion: {
                        type: TrafficProportionTypes.SPLIT_EVENLY,
                        variants: [buildVariant('DEFAULT', 100)]
                    }
                })
            );

            dispatcher.dispatch(pageEvents.pageSelected(OTHER_PAGE));

            expect(store.selectedPage()).not.toEqual(OTHER_PAGE);
        });

        /**
         * The gate reads state, and state can be stale — a variant added in another tab is enough.
         * When the server refuses, the page on screen has to go back to the stored one rather than
         * keep showing something that exists nowhere.
         */
        it('should put the displayed page back when the server refuses the change', () => {
            patchExperiment.mockReturnValueOnce(throwError(() => httpError(400)));
            initExisting(controlOnlyDraft());
            contentSearchGet.mockClear();

            dispatcher.dispatch(pageEvents.pageSelected(OTHER_PAGE));
            saveDraft();

            expect(httpErrorManager.handle).toHaveBeenCalled();
            // Re-resolved from the experiment's own pageId, not from the one that was refused.
            expect(contentSearchGet).toHaveBeenCalledWith(
                expect.objectContaining({
                    query: expect.stringContaining(PAGE.pageId)
                })
            );
        });
    });

    describe('clearing the variants for a page change', () => {
        /** The draft the Change Page dialog is raised over: the control plus two of its own. */
        const draftWithTwoVariants = () =>
            buildExperiment({
                trafficProportion: {
                    type: TrafficProportionTypes.SPLIT_EVENLY,
                    variants: [
                        buildVariant('DEFAULT', 34),
                        buildVariant('variant-b', 33),
                        buildVariant('variant-c', 33)
                    ]
                }
            });

        /** What is left once both of them are gone. */
        const controlOnly = () =>
            buildExperiment({
                trafficProportion: {
                    type: TrafficProportionTypes.SPLIT_EVENLY,
                    variants: [buildVariant('DEFAULT', 100)]
                }
            });

        const deletedVariantIds = () => removeVariant.mock.calls.map(([, variantId]) => variantId);

        it('should offer every variant but the control as the ones in the way', () => {
            initExisting(draftWithTwoVariants());

            expect(store.$deletableVariants().map(({ id }) => id)).toEqual([
                'variant-b',
                'variant-c'
            ]);
        });

        it('should delete each of them, leaving the control alone', () => {
            initExisting(draftWithTwoVariants());
            removeVariant.mockReturnValue(of(controlOnly()));

            dispatcher.dispatch(pageEvents.pageChangeConfirmed());

            expect(deletedVariantIds()).toEqual(['variant-b', 'variant-c']);
        });

        /**
         * One at a time, because each deletion has the backend recompute the traffic proportion
         * over what is left: fired at once, which of the answers describes the experiment
         * afterwards would come down to arrival order.
         */
        it('should wait for each deletion before starting the next', () => {
            initExisting(draftWithTwoVariants());
            const firstDeletion = pendingCall(removeVariant);

            dispatcher.dispatch(pageEvents.pageChangeConfirmed());

            expect(deletedVariantIds()).toEqual(['variant-b']);
            expect(store.status()).toBe(ComponentStatus.SAVING);
            // The flag the confirmation's own button spins on, separate from the shared status.
            expect(store.deletingVariants()).toBe(true);

            removeVariant.mockReturnValue(of(controlOnly()));
            firstDeletion.next(draftWithTwoVariants());
            firstDeletion.complete();

            expect(deletedVariantIds()).toEqual(['variant-b', 'variant-c']);
        });

        it('should take the experiment the last deletion answered with', () => {
            const remaining = controlOnly();
            initExisting(draftWithTwoVariants());
            removeVariant.mockReturnValue(of(remaining));

            dispatcher.dispatch(pageEvents.pageChangeConfirmed());

            expect(store.experiment()).toEqual(remaining);
            expect(store.$deletableVariants()).toEqual([]);
            expect(store.status()).toBe(ComponentStatus.LOADED);
            expect(store.deletingVariants()).toBe(false);
            expect(store.deleteVariantsFailed()).toBe(false);
        });

        it('should report the go-ahead only once every variant is gone', () => {
            const settled: string[] = [];
            initExisting(draftWithTwoVariants());
            removeVariant.mockReturnValue(of(controlOnly()));
            events.on(apiEvents.deleteVariantsSucceeded).subscribe(() => settled.push('succeeded'));

            dispatcher.dispatch(pageEvents.pageChangeConfirmed());

            expect(settled).toEqual(['succeeded']);
        });

        /**
         * A rejection halfway leaves the deletions that went through gone, so the store keeps the
         * last answer it did get: the Variants card would otherwise go on offering weights for a
         * row that no longer exists.
         */
        it('should keep the variants that did go when one deletion is refused', () => {
            const afterFirst = buildExperiment({
                trafficProportion: {
                    type: TrafficProportionTypes.SPLIT_EVENLY,
                    variants: [buildVariant('DEFAULT', 50), buildVariant('variant-c', 50)]
                }
            });
            let failure: { experiment: DotExperiment | null } | null = null;

            initExisting(draftWithTwoVariants());
            removeVariant
                .mockReturnValueOnce(of(afterFirst))
                .mockReturnValueOnce(throwError(() => httpError(400)));
            events
                .on(apiEvents.deleteVariantsFailed)
                .subscribe(({ payload }) => (failure = payload));

            dispatcher.dispatch(pageEvents.pageChangeConfirmed());

            expect(store.experiment()).toEqual(afterFirst);
            expect(store.status()).toBe(ComponentStatus.LOADED);
            expect(failure?.experiment).toEqual(afterFirst);
        });

        /** The confirmation stays open on it, so the message has to outlive the run. */
        it('should hold on to a refusal until the next attempt', () => {
            initExisting(draftWithTwoVariants());
            removeVariant.mockReturnValue(throwError(() => httpError(500)));

            dispatcher.dispatch(pageEvents.pageChangeConfirmed());

            expect(store.deleteVariantsFailed()).toBe(true);
            expect(store.deletingVariants()).toBe(false);

            // A retry left in flight, so what is observed is the start of the run and not its end.
            pendingCall(removeVariant);
            dispatcher.dispatch(pageEvents.pageChangeConfirmed());

            expect(store.deleteVariantsFailed()).toBe(false);
            expect(store.deletingVariants()).toBe(true);
        });

        /** So a cancelled failure is not still on screen the next time the dialog opens. */
        it('should forget a refusal when Change Page is pressed again', () => {
            initExisting(draftWithTwoVariants());
            removeVariant.mockReturnValue(throwError(() => httpError(500)));
            dispatcher.dispatch(pageEvents.pageChangeConfirmed());

            dispatcher.dispatch(pageEvents.pageChangeRequested());

            expect(store.deleteVariantsFailed()).toBe(false);
            // The press deletes nothing on its own: only the confirmation does.
            expect(removeVariant).toHaveBeenCalledTimes(1);
        });

        it('should report a failure as a toast rather than a dialog over the open one', () => {
            initExisting(draftWithTwoVariants());
            removeVariant.mockReturnValue(throwError(() => httpError(500)));

            dispatcher.dispatch(pageEvents.pageChangeConfirmed());

            expect(httpErrorManager.handle).toHaveBeenCalledWith(expect.anything(), true);
        });

        it('should report no experiment when the very first deletion is refused', () => {
            let failure: { experiment: DotExperiment | null } | null = null;
            const draft = draftWithTwoVariants();

            initExisting(draft);
            removeVariant.mockReturnValue(throwError(() => httpError(500)));
            events
                .on(apiEvents.deleteVariantsFailed)
                .subscribe(({ payload }) => (failure = payload));

            dispatcher.dispatch(pageEvents.pageChangeConfirmed());

            expect(failure?.experiment).toBeNull();
            expect(store.experiment()).toEqual(draft);
        });

        /**
         * Reachable: the variants can go from another tab between opening the confirmation and
         * pressing it. Answered rather than dropped, or the dialog waits on a run that never went.
         */
        it('should answer a confirmation with nothing left to delete as done', () => {
            const settled: string[] = [];
            initExisting(controlOnly());
            events.on(apiEvents.deleteVariantsSucceeded).subscribe(() => settled.push('succeeded'));

            dispatcher.dispatch(pageEvents.pageChangeConfirmed());

            expect(removeVariant).not.toHaveBeenCalled();
            expect(settled).toEqual(['succeeded']);
            // And no in-flight state was ever raised: nothing was on the wire to bring home.
            expect(store.status()).toBe(ComponentStatus.LOADED);
            expect(store.deletingVariants()).toBe(false);
        });

        /**
         * The same contract `create$` and `start$` keep: the flag closes the door as the first call
         * leaves, so an impatient second confirmation is dropped here rather than cancelling a run
         * whose deletions have already partly landed.
         */
        it('should ignore a second confirmation while the first run is still going', () => {
            initExisting(draftWithTwoVariants());
            pendingCall(removeVariant);

            dispatcher.dispatch(pageEvents.pageChangeConfirmed());

            expect(deletedVariantIds()).toEqual(['variant-b']);
            expect(store.deletingVariants()).toBe(true);

            dispatcher.dispatch(pageEvents.pageChangeConfirmed());

            // No second call for a variant whose deletion is already on the wire.
            expect(deletedVariantIds()).toEqual(['variant-b']);
        });
    });

    describe('save draft', () => {
        it('should collapse rapid edits of one field into a single call, sending the last value', () => {
            initExisting();

            edit({ name: 'Alpha' });
            edit({ name: 'Alpha c' });
            edit({ name: 'Alpha campaign v2' });
            saveDraft();

            expect(patchExperiment).toHaveBeenCalledTimes(1);
            expect(patchExperiment).toHaveBeenCalledWith(
                EXPERIMENT_ID,
                expect.objectContaining({
                    name: 'Alpha campaign v2'
                })
            );
        });

        it('should not call anything until Save Draft is pressed', () => {
            initExisting();

            edit({ name: 'Alpha campaign v2' });

            expect(patchExperiment).not.toHaveBeenCalled();

            saveDraft();

            expect(patchExperiment).toHaveBeenCalledTimes(1);
        });

        it('should report SAVING for the flight only, never for the debounce window', () => {
            const inFlight = new Subject<DotExperiment>();
            patchExperiment.mockReturnValueOnce(inFlight);
            initExisting();

            // Typing: the diff is pending but nothing is on the wire yet — a prominent
            // progress indicator here would run for as long as the user keeps typing.
            edit({ name: 'Alpha campaign v2' });
            expect(store.$isSaving()).toBe(false);

            // The flush puts the PATCH on the wire.
            saveDraft();
            expect(store.$isSaving()).toBe(true);

            inFlight.next({ ...VALID_DRAFT, name: 'Alpha campaign v2' });
            inFlight.complete();
            expect(store.$isSaving()).toBe(false);
        });

        it('should bring SAVING home when a cancelled flight re-debounces into a skip', () => {
            const inFlight = new Subject<DotExperiment>();
            patchExperiment.mockReturnValueOnce(inFlight);
            initExisting();

            edit({ name: 'Alpha campaign v2' });
            saveDraft();
            expect(store.$isSaving()).toBe(true);

            // An edit mid-flight cancels the PATCH (switchMap); undoing it back to the saved
            // value leaves nothing to send, so the re-debounced flush skips — and the skip is
            // what must reset the status, or the bar would run forever.
            edit({ name: VALID_DRAFT.name });
            saveDraft();

            expect(store.$isSaving()).toBe(false);
        });

        /**
         * The response only settles the value it actually carried. `switchMap` cancels a flight
         * when the *next* debounce emits, so an edit made while the response is travelling reaches
         * the diff before it lands: settling by key name would drop it, leaving the form showing a
         * value the server never got and nothing pending to resend it (#37003).
         */
        it('should resend a field re-edited while its own PATCH was still travelling', () => {
            const inFlight = new Subject<DotExperiment>();
            patchExperiment.mockReturnValueOnce(inFlight);
            initExisting();

            edit({ name: 'Alpha' });
            saveDraft();

            expect(patchExperiment).toHaveBeenCalledWith(
                EXPERIMENT_ID,
                expect.objectContaining({ name: 'Alpha' })
            );

            // The user resumes typing before the answer to 'Alpha' comes back.
            edit({ name: 'Alpha campaign' });

            inFlight.next({ ...VALID_DRAFT, name: 'Alpha' });
            inFlight.complete();

            saveDraft();

            expect(patchExperiment).toHaveBeenCalledTimes(2);
            expect(patchExperiment).toHaveBeenLastCalledWith(
                EXPERIMENT_ID,
                expect.objectContaining({
                    name: 'Alpha campaign'
                })
            );
        });

        it('should settle a field left untouched while its PATCH was travelling', () => {
            const inFlight = new Subject<DotExperiment>();
            patchExperiment.mockReturnValueOnce(inFlight);
            initExisting();

            edit({ name: 'Alpha' });
            saveDraft();

            inFlight.next({ ...VALID_DRAFT, name: 'Alpha' });
            inFlight.complete();

            saveDraft();

            expect(patchExperiment).toHaveBeenCalledTimes(1);
        });

        /**
         * The point of the accumulated diff: the endpoint applies every key of its body in one
         * atomic update, so two cards edited in the same window are one call, not two.
         */
        it('should merge the fields of every card edited in the same window into one call', () => {
            const goals = buildGoals({ type: GOAL_TYPES.EXIT_RATE, name: 'Exit rate' });
            initExisting();

            edit({ name: 'Alpha campaign v2' });
            edit({ goal: toGoalSlice(goals) });
            edit({ trafficAllocation: 60 });
            saveDraft();

            expect(patchExperiment).toHaveBeenCalledTimes(1);
            expect(patchExperiment).toHaveBeenCalledWith(
                EXPERIMENT_ID,
                expect.objectContaining({
                    name: 'Alpha campaign v2',
                    goals,
                    trafficAllocation: 60
                })
            );
        });

        it('should keep the last value of a key edited twice in the same window', () => {
            initExisting();

            edit({ trafficAllocation: 40 });
            edit({ trafficAllocation: 60 });
            saveDraft();

            expect(patchExperiment).toHaveBeenCalledTimes(1);
            expect(patchExperiment).toHaveBeenCalledWith(
                EXPERIMENT_ID,
                expect.objectContaining({ trafficAllocation: 60 })
            );
        });

        it('should never PATCH a blank name', () => {
            initExisting();

            edit({ name: '   ' });
            saveDraft();

            expect(patchExperiment).not.toHaveBeenCalled();
            expect(store.$canSave()).toBe(false);
        });

        it('should refuse the whole save while the name is blank, not send the rest', () => {
            // The name is required on both endpoints, so there is no half-save to fall back on:
            // the press does nothing and the footer says which field is in the way.
            initExisting();

            edit({ name: '   ', trafficAllocation: 60 });
            saveDraft();

            expect(patchExperiment).not.toHaveBeenCalled();
            expect(store.$canSave()).toBe(false);
        });

        /**
         * An edit with nothing to send still has to settle the diff. Before #37003 the reducer
         * marked it pending and no call ever came back to unmark it, so the screen reported itself
         * as autosaving for the rest of the session.
         */
        it('should not resend the name the server already holds', () => {
            initExisting();

            edit({ name: VALID_DRAFT.name });
            saveDraft();

            expect(patchExperiment).not.toHaveBeenCalled();
            expect(store.$canSave()).toBe(false);
        });

        it('should not resend the description the server already holds', () => {
            // Typing a value and undoing it back is a no-op, not a PATCH.
            initExisting();

            edit({ description: VALID_DRAFT.description });
            saveDraft();

            expect(patchExperiment).not.toHaveBeenCalled();
            expect(store.$canSave()).toBe(false);
        });

        it('should stop autosaving when an edit is undone inside the debounce window', () => {
            // The first keystroke is worth sending and the last one is not, so the diff is left
            // pending by an edit that never becomes a call (#37003).
            initExisting();

            edit({ name: 'Alpha campaign v2' });
            edit({ name: VALID_DRAFT.name });
            saveDraft();

            expect(patchExperiment).not.toHaveBeenCalled();
            expect(store.$canSave()).toBe(false);
        });

        it('should send a cleared schedule, which is a change like any other', () => {
            initExisting(buildExperiment({ scheduling: { startDate: 1000, endDate: 2000 } }));

            edit({ scheduling: { startDate: null, endDate: null } });
            saveDraft();

            expect(patchExperiment).toHaveBeenCalledWith(
                EXPERIMENT_ID,
                expect.objectContaining({ scheduling: null })
            );
        });

        it('should show the edit before any call goes out, without touching the experiment', () => {
            initExisting();

            edit({ name: 'Alpha campaign v2', trafficAllocation: 60 });

            // The form is what the screen reads, so it moves on the keystroke.
            expect(store.draftName()).toBe('Alpha campaign v2');
            expect(store.$trafficAllocation()).toBe(60);
            expect(store.$canSave()).toBe(true);

            // The experiment is what the server last answered with, and no answer has come.
            expect(store.experiment()?.trafficAllocation).toBe(VALID_DRAFT.trafficAllocation);
        });

        it('should replace the experiment with what the server answered and settle the keys it wrote', () => {
            const renamed = buildExperiment({ name: 'Alpha campaign v2' });
            patchExperiment.mockReturnValue(of(renamed));
            initExisting();

            edit({ name: 'Alpha campaign v2' });
            saveDraft();

            expect(store.experiment()).toEqual(renamed);
            // The body carried the whole diff, so nothing is left of it.
            expect(store.$hasUnsavedChanges()).toBe(false);
            expect(store.$canSave()).toBe(false);
        });

        it('should report a failed save and leave the work on the table', () => {
            const error = httpError(400);
            patchExperiment.mockReturnValueOnce(throwError(() => error));
            initExisting();

            edit({ name: 'Alpha campaign v2' });
            saveDraft();

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(store.status()).toBe(ComponentStatus.LOADED);
            // The whole point of the button: the edit is still only on screen, and still
            // sendable, so pressing again is the way out.
            expect(store.$canSave()).toBe(true);
            expect(store.$hasUnsavedChanges()).toBe(true);
        });

        it('should re-send what a failed save could not write, merged with the next edit', () => {
            patchExperiment.mockReturnValueOnce(throwError(() => httpError(400)));
            // The retry answers with what it wrote, which is what settles the screen.
            patchExperiment.mockReturnValue(
                of(buildExperiment({ name: 'Alpha campaign v2', trafficAllocation: 60 }))
            );
            initExisting();

            edit({ name: 'Alpha campaign v2' });
            saveDraft();

            expect(store.$hasUnsavedChanges()).toBe(true);

            edit({ trafficAllocation: 60 });
            saveDraft();

            expect(patchExperiment).toHaveBeenLastCalledWith(
                EXPERIMENT_ID,
                expect.objectContaining({
                    name: 'Alpha campaign v2',
                    trafficAllocation: 60
                })
            );
            expect(store.$hasUnsavedChanges()).toBe(false);
        });

        it('should not autosave anything before the draft exists', () => {
            initNew();

            edit({ description: 'Typed before the page was picked' });
            saveDraft();

            expect(patchExperiment).not.toHaveBeenCalled();
        });

        it('should flush what was typed while the creation POST was in flight', () => {
            const created = pendingCall(add);
            initNew();

            createDraft();
            edit({ description: 'Typed while the POST was travelling' });

            expect(patchExperiment).not.toHaveBeenCalled();

            created.next(VALID_DRAFT);
            saveDraft();

            expect(patchExperiment).toHaveBeenCalledWith(
                VALID_DRAFT.id,
                expect.objectContaining({
                    description: 'Typed while the POST was travelling'
                })
            );
        });

        it('should not follow the creation POST with a PATCH of what it already saved', () => {
            initNew();

            createDraft();
            saveDraft();

            expect(patchExperiment).not.toHaveBeenCalled();
            expect(store.$canSave()).toBe(false);
        });
    });

    /**
     * The bodies are asserted against the real service, not against the arguments a mock
     * received: `targetingConditions` would be added by the service, not by the store, so a
     * mocked call could never show it (AC7). Live round-trips stay out of scope (AC9).
     */
    describe('outgoing payload shape (AC7/AC9)', () => {
        let httpMock: HttpTestingController;

        /**
         * Declared inside this block on purpose: a service factory registers its own
         * `beforeEach`, so a second one at the top level would configure the real API for every
         * test in the file, not just for these.
         */
        const createServiceWithRealApi = createServiceFactory({
            service: DotExperimentsConfigureStore,
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                DotExperimentsService,
                ...surroundingProviders()
            ]
        });

        const initWithRealApi = (params: Params = { experimentId: EXPERIMENT_ID }) => {
            routeParams = params;
            routeQueryParams = {};
            spectator = createServiceWithRealApi();
            store = spectator.service;
            dispatcher = spectator.inject(Dispatcher);
            httpMock = spectator.inject(HttpTestingController);
            spectator.flushEffects();

            if (params['experimentId']) {
                httpMock
                    .expectOne(`/api/v1/experiments/${EXPERIMENT_ID}`)
                    .flush({ entity: VALID_DRAFT });
                mirrorForm(VALID_DRAFT);
            }
        };

        afterEach(() => httpMock.verify());

        /**
         * The body is the form, so the interesting assertions are no longer about which keys were
         * omitted — every editable one travels. What still matters is that a card's value arrives
         * *correctly mapped*, and that `targetingConditions` never does: sending it would have the
         * backend rebuild the experiment's Rule.
         */
        it.each([
            {
                field: 'name',
                patch: { name: 'Alpha campaign v2' },
                expected: { name: 'Alpha campaign v2' }
            },
            {
                field: 'description',
                patch: { description: 'Reworked' },
                expected: { description: 'Reworked' }
            },
            {
                field: 'goal',
                patch: {
                    goal: toGoalSlice(buildGoals({ type: GOAL_TYPES.EXIT_RATE, name: 'Exit rate' }))
                },
                expected: {
                    goals: buildGoals({ type: GOAL_TYPES.EXIT_RATE, name: 'Exit rate' })
                }
            },
            {
                field: 'trafficAllocation',
                patch: { trafficAllocation: 60 },
                expected: { trafficAllocation: 60 }
            }
        ])('should PATCH $field, mapped, and never targetingConditions', ({ patch, expected }) => {
            initWithRealApi();

            edit(patch);
            saveDraft();

            const request = httpMock.expectOne(`/api/v1/experiments/${EXPERIMENT_ID}`);

            expect(request.request.method).toBe('PATCH');
            expect(request.request.body).toMatchObject(expected);
            expect(request.request.body).not.toHaveProperty('targetingConditions');

            request.flush({ entity: VALID_DRAFT });
        });

        it('should PATCH every card the user touched as one body, not one call each', () => {
            const goals = buildGoals({ type: GOAL_TYPES.EXIT_RATE, name: 'Exit rate' });
            initWithRealApi();

            edit({
                name: 'Alpha campaign v2',
                goal: toGoalSlice(goals),
                scheduling: { startDate: null, endDate: null }
            });
            saveDraft();

            // One request, not three: the endpoint applies every key of its body in one atomic
            // update, and the body is the whole form rather than a diff of it.
            const request = httpMock.expectOne(`/api/v1/experiments/${EXPERIMENT_ID}`);

            expect(request.request.method).toBe('PATCH');
            expect(request.request.body).toMatchObject({
                name: 'Alpha campaign v2',
                goals,
                scheduling: null
            });
            expect(request.request.body).not.toHaveProperty('targetingConditions');
            expect(request.request.body).not.toHaveProperty('pageId');

            request.flush({ entity: VALID_DRAFT });
        });

        it('should POST the creation with the page, the name and the description only', () => {
            initWithRealApi({});

            createDraft();

            const request = httpMock.expectOne('/api/v1/experiments');

            expect(request.request.method).toBe('POST');
            expect(request.request.body).toEqual({
                pageId: PAGE.pageId,
                name: VALID_DRAFT.name,
                description: VALID_DRAFT.description ?? '',
                trafficAllocation: DEFAULT_TRAFFIC_ALLOCATION
            });
            expect(request.request.body).not.toHaveProperty('targetingConditions');

            // Nothing was entered that the POST could not carry, so no PATCH follows it.
            request.flush({ entity: CREATED_DRAFT });

            httpMock.verify();
        });
    });

    describe('page prefill (AC14/AC15)', () => {
        it('should prefill the page from ?pageId=', () => {
            initNew({ pageId: PAGE.pageId });

            expect(contentSearchGet).toHaveBeenCalledWith({
                query: `+contentType:htmlpageasset +working:true +identifier:${PAGE.pageId}`,
                limit: 1
            });
            expect(store.selectedPage()).toEqual(PAGE);
            expect(store.pagePrefillError()).toBeNull();
        });

        it('should prefill the page from ?url= through the page search', () => {
            // Trailing slash and casing are not part of the identity of a path.
            initNew({ url: '/Home/' });

            expect(searchPages).toHaveBeenCalledWith({ hostname: SITE_HOSTNAME, path: '/Home/' });
            expect(store.selectedPage()).toEqual(PAGE);
            expect(store.pagePrefillError()).toBeNull();
        });

        it('should show an inline error when ?url= matches no page', () => {
            searchPages.mockReturnValue(of([buildBrowserPage({ path: '/other', url: '/other' })]));

            initNew({ url: '/home' });

            expect(store.selectedPage()).toBeNull();
            expect(store.pagePrefillError()).toBe(PAGE_PREFILL_ERROR_KEY);
            expect(store.status()).toBe(ComponentStatus.LOADED);
        });

        it('should show an inline error when ?pageId= resolves to nothing', () => {
            contentSearchGet.mockReturnValue(of({ jsonObjectView: { contentlets: [] } }));

            initNew({ pageId: 'page-gone' });

            expect(store.selectedPage()).toBeNull();
            expect(store.pagePrefillError()).toBe(PAGE_PREFILL_ERROR_KEY);
        });

        /**
         * A rejected lookup is not an answer about the page: saying "not found" would blame the
         * link for what is a failed call, and would leave a backend outage with no signal at all.
         */
        it('should tell a failed lookup apart from a page that is not there, and report it', () => {
            const error = httpError(403);
            contentSearchGet.mockReturnValue(throwError(() => error));

            initNew({ pageId: PAGE.pageId });

            expect(store.selectedPage()).toBeNull();
            expect(store.pagePrefillError()).toBe(PAGE_PREFILL_LOOKUP_ERROR_KEY);
            expect(store.status()).toBe(ComponentStatus.LOADED);
            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
        });

        /**
         * `?pageId=` is whatever the address bar carries, and it is concatenated into a Lucene
         * query: a value with operators would widen the search and prefill the card with some
         * other contentlet. It is reported as not found without a request going out.
         */
        it('should not look up a ?pageId= that is not shaped like an identifier', () => {
            initNew({ pageId: `${PAGE.pageId} OR +contentType:Host` });

            expect(contentSearchGet).not.toHaveBeenCalled();
            expect(store.selectedPage()).toBeNull();
            expect(store.pagePrefillError()).toBe(PAGE_PREFILL_ERROR_KEY);
        });

        it('should look nothing up without ?pageId= or ?url=', () => {
            initNew();

            expect(contentSearchGet).not.toHaveBeenCalled();
            expect(searchPages).not.toHaveBeenCalled();
            expect(store.pagePrefillError()).toBeNull();
        });
    });

    describe('page lock', () => {
        it('should flag a page locked by another user', () => {
            getPageLockState.mockReturnValue(of({ locked: true, lockedBy: OTHER_USER_ID }));

            initExisting();

            expect(getPageLockState).toHaveBeenCalledWith(PAGE.pageId);
            expect(store.$lockedByAnotherUser()).toBe(true);
            expect(store.$disabledTooltipKey()).toBe(EXP_CONFIG_ERROR_LABEL_PAGE_BLOCKED);
        });

        it('should not flag a page this user locked themselves', () => {
            getPageLockState.mockReturnValue(of({ locked: true, lockedBy: CURRENT_USER_ID }));

            initExisting();

            expect(store.$lockedByAnotherUser()).toBe(false);
            expect(store.$disabledTooltipKey()).toBeNull();
        });

        it('should not flag an unlocked page', () => {
            initExisting();

            expect(store.$lockedByAnotherUser()).toBe(false);
        });

        it('should treat an unresolvable lock state as unlocked', () => {
            getPageLockState.mockReturnValue(throwError(() => httpError(500)));

            initExisting();

            expect(store.$lockedByAnotherUser()).toBe(false);
            expect(store.status()).toBe(ComponentStatus.LOADED);
        });

        it('should resolve the lock of a page picked before creation', () => {
            initNew();

            dispatcher.dispatch(pageEvents.pageSelected(PAGE));

            expect(getPageLockState).toHaveBeenCalledWith(PAGE.pageId);
        });
    });

    describe('validation (AC28/AC29)', () => {
        interface ValidationCase {
            rule: string;
            experiment: DotExperiment;
            expected: ConfigureValidationRule[];
        }

        const VALIDATION_CASES: ValidationCase[] = [
            {
                rule: 'name',
                experiment: buildExperiment({ name: '   ' }),
                expected: ['name']
            },
            {
                rule: 'page',
                experiment: buildExperiment({ pageId: '' }),
                expected: ['page']
            },
            {
                rule: 'goalType and goalName',
                experiment: buildExperiment({ goals: null }),
                expected: ['goalType', 'goalName']
            },
            {
                rule: 'goalName',
                experiment: buildExperiment({ goals: buildGoals({ name: '  ' }) }),
                expected: ['goalName']
            },
            {
                rule: 'goalConditionValue of a REACH_PAGE goal',
                experiment: buildExperiment({
                    goals: buildGoals({
                        type: GOAL_TYPES.REACH_PAGE,
                        name: 'Reach the thank you page',
                        conditions: [
                            {
                                parameter: GOAL_PARAMETERS.URL,
                                operator: GOAL_OPERATORS.CONTAINS,
                                value: ''
                            }
                        ]
                    })
                }),
                expected: ['goalConditionValue']
            },
            {
                rule: 'goalConditionValue of a URL_PARAMETER goal',
                experiment: buildExperiment({
                    goals: buildGoals({
                        type: GOAL_TYPES.URL_PARAMETER,
                        name: 'Arrive with utm_source',
                        conditions: [
                            {
                                parameter: GOAL_PARAMETERS.QUERY_PARAM,
                                operator: GOAL_OPERATORS.CONTAINS,
                                value: { name: 'utm_source', value: '' }
                            }
                        ]
                    })
                }),
                expected: ['goalConditionValue']
            },
            {
                // EXISTS only asks whether the parameter is there, so only its name is required.
                rule: 'goalParameterName of an EXISTS goal',
                experiment: buildExperiment({
                    goals: buildGoals({
                        type: GOAL_TYPES.URL_PARAMETER,
                        name: 'Arrive with any utm',
                        conditions: [
                            {
                                parameter: GOAL_PARAMETERS.QUERY_PARAM,
                                operator: GOAL_OPERATORS.EXISTS,
                                value: { name: '  ', value: '' }
                            }
                        ]
                    })
                }),
                expected: ['goalParameterName']
            },
            {
                rule: 'minVariants',
                experiment: buildExperiment({
                    trafficProportion: {
                        type: TrafficProportionTypes.SPLIT_EVENLY,
                        variants: [buildVariant('DEFAULT', 100)]
                    }
                }),
                expected: ['minVariants']
            },
            {
                rule: 'weightsTotal',
                experiment: buildExperiment({
                    trafficProportion: {
                        type: TrafficProportionTypes.CUSTOM_PERCENTAGES,
                        variants: [buildVariant('DEFAULT', 50), buildVariant('variant-b', 40)]
                    }
                }),
                expected: ['weightsTotal']
            }
        ];

        it('should reveal nothing until Start is pressed', () => {
            initExisting(buildExperiment({ name: '', goals: null }));

            expect(store.$validationErrors()).toEqual([]);
            expect(store.$validationErrorCount()).toBe(0);
        });

        it.each(VALIDATION_CASES)(
            'should reveal $rule when Start is pressed',
            ({ experiment, expected }) => {
                initExisting(experiment);

                dispatcher.dispatch(pageEvents.startRequested());

                expect(store.$validationErrors()).toEqual(expected);
                expect(store.$validationErrorCount()).toBe(expected.length);
                expect(start).not.toHaveBeenCalled();
            }
        );

        /**
         * Revealing is what Start latches, not the errors themselves: a user who presses Start,
         * reads what is missing and fills it in must see each error go as they fix it, rather
         * than a form that stays red until Start is pressed again (#37003).
         */
        it('should drop the page rule as soon as a page is picked', () => {
            initNew();
            edit({ name: 'Alpha campaign' });

            dispatcher.dispatch(pageEvents.startRequested());
            expect(store.$validationErrors()).toContain('page');

            dispatcher.dispatch(pageEvents.pageSelected(PAGE));

            expect(store.$validationErrors()).not.toContain('page');
        });

        /**
         * On `/experiments/new` the goal has nowhere to live but the pending diff: there is no
         * experiment yet for `applyPatchToExperiment` to apply it to. Reading only the persisted
         * experiment left a goal the user had just filled in still counted as missing.
         */
        it('should count a goal entered before the draft exists as entered', () => {
            initNew();
            edit({ name: 'Alpha campaign' });

            dispatcher.dispatch(pageEvents.startRequested());
            expect(store.$validationErrors()).toContain('goalType');
            expect(store.$validationErrors()).toContain('goalName');

            edit({
                goal: toGoalSlice(buildGoals({ type: GOAL_TYPES.BOUNCE_RATE, name: 'Bounce rate' }))
            });

            expect(store.$validationErrors()).not.toContain('goalType');
            expect(store.$validationErrors()).not.toContain('goalName');
        });

        it('should keep the rules revealed for the fields still missing', () => {
            initExisting(buildExperiment({ name: '', goals: null }));

            dispatcher.dispatch(pageEvents.startRequested());
            expect(store.$validationErrors()).toContain('name');
            expect(store.$validationErrors()).toContain('goalType');

            edit({ name: 'Alpha campaign' });

            expect(store.$validationErrors()).not.toContain('name');
            expect(store.$validationErrors()).toContain('goalType');
        });

        it('should start a complete draft and clear the errors', () => {
            const running = buildExperiment({ status: DotExperimentStatus.RUNNING });
            start.mockReturnValue(of(running));
            initExisting();

            dispatcher.dispatch(pageEvents.startRequested());

            expect(store.$validationErrors()).toEqual([]);
            expect(start).toHaveBeenCalledWith(EXPERIMENT_ID);
            expect(store.experiment()).toEqual(running);
            expect(store.status()).toBe(ComponentStatus.LOADED);
        });
    });

    describe('start (AC31/AC32)', () => {
        it('should schedule instead of starting when the start date is in the future', () => {
            const scheduled = buildExperiment({
                scheduling: { startDate: Date.now() + 86400000, endDate: null }
            });
            initExisting(scheduled);

            expect(store.$isScheduledStart()).toBe(true);

            dispatcher.dispatch(pageEvents.startRequested());

            // One endpoint answers both: what changes is the copy the shell shows.
            expect(start).toHaveBeenCalledTimes(1);
            expect(start).toHaveBeenCalledWith(EXPERIMENT_ID);
        });

        it('should not read an unscheduled draft as a scheduled start', () => {
            initExisting();

            expect(store.$isScheduledStart()).toBe(false);
        });

        it('should not read a past start date as a scheduled start', () => {
            initExisting(
                buildExperiment({ scheduling: { startDate: Date.now() - 1000, endDate: null } })
            );

            expect(store.$isScheduledStart()).toBe(false);
        });

        it('should not fire a second start while the first one is in flight', () => {
            const started = new Subject<DotExperiment>();
            start.mockReturnValue(started);
            initExisting();

            dispatcher.dispatch(pageEvents.startRequested());
            dispatcher.dispatch(pageEvents.startRequested());

            expect(start).toHaveBeenCalledTimes(1);
            expect(store.starting()).toBe(true);

            started.next(buildExperiment({ status: DotExperimentStatus.RUNNING }));

            expect(store.starting()).toBe(false);
        });

        it('should allow starting again once a failed start has settled', () => {
            start.mockReturnValueOnce(throwError(() => httpError(400, {})));
            initExisting();

            dispatcher.dispatch(pageEvents.startRequested());

            expect(store.starting()).toBe(false);

            dispatcher.dispatch(pageEvents.startRequested());

            expect(start).toHaveBeenCalledTimes(2);
        });

        it('should title a rejection that came back without a header', () => {
            messageGet.mockReturnValue('Could not schedule the experiment');
            start.mockReturnValue(throwError(() => httpError(400, {})));
            initExisting();

            dispatcher.dispatch(pageEvents.startRequested());

            expect(httpErrorManager.handle).toHaveBeenCalledWith(
                expect.objectContaining({
                    error: expect.objectContaining({ header: 'Could not schedule the experiment' })
                })
            );
            expect(store.status()).toBe(ComponentStatus.LOADED);
        });

        it('should keep the header the backend sent', () => {
            start.mockReturnValue(throwError(() => httpError(400, { header: 'Already running' })));
            initExisting();

            dispatcher.dispatch(pageEvents.startRequested());

            expect(httpErrorManager.handle).toHaveBeenCalledWith(
                expect.objectContaining({
                    error: expect.objectContaining({ header: 'Already running' })
                })
            );
        });
    });

    describe('transitions', () => {
        /** Records the API events the shell listens to for its toasts. */
        const recordOutcomes = () => {
            const seen: string[] = [];

            events
                .on(
                    apiEvents.startSucceeded,
                    apiEvents.stopSucceeded,
                    apiEvents.cancelScheduleSucceeded,
                    apiEvents.abortSucceeded
                )
                .subscribe(({ type }) => seen.push(type));

            return seen;
        };

        it('should end a running experiment through the stop endpoint', () => {
            initExisting(buildExperiment({ status: DotExperimentStatus.RUNNING }));
            const outcomes = recordOutcomes();

            dispatcher.dispatch(pageEvents.stopRequested());

            expect(stop).toHaveBeenCalledWith(EXPERIMENT_ID);
            expect(outcomes).toEqual([apiEvents.stopSucceeded.type]);
        });

        it('should cancel a schedule through the cancel endpoint', () => {
            initExisting(buildExperiment({ status: DotExperimentStatus.SCHEDULED }));
            const outcomes = recordOutcomes();

            dispatcher.dispatch(pageEvents.cancelScheduleRequested());

            expect(cancelSchedule).toHaveBeenCalledWith(EXPERIMENT_ID);
            expect(outcomes).toEqual([apiEvents.cancelScheduleSucceeded.type]);
        });

        it('should abort through the same cancel endpoint, under its own event', () => {
            initExisting(buildExperiment({ status: DotExperimentStatus.RUNNING }));
            const outcomes = recordOutcomes();

            dispatcher.dispatch(pageEvents.abortRequested());

            // There is no dedicated abort endpoint; only the toast copy differs, so the event
            // has to stay distinct from `cancelScheduleSucceeded`.
            expect(cancelSchedule).toHaveBeenCalledWith(EXPERIMENT_ID);
            expect(outcomes).toEqual([apiEvents.abortSucceeded.type]);
        });

        it('should keep the screen usable when a transition fails', () => {
            const error = httpError(400);
            stop.mockReturnValue(throwError(() => error));
            initExisting(buildExperiment({ status: DotExperimentStatus.RUNNING }));

            dispatcher.dispatch(pageEvents.stopRequested());

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(store.status()).toBe(ComponentStatus.LOADED);
        });
    });

    describe('locking (AC34/AC35)', () => {
        it.each([
            DotExperimentStatus.SCHEDULED,
            DotExperimentStatus.RUNNING,
            DotExperimentStatus.ENDED,
            DotExperimentStatus.ARCHIVED
        ])('should lock every field while the experiment is %s', (status) => {
            initExisting(buildExperiment({ status }));

            expect(store.$isLocked()).toBe(true);
            expect(store.$disabledTooltipKey()).toBe(EXP_CONFIG_ERROR_LABEL_CANT_EDIT);
        });

        it('should leave a draft editable', () => {
            initExisting();

            expect(store.$isLocked()).toBe(false);
            expect(store.$lockedBannerKey()).toBeNull();
        });

        it('should explain a running experiment with its own copy', () => {
            initExisting(buildExperiment({ status: DotExperimentStatus.RUNNING }));

            expect(store.$lockedBannerKey()).toBe(LOCKED_BANNER_KEY_RUNNING);
        });

        it('should explain every other locked status with the generic copy', () => {
            initExisting(buildExperiment({ status: DotExperimentStatus.SCHEDULED }));

            expect(store.$lockedBannerKey()).toBe(LOCKED_BANNER_KEY_READ_ONLY);
        });

        it('should explain the locked status before the page lock', () => {
            getPageLockState.mockReturnValue(of({ locked: true, lockedBy: OTHER_USER_ID }));

            initExisting(buildExperiment({ status: DotExperimentStatus.RUNNING }));

            expect(store.$lockedByAnotherUser()).toBe(true);
            expect(store.$disabledTooltipKey()).toBe(EXP_CONFIG_ERROR_LABEL_CANT_EDIT);
        });
    });

    /**
     * Variants have dedicated endpoints, each answering with the whole experiment — including the
     * proportion the backend recomputed — so every outcome below replaces it wholesale.
     */
    describe('variants', () => {
        const VARIANT_B_ID = 'variant-b';

        const withVariants = (variants: Variant[]): DotExperiment =>
            buildExperiment({
                trafficProportion: {
                    type: TrafficProportionTypes.SPLIT_EVENLY,
                    variants
                }
            });

        it('should add a variant and take the proportion the endpoint answered with', () => {
            const withThree = withVariants([
                buildVariant('DEFAULT', 34),
                buildVariant(VARIANT_B_ID, 33),
                buildVariant('variant-c', 33)
            ]);
            const added = pendingCall(addVariant);
            initExisting();

            dispatcher.dispatch(pageEvents.variantAdded('Variant C'));

            expect(addVariant).toHaveBeenCalledWith(EXPERIMENT_ID, 'Variant C');
            expect(store.status()).toBe(ComponentStatus.SAVING);

            added.next(withThree);

            expect(store.experiment()).toEqual(withThree);
            expect(store.$variants()).toHaveLength(3);
            expect(store.status()).toBe(ComponentStatus.LOADED);
        });

        it('should rename a variant through the endpoint that takes its description', () => {
            const renamed = withVariants([
                buildVariant('DEFAULT', 50),
                { id: VARIANT_B_ID, name: 'Hero image B', weight: 50 }
            ]);
            const edited = pendingCall(editVariant);
            initExisting();

            dispatcher.dispatch(
                pageEvents.variantRenamed({ variantId: VARIANT_B_ID, name: 'Hero image B' })
            );

            // The variant endpoint carries the name under `description`, not under `name`.
            expect(editVariant).toHaveBeenCalledWith(EXPERIMENT_ID, VARIANT_B_ID, {
                description: 'Hero image B'
            });
            expect(store.status()).toBe(ComponentStatus.SAVING);

            edited.next(renamed);

            expect(store.$variants()).toEqual(renamed.trafficProportion.variants);
            expect(store.status()).toBe(ComponentStatus.LOADED);
        });

        it('should remove a variant and take the proportion the endpoint answered with', () => {
            const withOne = withVariants([buildVariant('DEFAULT', 100)]);
            const removed = pendingCall(removeVariant);
            initExisting();

            dispatcher.dispatch(pageEvents.variantDeleted(VARIANT_B_ID));

            expect(removeVariant).toHaveBeenCalledWith(EXPERIMENT_ID, VARIANT_B_ID);
            expect(store.status()).toBe(ComponentStatus.SAVING);

            removed.next(withOne);

            expect(store.experiment()).toEqual(withOne);
            expect(store.$variants()).toHaveLength(1);
            expect(store.status()).toBe(ComponentStatus.LOADED);
        });

        interface VariantFailureCase {
            action: string;
            call: jest.Mock;
            dispatch: () => void;
        }

        const FAILURE_CASES: VariantFailureCase[] = [
            {
                action: 'adding',
                call: addVariant,
                dispatch: () => dispatcher.dispatch(pageEvents.variantAdded('Variant C'))
            },
            {
                action: 'renaming',
                call: editVariant,
                dispatch: () =>
                    dispatcher.dispatch(
                        pageEvents.variantRenamed({
                            variantId: VARIANT_B_ID,
                            name: 'Hero image B'
                        })
                    )
            },
            {
                action: 'removing',
                call: removeVariant,
                dispatch: () => dispatcher.dispatch(pageEvents.variantDeleted(VARIANT_B_ID))
            }
        ];

        it.each(FAILURE_CASES)(
            'should report a failure while $action and leave the card usable',
            ({ call, dispatch }) => {
                const error = httpError(400);
                call.mockReturnValue(throwError(() => error));
                initExisting();

                dispatch();

                expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
                // The list on screen is still the one the server holds.
                expect(store.experiment()).toEqual(VALID_DRAFT);
                expect(store.status()).toBe(ComponentStatus.LOADED);
            }
        );

        it('should not let one variant call cancel another', () => {
            // `mergeMap`, not `switchMap`: deleting a row must not abandon the rename of another.
            const added = pendingCall(addVariant);
            const removed = pendingCall(removeVariant);
            initExisting();

            dispatcher.dispatch(pageEvents.variantAdded('Variant C'));
            dispatcher.dispatch(pageEvents.variantDeleted(VARIANT_B_ID));

            expect(addVariant).toHaveBeenCalledTimes(1);
            expect(removeVariant).toHaveBeenCalledTimes(1);
            expect(added.observed).toBe(true);
            expect(removed.observed).toBe(true);
        });
    });

    describe('$isSaving', () => {
        it('should read the creation POST as saving, before any status says so', () => {
            // `/experiments/new` sits on LOADED throughout, so `creating` is the only signal.
            const created = pendingCall(add);
            initNew();

            createDraft();

            expect(store.creating()).toBe(true);
            expect(store.status()).toBe(ComponentStatus.LOADED);
            expect(store.$isSaving()).toBe(true);

            created.next(VALID_DRAFT);

            expect(store.$isSaving()).toBe(false);
        });

        it('should read a variant call in flight as saving', () => {
            const added = pendingCall(addVariant);
            initExisting();

            dispatcher.dispatch(pageEvents.variantAdded('Variant C'));

            expect(store.$isSaving()).toBe(true);

            added.next(VALID_DRAFT);

            expect(store.$isSaving()).toBe(false);
        });

        it('should not read a settled screen as saving', () => {
            initExisting();

            expect(store.$isSaving()).toBe(false);
        });
    });

    describe('allowed actions', () => {
        /** Every action the kebab knows about, so a status is described by what it allows. */
        const ALL_ACTIONS: ExperimentListAction[] = [
            'delete',
            'abort',
            'results',
            'configuration',
            'archive',
            'end',
            'addToBundle',
            'pushPublish',
            'cancelSchedule'
        ];

        const only = (allowed: ExperimentListAction[]): Record<ExperimentListAction, boolean> =>
            ALL_ACTIONS.reduce(
                (actions, action) => ({ ...actions, [action]: allowed.includes(action) }),
                {} as Record<ExperimentListAction, boolean>
            );

        it.each<[DotExperimentStatus, ExperimentListAction[]]>([
            [DotExperimentStatus.DRAFT, ['delete', 'configuration', 'addToBundle', 'pushPublish']],
            [
                DotExperimentStatus.SCHEDULED,
                ['delete', 'configuration', 'addToBundle', 'pushPublish', 'cancelSchedule']
            ],
            [
                DotExperimentStatus.RUNNING,
                ['abort', 'results', 'configuration', 'end', 'addToBundle', 'pushPublish']
            ],
            [
                DotExperimentStatus.ENDED,
                ['results', 'configuration', 'archive', 'addToBundle', 'pushPublish']
            ],
            [DotExperimentStatus.ARCHIVED, ['configuration', 'addToBundle', 'pushPublish']]
        ])('should offer exactly the actions %s allows', (status, allowed) => {
            initExisting(buildExperiment({ status }));

            expect(store.$allowedActions()).toEqual(only(allowed));
        });

        it('should treat a screen with no experiment yet as a draft', () => {
            initNew();

            expect(store.$allowedActions()).toEqual(
                only(['delete', 'configuration', 'addToBundle', 'pushPublish'])
            );
        });
    });

    /**
     * The weights themselves are the form's, not the store's: the Variants card writes them into its
     * slice — Split Evenly included (AC23) — and they arrive here as `formEdited` like any other
     * edit. What the store still owns is the *state* they produce.
     */
    /**
     * The weights are a slice of the form, not state the store keeps: the card owns the rows and
     * the store's only say is whether they are in a state worth sending.
     */
    describe('weights (AC25)', () => {
        /** A split as the card reports it: the whole set of rows, not the one that changed. */
        const split = (...rows: [string, number][]): Partial<ConfigureFormModel> => ({
            trafficProportionType: TrafficProportionTypes.CUSTOM_PERCENTAGES,
            variantWeights: rows.map(([id, weight]) => ({ id, weight }))
        });

        /** The proportion those rows become on the wire, merged back onto the stored variants. */
        const proportion = (...rows: [string, number][]) => ({
            trafficProportion: {
                type: TrafficProportionTypes.CUSTOM_PERCENTAGES,
                variants: rows.map(([id, weight]) => buildVariant(id, weight))
            }
        });

        const THREE_VARIANTS = buildExperiment({
            trafficProportion: {
                type: TrafficProportionTypes.CUSTOM_PERCENTAGES,
                variants: [
                    buildVariant('DEFAULT', 100),
                    buildVariant('variant-b', 0),
                    buildVariant('variant-c', 0)
                ]
            }
        });

        it('should PATCH a complete split as one proportion, not row by row', () => {
            initExisting(THREE_VARIANTS);

            edit(split(['DEFAULT', 34], ['variant-b', 33], ['variant-c', 33]));
            saveDraft();

            expect(patchExperiment).toHaveBeenCalledTimes(1);
            expect(patchExperiment).toHaveBeenCalledWith(
                EXPERIMENT_ID,
                expect.objectContaining(
                    proportion(['DEFAULT', 34], ['variant-b', 33], ['variant-c', 33])
                )
            );
        });

        it('should merge a split with a field edited beside it into one call', () => {
            initExisting(THREE_VARIANTS);

            edit({
                name: 'Alpha campaign v2',
                ...split(['DEFAULT', 34], ['variant-b', 33], ['variant-c', 33])
            });
            saveDraft();

            expect(patchExperiment).toHaveBeenCalledTimes(1);
            expect(patchExperiment).toHaveBeenCalledWith(
                EXPERIMENT_ID,
                expect.objectContaining({
                    name: 'Alpha campaign v2',
                    trafficProportion: expect.objectContaining({
                        type: TrafficProportionTypes.CUSTOM_PERCENTAGES
                    })
                })
            );
        });

        it('should leave the proportion out of a save the form has nothing to say about', () => {
            // The rows have not been seeded yet — the state a just-loaded screen is in before the
            // card mirrors them. That is not a reason to refuse the save, only to send no split.
            initExisting();
            edit({ name: 'Alpha campaign v2', variantWeights: [] });

            saveDraft();

            expect(patchExperiment).toHaveBeenCalledWith(
                EXPERIMENT_ID,
                expect.not.objectContaining({ trafficProportion: expect.anything() })
            );
        });
    });

    /**
     * Typing a weight goes through totals that are not 100, and `TrafficProportion` rejects those on
     * construction — so a PATCH carrying one is a guaranteed 400 and an error toast in the middle of
     * an edit. Save Draft refuses instead, and refuses the whole press rather than sending the rest:
     * a half-save would let `saveSucceeded` call the weights still on screen saved.
     */
    describe('mid-edit weights (#37003)', () => {
        const split = (control: number, variantB: number): Partial<ConfigureFormModel> => ({
            trafficProportionType: TrafficProportionTypes.CUSTOM_PERCENTAGES,
            variantWeights: [
                { id: 'DEFAULT', weight: control },
                { id: 'variant-b', weight: variantB }
            ]
        });

        it('should not PATCH a proportion whose weights do not add up to 100', () => {
            initExisting();

            edit(split(70, 50));
            saveDraft();

            expect(patchExperiment).not.toHaveBeenCalled();
            expect(httpErrorManager.handle).not.toHaveBeenCalled();
            expect(store.$canSave()).toBe(false);
        });

        it('should refuse the press outright rather than send the fields beside them', () => {
            initExisting();

            edit({ ...split(70, 50), name: 'Alpha campaign v2', trafficAllocation: 60 });
            saveDraft();

            expect(patchExperiment).not.toHaveBeenCalled();
            expect(store.$hasUnsavedChanges()).toBe(true);
            expect(store.$canSave()).toBe(false);
        });

        it('should PATCH the whole proportion once the total is back to 100', () => {
            initExisting();

            edit(split(70, 50));
            saveDraft();

            edit(split(70, 30));
            saveDraft();

            expect(patchExperiment).toHaveBeenCalledTimes(1);
            expect(patchExperiment).toHaveBeenCalledWith(
                EXPERIMENT_ID,
                expect.objectContaining({
                    trafficProportion: {
                        type: TrafficProportionTypes.CUSTOM_PERCENTAGES,
                        variants: [buildVariant('DEFAULT', 70), buildVariant('variant-b', 30)]
                    }
                })
            );
        });

        it('should not report a refused press as saving', () => {
            initExisting();

            edit(split(70, 50));
            saveDraft();

            // Nothing is on its way, so the footer must not claim to be saving anything.
            expect(store.$isSaving()).toBe(false);
        });

        it('should leave the weights being fixed untouched by a save of another field', () => {
            // The response knows nothing about the weights being typed, so it must not reach them:
            // the defect was the rows snapping back to the stored split mid-edit.
            patchExperiment.mockReturnValue(of(buildExperiment({ name: 'Alpha campaign v2' })));
            initExisting();

            edit({ name: 'Alpha campaign v2' });
            saveDraft();

            edit(split(70, 50));

            expect(store.formValue()?.variantWeights).toEqual([
                { id: 'DEFAULT', weight: 70 },
                { id: 'variant-b', weight: 50 }
            ]);
            expect(store.$canSave()).toBe(false);
        });
    });

    /**
     * `withReducer` is installed before `withEventHandlers`, and the reducer listens on
     * `ReducerEvents` while the handlers listen on `Events` — so a handler always reads the state
     * its own event has already produced. Both flows below depend on it.
     */
    describe('reducer before handler', () => {
        it('should create from the page the same dispatch selected', () => {
            initNew();

            edit({ name: 'Alpha campaign' });
            dispatcher.dispatch(pageEvents.pageSelected(PAGE));
            saveDraft();

            expect(add).toHaveBeenCalledWith(
                expect.objectContaining({ pageId: PAGE.pageId, name: 'Alpha campaign' })
            );
        });

        it('should read the validation errors its own start produced', () => {
            initExisting(buildExperiment({ name: '' }));

            dispatcher.dispatch(pageEvents.startRequested());

            expect(store.$validationErrors()).toEqual(['name']);
            expect(start).not.toHaveBeenCalled();

            edit({ name: 'Alpha campaign' });
            dispatcher.dispatch(pageEvents.startRequested());

            expect(start).toHaveBeenCalledTimes(1);
        });
    });
});
