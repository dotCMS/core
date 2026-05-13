/**
 * Single-series pie slice shape consumed by the analytics D3 pie chart (`dot-analytics-pie-chart`).
 */
export interface PieChartEntry {
    /** Display label (shown in legend and tooltips). */
    name: string;
    /** Numeric value for the slice size. */
    value: number;
    /** Optional extra payload for tooltip templates. */
    extra?: Record<string, unknown>;
}
