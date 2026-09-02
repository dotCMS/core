import { HttpErrorResponse } from '@angular/common/http';

import type { TrafficVsConversionsDayData } from './analytics-data.utils';
import type { UniqueVisitorsByDayData } from '../../types';

/**
 * Joins visitor and converting-series by calendar day so index misalignment cannot skew results
 * when one series has sparse days.
 */
export function zipDailyUniqueVisitorsForTrafficChart(
    visitors: UniqueVisitorsByDayData[],
    converting: UniqueVisitorsByDayData[]
): TrafficVsConversionsDayData[] {
    const convertingByDay = new Map(converting.map((c) => [c.day, c.uniqueVisitors]));

    return visitors.map((v) => ({
        day: v.day,
        uniqueVisitors: v.uniqueVisitors,
        uniqueConvertingVisitors: convertingByDay.get(v.day) ?? 0
    }));
}

/**
 * Extract a plain-text error body from HttpErrorResponse for user-facing fallback messages.
 * Returns null for HTML payloads (common proxy/HTML error pages) so callers can fall back to i18n.
 */
export function analyticsResponseBodyMessage(error: HttpErrorResponse): string | null {
    const body = error.error;
    if (typeof body === 'string' && body.trim()) {
        const trimmed = body.trim();
        if (trimmed.startsWith('<')) {
            return null;
        }
        return trimmed;
    }
    if (body && typeof body === 'object' && 'message' in body) {
        const m = (body as { message: unknown }).message;
        if (typeof m === 'string' && m.trim()) {
            return m.trim();
        }
    }
    return null;
}
