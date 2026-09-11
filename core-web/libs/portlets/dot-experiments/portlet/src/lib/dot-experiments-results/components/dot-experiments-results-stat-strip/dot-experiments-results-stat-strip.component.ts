import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import {
    DEFAULT_VARIANT_ID,
    DotExperimentStatus,
    DotResultVariant,
    RangeOfDateAndTime,
    Variant
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

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
 * The leader is whatever the backend suggested (`suggestedWinner`), never the highest conversion
 * rate: only the backend applies the significance threshold, and a rate-based pick would always
 * name someone, leaving the "no winner yet" state unreachable (AC8).
 */
@Component({
    selector: 'dot-experiments-results-stat-strip',
    imports: [DatePipe, DecimalPipe, ButtonModule, TooltipModule, DotMessagePipe],
    templateUrl: './dot-experiments-results-stat-strip.component.html'
})
export class DotExperimentsResultsStatStripComponent {
    /** Status of the experiment being reported on; decides Winner vs Leading Variant. */
    readonly $status = input.required<DotExperimentStatus>({ alias: 'status' });

    /** The variant the backend suggested, or `null` when it suggested none. */
    readonly $suggestedWinner = input<DotResultVariant | null>(null, { alias: 'suggestedWinner' });

    /** The already promoted variant, if any: promoting twice is not offered. */
    readonly $promotedVariant = input<Variant | null>(null, { alias: 'promotedVariant' });

    /** Name of the primary goal the experiment measures. */
    readonly $goalName = input<string | null>(null, { alias: 'goalName' });

    /** Start and end of the measured period. */
    readonly $scheduling = input<RangeOfDateAndTime | null>(null, { alias: 'scheduling' });

    /** Sessions counted so far across every variant. */
    readonly $sessionsReached = input<number>(0, { alias: 'sessionsReached' });

    /** Nothing has been measured yet, so there is no leader to name (AC10). */
    readonly $isWaitingForData = input<boolean>(false, { alias: 'isWaitingForData' });

    /** A mutation is on the wire; Promote stays closed until it settles. */
    readonly $isSaving = input<boolean>(false, { alias: 'isSaving' });

    /** Promote was pressed, carrying the id of the variant to publish. */
    readonly promoteRequested = output<string>();

    /** The experiment is over, so its leader is final. */
    protected readonly $isEnded = computed<boolean>(
        () => this.$status() === DotExperimentStatus.ENDED
    );

    /** A winner was suggested — the only state that may claim a leader (AC8). */
    protected readonly $hasSuggestedWinner = computed<boolean>(() => !!this.$suggestedWinner());

    /**
     * The leader, named.
     *
     * The design puts the variant's own name in the cell rather than a sentence about it, so the
     * only state left to express is having no leader at all — an em dash, the same fallback the
     * Goal and Period cells use. Why it leads, and whether the lead has cleared the significance
     * threshold, is what the tooltip carries.
     */
    protected readonly $leadingVariantName = computed<string>(
        () => this.$suggestedWinner()?.variantDescription ?? '—'
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
