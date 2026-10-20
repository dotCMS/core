import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { REVEAL_STYLE_ID } from './constants';
import { holdRequests, observeMarks, readPageExperiments, revealRows } from './dom';

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

    it('shows only the variant it decided, so an earlier reveal never shows another render', () => {
        revealRows('experiment-4', 'variant-b');

        expect(document.getElementById(REVEAL_STYLE_ID)?.textContent).toContain(
            ':root [data-dot-experiment="experiment-4"][data-dot-variant="variant-b"]{visibility:visible !important'
        );
    });

    it('writes the rule to an adopted style sheet where the browser has them, which no CSP blocks', () => {
        let adopted: CSSStyleSheet[] = [];
        Object.defineProperty(document, 'adoptedStyleSheets', {
            configurable: true,
            get: () => adopted,
            set: (sheets: CSSStyleSheet[]) => {
                adopted = sheets;
            }
        });
        const styleText = document.getElementById(REVEAL_STYLE_ID)?.textContent ?? '';

        try {
            revealRows('experiment-5', 'DEFAULT');

            expect(adopted).toHaveLength(1);
            expect(adopted[0]?.cssRules[0]?.cssText).toContain('experiment-5');
            expect(document.getElementById(REVEAL_STYLE_ID)?.textContent ?? '').toBe(styleText);
        } finally {
            delete (document as unknown as Record<string, unknown>)['adoptedStyleSheets'];
        }
    });
});

describe('readPageExperiments', () => {
    beforeEach(() => {
        document.body.innerHTML = '';
    });

    it('reads every experiment the page renders, once each', () => {
        document.body.innerHTML =
            '<div data-dot-experiment="e1" data-dot-variant="B"></div>' +
            '<div data-dot-experiment="e1" data-dot-variant="B"></div>' +
            '<section data-dot-experiment="e2"></section>';

        expect(readPageExperiments()).toEqual([
            { experimentId: 'e1', variant: 'B' },
            { experimentId: 'e2', variant: 'DEFAULT' }
        ]);
    });

    it('skips the marks of routes React keeps hidden and of content still streaming in', () => {
        document.body.innerHTML =
            // A route Activity hides, and a Suspense boundary's content before it is moved in
            '<div style="display: none"><div data-dot-experiment="old" data-dot-variant="DEFAULT"></div></div>' +
            '<div hidden><div data-dot-experiment="streaming"></div></div>' +
            '<div data-dot-experiment="shown" data-dot-variant="B"></div>';

        expect(readPageExperiments()).toEqual([{ experimentId: 'shown', variant: 'B' }]);
    });
});

describe('observeMarks', () => {
    const settle = () => new Promise((resolve) => setTimeout(resolve, 0));

    it('calls back when a mark is inserted or rewritten, and not for other changes', async () => {
        const changed = vi.fn();
        const stop = observeMarks(changed);

        document.body.appendChild(document.createElement('p'));
        await settle();
        expect(changed).not.toHaveBeenCalled();

        const main = document.createElement('main');
        const row = document.createElement('div');
        row.setAttribute('data-dot-experiment', 'e1');
        main.appendChild(row);
        document.body.appendChild(main);
        await settle();
        expect(changed).toHaveBeenCalledTimes(1);

        // A query-only navigation that keeps the segment rewrites the mark in place
        row.setAttribute('data-dot-variant', 'B');
        await settle();
        expect(changed).toHaveBeenCalledTimes(2);

        stop();
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
