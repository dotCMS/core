import { provideDispatcher } from '@ngrx/signals/events';
import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { signal } from '@angular/core';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import { ConfirmationService, MenuItem } from 'primeng/api';

import { DotMessageDisplayService, DotMessageService } from '@dotcms/data-access';
import { ComponentStatus, DotExperiment } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsResultsComponent } from './dot-experiments-results.component';

import { DotExperimentsResultsStore } from '../store/dot-experiments-results.store';

const NOT_STARTED_COPY = 'This Experiment has not started collecting sessions yet.';
const NOT_ENOUGH_SESSIONS_COPY = 'An Experiment needs at least 10 sessions.';
const UNAVAILABLE_COPY = 'The report could not be loaded, so there are no numbers to show here.';

const LIST_TITLE_COPY = 'Experiments List';
const RESULTS_TITLE_COPY = 'Experiments Reports';

/** The experiment the screen reports on, as the store publishes it once the load settles. */
const EXPERIMENT = {
    id: '211e12ca-23cc-4647-a3de-c030ca9cb971',
    name: 'Summer landing test'
} as DotExperiment;

const messageServiceMock = new MockDotMessageService({
    'experiment.container.list.title': LIST_TITLE_COPY,
    'experiment.container.report.title': RESULTS_TITLE_COPY,
    'experiments.results.empty.title': 'No results to report yet',
    'experiments.results.empty.description': NOT_ENOUGH_SESSIONS_COPY,
    'experiments.results.empty.not-started.description': NOT_STARTED_COPY,
    'experiments.results.empty.unavailable.description': UNAVAILABLE_COPY
});

/** Real signals rather than `jest.fn()`, for the reason the summary-table spec gives. */
const createStoreMock = () => ({
    /** No experiment yet, which is also what the screen holds while the load is in flight. */
    experiment: signal<DotExperiment | null>(null),
    status: signal(ComponentStatus.LOADED),
    reportUnavailable: signal(false),
    $isWaitingForData: signal(false),
    $hasEnoughSessionsForTable: signal(false),
    $hasLoadError: signal(false)
});

/**
 * The subtitle alone, so the view is never rendered: every reason the body can be empty is a
 * computed over store state, and rendering would drag in the header, stat strip, charts and table
 * with all of their own store reads for no extra coverage of this decision.
 */
/**
 * The trail, as a real list rather than four independent spies: whether a crumb is appended or
 * written in place is the whole question here, and a mock that only records the calls answers it
 * the same way either way.
 *
 * Not `mockProvider`: `GlobalStore` is a signal store, so its methods live on the instance rather
 * than the prototype and Spectator's auto-mock finds none of them.
 */
const createGlobalStoreMock = () => {
    const trail: MenuItem[] = [];

    return {
        trail,
        addNewBreadcrumb: jest.fn((crumb: MenuItem) => {
            trail.push(crumb);
        }),
        setLastBreadcrumb: jest.fn((crumb: MenuItem) => {
            trail[trail.length - 1] = crumb;
        }),
        breadcrumbs: jest.fn(() => [...trail]),
        lastBreadcrumb: jest.fn(() => trail.at(-1) ?? null)
    };
};

describe('DotExperimentsResultsComponent', () => {
    let spectator: Spectator<DotExperimentsResultsComponent>;
    let storeMock: ReturnType<typeof createStoreMock>;
    let globalStore: ReturnType<typeof createGlobalStoreMock>;

    const createComponent = createComponentFactory({
        component: DotExperimentsResultsComponent,
        // Replaces the component's own `providers`, so `ConfirmationService` has to be restated:
        // dropping it takes away a real dependency rather than just swapping the store.
        componentProviders: [
            { provide: DotExperimentsResultsStore, useFactory: () => storeMock },
            ConfirmationService
        ],
        providers: [
            provideDispatcher(),
            { provide: DotMessageService, useValue: messageServiceMock },
            {
                provide: ActivatedRoute,
                useValue: { data: of({}), snapshot: { paramMap: new Map(), queryParams: {} } }
            },
            { provide: GlobalStore, useFactory: () => globalStore },
            mockProvider(DotMessageDisplayService)
        ],
        detectChanges: false
    });

    beforeEach(() => {
        storeMock = createStoreMock();
        globalStore = createGlobalStoreMock();
    });

    describe('why the report is empty', () => {
        beforeEach(() => {
            spectator = createComponent();
        });

        it('says the Experiment has not started when there is nothing to measure yet', () => {
            storeMock.$isWaitingForData.set(true);

            expect(spectator.component.$emptyReportSubtitle()).toBe(NOT_STARTED_COPY);
        });

        it('names the session threshold once the Experiment is running', () => {
            expect(spectator.component.$emptyReportSubtitle()).toBe(NOT_ENOUGH_SESSIONS_COPY);
        });

        /**
         * A failed report settles as LOADED with `results: null`, which leaves the session gate
         * closed through no fault of the session count. Without its own branch the screen blamed
         * the sessions, directly under a banner saying the report could not be loaded.
         */
        it('blames the failed load, not the session count, when the report is unavailable', () => {
            storeMock.reportUnavailable.set(true);

            expect(spectator.component.$emptyReportSubtitle()).toBe(UNAVAILABLE_COPY);
        });

        it('prefers the failed load over the not-started copy when both could apply', () => {
            storeMock.reportUnavailable.set(true);
            storeMock.$isWaitingForData.set(true);

            expect(spectator.component.$emptyReportSubtitle()).toBe(UNAVAILABLE_COPY);
        });
    });

    /**
     * #37005. Nothing else puts this screen on the trail, so it ended at the list's crumb and the
     * shell titled the Results screen "Experiments List" — the screen the editor had just left.
     *
     * Mounted with an empty template: the crumb is decided in an effect, and flushing effects
     * renders, which would drag in the header, stat strip, charts and table with all of their own
     * store reads for no extra coverage of this decision.
     */
    describe('breadcrumbs', () => {
        const createComponent = createComponentFactory({
            component: DotExperimentsResultsComponent,
            componentProviders: [
                { provide: DotExperimentsResultsStore, useFactory: () => storeMock },
                ConfirmationService
            ],
            providers: [
                provideDispatcher(),
                { provide: DotMessageService, useValue: messageServiceMock },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        data: of({}),
                        snapshot: {
                            paramMap: convertToParamMap({ experimentId: EXPERIMENT.id }),
                            queryParams: {}
                        }
                    }
                },
                { provide: GlobalStore, useFactory: () => globalStore },
                mockProvider(DotMessageDisplayService)
            ],
            overrideComponents: [[DotExperimentsResultsComponent, { set: { template: '' } }]],
            detectChanges: false
        });

        const crumbLabels = () => globalStore.trail.map(({ label }) => label);

        it('should name the screen, with the list above it', () => {
            spectator = createComponent();

            expect(crumbLabels()).toEqual([LIST_TITLE_COPY, RESULTS_TITLE_COPY]);
            expect(globalStore.trail.at(-1)).toEqual(
                expect.objectContaining({
                    id: 'experiments-results',
                    url: `/dotAdmin/#/experiments/${EXPERIMENT.id}/results`
                })
            );
        });

        // Reached from Configure, so the list is already on the trail and adding it again would
        // put a second copy at the end.
        it('should put itself on top of a trail that already carries the list', () => {
            globalStore.trail.push(
                { id: 'experiments-list', label: LIST_TITLE_COPY },
                { id: 'experiments-configure', label: 'Configure Experiment' }
            );
            spectator = createComponent();

            expect(globalStore.trail.map(({ id }) => id)).toEqual([
                'experiments-list',
                'experiments-configure',
                'experiments-results'
            ]);
        });
    });
});
