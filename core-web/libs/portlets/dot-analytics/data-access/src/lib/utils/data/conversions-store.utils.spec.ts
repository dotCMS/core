import { HttpErrorResponse } from '@angular/common/http';

import {
    analyticsResponseBodyMessage,
    zipDailyUniqueVisitorsForTrafficChart
} from './conversions-store.utils';

describe('conversions-store.utils', () => {
    describe('zipDailyUniqueVisitorsForTrafficChart', () => {
        it('should align converting visitors by day when series have different calendar coverage', () => {
            const visitors = [
                { day: '2026-01-01', uniqueVisitors: 100 },
                { day: '2026-01-03', uniqueVisitors: 50 }
            ];
            const converting = [{ day: '2026-01-01', uniqueVisitors: 10 }];

            expect(zipDailyUniqueVisitorsForTrafficChart(visitors, converting)).toEqual([
                { day: '2026-01-01', uniqueVisitors: 100, uniqueConvertingVisitors: 10 },
                { day: '2026-01-03', uniqueVisitors: 50, uniqueConvertingVisitors: 0 }
            ]);
        });
    });

    describe('analyticsResponseBodyMessage', () => {
        it('should return null when error body is HTML', () => {
            const error = new HttpErrorResponse({
                status: 502,
                error: '<html><body>Bad Gateway</body></html>'
            });

            expect(analyticsResponseBodyMessage(error)).toBeNull();
        });

        it('should return trimmed string body when not HTML', () => {
            const error = new HttpErrorResponse({
                status: 400,
                error: '  plain message  '
            });

            expect(analyticsResponseBodyMessage(error)).toBe('plain message');
        });
    });
});
