import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import {
    DEFAULT_VARIANT_ID,
    DotExperimentStatus,
    DotResultVariant,
    RangeOfDateAndTime,
    SummaryLegend,
    Variant
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

/** Icon the legend carries when the backend did suggest a winner. */
const WINNER_LEGEND_ICON = 'dot-trophy';

/** Explains why the leader may be promoted: the backend already cleared the 95% threshold. */
const THRESHOLD_MET_KEY = 'experiments.results.stat-strip.threshold-met';

/** Explains why there is no leader to promote yet. */
const THRESHOLD_NOT_MET_KEY = 'experiments.results.stat-strip.threshold-not-met';

/**
 * The four numbers a report is read by, in one strip above the charts: the winning or leading
 * variant, the goal being measured, the period measured over, and the sessions counted so far.
 *
 * Purely presentational — the Results shell owns the store and wires every input, so the strip can
 * be rendered from any state, including the ones with nothing to show.
 *
 * The leader is whatever the backend suggested (`winnerLegend` / `suggestedWinner`), never the
 * highest conversion rate: only the backend applies the significance threshold, and a rate-based
 * pick would always name someone, leaving the "no winner yet" state unreachable (AC8).
 */
@Component({
    selector: 'dot-experiments-results-stat-strip',
    imports: [DatePipe, DecimalPipe, ButtonModule, TooltipModule, DotMessagePipe],
    templateUrl: './dot-experiments-results-stat-strip.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsResultsStatStripComponent {
    /** Status of the experiment being reported on; decides Winner vs Leading Variant. */
    $status = input.required<DotExperimentStatus>({ alias: 'status' });

    /** Icon and i18n key for the winner copy, negative states included — never `null` downstream. */
    $winnerLegend = input<SummaryLegend | null>(null, { alias: 'winnerLegend' });

    /** The variant the backend suggested, or `null` when it suggested none. */
    $suggestedWinner = input<DotResultVariant | null>(null, { alias: 'suggestedWinner' });

    /** The already promoted variant, if any: promoting twice is not offered. */
    $promotedVariant = input<Variant | null>(null, { alias: 'promotedVariant' });

    /** Name of the primary goal the experiment measures. */
    $goalName = input<string | null>(null, { alias: 'goalName' });

    /** Start and end of the measured period. */
    $scheduling = input<RangeOfDateAndTime | null>(null, { alias: 'scheduling' });

    /** Sessions counted so far across every variant. */
    $sessionsReached = input<number>(0, { alias: 'sessionsReached' });

    /** Nothing has been measured yet: no winner tile and no refresh control (AC10). */
    $isWaitingForData = input<boolean>(false, { alias: 'isWaitingForData' });

    /** There are results on screen worth re-fetching (AC9). */
    $canRefresh = input<boolean>(false, { alias: 'canRefresh' });

    /** A refresh is on the wire; the control stays closed until it settles. */
    $refreshing = input<boolean>(false, { alias: 'refreshing' });

    /** A mutation is on the wire; Promote stays closed until it settles. */
    $isSaving = input<boolean>(false, { alias: 'isSaving' });

    /** The refresh control was pressed. */
    refreshRequested = output<void>();

    /** Promote was pressed, carrying the id of the variant to publish. */
    promoteRequested = output<string>();

    /** The experiment is over, so its leader is final. */
    protected readonly $isEnded = computed<boolean>(
        () => this.$status() === DotExperimentStatus.ENDED
    );

    /** A winner was suggested — the only state that may claim a leader (AC8). */
    protected readonly $hasSuggestedWinner = computed<boolean>(() => !!this.$suggestedWinner());

    /** The legend names the winner, so the variant's description fills its placeholder. */
    protected readonly $winnerLegendArgs = computed<string[]>(() => [
        this.$suggestedWinner()?.variantDescription ?? ''
    ]);

    /** The trophy belongs to a suggested winner; every other state gets the negative icon. */
    protected readonly $hasWinnerIcon = computed<boolean>(
        () => this.$winnerLegend()?.icon === WINNER_LEGEND_ICON
    );

    protected readonly $thresholdHintKey = computed<string>(() =>
        this.$hasSuggestedWinner() ? THRESHOLD_MET_KEY : THRESHOLD_NOT_MET_KEY
    );

    /** The control is already the published content, and a promoted experiment is settled. */
    protected readonly $canPromote = computed<boolean>(() => {
        const suggestedWinner = this.$suggestedWinner();

        return (
            !!suggestedWinner &&
            suggestedWinner.variantName !== DEFAULT_VARIANT_ID &&
            !this.$promotedVariant()
        );
    });
}
