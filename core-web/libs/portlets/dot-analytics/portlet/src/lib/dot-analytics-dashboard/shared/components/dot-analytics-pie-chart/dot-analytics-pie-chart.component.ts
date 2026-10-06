import { signalMethod } from '@ngrx/signals';
import { scaleOrdinal } from 'd3-scale';
import { select } from 'd3-selection';
import { arc, pie, type PieArcDatum } from 'd3-shape';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    ElementRef,
    inject,
    input,
    NgZone,
    signal,
    viewChild
} from '@angular/core';

import { CardModule } from 'primeng/card';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus } from '@dotcms/dotcms-models';
import {
    ANALYTICS_CATEGORY_CHART_PALETTE,
    PieChartEntry
} from '@dotcms/portlets/dot-analytics/data-access';

import { distributePercentages } from '../../utils/dot-analytics.utils';
import { DotAnalyticsEmptyStateComponent } from '../dot-analytics-empty-state/dot-analytics-empty-state.component';
import { DotAnalyticsStateMessageComponent } from '../dot-analytics-state-message/dot-analytics-state-message.component';

const PIE_CHART_HEIGHT_REM = '23.125rem';

/** Padding inside the SVG viewBox (`0 … 100`) so arcs are not clipped by stroke. */
const VIEWBOX_MARGIN = 2;

/** Inputs for D3 paint; recomputed whenever refs, size, or data change. */
interface PieRenderModel {
    svgEl: SVGSVGElement | null;
    measureWidth: number;
    rows: PieChartEntry[];
    scheme: { domain: string[] };
    doughnut: boolean;
}

@Component({
    selector: 'dot-analytics-pie-chart',
    imports: [
        CardModule,
        SkeletonModule,
        DotAnalyticsEmptyStateComponent,
        DotAnalyticsStateMessageComponent
    ],
    templateUrl: './dot-analytics-pie-chart.component.html',
    styleUrl: './dot-analytics-pie-chart.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAnalyticsPieChartComponent {
    readonly #messageService = inject(DotMessageService);
    readonly #destroyRef = inject(DestroyRef);
    readonly #zone = inject(NgZone);

    protected readonly pieMeasureRef = viewChild<ElementRef<HTMLElement>>('pieMeasure');
    protected readonly pieSvgRef = viewChild<ElementRef<SVGSVGElement>>('pieSvg');

    readonly $results = input.required<PieChartEntry[]>({ alias: 'results' });
    readonly $status = input<ComponentStatus>(ComponentStatus.INIT, { alias: 'status' });
    readonly $title = input<string>('', { alias: 'title' });
    readonly $doughnut = input<boolean>(false, { alias: 'doughnut' });
    readonly $showLegend = input<boolean>(true, { alias: 'showLegend' });
    readonly $customHeight = input<string | undefined>(undefined, { alias: 'height' });
    readonly $scheme = input<{ domain: string[] } | undefined>(undefined, { alias: 'scheme' });
    /** Forces the legend position regardless of screen size. When undefined the default
     *  responsive behaviour applies: legend is below on mobile, side on md+. */
    readonly $legendPosition = input<'below' | 'side' | undefined>(undefined, {
        alias: 'legendPosition'
    });

    /** Width (px) of the pie measure box; drives D3 layout. */
    protected readonly $pieMeasureWidth = signal(0);

    /** `PieChartEntry.name` matching the hovered slice — highlights legend row via template. */
    protected readonly $hoveredPieSeriesName = signal<string | null>(null);

    #resizeObserver?: ResizeObserver;

    /** Keeps `$pieMeasureWidth` in sync with `#pieMeasure` via ResizeObserver. */
    readonly #syncMeasureFromHost = signalMethod<HTMLElement | null>((host) => {
        this.#resizeObserver?.disconnect();
        this.#resizeObserver = undefined;

        if (!host) {
            this.$pieMeasureWidth.set(0);
            return;
        }

        const readSyncWidth = () =>
            Math.max(host.getBoundingClientRect().width, host.clientWidth, 0);

        /** Preserve last ResizeObserver entry so a follow-up sync read (often 0 in JSDOM) does not clobber width. */
        let lastRosWidth = 0;
        const publishWidth = (): void => {
            this.$pieMeasureWidth.set(Math.max(lastRosWidth, readSyncWidth()));
        };

        const ro = new ResizeObserver((entries) => {
            lastRosWidth = entries[0]?.contentRect?.width ?? lastRosWidth;
            publishWidth();
        });
        ro.observe(host);
        this.#resizeObserver = ro;
        publishWidth();
    });

    readonly #pieRenderModel = computed(
        (): PieRenderModel => ({
            svgEl: this.pieSvgRef()?.nativeElement ?? null,
            measureWidth: this.$pieMeasureWidth(),
            rows: this.$results(),
            scheme: this.$resolvedScheme(),
            doughnut: this.$doughnut()
        })
    );

    /** Paints D3 arcs when `#pieRenderModel` changes (data, width, SVG ref). */
    readonly #paintD3Pie = signalMethod<PieRenderModel>((ctx) => {
        const { svgEl, measureWidth: width, rows, scheme, doughnut } = ctx;

        if (!svgEl || width <= 0 || !rows.length) {
            this.#zone.run(() => this.$hoveredPieSeriesName.set(null));
            return;
        }

        const colorScale = scaleOrdinal<string, string>()
            .domain(rows.map((r) => r.name))
            .range([...scheme.domain]);

        const pieGenerator = pie<PieChartEntry>()
            .sort(null)
            .value((d) => d.value)
            .padAngle(0);
        const pieData = pieGenerator(rows);

        const outerR = 50 - VIEWBOX_MARGIN;
        const innerR = doughnut ? outerR * 0.55 : 0;

        const arcGenerator = arc<PieArcDatum<PieChartEntry>>()
            .innerRadius(innerR)
            .outerRadius(outerR);

        const svg = select(svgEl);
        svg.selectAll('*').remove();
        svg.attr('viewBox', '-50 -50 100 100').attr('preserveAspectRatio', 'xMidYMid meet');

        this.#zone.run(() => this.$hoveredPieSeriesName.set(null));

        const g = svg.append('g');

        g.selectAll<SVGPathElement, PieArcDatum<PieChartEntry>>('path')
            .data(pieData)
            .join('path')
            .attr('fill', (d) => colorScale(d.data.name))
            .attr('d', arcGenerator)
            .attr('data-series', (d) => d.data.name)
            .attr('stroke', '#ffffff')
            .attr('stroke-width', 0.45)
            .attr('stroke-linejoin', 'round')
            .style('transition', 'opacity 200ms ease')
            .style('cursor', 'pointer')
            .on('mouseover', (event: MouseEvent, d: PieArcDatum<PieChartEntry>) => {
                this.#zone.run(() => this.$hoveredPieSeriesName.set(d.data.name));
                const pathEl = event.currentTarget as SVGPathElement;
                g.selectAll<SVGPathElement, PieArcDatum<PieChartEntry>>('path').style(
                    'opacity',
                    0.4
                );
                select(pathEl).style('opacity', 1);
            })
            .on('mouseout', () => {
                this.#zone.run(() => this.$hoveredPieSeriesName.set(null));
                g.selectAll<SVGPathElement, PieArcDatum<PieChartEntry>>('path').style('opacity', 1);
            });
    });

    /** CSS classes for the pie+legend wrapper, respecting the optional `legendPosition` override. */
    protected readonly $hostLayoutClass = computed(() => {
        const position = this.$legendPosition();
        if (position === 'side') {
            return 'flex-row gap-6 items-center';
        }
        if (position === 'below') {
            return 'flex-col gap-4 items-center';
        }

        return 'flex-col md:flex-row gap-4 md:gap-6 items-center';
    });

    protected readonly $legendRowJustifyGapClass = computed(() => {
        const position = this.$legendPosition();
        if (position === 'below') {
            return 'justify-between gap-4';
        }
        if (position === 'side') {
            return 'justify-start gap-2';
        }

        return 'justify-between md:justify-start gap-4 md:gap-2';
    });

    protected readonly $resolvedCardHeader = computed(() => {
        const title = this.$title();
        if (!title?.trim()) {
            return undefined;
        }

        return this.#messageService.get(title);
    });

    protected readonly $height = computed(() => this.$customHeight() ?? PIE_CHART_HEIGHT_REM);

    protected readonly $resolvedScheme = computed(
        () => this.$scheme() ?? { domain: [...ANALYTICS_CATEGORY_CHART_PALETTE] }
    );

    protected readonly $legendRows = computed(() => {
        const rows = this.$results();
        const scheme = this.$resolvedScheme();
        const colorScale = scaleOrdinal<string, string>()
            .domain(rows.map((r) => r.name))
            .range([...scheme.domain]);
        const percentages = distributePercentages(rows.map((r) => r.value));

        return rows.map((r, i) => {
            return {
                key: `${r.name}-${i}`,
                fullName: r.name,
                label: r.name,
                value: r.value,
                pct: percentages[i],
                color: colorScale(r.name) ?? scheme.domain[i % scheme.domain.length]
            };
        });
    });

    protected readonly $isLoading = computed(() => {
        const status = this.$status();

        return status === ComponentStatus.INIT || status === ComponentStatus.LOADING;
    });

    protected readonly $isError = computed(() => this.$status() === ComponentStatus.ERROR);

    protected readonly $isEmpty = computed(() => this.$results().length === 0);

    protected onLegendRowPointerEnter(fullName: string): void {
        this.$hoveredPieSeriesName.set(fullName);
        const svg = this.pieSvgRef()?.nativeElement;
        if (!svg) {
            return;
        }
        svg.querySelectorAll<SVGPathElement>('path[data-series]').forEach((path) => {
            const matches = path.getAttribute('data-series') === fullName;
            path.style.opacity = matches ? '1' : '0.4';
        });
    }

    protected onLegendRowPointerLeave(): void {
        this.$hoveredPieSeriesName.set(null);
        const svg = this.pieSvgRef()?.nativeElement;
        if (!svg) {
            return;
        }
        svg.querySelectorAll<SVGPathElement>('path[data-series]').forEach((path) => {
            path.style.opacity = '1';
        });
    }

    constructor() {
        this.#destroyRef.onDestroy(() => {
            this.#resizeObserver?.disconnect();
            this.#resizeObserver = undefined;
        });

        this.#syncMeasureFromHost(computed(() => this.pieMeasureRef()?.nativeElement ?? null));

        this.#paintD3Pie(this.#pieRenderModel);
    }
}
