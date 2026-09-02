import { injectDispatch } from '@ngrx/signals/events';

import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { DotMessageService } from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID, DotExperimentStatus } from '@dotcms/dotcms-models';
import { DotEmptyContainerComponent, DotMessagePipe, PrincipalConfiguration } from '@dotcms/ui';

import { DotExperimentResultVariantDetail, LiftTone } from '../../../shared/models';
import { dotExperimentsResultsPageEvents } from '../../../store/dot-experiments-results-page.events';
import { DotExperimentsResultsStore } from '../../../store/dot-experiments-results.store';

/**
 * Row dot colours, by row position. The control is always drawn first, so it always reads in the
 * first colour — the same one the charts give it. Beyond the fifth variant the palette repeats.
 */
const VARIANT_COLORS: readonly string[] = ['#0ea5e9', '#a855f7', '#fb923c', '#22c55e', '#f43f5e'];

/** How a Lift vs Original reads: nothing to compare against, a gain, or a loss (AC16). */
const LIFT_TONE_CLASSES: Record<LiftTone, string> = {
    neutral: 'text-surface-400',
    positive: 'text-green-800',
    negative: 'text-red-700'
};

/**
 * Promote confirm copy. The first sentence says whether the result is worth promoting, and the
 * second is appended only while the experiment is still RUNNING, because promoting one ends it
 * (AC19).
 */
const PROMOTE_CONFIRM_KEYS = {
    header: 'experiments.results.promote.confirm.header',
    aboveThreshold: 'experiments.results.promote.confirm.above-threshold',
    belowThreshold: 'experiments.results.promote.confirm.below-threshold',
    endsExperiment: 'experiments.results.promote.confirm.ends-experiment'
} as const;

/** A summary-table row, plus everything the template would otherwise have to derive per row. */
export interface DotExperimentsSummaryTableRow extends DotExperimentResultVariantDetail {
    /** Colour of the row's dot, matching the variant's chart series. */
    color: string;
    /** True for the `DEFAULT` variant, which is never promoted and has no lift of its own. */
    isControl: boolean;
    /** Text colour the Lift vs Original is rendered in, resolved from its tone. */
    liftClass: string;
}

/**
 * Summary table of the Results screen: one row per variant of the primary goal.
 *
 * It renders under both chart tabs and reads everything from `DotExperimentsResultsStore`, so the
 * shell places it without wiring anything through. A plain `p-table`: real table semantics, and
 * the column widths come from the content rather than from a twelve-column grid the eight columns
 * never divided evenly.
 *
 * The gate on the data is experiment-wide: below the session threshold the whole table is replaced
 * by one empty state, and above it every row shows its full data however few sessions it saw. No
 * row is ever filtered out on its own count (AC15).
 *
 * The Promote confirmation lives here, next to the button that opens it, and is answered by
 * dispatching `promoteRequested`: promoting a RUNNING experiment ends it in the same backend call,
 * so the confirm says so beforehand and nothing else is dispatched afterwards (AC19/AC20).
 */
@Component({
    selector: 'dot-experiments-results-summary-table',
    imports: [
        ButtonModule,
        ConfirmDialogModule,
        SkeletonModule,
        TableModule,
        TagModule,
        DotEmptyContainerComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-experiments-results-summary-table.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    // Its own instance, so this confirmation and the shell's Stop confirmation never answer for
    // each other — they are two dialogs of the same kind, opened from two different places (AC21).
    providers: [ConfirmationService]
})
export class DotExperimentsResultsSummaryTableComponent {
    readonly store = inject(DotExperimentsResultsStore);

    /**
     * Rows as they are drawn: the control first, then the variants in the order the results name
     * them. `$detailData` arrives in `Object.values()` order, which guarantees nothing, and the
     * sort is presentation only — every row's lift is measured against the control by key,
     * whichever position it happens to arrive in.
     */
    readonly $rows = computed<DotExperimentsSummaryTableRow[]>(() =>
        [...this.store.$detailData()]
            .sort(
                (first, second) =>
                    Number(second.id === DEFAULT_VARIANT_ID) -
                    Number(first.id === DEFAULT_VARIANT_ID)
            )
            .map((row, index) => ({
                ...row,
                color: VARIANT_COLORS[index % VARIANT_COLORS.length],
                isControl: row.id === DEFAULT_VARIANT_ID,
                liftClass: LIFT_TONE_CLASSES[row.liftTone]
            }))
    );

    /** Column widths follow the content; `auto` lets the browser weigh the headers against it. */
    readonly TABLE_STYLE = { 'min-width': '100%', 'table-layout': 'auto' };

    /**
     * What the table renders.
     *
     * The session gate empties it rather than hiding it, so the "not enough sessions" copy arrives
     * through `p-table`'s own empty template instead of a second branch that has to be kept in
     * step with the table beside it.
     */
    readonly $tableRows = computed<DotExperimentsSummaryTableRow[]>(() =>
        this.store.$hasEnoughSessionsForTable() ? this.$rows() : []
    );

    /** One promotion is all there is: once any variant has been promoted, no row offers it (AC17). */
    readonly $canPromote = computed<boolean>(() => !this.store.$promotedVariant());

    readonly #dispatch = injectDispatch(dotExperimentsResultsPageEvents);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #dotMessageService = inject(DotMessageService);

    /**
     * Copy for the below-threshold state, in the shared empty container the rest of the screen
     * already uses for its error and misconfiguration states.
     */
    readonly EMPTY_CONFIGURATION: PrincipalConfiguration = {
        title: this.#dotMessageService.get('experiments.reports.summary.empty.title'),
        subtitle: this.#dotMessageService.get('experiments.reports.summary.empty.description'),
        icon: 'table_chart',
        iconStyle: 'material-symbols-rounded'
    };

    /**
     * Asks before promoting, and says what promoting will cost: while the experiment is RUNNING the
     * same call ends it, which the copy states outright and omits once it has already ended (AC19).
     *
     * @param row - The row whose Promote button was pressed
     */
    promoteVariant(row: DotExperimentsSummaryTableRow): void {
        this.#confirmationService.confirm({
            header: this.#dotMessageService.get(PROMOTE_CONFIRM_KEYS.header),
            message: this.#buildConfirmMessage(row),
            acceptLabel: this.#dotMessageService.get('experiments.reports.promote'),
            rejectLabel: this.#dotMessageService.get('dot.common.dialog.reject'),
            rejectButtonStyleClass: 'p-button-secondary',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => this.#dispatch.promoteRequested(row.id)
        });
    }

    /**
     * Whether the row clears the significance threshold is the backend's call, not a comparison of
     * rendered percentages: `isWinner` is the suggested winner it named, and only it applies the
     * threshold.
     */
    #buildConfirmMessage(row: DotExperimentsSummaryTableRow): string {
        const threshold = this.#dotMessageService.get(
            row.isWinner ? PROMOTE_CONFIRM_KEYS.aboveThreshold : PROMOTE_CONFIRM_KEYS.belowThreshold
        );

        if (this.store.$status() !== DotExperimentStatus.RUNNING) {
            return threshold;
        }

        return `${threshold} ${this.#dotMessageService.get(PROMOTE_CONFIRM_KEYS.endsExperiment)}`;
    }
}
