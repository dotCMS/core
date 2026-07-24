import { AxeImpact } from '@dotcms/portlets/dot-ema/ui';

import { A11yGroup } from './a11y-groups';

/**
 * Severity model for the Studio score widget + issue-type list. Mirrors axe-core's
 * four impact levels; the donut ring + legend are colored by these. A null impact
 * (axe occasionally omits it) buckets to the lowest, `minor`, so every issue is
 * always counted somewhere.
 */
export type Severity = 'critical' | 'serious' | 'moderate' | 'minor';

/** Highest → lowest. Drives legend order, ring segment order, and list sort. */
export const SEVERITY_ORDER: readonly Severity[] = ['critical', 'serious', 'moderate', 'minor'];

export const SEVERITY_LABEL: Record<Severity, string> = {
    critical: 'Critical',
    serious: 'Serious',
    moderate: 'Moderate',
    minor: 'Minor'
};

/**
 * Hex colors for each severity — used for the donut segments and the legend/list
 * dots. Taken from the Accessibility Studio design (.dc.html): red / orange /
 * amber / slate-gray.
 */
export const SEVERITY_COLOR: Record<Severity, string> = {
    critical: '#e0314f',
    serious: '#f06a1e',
    moderate: '#e8b838',
    minor: '#94a3b8'
};

/** Map an axe impact (or null) to a Severity bucket — null → minor. */
export function impactToSeverity(impact: AxeImpact | null): Severity {
    switch (impact) {
        case 'critical':
            return 'critical';
        case 'serious':
            return 'serious';
        case 'moderate':
            return 'moderate';
        default:
            return 'minor';
    }
}

export type SeverityCounts = Record<Severity, number>;

const EMPTY_COUNTS = (): SeverityCounts => ({ critical: 0, serious: 0, moderate: 0, minor: 0 });

/**
 * Sum the flagged ELEMENT counts per severity across the given groups (use the
 * `error` groups — confirmed violations — for the "open issues" breakdown).
 */
export function severityBreakdown(groups: A11yGroup[]): SeverityCounts {
    return groups.reduce<SeverityCounts>((acc, g) => {
        acc[impactToSeverity(g.impact)] += g.count;
        return acc;
    }, EMPTY_COUNTS());
}
