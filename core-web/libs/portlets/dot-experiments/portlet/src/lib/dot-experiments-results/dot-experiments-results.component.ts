import { Events, injectDispatch } from '@ngrx/signals/events';

import { Component, computed, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService } from 'primeng/api';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { SkeletonModule } from 'primeng/skeleton';

import { map } from 'rxjs/operators';

import { DotMessageDisplayService, DotMessageService } from '@dotcms/data-access';
import {
    ComponentStatus,
    DotExperiment,
    DotExperimentStatus,
    DotMessageSeverity,
    DotMessageType,
    HealthStatusTypes,
    MINIMUM_SESSIONS_TO_SHOW_CHART
} from '@dotcms/dotcms-models';
import { DotEmptyContainerComponent, DotMessagePipe, PrincipalConfiguration } from '@dotcms/ui';

import { DotExperimentsResultsChartsComponent } from './components/dot-experiments-results-charts/dot-experiments-results-charts.component';
import { DotExperimentsResultsEmptyComponent } from './components/dot-experiments-results-empty/dot-experiments-results-empty.component';
import { DotExperimentsResultsHeaderComponent } from './components/dot-experiments-results-header/dot-experiments-results-header.component';
import { DotExperimentsResultsStatStripComponent } from './components/dot-experiments-results-stat-strip/dot-experiments-results-stat-strip.component';
import { DotExperimentsResultsSummaryTableComponent } from './components/dot-experiments-results-summary-table/dot-experiments-results-summary-table.component';

import {
    EXPERIMENTS_URL,
    RESULTS_CONFIRM_DIALOG_KEY,
    SUCCESS_MESSAGE_LIFE
} from '../shared/constants';
import { dotExperimentsResultsApiEvents } from '../store/dot-experiments-results-api.events';
import { dotExperimentsResultsPageEvents } from '../store/dot-experiments-results-page.events';
import { DotExperimentsResultsStore } from '../store/dot-experiments-results.store';

/** Route `data` key `dotAnalyticsHealthCheckResolver` publishes the analytics health under. */
const HEALTH_STATUS_ROUTE_DATA_KEY = 'healthStatus';

/** Route parameter naming the experiment being reported on. */
const EXPERIMENT_ID_ROUTE_PARAM = 'experimentId';

/**
 * Shell of the Results screen, routed on `/experiments/:experimentId/results`.
 *
 * It owns the fixed-height layout the report sits in — a header that stays put over a scrolling
 * body — and everything that is screen-wide rather than panel-wide: which of the four states the
 * screen is in, the Stop confirmation's dialog, and the toasts that follow a mutation.
 *
 * The four states are deliberately exclusive, in this order: a misconfigured analytics app, which
 * takes out this screen and only this screen (AC22); a *first* load that failed, which is the one
 * failure with nothing to show behind it and therefore the only one that blanks the screen
 * (AC24); the first load itself, drawn as a skeleton of the report to come (AC23); and the report.
 * An experiment whose report did not load is none of them — everything the experiment alone can
 * draw stays where it is and the screen says the report is missing in a banner over it.
 *
 * Which experiment to show is not read here: the store follows the route itself, so the shell only
 * provides it. `DotExperimentsService` is not provided either — the route provides it for the
 * health resolver, and the route injector is this component's parent.
 */
@Component({
    selector: 'dot-experiments-results',
    imports: [
        ConfirmDialogModule,
        SkeletonModule,
        DotEmptyContainerComponent,
        DotExperimentsResultsEmptyComponent,
        DotMessagePipe,
        DotExperimentsResultsHeaderComponent,
        DotExperimentsResultsStatStripComponent,
        DotExperimentsResultsChartsComponent,
        DotExperimentsResultsSummaryTableComponent
    ],
    templateUrl: './dot-experiments-results.component.html',
    styleUrl: './dot-experiments-results.component.scss',
    providers: [DotExperimentsResultsStore, ConfirmationService],
    host: { class: 'flex flex-col h-full min-h-0 overflow-hidden' }
})
export class DotExperimentsResultsComponent {
    readonly store = inject(DotExperimentsResultsStore);

    readonly CONFIRM_KEY = RESULTS_CONFIRM_DIALOG_KEY;

    readonly #route = inject(ActivatedRoute);
    readonly #router = inject(Router);
    readonly #events = inject(Events);
    readonly #dispatch = injectDispatch(dotExperimentsResultsPageEvents);
    readonly #destroyRef = inject(DestroyRef);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dotMessageDisplayService = inject(DotMessageDisplayService);
    readonly #confirmationService = inject(ConfirmationService);

    /**
     * Analytics health, as the route resolved it.
     *
     * Followed rather than read once from the snapshot, for the same reason the store follows
     * `paramMap`: the component is reused across experiments, and the resolver runs again on each
     * of them.
     */
    readonly #healthStatus = toSignal(
        this.#route.data.pipe(
            map((data) => data[HEALTH_STATUS_ROUTE_DATA_KEY] as HealthStatusTypes | undefined)
        )
    );

    /** Anything but `OK` means the report cannot be trusted, so none of it is shown (AC22). */
    readonly $isMisconfigured = computed<boolean>(() => {
        const healthStatus = this.#healthStatus();

        return !!healthStatus && healthStatus !== HealthStatusTypes.OK;
    });

    /**
     * The first load is still out. `INIT` counts: the store reads the route in its `onInit`, so the
     * screen spends a tick there before the load starts, and treating it as loaded would flash an
     * empty report first.
     */
    readonly $isLoading = computed<boolean>(() => {
        const status = this.store.status();

        return status === ComponentStatus.INIT || status === ComponentStatus.LOADING;
    });

    /**
     * Copy shown instead of the report when Analytics is not usable. Mirrors the list screen's
     * inline state: only `NOT_CONFIGURED` means "never set up", every other non-OK status is a
     * broken configuration.
     */
    readonly $misconfiguredConfiguration = computed<PrincipalConfiguration>(() => {
        const isNotConfigured = this.#healthStatus() === HealthStatusTypes.NOT_CONFIGURED;

        return {
            title: this.#dotMessageService.get(
                isNotConfigured
                    ? 'experiments.analytics-app-no-configured.title'
                    : 'experiments.analytics-app-misconfiguration.title'
            ),
            subtitle: this.#dotMessageService.get(
                isNotConfigured
                    ? 'experiments.analytics-app-no-configured.subtitle'
                    : 'experiments.analytics-app-misconfiguration.subtitle'
            ),
            icon: 'analytics',
            iconStyle: 'material-symbols-rounded'
        };
    });

    /**
     * Shown when nothing could be loaded. The error itself is already surfaced by
     * `DotHttpErrorManagerService`; this is the screen's own state, so a failed load reads as a
     * failure with a way out rather than as an empty report (AC24).
     */
    readonly errorConfiguration: PrincipalConfiguration = {
        title: this.#dotMessageService.get('experiments.results.error.title'),
        subtitle: this.#dotMessageService.get('experiments.error.fetching.data'),
        icon: 'error',
        iconStyle: 'material-symbols-rounded'
    };

    /**
     * A report with nothing in it is one empty state for the whole body, not one per block: below
     * the session threshold the charts and the summary each say the same thing, and two "not enough
     * data" panels stacked read as two failures rather than as one Experiment that has simply not
     * run long enough.
     */
    readonly $hasNothingToReport = computed<boolean>(
        () => !this.store.$hasEnoughSessionsForTable()
    );

    readonly $emptyReportTitle = this.#dotMessageService.get('experiments.results.empty.title');

    /**
     * Why the body is empty, in the order the reasons override each other.
     *
     * A failed report comes first because it is the one case the other two would misdescribe. The
     * screen settles as LOADED with `results: null`, which leaves `$hasEnoughSessionsForTable()`
     * false through no fault of the session count — so without this branch a load failure read as
     * "needs N sessions", directly under a banner saying the report could not be loaded.
     */
    readonly $emptyReportSubtitle = computed<string>(() => {
        if (this.store.reportUnavailable()) {
            return this.#dotMessageService.get('experiments.results.empty.unavailable.description');
        }

        return this.store.$isWaitingForData()
            ? this.#dotMessageService.get('experiments.results.empty.not-started.description')
            : this.#dotMessageService.get(
                  'experiments.results.empty.description',
                  String(MINIMUM_SESSIONS_TO_SHOW_CHART)
              );
    });

    constructor() {
        this.#listenForActionSuccess();
    }

    /** Leaves the Results screen for the list. */
    onBackToList(): void {
        this.#router.navigate([EXPERIMENTS_URL]);
    }

    /** Runs the whole load again, experiment included: a failed load left nothing behind. */
    onRetry(): void {
        const experimentId = this.#route.snapshot.paramMap.get(EXPERIMENT_ID_ROUTE_PARAM);

        if (experimentId) {
            this.#dispatch.enter(experimentId);
        }
    }

    /**
     * Promotes the variant the stat strip offers inline when the leader is not the control (AC7).
     *
     * Asks first, exactly as the per-row Promote in the summary table does: the strip's button is a
     * shortcut to the same irreversible action, so it cannot be the one path that skips the
     * confirmation (AC19/AC21). The strip is presentational and raises the intent; the decision and
     * the dialog live here, on the component that owns the keyed `p-confirmDialog`.
     *
     * The leader the strip offers is the backend's suggested winner, which is what clears the
     * threshold — hence the above-threshold copy. While the experiment is RUNNING the same call
     * ends it, which the copy states outright.
     *
     * @param variantId - Id of the variant to promote
     */
    onPromote(variantId: string): void {
        const endsExperiment = this.store.$status() === DotExperimentStatus.RUNNING;
        const threshold = this.#dotMessageService.get(
            'experiments.results.promote.confirm.above-threshold'
        );

        this.#confirmationService.confirm({
            key: RESULTS_CONFIRM_DIALOG_KEY,
            header: this.#dotMessageService.get('experiments.results.promote.confirm.header'),
            message: endsExperiment
                ? `${threshold} ${this.#dotMessageService.get('experiments.results.promote.confirm.ends-experiment')}`
                : threshold,
            acceptLabel: this.#dotMessageService.get('experiments.reports.promote'),
            rejectLabel: this.#dotMessageService.get('dot.common.dialog.reject'),
            rejectButtonStyleClass: 'p-button-secondary',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => this.#dispatch.promoteRequested(variantId)
        });
    }

    /**
     * The store persists and reloads on its own; the toast is a UI concern and therefore lives
     * here. Only the outcomes the user asked for get one — a failed call is already reported by
     * `DotHttpErrorManagerService` inside the store.
     *
     * Promoting a RUNNING experiment ends it in the same call, which the confirmation warns about
     * beforehand, so the success is one toast and not two.
     */
    #listenForActionSuccess(): void {
        this.#events
            .on(dotExperimentsResultsApiEvents.stopSucceeded)
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe(({ payload }) =>
                this.#pushSuccess('experiments.action.stop.confirm-message', payload.name)
            );

        this.#events
            .on(dotExperimentsResultsApiEvents.promoteSucceeded)
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe(({ payload }) =>
                this.#pushSuccess(
                    'experiments.action.promote.variant.confirm-message',
                    this.#promotedVariantNameOf(payload)
                )
            );
    }

    /** The variant the answered experiment now carries as promoted, which the toast names. */
    #promotedVariantNameOf({ trafficProportion }: DotExperiment): string {
        return trafficProportion?.variants.find(({ promoted }) => promoted)?.name ?? '';
    }

    #pushSuccess(messageKey: string, argument: string): void {
        this.#dotMessageDisplayService.push({
            life: SUCCESS_MESSAGE_LIFE,
            severity: DotMessageSeverity.SUCCESS,
            message: this.#dotMessageService.get(messageKey, argument),
            type: DotMessageType.SIMPLE_MESSAGE
        });
    }
}
