import { Dispatcher, Events, provideDispatcher } from '@ngrx/signals/events';
import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { of, Subject, throwError } from 'rxjs';

import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ActivatedRoute, convertToParamMap, Params, Router } from '@angular/router';

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
    AUTOSAVE_DEBOUNCE_MS,
    LOCKED_BANNER_KEY_READ_ONLY,
    LOCKED_BANNER_KEY_RUNNING,
    PAGE_PREFILL_ERROR_KEY
} from '../shared/constants';
import {
    ConfigureValidationRule,
    DotExperimentConfigurePage,
    ExperimentListAction
} from '../shared/models';

const pageEvents = dotExperimentsConfigurePageEvents;
const apiEvents = dotExperimentsConfigureApiEvents;

const SITE_HOSTNAME = 'demo.dotcms.com';
const CURRENT_USER_ID = 'user-me';
const OTHER_USER_ID = 'user-someone-else';
const EXPERIMENT_ID = 'exp-1';

/** The page every fixture runs on, as the Page card renders it. */
const PAGE: DotExperimentConfigurePage = { pageId: 'page-1', title: 'Home', path: '/home' };

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

const buildPageContentlet = (contentlet: Partial<DotCMSContentlet> = {}): DotCMSContentlet =>
    ({
        identifier: PAGE.pageId,
        title: PAGE.title,
        url: PAGE.path,
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
    const setName = jest.fn();
    const setDescription = jest.fn();
    const setGoal = jest.fn();
    const setScheduling = jest.fn();
    const setTrafficAllocation = jest.fn();
    const setTrafficProportion = jest.fn();
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

    /** The store reads the route once, through the snapshot's param maps. */
    const activatedRouteStub = {
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
                setName,
                setDescription,
                setGoal,
                setScheduling,
                setTrafficAllocation,
                setTrafficProportion,
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

    /** `/experiments/:experimentId/configuration` for an experiment the load answers with. */
    const initExisting = (experiment: DotExperiment = VALID_DRAFT) => {
        getById.mockReturnValue(of(experiment));
        initExistingRoute(experiment.id);
    };

    /** The Name + Page combination that creates the draft. */
    const createDraft = (name = VALID_DRAFT.name) => {
        dispatcher.dispatch(pageEvents.nameChanged(name));
        dispatcher.dispatch(pageEvents.pageSelected(PAGE));
    };

    const flushAutosave = () => jest.advanceTimersByTime(AUTOSAVE_DEBOUNCE_MS);

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

        getById.mockReturnValue(of(VALID_DRAFT));
        add.mockReturnValue(of(VALID_DRAFT));
        setName.mockReturnValue(of(VALID_DRAFT));
        setDescription.mockReturnValue(of(VALID_DRAFT));
        setGoal.mockReturnValue(of(VALID_DRAFT));
        setScheduling.mockReturnValue(of(VALID_DRAFT));
        setTrafficAllocation.mockReturnValue(of(VALID_DRAFT));
        setTrafficProportion.mockReturnValue(of(VALID_DRAFT));
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

    describe('creation (AC2/AC3/AC4)', () => {
        it('should not create anything until both a name and a page are there', () => {
            initNew();

            dispatcher.dispatch(pageEvents.nameChanged('Alpha campaign'));

            expect(add).not.toHaveBeenCalled();

            dispatcher.dispatch(pageEvents.pageSelected(PAGE));

            expect(add).toHaveBeenCalledTimes(1);
            expect(add).toHaveBeenCalledWith({
                pageId: PAGE.pageId,
                name: 'Alpha campaign',
                description: ''
            });
        });

        it('should never create a draft from a blank name', () => {
            initNew();

            dispatcher.dispatch(pageEvents.nameChanged('   '));
            dispatcher.dispatch(pageEvents.pageSelected(PAGE));

            expect(add).not.toHaveBeenCalled();
        });

        it('should not fire a second POST while the first one is in flight', () => {
            const created = new Subject<DotExperiment>();
            add.mockReturnValue(created);
            initNew();

            createDraft();
            dispatcher.dispatch(pageEvents.nameChanged('Alpha campaign renamed'));

            expect(add).toHaveBeenCalledTimes(1);

            created.next(VALID_DRAFT);

            expect(store.creating()).toBe(false);
        });

        it('should never create a second experiment once one exists', () => {
            initNew();

            createDraft();
            dispatcher.dispatch(pageEvents.nameChanged('Alpha campaign renamed'));
            dispatcher.dispatch(pageEvents.pageSelected({ ...PAGE, pageId: 'page-2' }));

            expect(add).toHaveBeenCalledTimes(1);
        });

        it('should replace /new with the created experiment url', () => {
            const created = buildExperiment({ id: 'exp-created' });
            add.mockReturnValue(of(created));
            initNew();

            createDraft();

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

            dispatcher.dispatch(pageEvents.nameChanged('Alpha campaign'));
            dispatcher.dispatch(pageEvents.descriptionChanged('Checkout funnel rework'));
            dispatcher.dispatch(pageEvents.pageSelected(PAGE));

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
                pageEvents.pageSelected({ pageId: 'page-2', title: 'Pricing', path: '/pricing' })
            );

            expect(store.selectedPage()).toEqual(PAGE);
        });
    });

    describe('autosave debounce (AC6)', () => {
        it('should collapse rapid edits of one field group into a single call', () => {
            initExisting();

            dispatcher.dispatch(pageEvents.nameChanged('Alpha'));
            dispatcher.dispatch(pageEvents.nameChanged('Alpha c'));
            dispatcher.dispatch(pageEvents.nameChanged('Alpha campaign v2'));
            flushAutosave();

            expect(setName).toHaveBeenCalledTimes(1);
            expect(setName).toHaveBeenCalledWith(EXPERIMENT_ID, 'Alpha campaign v2');
        });

        it('should not call anything before the debounce window elapses', () => {
            initExisting();

            dispatcher.dispatch(pageEvents.nameChanged('Alpha campaign v2'));
            jest.advanceTimersByTime(AUTOSAVE_DEBOUNCE_MS - 1);

            expect(setName).not.toHaveBeenCalled();

            jest.advanceTimersByTime(1);

            expect(setName).toHaveBeenCalledTimes(1);
        });

        it('should fire one call per field group edited in the same window', () => {
            const goals = buildGoals({ type: GOAL_TYPES.EXIT_RATE, name: 'Exit rate' });
            initExisting();

            dispatcher.dispatch(pageEvents.nameChanged('Alpha campaign v2'));
            dispatcher.dispatch(pageEvents.goalChanged(goals));
            flushAutosave();

            expect(setName).toHaveBeenCalledTimes(1);
            expect(setGoal).toHaveBeenCalledTimes(1);
            expect(setGoal).toHaveBeenCalledWith(EXPERIMENT_ID, goals);
        });

        it('should never PATCH a blank name', () => {
            initExisting();

            dispatcher.dispatch(pageEvents.nameChanged('   '));
            flushAutosave();

            expect(setName).not.toHaveBeenCalled();
            expect(store.$isAutosaving()).toBe(false);
        });

        /**
         * An edit with nothing to send still has to settle its group. Before #37003 the reducer
         * marked the group pending and no call ever came back to unmark it, so the screen reported
         * itself as autosaving for the rest of the session.
         */
        it('should not resend the name the server already holds', () => {
            initExisting();

            dispatcher.dispatch(pageEvents.nameChanged(VALID_DRAFT.name));
            flushAutosave();

            expect(setName).not.toHaveBeenCalled();
            expect(store.$isAutosaving()).toBe(false);
        });

        it('should not resend the description the server already holds', () => {
            // Typing a value and undoing it back is a no-op, not a PATCH.
            initExisting();

            dispatcher.dispatch(pageEvents.descriptionChanged(VALID_DRAFT.description as string));
            flushAutosave();

            expect(setDescription).not.toHaveBeenCalled();
            expect(store.$isAutosaving()).toBe(false);
        });

        it('should never PATCH a goal that is not there', () => {
            // `setGoal` sends `{ goals }`, and the endpoint has nothing to do with a null one.
            initExisting();

            dispatcher.dispatch(pageEvents.goalChanged(null));
            flushAutosave();

            expect(setGoal).not.toHaveBeenCalled();
            expect(store.$isAutosaving()).toBe(false);
        });

        it('should stop autosaving when an edit is undone inside the debounce window', () => {
            // The first keystroke is worth sending and the last one is not, so the group is
            // marked pending by an edit that never becomes a call (#37003).
            initExisting();

            dispatcher.dispatch(pageEvents.nameChanged('Alpha campaign v2'));
            dispatcher.dispatch(pageEvents.nameChanged(VALID_DRAFT.name));
            flushAutosave();

            expect(setName).not.toHaveBeenCalled();
            expect(store.$isAutosaving()).toBe(false);
        });

        it('should send a cleared schedule, which is a change like any other', () => {
            initExisting(buildExperiment({ scheduling: { startDate: 1000, endDate: 2000 } }));

            dispatcher.dispatch(pageEvents.schedulingChanged(null));
            flushAutosave();

            expect(setScheduling).toHaveBeenCalledWith(EXPERIMENT_ID, null);
        });

        it('should apply the edit locally before the call goes out', () => {
            initExisting();

            dispatcher.dispatch(pageEvents.nameChanged('Alpha campaign v2'));

            // The card must not lag a debounce window behind the keystroke.
            expect(store.draftName()).toBe('Alpha campaign v2');
            expect(store.$isAutosaving()).toBe(true);
        });

        it('should replace the experiment with what the server answered', () => {
            const renamed = buildExperiment({ name: 'Alpha campaign v2' });
            setName.mockReturnValue(of(renamed));
            initExisting();

            dispatcher.dispatch(pageEvents.nameChanged('Alpha campaign v2'));
            flushAutosave();

            expect(store.experiment()).toEqual(renamed);
            expect(store.$isAutosaving()).toBe(false);
        });

        it('should report a failed autosave and stop autosaving', () => {
            const error = httpError(400);
            setName.mockReturnValue(throwError(() => error));
            initExisting();

            dispatcher.dispatch(pageEvents.nameChanged('Alpha campaign v2'));
            flushAutosave();

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(store.$isAutosaving()).toBe(false);
        });

        it('should not autosave anything before the draft exists', () => {
            initNew();

            dispatcher.dispatch(pageEvents.descriptionChanged('Typed before the page was picked'));
            flushAutosave();

            expect(setDescription).not.toHaveBeenCalled();
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
            }
        };

        afterEach(() => httpMock.verify());

        interface PatchCase {
            group: string;
            change: () => void;
            body: Record<string, unknown>;
        }

        const PATCH_CASES: PatchCase[] = [
            {
                group: 'name',
                change: () => dispatcher.dispatch(pageEvents.nameChanged('Alpha campaign v2')),
                body: { name: 'Alpha campaign v2' }
            },
            {
                group: 'description',
                change: () => dispatcher.dispatch(pageEvents.descriptionChanged('Reworked')),
                body: { description: 'Reworked' }
            },
            {
                group: 'goal',
                change: () =>
                    dispatcher.dispatch(
                        pageEvents.goalChanged(
                            buildGoals({ type: GOAL_TYPES.EXIT_RATE, name: 'Exit rate' })
                        )
                    ),
                body: { goals: buildGoals({ type: GOAL_TYPES.EXIT_RATE, name: 'Exit rate' }) }
            },
            {
                group: 'scheduling',
                change: () =>
                    dispatcher.dispatch(
                        pageEvents.schedulingChanged({ startDate: 1000, endDate: 2000 })
                    ),
                body: { scheduling: { startDate: 1000, endDate: 2000 } }
            },
            {
                group: 'a cleared scheduling',
                change: () => dispatcher.dispatch(pageEvents.schedulingChanged(null)),
                body: { scheduling: null }
            },
            {
                group: 'trafficAllocation',
                change: () => dispatcher.dispatch(pageEvents.trafficAllocationChanged(60)),
                body: { trafficAllocation: 60 }
            },
            {
                group: 'trafficProportion',
                change: () =>
                    dispatcher.dispatch(
                        pageEvents.trafficProportionChanged({
                            type: TrafficProportionTypes.CUSTOM_PERCENTAGES,
                            variants: [buildVariant('DEFAULT', 70), buildVariant('variant-b', 30)]
                        })
                    ),
                body: {
                    trafficProportion: {
                        type: TrafficProportionTypes.CUSTOM_PERCENTAGES,
                        variants: [buildVariant('DEFAULT', 70), buildVariant('variant-b', 30)]
                    }
                }
            }
        ];

        it.each(PATCH_CASES)(
            'should PATCH $group alone, without targetingConditions',
            ({ change, body }) => {
                initWithRealApi();

                change();
                flushAutosave();

                const request = httpMock.expectOne(`/api/v1/experiments/${EXPERIMENT_ID}`);

                expect(request.request.method).toBe('PATCH');
                expect(request.request.body).toEqual(body);
                expect(request.request.body).not.toHaveProperty('targetingConditions');

                request.flush({ entity: VALID_DRAFT });
            }
        );

        it('should POST the creation with the page, the name and the description only', () => {
            initWithRealApi({});

            createDraft();

            const request = httpMock.expectOne('/api/v1/experiments');

            expect(request.request.method).toBe('POST');
            expect(request.request.body).toEqual({
                pageId: PAGE.pageId,
                name: VALID_DRAFT.name,
                description: ''
            });
            expect(request.request.body).not.toHaveProperty('targetingConditions');

            request.flush({ entity: VALID_DRAFT });
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

        it('should show an inline error rather than crash when the lookup fails', () => {
            contentSearchGet.mockReturnValue(throwError(() => httpError(403)));

            initNew({ pageId: PAGE.pageId });

            expect(store.selectedPage()).toBeNull();
            expect(store.pagePrefillError()).toBe(PAGE_PREFILL_ERROR_KEY);
            expect(store.status()).toBe(ComponentStatus.LOADED);
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

            expect(store.validationErrors()).toEqual([]);
            expect(store.$validationErrorCount()).toBe(0);
        });

        it.each(VALIDATION_CASES)(
            'should reveal $rule when Start is pressed',
            ({ experiment, expected }) => {
                initExisting(experiment);

                dispatcher.dispatch(pageEvents.startRequested());

                expect(store.validationErrors()).toEqual(expected);
                expect(store.$validationErrorCount()).toBe(expected.length);
                expect(start).not.toHaveBeenCalled();
            }
        );

        it('should start a complete draft and clear the errors', () => {
            const running = buildExperiment({ status: DotExperimentStatus.RUNNING });
            start.mockReturnValue(of(running));
            initExisting();

            dispatcher.dispatch(pageEvents.startRequested());

            expect(store.validationErrors()).toEqual([]);
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

    describe('split evenly (AC23/AC25)', () => {
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

        it('should give the remainder to the first variant so the total is exactly 100', () => {
            initExisting(THREE_VARIANTS);

            dispatcher.dispatch(pageEvents.splitEvenly());

            expect(store.$variants().map(({ weight }) => weight)).toEqual([34, 33, 33]);
            expect(store.$totalWeight()).toBe(100);
            expect(store.$hasInvalidWeights()).toBe(false);
        });

        it('should persist the split through the debounced trafficProportion PATCH', () => {
            initExisting(THREE_VARIANTS);

            dispatcher.dispatch(pageEvents.splitEvenly());
            flushAutosave();

            expect(setTrafficProportion).toHaveBeenCalledWith(EXPERIMENT_ID, {
                type: TrafficProportionTypes.SPLIT_EVENLY,
                variants: [
                    buildVariant('DEFAULT', 34),
                    buildVariant('variant-b', 33),
                    buildVariant('variant-c', 33)
                ]
            });
        });

        it('should do nothing while there are no variants', () => {
            initExisting(
                buildExperiment({
                    trafficProportion: {
                        type: TrafficProportionTypes.SPLIT_EVENLY,
                        variants: []
                    }
                })
            );

            dispatcher.dispatch(pageEvents.splitEvenly());
            flushAutosave();

            expect(setTrafficProportion).not.toHaveBeenCalled();
            expect(store.$hasInvalidWeights()).toBe(false);
        });

        it('should warn while the weights do not add up to 100', () => {
            initExisting(
                buildExperiment({
                    trafficProportion: {
                        type: TrafficProportionTypes.CUSTOM_PERCENTAGES,
                        variants: [buildVariant('DEFAULT', 50), buildVariant('variant-b', 40)]
                    }
                })
            );

            expect(store.$hasInvalidWeights()).toBe(true);
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

            dispatcher.dispatch(pageEvents.nameChanged('Alpha campaign'));

            expect(add).not.toHaveBeenCalled();

            dispatcher.dispatch(pageEvents.pageSelected(PAGE));

            expect(add).toHaveBeenCalledWith(
                expect.objectContaining({ pageId: PAGE.pageId, name: 'Alpha campaign' })
            );
        });

        it('should read the validation errors its own start produced', () => {
            initExisting(buildExperiment({ name: '' }));

            dispatcher.dispatch(pageEvents.startRequested());

            expect(store.validationErrors()).toEqual(['name']);
            expect(start).not.toHaveBeenCalled();

            dispatcher.dispatch(pageEvents.nameChanged('Alpha campaign'));
            dispatcher.dispatch(pageEvents.startRequested());

            expect(start).toHaveBeenCalledTimes(1);
        });
    });
});
