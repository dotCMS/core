import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { BOOT_STATE_KEY, REVEAL_STYLE_ID, STORAGE_KEYS } from './constants';
import { createExperimentsEngine } from './engine';

import { getSessionId } from '../pipeline/utils';

import type { ExperimentsEngine } from './engine';

const VARIANT = 'dotexperiment-experiment-variant-1';

const markRows = (experimentId: string, variant = 'DEFAULT') => {
    const row = document.createElement('div');
    row.setAttribute('data-dot-experiment', experimentId);
    row.setAttribute('data-dot-variant', variant);
    document.body.appendChild(row);
};

const storeAssignment = (experimentId: string, variant: string) =>
    localStorage.setItem(
        STORAGE_KEYS.assignments,
        JSON.stringify({
            fetchedAt: Date.now(),
            experiments: [
                {
                    id: experimentId,
                    runningId: 'run-1',
                    variant: { name: variant, url: '' },
                    expiresAt: Date.now() + 60_000
                }
            ],
            evaluatedIds: [experimentId]
        })
    );

/** An isUserIncluded response that assigns the visitor to a variant. */
const answerWith = (experimentId: string, variant: string) => ({
    status: 200,
    ok: true,
    json: () =>
        Promise.resolve({
            entity: {
                experiments: [
                    {
                        id: experimentId,
                        name: 'Experiment',
                        runningId: 'run-1',
                        pageUrl: '/',
                        lookBackWindow: { expireMillis: 60_000, value: '' },
                        regexs: { isExperimentPage: '', isTargetPage: null },
                        variant: { name: variant, url: '' }
                    }
                ],
                includedExperimentIds: [experimentId],
                excludedExperimentIds: [],
                excludedExperimentIdsEnded: []
            }
        })
});

/** A stored assignment whose isExperimentPage rule matches /blog. */
const storeBlogAssignment = (variant: string) =>
    localStorage.setItem(
        STORAGE_KEYS.assignments,
        JSON.stringify({
            fetchedAt: Date.now(),
            experiments: [
                {
                    id: 'experiment-blog',
                    name: 'Blog Experiment',
                    runningId: 'run-1',
                    pageUrl: '/blog/index',
                    regexs: {
                        isExperimentPage: '^https?:\\/\\/[^/]+\\/blog(\\/index|\\/)?(\\/?\\?.*)?$',
                        isTargetPage: null
                    },
                    variant: { name: variant, url: '/blog/index?variantName=' + variant },
                    expiresAt: Date.now() + 60_000
                }
            ],
            evaluatedIds: ['experiment-blog']
        })
    );

describe('createExperimentsEngine', () => {
    const fetchSpy = vi.fn();
    const navigate = vi.fn();
    const warn = vi.fn();
    const globalScope = window as unknown as Record<string, unknown>;
    const engines: ExperimentsEngine[] = [];

    const createEngine = () => {
        const engine = createExperimentsEngine({
            dotcmsUrl: 'https://dotcms.test',
            timeoutMs: 1000,
            log: () => undefined,
            warn,
            navigate
        });
        engines.push(engine);

        return engine;
    };

    beforeEach(() => {
        // jsdom has no CSS namespace; every browser the package targets does.
        vi.stubGlobal('CSS', { escape: (value: string) => value });
        vi.stubGlobal('fetch', fetchSpy);
        fetchSpy.mockReset();
        navigate.mockReset();
        warn.mockReset();
        localStorage.clear();
        sessionStorage.clear();
        document.body.innerHTML = '';
        document.getElementById(REVEAL_STYLE_ID)?.remove();
    });

    afterEach(() => {
        // Each engine watches the page for marks; the next test's page is not theirs
        engines.splice(0).forEach((engine) => engine.stop());
        vi.unstubAllGlobals();
        delete globalScope[BOOT_STATE_KEY];
        window.history.replaceState(null, '', '/');
    });

    it('does not redirect again, nor send the pageview, when the page script replaces the page', async () => {
        markRows('experiment-a');
        storeAssignment('experiment-a', VARIANT);
        globalScope[BOOT_STATE_KEY] = {
            redirectedTo: `${window.location.href}?variantName=${VARIANT}`
        };
        const engine = createEngine();

        await expect(engine.decide()).resolves.toEqual({ redirected: true });
        expect(navigate).not.toHaveBeenCalled();
    });

    it('asks isUserIncluded on a first visit, holding the page, and redirects to the assigned variant', async () => {
        markRows('experiment-f');
        fetchSpy.mockResolvedValue(answerWith('experiment-f', VARIANT));
        const engine = createEngine();

        engine.start();

        await expect(engine.decide()).resolves.toEqual({ redirected: true });
        expect(fetchSpy).toHaveBeenCalledTimes(1);
        expect(navigate).toHaveBeenCalledWith(expect.stringContaining(`variantName=${VARIANT}`));
    });

    it('asks isUserIncluded on a first visit and shows the page when it is the assigned variant', async () => {
        markRows('experiment-g');
        fetchSpy.mockResolvedValue(answerWith('experiment-g', 'DEFAULT'));
        const engine = createEngine();

        await expect(engine.decide()).resolves.toEqual({ redirected: false });
        expect(navigate).not.toHaveBeenCalled();
        expect(engine.contextExperiments(getSessionId())).toEqual([
            expect.objectContaining({ id: 'experiment-g', variant: 'DEFAULT' })
        ]);
        expect(document.getElementById(REVEAL_STYLE_ID)?.textContent).toContain('experiment-g');
    });

    it('waits 3000 ms at most, the time the hiding rule keeps the content hidden, whatever the timeout', async () => {
        vi.useFakeTimers();

        try {
            markRows('experiment-t');
            let answer: (response: unknown) => void = () => undefined;
            fetchSpy.mockReturnValue(
                new Promise((resolve) => {
                    answer = resolve;
                })
            );
            const engine = createExperimentsEngine({
                dotcmsUrl: 'https://dotcms.test',
                timeoutMs: 5000,
                log: () => undefined,
                warn,
                navigate
            });
            engines.push(engine);
            let decided: unknown;

            engine.start();
            void engine.decide().then((decision) => (decided = decision));
            await vi.advanceTimersByTimeAsync(3000);

            // The rule shows the original at 3 s, so the page is shown then, and an answer that
            // comes later does not replace it with the variant
            expect(decided).toEqual({ redirected: false });
            answer(answerWith('experiment-t', VARIANT));
            await vi.advanceTimersByTimeAsync(2000);
            expect(navigate).not.toHaveBeenCalled();
            expect(warn).toHaveBeenCalledWith(expect.stringContaining('3000 ms'));
        } finally {
            vi.useRealTimers();
        }
    });

    it('decides on its own when no boot script ran, and redirects to the assigned variant', async () => {
        markRows('experiment-a');
        storeAssignment('experiment-a', VARIANT);
        sessionStorage.setItem(STORAGE_KEYS.checkedThisTab, 'true');
        const engine = createEngine();

        await expect(engine.decide()).resolves.toEqual({ redirected: true });
        expect(navigate).toHaveBeenCalledWith(expect.stringContaining(`variantName=${VARIANT}`));
    });

    it('shows the rows without waiting when the experiment was evaluated and not assigned', async () => {
        markRows('experiment-b');
        localStorage.setItem(
            STORAGE_KEYS.assignments,
            JSON.stringify({
                fetchedAt: Date.now(),
                experiments: [],
                evaluatedIds: ['experiment-b']
            })
        );
        sessionStorage.setItem(STORAGE_KEYS.checkedThisTab, 'true');
        const engine = createEngine();

        await expect(engine.decide()).resolves.toEqual({ redirected: false });
        expect(fetchSpy).not.toHaveBeenCalled();
        expect(document.getElementById(REVEAL_STYLE_ID)?.textContent).toContain('experiment-b');
    });

    it('shows the page and warns instead of reloading it when the URL already asks for the assigned variant', async () => {
        // The route ignored variantName: the variant's URL rendered DEFAULT
        window.history.replaceState(null, '', `/?variantName=${VARIANT}`);
        markRows('experiment-d');
        storeAssignment('experiment-d', VARIANT);
        sessionStorage.setItem(STORAGE_KEYS.checkedThisTab, 'true');
        const engine = createEngine();

        await expect(engine.decide()).resolves.toEqual({ redirected: false });
        expect(navigate).not.toHaveBeenCalled();
        expect(warn).toHaveBeenCalledWith(expect.stringContaining('variantName'));
        expect(sessionStorage.getItem(STORAGE_KEYS.sessionExperiments)).toBeNull();
        expect(document.getElementById(REVEAL_STYLE_ID)?.textContent).toContain('experiment-d');
    });

    it('warns when an experiment the visitor is in runs on a page that carries no marks', async () => {
        window.history.replaceState(null, '', '/blog');
        storeBlogAssignment(VARIANT);
        sessionStorage.setItem(STORAGE_KEYS.checkedThisTab, 'true');
        const engine = createEngine();

        await expect(engine.decide()).resolves.toEqual({ redirected: false });
        expect(warn).toHaveBeenCalledTimes(1);
        expect(warn).toHaveBeenCalledWith(expect.stringContaining('Blog Experiment'));

        // Not on a page the experiment does not run on
        warn.mockReset();
        window.history.replaceState(null, '', '/destinations');
        await engine.decide();
        expect(warn).not.toHaveBeenCalled();
    });

    it('waits for the marks of a page its experiment runs on, which stream in after the page loads', async () => {
        window.history.replaceState(null, '', '/blog');
        storeBlogAssignment(VARIANT);
        sessionStorage.setItem(STORAGE_KEYS.checkedThisTab, 'true');
        const engine = createEngine();

        const decided = engine.decide();
        setTimeout(() => markRows('experiment-blog'), 50);

        // The pageview waits, so it is dropped with the redirect instead of counting the original
        await expect(decided).resolves.toEqual({ redirected: true });
        expect(navigate).toHaveBeenCalledWith(expect.stringContaining(`variantName=${VARIANT}`));
        expect(warn).not.toHaveBeenCalled();
    });

    it('decides marks that arrive after the page was decided', async () => {
        fetchSpy.mockResolvedValue(answerWith('experiment-late', VARIANT));
        const engine = createEngine();

        // A new visitor: nothing stored says the page runs an experiment, and nothing is marked yet
        await expect(engine.decide()).resolves.toEqual({ redirected: false });

        // Content behind Suspense arrives
        markRows('experiment-late');

        await vi.waitFor(() =>
            expect(navigate).toHaveBeenCalledWith(expect.stringContaining(`variantName=${VARIANT}`))
        );
    });

    it('ignores the marks of a route React keeps hidden', async () => {
        // Activity keeps the previous route in the page with display:none
        const hiddenRoute = document.createElement('div');
        hiddenRoute.style.display = 'none';
        hiddenRoute.innerHTML =
            '<div data-dot-experiment="experiment-old" data-dot-variant="DEFAULT"></div>';
        document.body.appendChild(hiddenRoute);
        storeAssignment('experiment-old', VARIANT);
        sessionStorage.setItem(STORAGE_KEYS.checkedThisTab, 'true');
        const engine = createEngine();

        await expect(engine.decide()).resolves.toEqual({ redirected: false });
        expect(navigate).not.toHaveBeenCalled();
    });

    it('shows only the variant it decided', async () => {
        markRows('experiment-pair', VARIANT);
        storeAssignment('experiment-pair', VARIANT);
        sessionStorage.setItem(STORAGE_KEYS.checkedThisTab, 'true');
        const engine = createEngine();

        await expect(engine.decide()).resolves.toEqual({ redirected: false });
        expect(document.getElementById(REVEAL_STYLE_ID)?.textContent).toContain(
            `[data-dot-experiment="experiment-pair"][data-dot-variant="${VARIANT}"]`
        );
    });

    it('neither redirects nor sends the pageview when the visitor leaves the page while it is decided', async () => {
        markRows('experiment-left');
        let answer: (response: unknown) => void = () => undefined;
        fetchSpy.mockReturnValue(
            new Promise((resolve) => {
                answer = resolve;
            })
        );
        const engine = createEngine();

        const decided = engine.decide();
        // A click on the header during the wait
        window.history.pushState(null, '', '/about');
        answer(answerWith('experiment-left', VARIANT));

        await expect(decided).resolves.toEqual({ redirected: false, left: true });
        expect(navigate).not.toHaveBeenCalled();
    });

    it('drops the first pageview when the visitor goes to another page and back during the wait', async () => {
        markRows('experiment-back');
        let answer: (response: unknown) => void = () => undefined;
        fetchSpy.mockReturnValue(
            new Promise((resolve) => {
                answer = resolve;
            })
        );
        const engine = createEngine();
        const macrotask = () => new Promise((resolve) => setTimeout(resolve, 0));

        const first = engine.decide();
        // A round trip to another page, then back, while isUserIncluded is pending
        window.history.pushState(null, '', '/other');
        await macrotask();
        window.history.pushState(null, '', '/');
        await macrotask();
        // The return sends its own pageview, so the page is decided again
        const second = engine.decide();
        answer(answerWith('experiment-back', 'DEFAULT'));

        await expect(first).resolves.toEqual({ redirected: false, left: true });
        await expect(second).resolves.toEqual({ redirected: false });
    });

    it('leaves the experiments out of the context on a page that runs none', async () => {
        markRows('experiment-h');
        storeAssignment('experiment-h', 'DEFAULT');
        sessionStorage.setItem(STORAGE_KEYS.checkedThisTab, 'true');
        const engine = createEngine();
        const macrotask = () => new Promise((resolve) => setTimeout(resolve, 0));

        await engine.decide();
        expect(engine.contextExperiments(getSessionId())).toEqual([
            expect.objectContaining({ id: 'experiment-h' })
        ]);

        // An SPA navigation to a page without the experiment
        document.body.innerHTML = '';
        window.history.pushState(null, '', '/product');
        await macrotask();
        await engine.decide();

        expect(engine.contextExperiments(getSessionId())).toEqual([]);
    });

    it('keeps the experiment in the context of a page the browser restores from its back/forward cache', async () => {
        markRows('experiment-h');
        storeAssignment('experiment-h', 'DEFAULT');
        sessionStorage.setItem(STORAGE_KEYS.checkedThisTab, 'true');
        const engine = createEngine();

        await engine.decide();
        // Back to this page from another one: the restore is not a navigation inside the page
        window.dispatchEvent(Object.assign(new Event('pageshow'), { persisted: true }));

        // The restore's pageview asks again, and gets the decision the page already has
        await expect(engine.decide()).resolves.toEqual({ redirected: false });
        expect(navigate).not.toHaveBeenCalled();
        expect(engine.contextExperiments(getSessionId())).toEqual([
            expect.objectContaining({ id: 'experiment-h' })
        ]);
    });

    it("sends every experiment of the session from a page that runs one, the earlier pages' too", async () => {
        localStorage.setItem(
            STORAGE_KEYS.assignments,
            JSON.stringify({
                fetchedAt: Date.now(),
                experiments: ['experiment-i', 'experiment-j'].map((id) => ({
                    id,
                    runningId: 'run-1',
                    variant: { name: 'DEFAULT', url: '' },
                    expiresAt: Date.now() + 60_000
                })),
                evaluatedIds: ['experiment-i', 'experiment-j']
            })
        );
        sessionStorage.setItem(STORAGE_KEYS.checkedThisTab, 'true');
        markRows('experiment-i');
        const engine = createEngine();
        const macrotask = () => new Promise((resolve) => setTimeout(resolve, 0));

        await engine.decide();

        // An SPA navigation to the page of the other experiment
        document.body.innerHTML = '';
        window.history.pushState(null, '', '/pricing');
        await macrotask();
        markRows('experiment-j');
        await engine.decide();

        expect(engine.contextExperiments(getSessionId())).toEqual([
            expect.objectContaining({ id: 'experiment-i' }),
            expect.objectContaining({ id: 'experiment-j' })
        ]);
    });

    it('reports a failed experiments check to onError, with its status', async () => {
        markRows('experiment-h');
        fetchSpy.mockResolvedValue({ ok: false, status: 500, json: async () => ({}) });
        const onError = vi.fn();
        const engine = createExperimentsEngine({
            dotcmsUrl: 'https://dotcms.test',
            timeoutMs: 1000,
            log: () => undefined,
            warn,
            onError,
            navigate
        });

        await expect(engine.decide()).resolves.toEqual({ redirected: false });
        expect(onError).toHaveBeenCalledWith(
            expect.objectContaining({ code: 'EXPERIMENTS', status: 500 })
        );
    });

    it('reports a 403 as experiments off for the site', async () => {
        markRows('experiment-i');
        fetchSpy.mockResolvedValue({ ok: false, status: 403, json: async () => ({}) });
        const onError = vi.fn();
        const engine = createExperimentsEngine({
            dotcmsUrl: 'https://dotcms.test',
            timeoutMs: 1000,
            log: () => undefined,
            warn,
            onError,
            navigate
        });

        await engine.decide();
        expect(onError).toHaveBeenCalledWith(
            expect.objectContaining({ code: 'EXPERIMENTS', status: 403 })
        );
        expect(localStorage.getItem(STORAGE_KEYS.disabledUntil)).not.toBeNull();
    });
});

describe('createExperimentsEngine on pages dotCMS renders', () => {
    const fetchSpy = vi.fn();
    const navigate = vi.fn();
    // The home page's rule, as isUserIncluded returns it: any query, the path in lowercase
    const HOME_RULE = '^http://localhost/(\\?.*)?$';
    const HIDING_STYLE_ID = 'dotcms-experiment-contentlets';

    const createEngine = () =>
        createExperimentsEngine({
            dotcmsUrl: 'https://dotcms.test',
            timeoutMs: 1000,
            log: () => undefined,
            warn: () => undefined,
            navigate,
            pages: 'contentlets'
        });

    /** The wrapper dotCMS prints around each contentlet, with the variant it rendered. */
    const wrapContentlet = (variant: string) => {
        const wrapper = document.createElement('div');
        wrapper.className = 'dotcms-contentlet';
        wrapper.setAttribute('data-dot-identifier', `contentlet-${variant}`);
        wrapper.setAttribute('data-dot-variant', variant);
        document.body.appendChild(wrapper);
    };

    const storeWithRule = (experimentId: string, variant: string, rule: string) =>
        localStorage.setItem(
            STORAGE_KEYS.assignments,
            JSON.stringify({
                fetchedAt: Date.now(),
                experiments: [
                    {
                        id: experimentId,
                        runningId: 'run-1',
                        regexs: { isExperimentPage: rule, isTargetPage: null },
                        variant: { name: variant, url: '' },
                        expiresAt: Date.now() + 60_000
                    }
                ],
                evaluatedIds: [experimentId]
            })
        );

    const answerWithRule = (experimentId: string, variant: string, rule: string) => ({
        status: 200,
        ok: true,
        json: () =>
            Promise.resolve({
                entity: {
                    experiments: [
                        {
                            id: experimentId,
                            name: 'Experiment',
                            runningId: 'run-1',
                            pageUrl: '/',
                            lookBackWindow: { expireMillis: 60_000, value: '' },
                            regexs: { isExperimentPage: rule, isTargetPage: null },
                            variant: { name: variant, url: '' }
                        }
                    ],
                    includedExperimentIds: [experimentId],
                    excludedExperimentIds: [],
                    excludedExperimentIdsEnded: []
                }
            })
    });

    beforeEach(() => {
        vi.stubGlobal('CSS', { escape: (value: string) => value });
        vi.stubGlobal('fetch', fetchSpy);
        fetchSpy.mockReset();
        navigate.mockReset();
        localStorage.clear();
        sessionStorage.clear();
        document.body.innerHTML = '';
        document.getElementById(HIDING_STYLE_ID)?.remove();
        document.getElementById(REVEAL_STYLE_ID)?.remove();
    });

    afterEach(() => {
        vi.unstubAllGlobals();
        window.history.replaceState(null, '', '/');
    });

    it("replaces a returning visitor's page with the assigned variant before it paints", async () => {
        storeWithRule('experiment-c', VARIANT, HOME_RULE);
        const engine = createEngine();

        expect(engine.prepare()).toBe(true);
        expect(navigate).toHaveBeenCalledWith(`http://localhost/?variantName=${VARIANT}`);
        await expect(engine.decide()).resolves.toEqual({ redirected: true });
        expect(navigate).toHaveBeenCalledTimes(1);
    });

    it("counts the variant the page's contentlets carry, without redirecting", async () => {
        window.history.replaceState(null, '', `/?variantName=${VARIANT}`);
        wrapContentlet(VARIANT);
        wrapContentlet('DEFAULT');
        storeWithRule('experiment-c', VARIANT, HOME_RULE);
        const engine = createEngine();

        expect(engine.prepare()).toBe(false);
        await expect(engine.decide()).resolves.toEqual({ redirected: false });
        expect(navigate).not.toHaveBeenCalled();
        expect(engine.contextExperiments(getSessionId())).toEqual([
            expect.objectContaining({ id: 'experiment-c', variant: VARIANT })
        ]);
    });

    it('counts a variant that changes none of the contentlets on the page', async () => {
        // The URL asks for the variant, and every contentlet on this page is the original's
        window.history.replaceState(null, '', `/?variantName=${VARIANT}`);
        wrapContentlet('DEFAULT');
        storeWithRule('experiment-c', VARIANT, HOME_RULE);
        const engine = createEngine();

        await expect(engine.decide()).resolves.toEqual({ redirected: false });
        expect(navigate).not.toHaveBeenCalled();
        expect(engine.contextExperiments(getSessionId())).toEqual([
            expect.objectContaining({ id: 'experiment-c', variant: VARIANT })
        ]);
    });

    it('hides the contentlets on a first visit, asks isUserIncluded, and redirects to the variant', async () => {
        wrapContentlet('DEFAULT');
        fetchSpy.mockResolvedValue(answerWithRule('experiment-c', VARIANT, HOME_RULE));
        const engine = createEngine();

        expect(engine.prepare()).toBe(false);
        expect(document.getElementById(HIDING_STYLE_ID)?.textContent).toContain(
            '.dotcms-contentlet{visibility:hidden'
        );

        engine.start();

        await expect(engine.decide()).resolves.toEqual({ redirected: true });
        expect(fetchSpy).toHaveBeenCalledTimes(1);
        expect(navigate).toHaveBeenCalledWith(`http://localhost/?variantName=${VARIANT}`);
    });

    it('shows the contentlets again when no experiment runs on the page', async () => {
        wrapContentlet('DEFAULT');
        fetchSpy.mockResolvedValue(
            answerWithRule('experiment-c', VARIANT, '^http://localhost/other(\\?.*)?$')
        );
        const dispatch = vi.spyOn(window, 'dispatchEvent');
        const engine = createEngine();

        engine.prepare();
        engine.start();

        await expect(engine.decide()).resolves.toEqual({ redirected: false });
        expect(navigate).not.toHaveBeenCalled();
        expect(document.getElementById(HIDING_STYLE_ID)).toBeNull();
        expect(dispatch).toHaveBeenCalledWith(
            expect.objectContaining({ type: 'dotcms:events:rescan' })
        );
        dispatch.mockRestore();
    });

    it('tests the rules as dotCMS writes them: the path in lowercase, the query as it is', async () => {
        window.history.replaceState(null, '', '/About-Us?Ref=Mail');
        storeWithRule('experiment-c', 'DEFAULT', '^http://localhost/about-us\\?Ref=Mail$');
        const engine = createEngine();

        await expect(engine.decide()).resolves.toEqual({ redirected: false });
        expect(engine.contextExperiments(getSessionId())).toEqual([
            expect.objectContaining({ id: 'experiment-c', variant: 'DEFAULT' })
        ]);
    });

    it('follows the experiment markup when the page carries it', async () => {
        const row = document.createElement('div');
        row.setAttribute('data-dot-experiment', 'experiment-m');
        row.setAttribute('data-dot-variant', 'DEFAULT');
        document.body.appendChild(row);
        storeWithRule('experiment-m', 'DEFAULT', '');
        const engine = createEngine();

        await expect(engine.decide()).resolves.toEqual({ redirected: false });
        expect(document.getElementById(REVEAL_STYLE_ID)?.textContent).toContain('experiment-m');
    });

    it('prepares nothing on pages that print the experiment markup', () => {
        storeWithRule('experiment-c', VARIANT, HOME_RULE);
        const engine = createExperimentsEngine({
            dotcmsUrl: 'https://dotcms.test',
            timeoutMs: 1000,
            log: () => undefined,
            warn: () => undefined,
            navigate
        });

        expect(engine.prepare()).toBe(false);
        expect(navigate).not.toHaveBeenCalled();
        expect(document.getElementById(HIDING_STYLE_ID)).toBeNull();
    });
});
