import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    effect,
    inject,
    input,
    signal,
    untracked
} from '@angular/core';

import { ChartModule } from 'primeng/chart';

import { DotMessagePipe } from '@dotcms/ui';

import {
    SEVERITY_COLOR,
    SEVERITY_LABEL,
    SEVERITY_ORDER,
    type Severity,
    type SeverityCounts
} from '../../models/a11y-severity';
import { StudioPhase } from '../../models/accessibility-studio.models';

/** A severity legend / breakdown row beside the donut. */
interface SeverityRow {
    severity: Severity;
    label: string;
    color: string;
    count: number;
}

/**
 * The score widget at the top of the scanner panel: a severity-segmented donut with
 * the live open count rolling in its center, the before → now delta, and the
 * severity legend.
 *
 * Owns the count-up animation, which is why it's a component rather than markup in
 * the run screen — the rAF handle and its teardown belong with the number they
 * animate.
 */
@Component({
    selector: 'dot-a11y-score',
    imports: [ChartModule, DotMessagePipe],
    templateUrl: './a11y-score.component.html',
    styleUrl: '../studio-fade-in.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'contents' }
})
export class DotA11yScoreComponent {
    /** Drives the headline copy and the skeleton/results swap. */
    readonly phase = input.required<StudioPhase>();

    /** True once a scan has produced results — before that, nothing but the skeleton. */
    readonly hasResults = input(false);

    /** True once a fix run started — keeps the before → now delta and empty buckets on. */
    readonly runStarted = input(false);

    /** Open (unfixed) violation count — the number the ring rolls up to. */
    readonly openCount = input(0);

    /** Violations at the start of the run, for the before → now delta. */
    readonly beforeCount = input(0);

    /** Elements axe flagged but couldn't confirm — surfaced as a note, never fixed. */
    readonly warningCount = input(0);

    /** Open violations per severity — drives both the donut arcs and the legend. */
    readonly severityCounts = input.required<SeverityCounts>();

    readonly #destroyRef = inject(DestroyRef);

    /**
     * The number shown in the ring center. Eased from its previous value up to
     * {@link openCount} whenever a scan resolves (or the count changes while
     * fixing), so the score "rolls" in sync with the donut sweep instead of
     * snapping. See {@link animateCountTo}.
     */
    protected readonly $displayCount = signal(0);

    /** rAF handle for the in-flight count-up, so a new scan can cancel it. */
    #countRaf: number | null = null;

    constructor() {
        // Roll the ring count up to the live open-count whenever it changes and a
        // scan has produced results — the score animates in sync with the donut
        // sweep. Before any results (ready/scanning) it stays parked at 0, so each
        // scan / rescan rolls up fresh.
        effect(() => {
            const target = this.openCount();
            const scanned = this.hasResults();
            untracked(() => this.#animateCountTo(scanned ? target : 0));
        });

        // Cancel any in-flight count-up when the component is torn down.
        this.#destroyRef.onDestroy(() => this.#cancelCount());
    }

    /**
     * Ease {@link displayCount} from its current value to `target` over ~600ms
     * (easeOutCubic), synced with the donut's sweep. Snaps immediately when the
     * user prefers reduced motion or the delta is trivial.
     */
    #animateCountTo(target: number): void {
        this.#cancelCount();

        const from = this.$displayCount();
        if (from === target) {
            return;
        }
        const reduceMotion =
            typeof matchMedia === 'function' &&
            matchMedia('(prefers-reduced-motion: reduce)').matches;
        if (reduceMotion) {
            this.$displayCount.set(target);

            return;
        }

        const duration = 600;
        let start: number | null = null;
        const step = (now: number) => {
            start ??= now;
            const t = Math.min(1, (now - start) / duration);
            const eased = 1 - Math.pow(1 - t, 3); // easeOutCubic
            this.$displayCount.set(Math.round(from + (target - from) * eased));
            if (t < 1) {
                this.#countRaf = requestAnimationFrame(step);
            } else {
                this.#countRaf = null;
            }
        };
        this.#countRaf = requestAnimationFrame(step);
    }

    #cancelCount(): void {
        if (this.#countRaf !== null) {
            cancelAnimationFrame(this.#countRaf);
            this.#countRaf = null;
        }
    }

    /** Headline above the severity legend, by phase. */
    protected readonly $headlineKey = computed<string>(() => {
        if (this.phase() === 'fixing') {
            return 'accessibility.studio.score.fixing';
        }
        if (['done', 'published'].includes(this.phase())) {
            return 'accessibility.studio.score.remaining';
        }
        return 'accessibility.studio.score.found';
    });

    /**
     * Severity legend rows beside the donut (Critical/Serious/Moderate/Minor with
     * their element counts). Drives both the legend and the donut segments. While
     * scanned we hide empty buckets (matches the mockup); once fixing/done we keep
     * them so the user sees a bucket reach 0.
     */
    protected readonly $severityRows = computed<SeverityRow[]>(() => {
        const counts = this.severityCounts();
        const keepZeros = this.runStarted();
        return SEVERITY_ORDER.map((severity) => ({
            severity,
            label: SEVERITY_LABEL[severity],
            color: SEVERITY_COLOR[severity],
            count: counts[severity]
        })).filter((row) => keepZeros || row.count > 0);
    });

    /** PrimeNG doughnut data — one arc per severity, colored by SEVERITY_COLOR. */
    protected readonly $donutData = computed(() => {
        const counts = this.severityCounts();
        const open = this.openCount();
        const total = SEVERITY_ORDER.reduce((sum, s) => sum + counts[s], 0);
        // No open issues → render a single full "clear" ring (green) so the donut
        // still reads as a complete circle rather than collapsing.
        if (total === 0 || open === 0) {
            return {
                labels: ['Clear'],
                datasets: [{ data: [1], backgroundColor: ['#22c55e'], borderWidth: 0 }]
            };
        }
        return {
            labels: SEVERITY_ORDER.map((s) => SEVERITY_LABEL[s]),
            datasets: [
                {
                    data: SEVERITY_ORDER.map((s) => counts[s]),
                    backgroundColor: SEVERITY_ORDER.map((s) => SEVERITY_COLOR[s]),
                    borderWidth: 0
                }
            ]
        };
    });

    /**
     * Doughnut options — thin ring, no legend/tooltip (the center text is overlaid).
     * p-chart is sized via its `width`/`height` inputs (124px square); PrimeNG then
     * sets `maintainAspectRatio: false` itself so the ring fills that square. We
     * don't set responsive/aspect here — letting PrimeNG own the sizing keeps the
     * ring centered in the box, aligned with the absolutely-centered count.
     */
    protected readonly donutOptions = {
        cutout: '74%',
        plugins: { legend: { display: false }, tooltip: { enabled: false } },
        animation: { duration: 500 }
    };
}
