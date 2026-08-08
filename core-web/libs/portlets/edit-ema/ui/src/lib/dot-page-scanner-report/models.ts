import { AxeImpact, AxeNode } from './dot-page-scanner.service';

export type ReportType = 'a11y' | 'geo';

/**
 * UI classification derived from the axe section a rule came from:
 * confirmed `violations` map to `error`, `incomplete` (needs review) to `warning`.
 */
export type A11yFindingType = 'error' | 'warning';

/**
 * A flattened element flagged by an axe rule, ready for display.
 */
export interface A11yGroupItem {
    /** Outer HTML of the offending element. */
    context: string;
    /** First CSS selector axe reported for the element. */
    selector: string;
}

/**
 * One axe rule grouped with every element it flagged.
 */
export interface A11yGroup {
    code: string;
    type: A11yFindingType;
    message: string;
    impact: AxeImpact;
    helpUrl: string;
    items: A11yGroupItem[];
    count: number;
}

/** Re-exported for convenience where node-level data is needed. */
export type { AxeNode };

export interface GeoCategorySignal {
    key: string;
    score: number;
    message: string;
}

export interface GeoCategory {
    key: string;
    label: string;
    score: number;
    weight: number;
    passedCount: number;
    totalCount: number;
    signals: GeoCategorySignal[];
}
