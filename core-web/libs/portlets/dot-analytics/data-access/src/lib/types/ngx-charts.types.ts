/**
 * Shape for `@swimlane/ngx-charts` single-series charts (pie, advanced pie grid, etc.)
 * @see https://swimlane.gitbook.io/ngx-charts/examples/pie-charts/pie-chart
 */
export interface NgxChartsPieEntry {
    /** Display label (shown in legend and tooltips). */
    name: string;
    /** Numeric value for the slice size. */
    value: number;
    /** Optional extra payload for tooltip templates. */
    extra?: Record<string, unknown>;
}
