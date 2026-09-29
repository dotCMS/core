import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';

import type { DotCMSEvents } from './models';

// Analytics.js is replaced by an instance that records what `events` hands it
const fakeInstance = vi.hoisted(() => ({ track: vi.fn(), page: vi.fn() }));
const Analytics = vi.hoisted(() => vi.fn(() => fakeInstance));

vi.mock('analytics', () => ({ Analytics }));
vi.mock('@dotcms/uve', () => ({ getUVEState: () => undefined }));

interface AnalyticsJsConfig {
    app: string;
    storage: {
        getItem(key: string): unknown;
        setItem(key: string, value: unknown): void;
        removeItem(key: string): void;
    };
}

describe('events', () => {
    let events: DotCMSEvents;

    beforeAll(async () => {
        ({ events } = await import('./events'));

        // Experiments and automatic pageviews off: the specs below send nothing on their own
        events.init({
            dotcmsUrl: 'https://cms.test',
            siteAuth: 'site-auth',
            experiments: false,
            autoPageView: false
        });

        // Analytics.js loads through a dynamic import
        await vi.waitFor(() => expect(Analytics).toHaveBeenCalled());
    });

    beforeEach(() => {
        fakeInstance.track.mockClear();
    });

    it('is reachable as window.dotEvents, for traditional pages and plain scripts', () => {
        expect((window as unknown as Record<string, unknown>)['dotEvents']).toBe(events);
    });

    it('offers init, conversion and pageView: the events dotCMS accepts', () => {
        expect(Object.keys(events).sort()).toEqual(['conversion', 'init', 'pageView']);
    });

    it('sends a conversion with its name only, the one field dotCMS accepts', () => {
        events.conversion('signup');

        expect(fakeInstance.track).toHaveBeenCalledWith('conversion', { name: 'signup' });
    });

    it('gives Analytics.js a storage that writes nothing to localStorage or cookies', () => {
        const [config] = Analytics.mock.calls[0] as unknown as [AnalyticsJsConfig];
        const localSetItem = vi.spyOn(Storage.prototype, 'setItem');

        config.storage.setItem('__anon_id', 'anonymous-id');

        expect(config.app).toBe('dotEvents');
        expect(config.storage.getItem('__anon_id')).toBe('anonymous-id');
        expect(localSetItem).not.toHaveBeenCalled();
        expect(document.cookie).toBe('');
        localSetItem.mockRestore();
    });
});
