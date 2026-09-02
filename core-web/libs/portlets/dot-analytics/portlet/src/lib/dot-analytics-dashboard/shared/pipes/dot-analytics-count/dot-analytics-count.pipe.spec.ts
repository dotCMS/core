import { createPipeFactory, SpectatorPipe } from '@openng/spectator/jest';

import { DotAnalyticsCountPipe } from './dot-analytics-count.pipe';

describe('DotAnalyticsCountPipe', () => {
    let spectator: SpectatorPipe<DotAnalyticsCountPipe>;

    const createPipe = createPipeFactory({
        pipe: DotAnalyticsCountPipe
    });

    it('should format small values as integers in compact mode', () => {
        spectator = createPipe(`{{ 804 | dotAnalyticsCount }}`);
        expect(spectator.element).toHaveText('804');
    });

    it('should use compact notation for large values', () => {
        spectator = createPipe(`{{ 1128 | dotAnalyticsCount }}`);
        const text = spectator.element.textContent?.trim() ?? '';
        expect(text).not.toBe('1128');
        expect(text).toMatch(/K/i);
    });

    it('should use full grouped format when mode is full', () => {
        spectator = createPipe(`{{ 1128 | dotAnalyticsCount:'full' }}`);
        const text = spectator.element.textContent?.trim() ?? '';
        expect(text).toContain('1');
        expect(text).toContain('128');
        expect(text).not.toMatch(/[kKmM]/);
    });

    it('should return 0 for nullish values', () => {
        spectator = createPipe(`{{ null | dotAnalyticsCount }}`);
        expect(spectator.element).toHaveText('0');
    });
});
