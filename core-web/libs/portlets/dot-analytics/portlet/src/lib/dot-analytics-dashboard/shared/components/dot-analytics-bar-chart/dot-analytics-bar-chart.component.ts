import { signalMethod } from '@ngrx/signals';
import { scaleBand, scaleLinear } from 'd3-scale';
import { select } from 'd3-selection';

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
    EngagementPlatformMetrics
} from '@dotcms/portlets/dot-analytics/data-access';

import { DotAnalyticsEmptyStateComponent } from '../dot-analytics-empty-state/dot-analytics-empty-state.component';
import { DotAnalyticsStateMessageComponent } from '../dot-analytics-state-message/dot-analytics-state-message.component';

/** Color of the primary bar fill (first palette entry). */
const BAR_COLOR = ANALYTICS_CATEGORY_CHART_PALETTE[0];

/** Fixed pixel margins around the SVG drawing area. */
const MARGIN = { top: 8, right: 48, bottom: 24, left: 100 } as const;

/** Y-axis category tick: short horizontal segment from axis toward labels (reference UI). */
const Y_CATEGORY_TICK_X1 = -10;
const Y_CATEGORY_TICK_X2 = 0;

/** Left edge for category labels (text-anchor start); text grows toward the chart. */
const LABEL_ANCHOR_X = -(MARGIN.left - 12);

/** Height (px) of each bar row including its gap. */
const ROW_HEIGHT = 40;

/** Maximum bar thickness (px); bands can be taller when there are few categories. */
const MAX_BAR_THICKNESS = 22;

/** Rect corners: 0 matches reference (sharp); use small radius if product prefers soft bars. */
const BAR_RADIUS = 0;

/** Number of vertical grid ticks on the x-axis. */
const GRID_TICK_COUNT = 5;

interface BarRenderModel {
    svgEl: SVGSVGElement | null;
    containerWidth: number;
    items: EngagementPlatformMetrics[];
    /** Fixed plot height = maxSlots × ROW_HEIGHT; bands use items.length so bar thickness scales with count. */
    maxSlots: number;
}

@Component({
    selector: 'dot-analytics-bar-chart',
    imports: [
        CardModule,
        SkeletonModule,
        DotAnalyticsEmptyStateComponent,
        DotAnalyticsStateMessageComponent
    ],
    templateUrl: './dot-analytics-bar-chart.component.html',
    styleUrl: './dot-analytics-bar-chart.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAnalyticsBarChartComponent {
    readonly #messageService = inject(DotMessageService);
    readonly #destroyRef = inject(DestroyRef);
    readonly #zone = inject(NgZone);

    protected readonly barMeasureRef = viewChild<ElementRef<HTMLElement>>('barMeasure');
    protected readonly barSvgRef = viewChild<ElementRef<SVGSVGElement>>('barSvg');

    readonly $data = input.required<EngagementPlatformMetrics[]>({ alias: 'data' });
    readonly $status = input<ComponentStatus>(ComponentStatus.INIT, { alias: 'status' });
    readonly $title = input<string>('', { alias: 'title' });
    readonly $maxItems = input<number>(5, { alias: 'maxItems' });

    protected readonly $containerWidth = signal(0);

    protected readonly $hoveredBarName = signal<string | null>(null);

    #resizeObserver?: ResizeObserver;

    protected readonly $topItems = computed(() => {
        const data = this.$data();
        const max = this.$maxItems();

        return [...data].sort((a, b) => b.percentage - a.percentage).slice(0, max);
    });

    readonly #syncWidthFromHost = signalMethod<HTMLElement | null>((host) => {
        this.#resizeObserver?.disconnect();
        this.#resizeObserver = undefined;

        if (!host) {
            this.$containerWidth.set(0);
            return;
        }

        const readWidth = () => Math.max(host.getBoundingClientRect().width, host.clientWidth, 0);

        let lastRosWidth = 0;
        const publishWidth = (): void => {
            this.$containerWidth.set(Math.max(lastRosWidth, readWidth()));
        };

        const ro = new ResizeObserver((entries) => {
            lastRosWidth = entries[0]?.contentRect?.width ?? lastRosWidth;
            publishWidth();
        });
        ro.observe(host);
        this.#resizeObserver = ro;
        publishWidth();
    });

    readonly #barRenderModel = computed(
        (): BarRenderModel => ({
            svgEl: this.barSvgRef()?.nativeElement ?? null,
            containerWidth: this.$containerWidth(),
            items: this.$topItems(),
            maxSlots: Math.max(1, this.$maxItems())
        })
    );

    readonly #paintD3Bar = signalMethod<BarRenderModel>((ctx) => {
        const { svgEl, containerWidth: width, items, maxSlots } = ctx;

        if (!svgEl || width <= 0 || !items.length) {
            this.#zone.run(() => this.$hoveredBarName.set(null));
            return;
        }

        const innerWidth = Math.max(width - MARGIN.left - MARGIN.right, 1);
        const innerHeight = maxSlots * ROW_HEIGHT;
        const svgHeight = innerHeight + MARGIN.top + MARGIN.bottom;

        const xScale = scaleLinear()
            .domain([0, Math.max(...items.map((d) => d.percentage), 1)])
            .range([0, innerWidth]);

        const slotDomain = Array.from({ length: items.length }, (_, i) => i);
        const yScale = scaleBand<number>()
            .domain(slotDomain)
            .range([0, innerHeight])
            .paddingInner(0.35)
            .paddingOuter(0.15);

        const bandThickness = yScale.bandwidth();
        const barHeight = Math.min(bandThickness, MAX_BAR_THICKNESS);
        const barTop = (i: number): number => (yScale(i) ?? 0) + (bandThickness - barHeight) / 2;
        const barCenterY = (i: number): number => barTop(i) + barHeight / 2;

        const svg = select(svgEl);
        svg.selectAll('*').remove();
        svg.attr('width', width).attr('height', svgHeight);

        this.#zone.run(() => this.$hoveredBarName.set(null));

        const g = svg.append('g').attr('transform', `translate(${MARGIN.left},${MARGIN.top})`);

        const gridTicks = xScale.ticks(GRID_TICK_COUNT);

        // Vertical grid lines (behind bars)
        g.selectAll<SVGLineElement, number>('line.grid')
            .data(gridTicks)
            .join('line')
            .attr('class', 'grid')
            .attr('x1', (d) => xScale(d))
            .attr('x2', (d) => xScale(d))
            .attr('y1', 0)
            .attr('y2', innerHeight)
            .attr('stroke', 'var(--p-content-border-color, #e5e7eb)')
            .attr('stroke-width', 1)
            .attr('shape-rendering', 'crispEdges');

        // Y-axis baseline (vertical line at x=0, left edge)
        g.append('line')
            .attr('class', 'y-axis-baseline')
            .attr('x1', 0)
            .attr('x2', 0)
            .attr('y1', 0)
            .attr('y2', innerHeight)
            .attr('stroke', 'var(--p-content-border-color, #e5e7eb)')
            .attr('stroke-width', 1)
            .attr('shape-rendering', 'crispEdges');

        // X-axis tick labels (below chart)
        g.selectAll<SVGTextElement, number>('text.x-tick')
            .data(gridTicks)
            .join('text')
            .attr('class', 'x-tick')
            .attr('x', (d) => xScale(d))
            .attr('y', innerHeight + 16)
            .attr('text-anchor', 'middle')
            .attr('font-size', 11)
            .attr('fill', 'var(--p-text-muted-color, #6b7280)')
            .attr('font-family', 'inherit')
            .text((d) => String(d));

        // Y-axis category ticks (small horizontal dash between label and x=0)
        g.selectAll<SVGLineElement, EngagementPlatformMetrics>('line.y-category-tick')
            .data(items)
            .join('line')
            .attr('class', 'y-category-tick')
            .attr('x1', Y_CATEGORY_TICK_X1)
            .attr('x2', Y_CATEGORY_TICK_X2)
            .attr('y1', (_d, i) => barCenterY(i))
            .attr('y2', (_d, i) => barCenterY(i))
            .attr('stroke', 'var(--p-content-border-color, #e5e7eb)')
            .attr('stroke-width', 1)
            .attr('shape-rendering', 'crispEdges');

        // Y-axis labels — left-aligned (same start x for every row)
        g.selectAll<SVGTextElement, EngagementPlatformMetrics>('text.label')
            .data(items)
            .join('text')
            .attr('class', 'label')
            .attr('x', LABEL_ANCHOR_X)
            .attr('y', (_d, i) => barCenterY(i))
            .attr('dy', '0.35em')
            .attr('text-anchor', 'start')
            .attr('font-size', 12)
            .attr('fill', 'var(--p-text-color, #374151)')
            .attr('font-family', 'inherit')
            .text((d) => d.name);

        // Bars
        const bars = g
            .selectAll<SVGRectElement, EngagementPlatformMetrics>('rect.bar')
            .data(items)
            .join('rect')
            .attr('class', 'bar')
            .attr('x', 0)
            .attr('y', (_d, i) => barTop(i))
            .attr('width', (d) => xScale(d.percentage))
            .attr('height', barHeight)
            .attr('fill', BAR_COLOR)
            .attr('rx', BAR_RADIUS)
            .attr('ry', BAR_RADIUS)
            .attr('data-name', (d) => d.name)
            .style('cursor', 'pointer')
            .style('transition', 'opacity 150ms ease');

        // Value labels (right of bar)
        g.selectAll<SVGTextElement, EngagementPlatformMetrics>('text.value')
            .data(items)
            .join('text')
            .attr('class', 'value')
            .attr('x', (d) => xScale(d.percentage) + 6)
            .attr('y', (_d, i) => barCenterY(i))
            .attr('dy', '0.35em')
            .attr('font-size', 11)
            .attr('fill', 'var(--p-text-muted-color, #6b7280)')
            .attr('font-family', 'inherit')
            .text((d) => `${d.percentage}%`);

        bars.on('mouseover', (event: MouseEvent, d: EngagementPlatformMetrics) => {
            this.#zone.run(() => this.$hoveredBarName.set(d.name));
            g.selectAll<SVGRectElement, EngagementPlatformMetrics>('rect.bar').style(
                'opacity',
                0.4
            );
            select(event.currentTarget as SVGRectElement).style('opacity', 1);
        }).on('mouseout', () => {
            this.#zone.run(() => this.$hoveredBarName.set(null));
            g.selectAll<SVGRectElement, EngagementPlatformMetrics>('rect.bar').style('opacity', 1);
        });
    });

    protected readonly $resolvedCardHeader = computed(() => {
        const title = this.$title();
        if (!title?.trim()) {
            return undefined;
        }

        return this.#messageService.get(title);
    });

    protected readonly $svgHeight = computed(
        () => Math.max(1, this.$maxItems()) * ROW_HEIGHT + MARGIN.top + MARGIN.bottom
    );

    protected readonly $isLoading = computed(() => {
        const status = this.$status();

        return status === ComponentStatus.INIT || status === ComponentStatus.LOADING;
    });

    protected readonly $isError = computed(() => this.$status() === ComponentStatus.ERROR);

    protected readonly $isEmpty = computed(() => this.$topItems().length === 0);

    constructor() {
        this.#destroyRef.onDestroy(() => {
            this.#resizeObserver?.disconnect();
            this.#resizeObserver = undefined;
        });

        this.#syncWidthFromHost(computed(() => this.barMeasureRef()?.nativeElement ?? null));

        this.#paintD3Bar(this.#barRenderModel);
    }
}
