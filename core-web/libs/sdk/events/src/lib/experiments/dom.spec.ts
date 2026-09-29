import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { REVEAL_STYLE_ID } from './constants';
import { holdRequests, revealRows } from './dom';

import { CONTENTLET_RESCAN_EVENT } from '../contentlets/constants';

describe('revealRows', () => {
    const rescans = vi.fn();

    beforeEach(() => {
        // jsdom has no CSS namespace; every browser the package targets does.
        vi.stubGlobal('CSS', { escape: (value: string) => value });
        rescans.mockClear();
        window.addEventListener(CONTENTLET_RESCAN_EVENT, rescans);
    });

    afterEach(() => {
        window.removeEventListener(CONTENTLET_RESCAN_EVENT, rescans);
        vi.unstubAllGlobals();
    });

    // revealRows remembers the rules it added for the life of the page, so every test uses
    // its own experiment id.

    it('shows the experiment rows through the reveal style', () => {
        revealRows('experiment-1');

        expect(document.getElementById(REVEAL_STYLE_ID)?.textContent).toContain(
            ':root [data-dot-experiment="experiment-1"]{visibility:visible !important'
        );
    });

    it('asks the content trackers to scan again once the rows show', () => {
        revealRows('experiment-2');

        expect(rescans).toHaveBeenCalledTimes(1);
    });

    it('does not ask again for rows it already revealed', () => {
        revealRows('experiment-3');
        revealRows('experiment-3');

        expect(rescans).toHaveBeenCalledTimes(1);
    });
});

describe('holdRequests', () => {
    const original = window.fetch;

    afterEach(() => {
        vi.useRealTimers();
        window.fetch = original;
    });

    it('holds new fetch calls except keepalive ones, and lets them go after the quiet period', async () => {
        vi.useFakeTimers();
        const send = vi.fn(() => Promise.resolve({} as Response));
        window.fetch = send;

        holdRequests();
        const held = window.fetch('/store?_rsc=1');
        const settled = vi.fn();
        void held.then(settled, settled);
        void window.fetch('/api/v1/analytics/content/event', { keepalive: true });

        await vi.advanceTimersByTimeAsync(1000);
        expect(settled).not.toHaveBeenCalled();
        expect(send).toHaveBeenCalledTimes(1);
        expect(send).toHaveBeenCalledWith('/api/v1/analytics/content/event', { keepalive: true });

        await vi.advanceTimersByTimeAsync(5000);
        expect(window.fetch).toBe(send);
    });
});
