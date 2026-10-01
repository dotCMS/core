import { convertToParamMap } from '@angular/router';

import { getProperQueryParamsFromUrl } from './state-from-url';

describe('Analytics custom date range URL state', () => {
    it('restores a today-only custom range rather than resetting to last7days', () => {
        const result = getProperQueryParamsFromUrl(
            convertToParamMap({
                time_range: 'custom',
                from: '2024-01-15',
                to: '2024-01-15'
            })
        );

        expect(result).toEqual({
            type: 'timeRange',
            timeRange: ['2024-01-15', '2024-01-15']
        });
    });

    it('restores a custom range shorter than seven days', () => {
        const result = getProperQueryParamsFromUrl(
            convertToParamMap({
                time_range: 'custom',
                from: '2024-01-14',
                to: '2024-01-15'
            })
        );

        expect(result).toEqual({
            type: 'timeRange',
            timeRange: ['2024-01-14', '2024-01-15']
        });
    });

    it.each([
        { from: '2024-01-16', to: '2024-01-15' },
        { from: 'invalid-date', to: '2024-01-15' },
        { from: '2024-01-15' }
    ])('rejects invalid, inverted, or incomplete custom dates: %j', (dates) => {
        const result = getProperQueryParamsFromUrl(
            convertToParamMap({ time_range: 'custom', ...dates })
        );

        expect(result).toEqual({ type: 'params', params: { time_range: 'last7days' } });
    });
});
