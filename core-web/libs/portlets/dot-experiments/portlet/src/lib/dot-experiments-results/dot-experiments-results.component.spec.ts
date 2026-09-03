import { provideDispatcher } from '@ngrx/signals/events';
import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ConfirmationService } from 'primeng/api';

import { DotMessageDisplayService, DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsResultsComponent } from './dot-experiments-results.component';

import { DotExperimentsResultsStore } from '../store/dot-experiments-results.store';

const NOT_STARTED_COPY = 'This Experiment has not started collecting sessions yet.';
const NOT_ENOUGH_SESSIONS_COPY = 'An Experiment needs at least 10 sessions.';
const UNAVAILABLE_COPY = 'The report could not be loaded, so there are no numbers to show here.';

const messageServiceMock = new MockDotMessageService({
    'experiments.results.empty.title': 'No results to report yet',
    'experiments.results.empty.description': NOT_ENOUGH_SESSIONS_COPY,
    'experiments.results.empty.not-started.description': NOT_STARTED_COPY,
    'experiments.results.empty.unavailable.description': UNAVAILABLE_COPY
});

/** Real signals rather than `jest.fn()`, for the reason the summary-table spec gives. */
const createStoreMock = () => ({
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
describe('DotExperimentsResultsComponent', () => {
    let spectator: Spectator<DotExperimentsResultsComponent>;
    let storeMock: ReturnType<typeof createStoreMock>;

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
                useValue: { data: of({}), snapshot: { paramMap: new Map() } }
            },
            mockProvider(DotMessageDisplayService)
        ],
        detectChanges: false
    });

    beforeEach(() => {
        storeMock = createStoreMock();
        spectator = createComponent();
    });

    describe('why the report is empty', () => {
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
});
