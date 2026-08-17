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
    DotExperimentPatchBody,
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

    /** `/experiments/:experimentId/configuration` for an experiment the load answers with. */
    const initExisting = (experiment: DotExperiment = VALID_DRAFT) => {
        getById.mockReturnValue(of(experiment));
        initExistingRoute(experiment.id);
    };

    /** The Name + Page combination that creates the draft. */
    const createDraft = (name = VALID_DRAFT.name) => {
        dispatcher.dispatch(pageEvents.formEdited({ name }));
        dispatcher.dispatch(pageEvents.pageSelected(PAGE));
    };

    /** An edit as the cards report it: only the PATCH keys that changed. */
    const edit = (patch: DotExperimentPatchBody) =>
        dispatcher.dispatch(pageEvents.formEdited(patch));

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
        laterUrls$ = new Subject<ParamMap>();

        getById.mockReturnValue(of(VALID_DRAFT));
        add.mockReturnValue(of(VALID_DRAFT));
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
            dispatcher.dispatch(pageEvents.formEdited({ name: 'Typed but not yet sent' }));
            expect(store.pendingPatch()).not.toEqual({});

            laterUrls$.next(convertToParamMap({ experimentId: OTHER_ID }));

            expect(store.pendingPatch()).toEqual({});
        });

        it('should ignore the swap to the experiment it just created', () => {
            initNew();
            createDraft();
            getById.mockClear();

            // What the store's own `createSucceeded` navigation produces.
            laterUrls$.next(convertToParamMap({ experimentId: VALID_DRAFT.id }));

            expect(getById).not.toHaveBeenCalled();
            expect(store.experiment()).toEqual(VALID_DRAFT);
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
        it('should not create anything until both a name and a page are there', () => {
            initNew();

            edit({ name: 'Alpha campaign' });

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

            edit({ name: '   ' });
            dispatcher.dispatch(pageEvents.pageSelected(PAGE));

            expect(add).not.toHaveBeenCalled();
        });

        it('should not report autosaving while the draft cannot be created yet', () => {
            initNew();

            // A name alone cannot go anywhere — no experiment to PATCH, no page for the POST —
            // so the footer must keep the plain hint instead of a "Saving…" that never settles.
            edit({ name: 'Alpha campaign' });
            jest.advanceTimersByTime(AUTOSAVE_DEBOUNCE_MS);

            expect(store.$isAutosaving()).toBe(false);
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

            edit({ name: 'Alpha campaign' });
            edit({ description: 'Checkout funnel rework' });
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
        it('should collapse rapid edits of one field into a single call, sending the last value', () => {
            initExisting();

            edit({ name: 'Alpha' });
            edit({ name: 'Alpha c' });
            edit({ name: 'Alpha campaign v2' });
            flushAutosave();

            expect(patchExperiment).toHaveBeenCalledTimes(1);
            expect(patchExperiment).toHaveBeenCalledWith(EXPERIMENT_ID, {
                name: 'Alpha campaign v2'
            });
        });

        it('should not call anything before the debounce window elapses', () => {
            initExisting();

            edit({ name: 'Alpha campaign v2' });
            jest.advanceTimersByTime(AUTOSAVE_DEBOUNCE_MS - 1);

            expect(patchExperiment).not.toHaveBeenCalled();

            jest.advanceTimersByTime(1);

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
            flushAutosave();
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
            flushAutosave();
            expect(store.$isSaving()).toBe(true);

            // An edit mid-flight cancels the PATCH (switchMap); undoing it back to the saved
            // value leaves nothing to send, so the re-debounced flush skips — and the skip is
            // what must reset the status, or the bar would run forever.
            edit({ name: VALID_DRAFT.name });
            flushAutosave();

            expect(store.$isSaving()).toBe(false);
        });

        /**
         * The point of the accumulated diff: the endpoint applies every key of its body in one
         * atomic update, so two cards edited in the same window are one call, not two.
         */
        it('should merge the fields of every card edited in the same window into one call', () => {
            const goals = buildGoals({ type: GOAL_TYPES.EXIT_RATE, name: 'Exit rate' });
            initExisting();

            edit({ name: 'Alpha campaign v2' });
            edit({ goals });
            edit({ trafficAllocation: 60 });
            flushAutosave();

            expect(patchExperiment).toHaveBeenCalledTimes(1);
            expect(patchExperiment).toHaveBeenCalledWith(EXPERIMENT_ID, {
                name: 'Alpha campaign v2',
                goals,
                trafficAllocation: 60
            });
        });

        it('should keep the last value of a key edited twice in the same window', () => {
            initExisting();

            edit({ trafficAllocation: 40 });
            edit({ trafficAllocation: 60 });
            flushAutosave();

            expect(patchExperiment).toHaveBeenCalledTimes(1);
            expect(patchExperiment).toHaveBeenCalledWith(EXPERIMENT_ID, { trafficAllocation: 60 });
        });

        it('should never PATCH a blank name', () => {
            initExisting();

            edit({ name: '   ' });
            flushAutosave();

            expect(patchExperiment).not.toHaveBeenCalled();
            expect(store.$isAutosaving()).toBe(false);
        });

        it('should strip a blank name from a diff that has other keys to send', () => {
            initExisting();

            edit({ name: '   ', trafficAllocation: 60 });
            flushAutosave();

            expect(patchExperiment).toHaveBeenCalledWith(EXPERIMENT_ID, { trafficAllocation: 60 });
        });

        /**
         * An edit with nothing to send still has to settle the diff. Before #37003 the reducer
         * marked it pending and no call ever came back to unmark it, so the screen reported itself
         * as autosaving for the rest of the session.
         */
        it('should not resend the name the server already holds', () => {
            initExisting();

            edit({ name: VALID_DRAFT.name });
            flushAutosave();

            expect(patchExperiment).not.toHaveBeenCalled();
            expect(store.$isAutosaving()).toBe(false);
        });

        it('should not resend the description the server already holds', () => {
            // Typing a value and undoing it back is a no-op, not a PATCH.
            initExisting();

            edit({ description: VALID_DRAFT.description });
            flushAutosave();

            expect(patchExperiment).not.toHaveBeenCalled();
            expect(store.$isAutosaving()).toBe(false);
        });

        it('should stop autosaving when an edit is undone inside the debounce window', () => {
            // The first keystroke is worth sending and the last one is not, so the diff is left
            // pending by an edit that never becomes a call (#37003).
            initExisting();

            edit({ name: 'Alpha campaign v2' });
            edit({ name: VALID_DRAFT.name });
            flushAutosave();

            expect(patchExperiment).not.toHaveBeenCalled();
            expect(store.$isAutosaving()).toBe(false);
        });

        it('should send a cleared schedule, which is a change like any other', () => {
            initExisting(buildExperiment({ scheduling: { startDate: 1000, endDate: 2000 } }));

            edit({ scheduling: null });
            flushAutosave();

            expect(patchExperiment).toHaveBeenCalledWith(EXPERIMENT_ID, { scheduling: null });
        });

        it('should apply the edit locally before the call goes out', () => {
            initExisting();

            edit({ name: 'Alpha campaign v2', trafficAllocation: 60 });

            // No card may lag a debounce window behind the keystroke that caused it.
            expect(store.draftName()).toBe('Alpha campaign v2');
            expect(store.experiment()?.trafficAllocation).toBe(60);
            expect(store.$isAutosaving()).toBe(true);
        });

        it('should replace the experiment with what the server answered and settle the keys it wrote', () => {
            const renamed = buildExperiment({ name: 'Alpha campaign v2' });
            patchExperiment.mockReturnValue(of(renamed));
            initExisting();

            edit({ name: 'Alpha campaign v2' });
            flushAutosave();

            expect(store.experiment()).toEqual(renamed);
            // The body carried the whole diff, so nothing is left of it.
            expect(store.pendingPatch()).toBeNull();
            expect(store.$isAutosaving()).toBe(false);
        });

        it('should report a failed save and stop reporting itself as autosaving', () => {
            const error = httpError(400);
            patchExperiment.mockReturnValueOnce(throwError(() => error));
            initExisting();

            edit({ name: 'Alpha campaign v2' });
            flushAutosave();

            expect(httpErrorManager.handle).toHaveBeenCalledWith(error);
            expect(store.status()).toBe(ComponentStatus.LOADED);
            // Nothing is on its way any more, even though the diff is still unsaved.
            expect(store.$isAutosaving()).toBe(false);
        });

        it('should re-send what a failed save could not write, merged with the next edit', () => {
            patchExperiment.mockReturnValueOnce(throwError(() => httpError(400)));
            initExisting();

            edit({ name: 'Alpha campaign v2' });
            flushAutosave();

            expect(store.pendingPatch()).toEqual({ name: 'Alpha campaign v2' });

            edit({ trafficAllocation: 60 });
            flushAutosave();

            expect(patchExperiment).toHaveBeenLastCalledWith(EXPERIMENT_ID, {
                name: 'Alpha campaign v2',
                trafficAllocation: 60
            });
            expect(store.pendingPatch()).toBeNull();
        });

        it('should not autosave anything before the draft exists', () => {
            initNew();

            edit({ description: 'Typed before the page was picked' });
            flushAutosave();

            expect(patchExperiment).not.toHaveBeenCalled();
        });

        it('should flush what was typed while the creation POST was in flight', () => {
            const created = pendingCall(add);
            initNew();

            createDraft();
            edit({ description: 'Typed while the POST was travelling' });

            expect(patchExperiment).not.toHaveBeenCalled();

            created.next(VALID_DRAFT);
            flushAutosave();

            expect(patchExperiment).toHaveBeenCalledWith(VALID_DRAFT.id, {
                description: 'Typed while the POST was travelling'
            });
        });

        it('should not follow the creation POST with a PATCH of what it already saved', () => {
            initNew();

            createDraft();
            flushAutosave();

            expect(patchExperiment).not.toHaveBeenCalled();
            expect(store.$isAutosaving()).toBe(false);
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
            field: string;
            body: DotExperimentPatchBody;
        }

        /** Each card's edit, and the body it has to reach the endpoint as. */
        const PATCH_CASES: PatchCase[] = [
            { field: 'name', body: { name: 'Alpha campaign v2' } },
            { field: 'description', body: { description: 'Reworked' } },
            {
                field: 'goal',
                body: { goals: buildGoals({ type: GOAL_TYPES.EXIT_RATE, name: 'Exit rate' }) }
            },
            { field: 'scheduling', body: { scheduling: { startDate: 1000, endDate: 2000 } } },
            { field: 'a cleared scheduling', body: { scheduling: null } },
            { field: 'trafficAllocation', body: { trafficAllocation: 60 } },
            {
                field: 'trafficProportion',
                body: {
                    trafficProportion: {
                        type: TrafficProportionTypes.CUSTOM_PERCENTAGES,
                        variants: [buildVariant('DEFAULT', 70), buildVariant('variant-b', 30)]
                    }
                }
            }
        ];

        it.each(PATCH_CASES)(
            'should PATCH $field with that key only, and never targetingConditions or pageId',
            ({ body }) => {
                initWithRealApi();

                dispatcher.dispatch(pageEvents.formEdited(body));
                flushAutosave();

                const request = httpMock.expectOne(`/api/v1/experiments/${EXPERIMENT_ID}`);

                expect(request.request.method).toBe('PATCH');
                expect(request.request.body).toEqual(body);
                expect(request.request.body).not.toHaveProperty('targetingConditions');
                expect(request.request.body).not.toHaveProperty('pageId');

                request.flush({ entity: VALID_DRAFT });
            }
        );

        it('should PATCH everything edited in one window as a single multi-key body', () => {
            const goals = buildGoals({ type: GOAL_TYPES.EXIT_RATE, name: 'Exit rate' });
            initWithRealApi();

            dispatcher.dispatch(pageEvents.formEdited({ name: 'Alpha campaign v2' }));
            dispatcher.dispatch(pageEvents.formEdited({ goals }));
            dispatcher.dispatch(pageEvents.formEdited({ scheduling: null }));
            flushAutosave();

            const request = httpMock.expectOne(`/api/v1/experiments/${EXPERIMENT_ID}`);

            expect(request.request.method).toBe('PATCH');
            expect(request.request.body).toEqual({
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

    /**
     * The weights themselves are the form's, not the store's: the Variants card writes them into its
     * slice — Split Evenly included (AC23) — and they arrive here as `formEdited` like any other
     * edit. What the store still owns is the *state* they produce.
     */
    describe('weights (AC25)', () => {
        const SPLIT_ACROSS_THREE = {
            trafficProportion: {
                type: TrafficProportionTypes.SPLIT_EVENLY,
                variants: [
                    buildVariant('DEFAULT', 34),
                    buildVariant('variant-b', 33),
                    buildVariant('variant-c', 33)
                ]
            }
        };

        it('should apply a reported split at once, and send it as one PATCH', () => {
            initExisting(
                buildExperiment({
                    trafficProportion: {
                        type: TrafficProportionTypes.CUSTOM_PERCENTAGES,
                        variants: [
                            buildVariant('DEFAULT', 100),
                            buildVariant('variant-b', 0),
                            buildVariant('variant-c', 0)
                        ]
                    }
                })
            );

            edit(SPLIT_ACROSS_THREE);

            expect(store.$variants().map(({ weight }) => weight)).toEqual([34, 33, 33]);
            expect(store.$totalWeight()).toBe(100);
            expect(store.$hasInvalidWeights()).toBe(false);

            flushAutosave();

            expect(patchExperiment).toHaveBeenCalledWith(EXPERIMENT_ID, SPLIT_ACROSS_THREE);
        });

        it('should merge a split with a field edited in the same window', () => {
            initExisting();

            edit({ name: 'Alpha campaign v2' });
            edit(SPLIT_ACROSS_THREE);
            flushAutosave();

            expect(patchExperiment).toHaveBeenCalledTimes(1);
            expect(patchExperiment).toHaveBeenCalledWith(
                EXPERIMENT_ID,
                expect.objectContaining({
                    name: 'Alpha campaign v2',
                    trafficProportion: expect.objectContaining({
                        type: TrafficProportionTypes.SPLIT_EVENLY
                    })
                })
            );
        });

        it('should say nothing about the weights of an experiment that has no variants', () => {
            initExisting(
                buildExperiment({
                    trafficProportion: {
                        type: TrafficProportionTypes.SPLIT_EVENLY,
                        variants: []
                    }
                })
            );

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
     * Typing a weight goes through totals that are not 100, and `TrafficProportion` rejects those on
     * construction — so a PATCH carrying one is a guaranteed 400 and an error toast in the middle of
     * an edit. The key is held back until the total is valid again.
     */
    describe('mid-edit weights (#37003)', () => {
        /** A weight edit as the card reports it: the whole proportion, not the row that changed. */
        const weights = (control: number, variantB: number): DotExperimentPatchBody => ({
            trafficProportion: {
                type: TrafficProportionTypes.CUSTOM_PERCENTAGES,
                variants: [buildVariant('DEFAULT', control), buildVariant('variant-b', variantB)]
            }
        });

        it('should not PATCH a proportion whose weights do not add up to 100', () => {
            initExisting();

            edit(weights(70, 50));
            flushAutosave();

            expect(patchExperiment).not.toHaveBeenCalled();
            expect(httpErrorManager.handle).not.toHaveBeenCalled();
            // What was typed is still on the rows, with the warning bar saying why (AC25).
            expect(store.$variants().map(({ weight }) => weight)).toEqual([70, 50]);
            expect(store.$hasInvalidWeights()).toBe(true);
            expect(store.$isAutosaving()).toBe(false);
        });

        it('should PATCH the whole proportion once the total is back to 100', () => {
            initExisting();

            edit(weights(70, 50));
            flushAutosave();

            edit(weights(70, 30));
            flushAutosave();

            expect(patchExperiment).toHaveBeenCalledTimes(1);
            expect(patchExperiment).toHaveBeenCalledWith(EXPERIMENT_ID, weights(70, 30));
        });

        it('should send the rest of the diff without the proportion it held back', () => {
            initExisting();

            edit({ ...weights(70, 50), trafficAllocation: 60 });
            flushAutosave();

            expect(patchExperiment).toHaveBeenCalledWith(EXPERIMENT_ID, { trafficAllocation: 60 });
        });

        it('should not report a held-back proportion as autosaving while it waits', () => {
            initExisting();

            edit(weights(70, 50));

            // Nothing is on its way: the key goes nowhere until the total is valid again, so the
            // footer must not claim to be saving it.
            expect(store.$isAutosaving()).toBe(false);
        });

        /**
         * The defect: the name went out alone, and the response — which knows nothing about the
         * weights being typed — replaced the experiment and took the held-back proportion with it,
         * so the rows snapped back to 50/50 mid-edit.
         */
        it('should keep the weights being fixed on screen when a field saves beside them', () => {
            patchExperiment.mockReturnValue(of(buildExperiment({ name: 'Alpha campaign v2' })));
            initExisting();

            edit(weights(70, 50));
            edit({ name: 'Alpha campaign v2' });
            flushAutosave();

            expect(patchExperiment).toHaveBeenCalledTimes(1);
            expect(patchExperiment).toHaveBeenCalledWith(EXPERIMENT_ID, {
                name: 'Alpha campaign v2'
            });
            expect(store.experiment()?.name).toBe('Alpha campaign v2');
            expect(store.$variants().map(({ weight }) => weight)).toEqual([70, 50]);
            expect(store.$hasInvalidWeights()).toBe(true);
            expect(store.pendingPatch()).toEqual(weights(70, 50));
            expect(store.$isAutosaving()).toBe(false);
        });

        it('should PATCH the proportion it held back once the total is fixed after that save', () => {
            patchExperiment.mockReturnValue(of(buildExperiment({ name: 'Alpha campaign v2' })));
            initExisting();

            edit(weights(70, 50));
            edit({ name: 'Alpha campaign v2' });
            flushAutosave();

            edit(weights(70, 30));
            flushAutosave();

            expect(patchExperiment).toHaveBeenCalledTimes(2);
            expect(patchExperiment).toHaveBeenLastCalledWith(EXPERIMENT_ID, weights(70, 30));
            expect(store.pendingPatch()).toBeNull();
        });

        /**
         * Same defect one window later: the flush that found nothing to send must not drop the
         * proportion either, or the next save of another field would snap the rows back.
         */
        it('should keep the weights being fixed through a flush that sent nothing', () => {
            patchExperiment.mockReturnValue(of(buildExperiment({ name: 'Alpha campaign v2' })));
            initExisting();

            edit(weights(70, 50));
            flushAutosave();

            expect(patchExperiment).not.toHaveBeenCalled();

            edit({ name: 'Alpha campaign v2' });
            flushAutosave();

            expect(patchExperiment).toHaveBeenCalledWith(EXPERIMENT_ID, {
                name: 'Alpha campaign v2'
            });
            expect(store.$variants().map(({ weight }) => weight)).toEqual([70, 50]);
            expect(store.$isAutosaving()).toBe(false);
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

            edit({ name: 'Alpha campaign' });
            dispatcher.dispatch(pageEvents.startRequested());

            expect(start).toHaveBeenCalledTimes(1);
        });
    });
});
