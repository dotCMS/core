export type AnalyticsCountFormatMode = 'compact' | 'full';

/**
 * Formats analytics session/view counts for display.
 * - compact: integers below 1000; compact notation (e.g. 1.1K) from 1000+
 * - full: grouped digits without decimals (e.g. 1,128) for tooltips and detail tables
 */
export function formatAnalyticsCount(
    value: number,
    mode: AnalyticsCountFormatMode = 'compact'
): string {
    if (!Number.isFinite(value)) {
        return '0';
    }

    const rounded = Math.round(value);

    if (mode === 'full') {
        return new Intl.NumberFormat(undefined, { maximumFractionDigits: 0 }).format(rounded);
    }

    if (rounded <= 0) {
        return '0';
    }

    if (rounded < 1000) {
        return String(rounded);
    }

    try {
        return new Intl.NumberFormat(undefined, {
            notation: 'compact',
            compactDisplay: 'short',
            maximumFractionDigits: 1
        }).format(rounded);
    } catch {
        if (rounded >= 1_000_000) {
            return `${(rounded / 1_000_000).toFixed(1)}M`;
        }
        if (rounded >= 1_000) {
            return `${(rounded / 1_000).toFixed(1)}K`;
        }
        return String(rounded);
    }
}
