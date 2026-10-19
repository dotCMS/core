import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';

import type { DotCMSEvents } from './models';

// Analytics.js is replaced by an instance that records the pageviews `dotEvents` sends
const fakeInstance = vi.hoisted(() => ({ track: vi.fn(), page: vi.fn() }));
const Analytics = vi.hoisted(() => vi.fn(() => fakeInstance));

vi.mock('analytics', () => ({ Analytics }));
vi.mock('@dotcms/uve', () => ({ getUVEState: () => undefined }));

const pageshow = (persisted: boolean) =>
    window.dispatchEvent(Object.assign(new Event('pageshow'), { persisted }));

describe('dotEvents automatic pageviews', () => {
    let dotEvents: DotCMSEvents;

    beforeAll(async () => {
        ({ dotEvents } = await import('./events'));

        // Experiments off: each pageview goes out without waiting for a decision
        dotEvents.init({
            dotcmsUrl: 'https://cms.test',
            siteAuth: 'site-auth',
            experiments: false
        });

        // The page's own pageview, once Analytics.js loads through its dynamic import
        await vi.waitFor(() => expect(fakeInstance.page).toHaveBeenCalledTimes(1));
    });

    beforeEach(() => {
        fakeInstance.page.mockClear();
    });

    it('sends a pageview when the browser restores the page from its back/forward cache', async () => {
        pageshow(true);

        await vi.waitFor(() => expect(fakeInstance.page).toHaveBeenCalledTimes(1));
    });

    it('sends one on every restore, though the URL is the one already counted', async () => {
        pageshow(true);
        pageshow(true);

        await vi.waitFor(() => expect(fakeInstance.page).toHaveBeenCalledTimes(2));
    });

    it('sends nothing more when a page that loads is shown: its load already counted it', async () => {
        pageshow(false);
        await new Promise((resolve) => setTimeout(resolve, 0));

        expect(fakeInstance.page).not.toHaveBeenCalled();
    });
});
