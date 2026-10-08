import { describe, expect, it, vi } from 'vitest';

const cleanupActivityTracking = vi.hoisted(() => vi.fn());

vi.mock('./activity', () => ({
    cleanupActivityTracking,
    initializeActivityTracking: vi.fn(),
    updateSessionActivity: vi.fn()
}));

import { identityPlugin } from './plugin';

import type { PipelineConfig } from '../models';

describe('identityPlugin', () => {
    const config: PipelineConfig = {
        server: 'https://dotcms.test',
        siteAuth: 'site-auth',
        debug: false
    };

    const pagehide = (persisted: boolean) =>
        window.dispatchEvent(Object.assign(new Event('pagehide'), { persisted }));

    it('cleans up activity tracking when the page is discarded', () => {
        cleanupActivityTracking.mockClear();
        identityPlugin(config).loaded();

        pagehide(false);

        expect(cleanupActivityTracking).toHaveBeenCalled();
    });

    it('keeps activity tracking on a page the browser keeps in the back/forward cache', () => {
        cleanupActivityTracking.mockClear();
        identityPlugin(config).loaded();

        // beforeunload fires before the page goes into that cache too
        window.dispatchEvent(new Event('beforeunload'));
        pagehide(true);

        expect(cleanupActivityTracking).not.toHaveBeenCalled();
    });
});
