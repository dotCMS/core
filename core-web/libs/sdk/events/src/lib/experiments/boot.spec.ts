import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
    buildExperimentBootScript,
    createExperimentBootConfig,
    dotcmsExperimentBoot
} from './boot';
import { BOOT_STATE_KEY, REVEAL_STYLE_ID, STORAGE_KEYS } from './constants';
import { decideVariant } from './decision';

import type { ExperimentBootState } from './models';

const EXPERIMENT = 'experiment-1';
const VARIANT = 'dotexperiment-experiment-variant-1';
const DAY = 24 * 60 * 60 * 1000;

const createStorage = (): Storage => {
    const data = new Map<string, string>();

    return {
        getItem: (key: string) => data.get(key) ?? null,
        setItem: (key: string, value: string) => void data.set(key, String(value)),
        removeItem: (key: string) => void data.delete(key),
        clear: () => data.clear(),
        key: (index: number) => [...data.keys()][index] ?? null,
        get length() {
            return data.size;
        }
    };
};

interface FakeWindow {
    document: Document;
    localStorage: Storage;
    location: { href: string; replace: ReturnType<typeof vi.fn> };
    stop: ReturnType<typeof vi.fn>;
    fetch: ReturnType<typeof vi.fn>;
    setTimeout: typeof setTimeout;
    parent: unknown;
    [BOOT_STATE_KEY]?: ExperimentBootState;
}

const createWindow = ({
    href = 'https://site.test/page',
    framed = false
}: { href?: string; framed?: boolean } = {}): FakeWindow => {
    const w: FakeWindow = {
        document,
        localStorage: createStorage(),
        location: { href, replace: vi.fn() },
        stop: vi.fn(),
        fetch: vi.fn(),
        setTimeout: ((callback: () => void, ms: number) =>
            setTimeout(callback, ms)) as typeof setTimeout,
        parent: null
    };
    w.parent = framed ? {} : w;

    return w;
};

const assign = (w: FakeWindow, variant: string) =>
    w.localStorage.setItem(
        STORAGE_KEYS.assignments,
        JSON.stringify({
            experiments: [
                { id: EXPERIMENT, variant: { name: variant }, expiresAt: Date.now() + DAY }
            ],
            evaluatedIds: [EXPERIMENT]
        })
    );

const run = (w: FakeWindow, rendered = 'DEFAULT') =>
    dotcmsExperimentBoot(
        decideVariant,
        createExperimentBootConfig({ experimentId: EXPERIMENT, variant: rendered }),
        w as unknown as Window
    );

const revealRule = () => document.getElementById(REVEAL_STYLE_ID)?.textContent ?? '';

describe('dotcmsExperimentBoot', () => {
    beforeEach(() => {
        document.getElementById(REVEAL_STYLE_ID)?.remove();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('shows the content when the stored assignment is the variant the server rendered', () => {
        const w = createWindow();
        assign(w, 'DEFAULT');

        run(w, 'DEFAULT');

        // Keyed by the variant too: a route kept in the page must not show another render
        expect(revealRule()).toContain(
            `[data-dot-experiment="${EXPERIMENT}"][data-dot-variant="DEFAULT"]{visibility:visible !important`
        );
        expect(w.location.replace).not.toHaveBeenCalled();
    });

    it('shows the content through an adopted style sheet where the browser has them, which no CSP blocks', () => {
        let adopted: CSSStyleSheet[] = [];
        Object.defineProperty(document, 'adoptedStyleSheets', {
            configurable: true,
            get: () => adopted,
            set: (sheets: CSSStyleSheet[]) => {
                adopted = sheets;
            }
        });
        const w = createWindow();
        Object.assign(w, { CSSStyleSheet: window.CSSStyleSheet });
        assign(w, 'DEFAULT');

        try {
            run(w, 'DEFAULT');

            expect(adopted).toHaveLength(1);
            expect(adopted[0]?.cssRules[0]?.cssText).toContain(EXPERIMENT);
            expect(revealRule()).toBe('');
        } finally {
            delete (document as unknown as Record<string, unknown>)['adoptedStyleSheets'];
        }
    });

    it('stops the page and replaces it with the assigned variant when it differs', () => {
        const w = createWindow();
        assign(w, VARIANT);

        run(w, 'DEFAULT');

        expect(w.stop).toHaveBeenCalled();
        expect(w.location.replace).toHaveBeenCalledWith(
            `https://site.test/page?variantName=${VARIANT}`
        );
        expect(w[BOOT_STATE_KEY]?.redirectedTo).toBe(
            `https://site.test/page?variantName=${VARIANT}`
        );
    });

    it('removes variantName when the assigned variant is the default one', () => {
        const w = createWindow({ href: `https://site.test/page?variantName=${VARIANT}` });
        assign(w, 'DEFAULT');

        run(w, VARIANT);

        expect(w.location.replace).toHaveBeenCalledWith('https://site.test/page');
    });

    it('shows the content instead of reloading when the URL already asks for the assigned variant', () => {
        // The route ignored variantName and rendered DEFAULT: replacing the page would loop
        const w = createWindow({ href: `https://site.test/page?variantName=${VARIANT}` });
        assign(w, VARIANT);

        run(w, 'DEFAULT');

        expect(w.location.replace).not.toHaveBeenCalled();
        expect(w.stop).not.toHaveBeenCalled();
        expect(w[BOOT_STATE_KEY]).toBeUndefined();
        expect(revealRule()).toContain(EXPERIMENT);
    });

    it('shows the content instead of reloading when DEFAULT is assigned and the URL asks for no variant', () => {
        const w = createWindow();
        assign(w, 'DEFAULT');

        run(w, VARIANT);

        expect(w.location.replace).not.toHaveBeenCalled();
        expect(revealRule()).toContain(EXPERIMENT);
    });

    it('holds the new requests of the page it replaces, except keepalive ones', async () => {
        vi.useFakeTimers();
        const w = createWindow();
        const send = w.fetch;
        assign(w, VARIANT);

        run(w, 'DEFAULT');
        const prefetch = (w.fetch as unknown as typeof fetch)('/store?_rsc=1');
        void (w.fetch as unknown as typeof fetch)('/api/v1/analytics/content/event', {
            keepalive: true
        });

        const settled = vi.fn();
        void prefetch.then(settled, settled);
        await vi.advanceTimersByTimeAsync(1000);
        expect(settled).not.toHaveBeenCalled();
        expect(send).toHaveBeenCalledTimes(1);
        expect(send).toHaveBeenCalledWith('/api/v1/analytics/content/event', { keepalive: true });

        // Still here after the quiet period: the page's requests go out again
        await vi.advanceTimersByTimeAsync(5000);
        expect(w.fetch).toBe(send);
    });

    it('shows the content without asking when the experiment was evaluated and not assigned', () => {
        const w = createWindow();
        w.localStorage.setItem(
            STORAGE_KEYS.assignments,
            JSON.stringify({ experiments: [], evaluatedIds: [EXPERIMENT] })
        );

        run(w);

        expect(revealRule()).toContain(EXPERIMENT);
        expect(w.location.replace).not.toHaveBeenCalled();
    });

    it('leaves the content hidden and asks nothing when nothing is stored: the engine decides', () => {
        const w = createWindow();

        run(w);

        expect(revealRule()).toBe('');
        expect(w.fetch).not.toHaveBeenCalled();
        expect(w.location.replace).not.toHaveBeenCalled();
        expect(w[BOOT_STATE_KEY]).toBeUndefined();
    });

    it('shows the content while a 403 keeps experiments off', () => {
        const w = createWindow();
        assign(w, VARIANT);
        w.localStorage.setItem(STORAGE_KEYS.disabledUntil, JSON.stringify(Date.now() + DAY));

        run(w);

        expect(revealRule()).toContain(EXPERIMENT);
        expect(w.location.replace).not.toHaveBeenCalled();
    });

    it('only shows the content inside the UVE editor', () => {
        const w = createWindow({ framed: true });
        assign(w, VARIANT);

        run(w);

        expect(revealRule()).toContain(EXPERIMENT);
        expect(w.location.replace).not.toHaveBeenCalled();
    });
});

describe('buildExperimentBootScript', () => {
    // What the page runs: the printed script alone, with no module around it, so a reference
    // to anything outside the two printed functions throws here
    const runPrinted = (w: FakeWindow, rendered = 'DEFAULT') =>
        new Function(
            'window',
            buildExperimentBootScript({ experimentId: EXPERIMENT, variant: rendered })
        )(w);

    beforeEach(() => {
        document.getElementById(REVEAL_STYLE_ID)?.remove();
    });

    it('replaces the page with the assigned variant when run as printed', () => {
        const w = createWindow();
        assign(w, VARIANT);

        runPrinted(w);

        expect(w.stop).toHaveBeenCalled();
        expect(w.location.replace).toHaveBeenCalledWith(
            `https://site.test/page?variantName=${VARIANT}`
        );
    });

    it('shows the content of the variant the server rendered when run as printed', () => {
        const w = createWindow();
        assign(w, VARIANT);

        runPrinted(w, VARIANT);

        expect(revealRule()).toContain(
            `[data-dot-experiment="${EXPERIMENT}"][data-dot-variant="${VARIANT}"]{visibility:visible !important`
        );
        expect(w.location.replace).not.toHaveBeenCalled();
    });

    it('builds a script no value can close', () => {
        const script = buildExperimentBootScript({
            experimentId: 'x</script><script>alert(1)</script>',
            variant: 'DEFAULT'
        });

        expect(script).not.toContain('</script>');
        expect(script.startsWith('(function')).toBe(true);
    });
});
