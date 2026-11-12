import { MockInstance, vi } from 'vitest';

import { DotCMSUVEAction } from '@dotcms/types';
import { __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';

import {
    addClassToEmptyContentlets,
    injectEmptyStateStyles,
    listenBlockEditorInlineEvent,
    registerUVEEvents,
    scrollHandler,
    setClientIsReady
} from './utils';

import { DOT_SECTION_ID_PREFIX } from '../internal/constants';
import { onScrollToSection } from '../internal/events';

describe('scrollHandler', () => {
    let postMessageSpy: MockInstance;
    let destroyScrollHandler: () => void;

    beforeEach(() => {
        postMessageSpy = vi.spyOn(window.parent, 'postMessage').mockImplementation(vi.fn());
    });

    afterEach(() => {
        destroyScrollHandler?.();
        vi.clearAllMocks();
    });

    it('sends IFRAME_SCROLL on window scroll', () => {
        ({ destroyScrollHandler } = scrollHandler());
        window.dispatchEvent(new Event('scroll'));

        // The page reports where it scrolled to (#38575); jsdom starts at the top.
        expect(postMessageSpy).toHaveBeenCalledWith(
            { action: DotCMSUVEAction.IFRAME_SCROLL, payload: { scrollY: 0 } },
            '*'
        );
    });

    it('sends IFRAME_SCROLL_END on window scrollend', () => {
        ({ destroyScrollHandler } = scrollHandler());
        window.dispatchEvent(new Event('scrollend'));

        expect(postMessageSpy).toHaveBeenCalledWith(
            { action: DotCMSUVEAction.IFRAME_SCROLL_END },
            '*'
        );
    });

    // #38575: the editor shifts its drop targets by how far the page scrolled during a drag.
    it('sends the page scrollY with IFRAME_SCROLL', () => {
        Object.defineProperty(window, 'scrollY', { value: 840, configurable: true });

        try {
            ({ destroyScrollHandler } = scrollHandler());
            window.dispatchEvent(new Event('scroll'));

            expect(postMessageSpy).toHaveBeenCalledWith(
                { action: DotCMSUVEAction.IFRAME_SCROLL, payload: { scrollY: 840 } },
                '*'
            );
        } finally {
            Object.defineProperty(window, 'scrollY', { value: 0, configurable: true });
        }
    });

    it('removes both listeners after destroyScrollHandler is called', () => {
        ({ destroyScrollHandler } = scrollHandler());
        destroyScrollHandler();

        window.dispatchEvent(new Event('scroll'));
        window.dispatchEvent(new Event('scrollend'));

        expect(postMessageSpy).not.toHaveBeenCalled();
    });

    it('binds through Zone.js native listeners when Zone is present, not the patched ones', () => {
        // Simulate a Zone.js-loaded page: expose the unpatched native methods
        // under the __zone_symbol__ keys the way Zone.js stashes them.
        const nativeAdd = vi.fn();
        const nativeRemove = vi.fn();
        const win = window as unknown as Record<string, unknown>;

        try {
            win['__zone_symbol__addEventListener'] = nativeAdd;
            win['__zone_symbol__removeEventListener'] = nativeRemove;
            (globalThis as unknown as { Zone: unknown }).Zone = {
                __symbol__: (name: string) => `__zone_symbol__${name}`
            };
            const patchedAddSpy = vi.spyOn(window, 'addEventListener');

            ({ destroyScrollHandler } = scrollHandler());

            expect(nativeAdd).toHaveBeenCalledWith('scroll', expect.any(Function));
            expect(nativeAdd).toHaveBeenCalledWith('scrollend', expect.any(Function));
            // Must NOT register on the Zone-patched window.addEventListener, which
            // goes dead after the iframe's document.open()/write()/close() rewrites.
            expect(patchedAddSpy).not.toHaveBeenCalledWith('scroll', expect.any(Function));

            destroyScrollHandler();
            expect(nativeRemove).toHaveBeenCalledWith('scroll', expect.any(Function));
            expect(nativeRemove).toHaveBeenCalledWith('scrollend', expect.any(Function));
        } finally {
            // Always clean up so Zone never leaks into other tests/suites.
            delete win['__zone_symbol__addEventListener'];
            delete win['__zone_symbol__removeEventListener'];
            delete (globalThis as unknown as { Zone?: unknown }).Zone;
        }
    });
});

// #38575: during a drag the editor sends a scroll velocity and the page scrolls itself, every
// frame, until told to stop. Older editors still send the one-step direction request.
describe('registerUVEEvents scroll during a drag', () => {
    /** The editor → page message, by its wire name, so a rename of the constant shows up here. */
    const SCROLL_VELOCITY = 'uve-scroll-velocity';

    /** One frame at 60fps, in ms. */
    const FRAME = 1000 / 60;

    let subscriptions: { unsubscribe: () => void }[];
    let scrollBy: MockInstance;
    let frames: Map<number, FrameRequestCallback>;
    let lastFrameId: number;

    /** Runs the frame the page asked for, at `time` ms. */
    const runFrame = (time: number) => {
        const pending = [...frames.values()];
        frames.clear();
        pending.forEach((callback) => callback(time));
    };

    /** The editor sends a message to the page. */
    const fromEditor = (data: Record<string, unknown>) => {
        window.dispatchEvent(new MessageEvent('message', { data }));
    };

    const scrollAtVelocity = (velocity: unknown) => fromEditor({ name: SCROLL_VELOCITY, velocity });

    /** Where the page is: `scrollY`, in a 3000px document seen through an 800px viewport. */
    const pageAt = (scrollY: number) => {
        Object.defineProperty(window, 'scrollY', { value: scrollY, configurable: true });
    };

    /** The `top` of the last instant scroll the page made. */
    const lastInstantScroll = (): number | undefined =>
        scrollBy.mock.calls
            .map(([options]) => options as ScrollToOptions)
            .filter((options) => options?.behavior === 'instant')
            .at(-1)?.top;

    beforeEach(() => {
        // Inside the editor: the page has a parent window, which receives what the page sends.
        vi.spyOn(window, 'parent', 'get').mockReturnValue({
            postMessage: vi.fn()
        } as unknown as Window);
        // jsdom has no layout observers; the auto-bounds channel needs them to start.
        vi.stubGlobal(
            'ResizeObserver',
            class {
                observe = vi.fn();
                unobserve = vi.fn();
                disconnect = vi.fn();
            }
        );

        frames = new Map();
        lastFrameId = 0;
        vi.stubGlobal(
            'requestAnimationFrame',
            vi.fn((callback: FrameRequestCallback) => {
                lastFrameId += 1;
                frames.set(lastFrameId, callback);

                return lastFrameId;
            })
        );
        vi.stubGlobal(
            'cancelAnimationFrame',
            vi.fn((id: number) => frames.delete(id))
        );

        scrollBy = vi.spyOn(window, 'scrollBy').mockImplementation(vi.fn());
        Object.defineProperty(window, 'innerHeight', { value: 800, configurable: true });
        Object.defineProperty(document.documentElement, 'scrollHeight', {
            value: 3000,
            configurable: true
        });
        pageAt(1000);

        ({ subscriptions } = registerUVEEvents());
    });

    afterEach(() => {
        subscriptions.forEach((subscription) => subscription.unsubscribe());
        pageAt(0);
        delete (document.documentElement as unknown as Record<string, unknown>)['scrollHeight'];
        vi.unstubAllGlobals();
        vi.restoreAllMocks();
    });

    it('should scroll instantly every frame at the velocity the editor sends', () => {
        scrollAtVelocity(12);

        runFrame(1000);
        runFrame(1000 + FRAME);

        expect(scrollBy).toHaveBeenLastCalledWith(
            expect.objectContaining({ top: expect.closeTo(12, 1), behavior: 'instant' })
        );
        // Still going: the page asked for the next frame.
        expect(frames.size).toBe(1);
    });

    it('should scale each step by how long the frame took', () => {
        scrollAtVelocity(12);

        runFrame(1000);
        runFrame(1000 + 2 * FRAME);

        expect(lastInstantScroll()).toBeCloseTo(24, 0);
    });

    it('should scroll up for a negative velocity', () => {
        scrollAtVelocity(-12);

        runFrame(1000);
        runFrame(1000 + FRAME);

        expect(lastInstantScroll()).toBeCloseTo(-12, 0);
    });

    it('should stop when the editor sends a velocity of 0', () => {
        scrollAtVelocity(12);
        runFrame(1000);
        runFrame(1000 + FRAME);
        const scrollsBeforeStop = scrollBy.mock.calls.length;

        scrollAtVelocity(0);
        runFrame(1000 + 2 * FRAME);
        runFrame(1000 + 3 * FRAME);

        expect(scrollsBeforeStop).toBeGreaterThan(0);
        expect(scrollBy).toHaveBeenCalledTimes(scrollsBeforeStop);
    });

    it('should stop at the top of the page when scrolling up', () => {
        pageAt(0);

        scrollAtVelocity(-12);
        runFrame(1000);
        runFrame(1000 + FRAME);

        expect(scrollBy).not.toHaveBeenCalled();
        expect(frames.size).toBe(0);
        // It stopped because of the limit, not because it never started.
        expect(requestAnimationFrame).toHaveBeenCalled();
    });

    it('should stop at the bottom of the page when scrolling down', () => {
        pageAt(2200);

        scrollAtVelocity(12);
        runFrame(1000);
        runFrame(1000 + FRAME);

        expect(scrollBy).not.toHaveBeenCalled();
        expect(frames.size).toBe(0);
        expect(requestAnimationFrame).toHaveBeenCalled();
    });

    it.each([NaN, Infinity, -Infinity, '12', null])(
        'should treat a velocity of %s as 0 and stop',
        (velocity) => {
            scrollAtVelocity(12);
            runFrame(1000);
            runFrame(1000 + FRAME);
            const scrollsBefore = scrollBy.mock.calls.length;

            scrollAtVelocity(velocity);
            runFrame(1000 + 2 * FRAME);
            runFrame(1000 + 3 * FRAME);

            expect(scrollsBefore).toBeGreaterThan(0);
            expect(scrollBy).toHaveBeenCalledTimes(scrollsBefore);
        }
    );

    it.each([
        { sent: 100, clamped: 40 },
        { sent: -100, clamped: -40 }
    ])(
        'should scroll no faster than 40px per frame when the editor sends $sent',
        ({ sent, clamped }) => {
            scrollAtVelocity(sent);

            runFrame(1000);
            runFrame(1000 + FRAME);

            expect(lastInstantScroll()).toBeCloseTo(clamped, 0);
        }
    );

    // Review of #38575: the editor measures the drop targets' shift from the last scrollY the
    // page reported. A report that waits for the next scroll event is a frame late, so bounds
    // taken mid-scroll would be shifted from a stale value.
    it('should report where the page is right after each step, in the same frame', () => {
        const editor = window.parent.postMessage as unknown as MockInstance;
        scrollBy.mockImplementation((options: ScrollToOptions) =>
            pageAt(window.scrollY + options.top)
        );
        scrollAtVelocity(12);

        runFrame(1000);
        expect(editor).toHaveBeenLastCalledWith(
            {
                action: DotCMSUVEAction.IFRAME_SCROLL,
                payload: { scrollY: expect.closeTo(1012, 5) }
            },
            '*'
        );

        runFrame(1000 + FRAME);
        expect(editor).toHaveBeenLastCalledWith(
            {
                action: DotCMSUVEAction.IFRAME_SCROLL,
                payload: { scrollY: expect.closeTo(1024, 5) }
            },
            '*'
        );
    });

    it('should stop when the page stops listening to the editor', () => {
        scrollAtVelocity(12);
        runFrame(1000);
        runFrame(1000 + FRAME);
        const scrollsBefore = scrollBy.mock.calls.length;

        subscriptions.forEach((subscription) => subscription.unsubscribe());
        runFrame(1000 + 2 * FRAME);

        expect(scrollsBefore).toBeGreaterThan(0);
        expect(scrollBy).toHaveBeenCalledTimes(scrollsBefore);
    });

    describe('legacy one-step scroll request', () => {
        const legacyScroll = (direction: 'up' | 'down') =>
            fromEditor({ name: __DOTCMS_UVE_EVENT__.UVE_SCROLL_INSIDE_IFRAME, direction });

        it('should still scroll 120px smoothly for an editor that sends no velocity', () => {
            legacyScroll('down');

            expect(scrollBy).toHaveBeenCalledWith({ left: 0, top: 120, behavior: 'smooth' });
        });

        it('should ignore it once the editor has sent a velocity', () => {
            scrollAtVelocity(0);

            legacyScroll('down');

            expect(scrollBy).not.toHaveBeenCalledWith(
                expect.objectContaining({ behavior: 'smooth' })
            );
        });
    });
});

describe('addClassToEmptyContentlets', () => {
    afterEach(() => {
        document.body.innerHTML = '';
    });

    it('adds empty-contentlet class to contentlets with no height', () => {
        const el = document.createElement('div');
        el.setAttribute('data-dot-object', 'contentlet');
        // jsdom reports clientHeight as 0 by default
        document.body.appendChild(el);

        addClassToEmptyContentlets();

        expect(el.classList.contains('empty-contentlet')).toBe(true);
    });

    it('does not add empty-contentlet class to contentlets with a height', () => {
        const el = document.createElement('div');
        el.setAttribute('data-dot-object', 'contentlet');
        Object.defineProperty(el, 'clientHeight', { value: 100, configurable: true });
        document.body.appendChild(el);

        addClassToEmptyContentlets();

        expect(el.classList.contains('empty-contentlet')).toBe(false);
    });

    it('does nothing when there are no contentlets', () => {
        // Should not throw
        expect(() => addClassToEmptyContentlets()).not.toThrow();
    });
});

describe('setClientIsReady', () => {
    let postMessageSpy: MockInstance;

    beforeEach(() => {
        postMessageSpy = vi.spyOn(window.parent, 'postMessage').mockImplementation(vi.fn());
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('sends CLIENT_READY with no payload when called without config', () => {
        setClientIsReady();

        expect(postMessageSpy).toHaveBeenCalledWith(
            { action: DotCMSUVEAction.CLIENT_READY, payload: undefined },
            '*'
        );
    });

    it('sends CLIENT_READY with the provided config as payload', () => {
        const config = { params: { depth: '2' } } as never;
        setClientIsReady(config);

        expect(postMessageSpy).toHaveBeenCalledWith(
            { action: DotCMSUVEAction.CLIENT_READY, payload: config },
            '*'
        );
    });
});

describe('listenBlockEditorInlineEvent', () => {
    afterEach(() => {
        vi.clearAllMocks();
        document.body.innerHTML = '';
    });

    it('adds a DOMContentLoaded listener when readyState is not "complete"', () => {
        Object.defineProperty(document, 'readyState', {
            configurable: true,
            get: vi.fn().mockReturnValue('loading')
        });

        const addEventListenerSpy = vi.spyOn(document, 'addEventListener');
        listenBlockEditorInlineEvent();

        expect(addEventListenerSpy).toHaveBeenCalledWith('DOMContentLoaded', expect.any(Function));
    });

    it('removes the DOMContentLoaded listener via destroyListenBlockEditorInlineEvent', () => {
        Object.defineProperty(document, 'readyState', {
            configurable: true,
            get: vi.fn().mockReturnValue('loading')
        });

        const removeEventListenerSpy = vi.spyOn(document, 'removeEventListener');
        const { destroyListenBlockEditorInlineEvent } = listenBlockEditorInlineEvent();
        destroyListenBlockEditorInlineEvent();

        expect(removeEventListenerSpy).toHaveBeenCalledWith(
            'DOMContentLoaded',
            expect.any(Function)
        );
    });

    it('does not add a DOMContentLoaded listener when readyState is "complete"', () => {
        Object.defineProperty(document, 'readyState', {
            configurable: true,
            get: vi.fn().mockReturnValue('complete')
        });

        const addEventListenerSpy = vi.spyOn(document, 'addEventListener');
        listenBlockEditorInlineEvent();

        expect(addEventListenerSpy).not.toHaveBeenCalledWith(
            'DOMContentLoaded',
            expect.any(Function)
        );
    });
});

describe('injectEmptyStateStyles', () => {
    afterEach(() => {
        vi.restoreAllMocks();
        document.head
            .querySelectorAll('[data-dot-styles="uve-empty-state"]')
            .forEach((el) => el.remove());
        localStorage.clear();
    });

    it('injects a <style> element with data-dot-styles="uve-empty-state" into document.head', () => {
        injectEmptyStateStyles();

        const style = document.head.querySelector('[data-dot-styles="uve-empty-state"]');
        expect(style).not.toBeNull();
    });

    it('includes empty container and empty contentlet CSS rules', () => {
        injectEmptyStateStyles();

        const style = document.head.querySelector(
            '[data-dot-styles="uve-empty-state"]'
        ) as HTMLStyleElement;

        expect(style.textContent).toContain('[data-dot-object="container"]:empty');
        expect(style.textContent).toContain('[data-dot-object="contentlet"].empty-contentlet');
    });

    it('uses the default "Empty container" label when localStorage has no entry', () => {
        injectEmptyStateStyles();

        const style = document.head.querySelector(
            '[data-dot-styles="uve-empty-state"]'
        ) as HTMLStyleElement;

        expect(style.textContent).toContain("content: 'Empty container'");
    });

    it('uses the i18n label from localStorage when available', () => {
        localStorage.setItem(
            'dotMessagesBundle',
            JSON.stringify({ messages: { 'editpage.container.is.empty': 'Contenedor vacío' } })
        );

        injectEmptyStateStyles();

        const style = document.head.querySelector(
            '[data-dot-styles="uve-empty-state"]'
        ) as HTMLStyleElement;

        expect(style.textContent).toContain("content: 'Contenedor vacío'");
    });

    it('ignores old split-field caches', () => {
        localStorage.setItem(
            'dotMessagesKeys',
            JSON.stringify({ 'editpage.container.is.empty': 'Legacy label' })
        );
        injectEmptyStateStyles();
        expect(
            document.head.querySelector('[data-dot-styles="uve-empty-state"]')?.textContent
        ).toContain("content: 'Empty container'");
    });

    it('ignores non-string labels in the atomic bundle', () => {
        localStorage.setItem(
            'dotMessagesBundle',
            JSON.stringify({ messages: { 'editpage.container.is.empty': 42 } })
        );
        injectEmptyStateStyles();
        expect(
            document.head.querySelector('[data-dot-styles="uve-empty-state"]')?.textContent
        ).toContain("content: 'Empty container'");
    });

    it('falls back to the default label when localStorage JSON is malformed', () => {
        vi.spyOn(Storage.prototype, 'getItem').mockReturnValue('not-valid-json');

        injectEmptyStateStyles();

        const style = document.head.querySelector(
            '[data-dot-styles="uve-empty-state"]'
        ) as HTMLStyleElement;

        expect(style.textContent).toContain("content: 'Empty container'");
    });

    it('escapes CSS-breaking characters in the empty container label', () => {
        localStorage.setItem(
            'dotMessagesBundle',
            JSON.stringify({
                messages: {
                    'editpage.container.is.empty': "Ain't \\ broken\nlabel"
                }
            })
        );

        injectEmptyStateStyles();

        const style = document.head.querySelector(
            '[data-dot-styles="uve-empty-state"]'
        ) as HTMLStyleElement;

        expect(style.textContent).toContain("content: 'Ain\\'t \\\\ broken\\a label'");
    });
});

describe('onScrollToSection', () => {
    let postMessageSpy: MockInstance;
    let unsubscribe: () => void;

    beforeEach(() => {
        postMessageSpy = vi.spyOn(window.parent, 'postMessage').mockImplementation(vi.fn());
        ({ unsubscribe } = onScrollToSection((payload) => {
            window.parent.postMessage({ action: DotCMSUVEAction.SECTION_OFFSET, payload }, '*');
        }));
    });

    afterEach(() => {
        unsubscribe();
        document.body.innerHTML = '';
        vi.clearAllMocks();
    });

    it('sends SECTION_OFFSET with offsetTop when the element is found via dot-section-{n}', () => {
        const el = document.createElement('div');
        el.id = `${DOT_SECTION_ID_PREFIX}2`;
        Object.defineProperty(el, 'offsetTop', { value: 320, configurable: true });
        document.body.appendChild(el);

        window.dispatchEvent(
            new MessageEvent('message', {
                data: { name: __DOTCMS_UVE_EVENT__.UVE_SCROLL_TO_SECTION, sectionIndex: 2 }
            })
        );

        expect(postMessageSpy).toHaveBeenCalledWith(
            {
                action: DotCMSUVEAction.SECTION_OFFSET,
                payload: { sectionIndex: 2, offsetTop: 320 }
            },
            '*'
        );
    });

    it('falls back to section-{n} when dot-section-{n} is not found', () => {
        const el = document.createElement('div');
        el.id = 'section-3';
        Object.defineProperty(el, 'offsetTop', { value: 800, configurable: true });
        document.body.appendChild(el);

        window.dispatchEvent(
            new MessageEvent('message', {
                data: { name: __DOTCMS_UVE_EVENT__.UVE_SCROLL_TO_SECTION, sectionIndex: 3 }
            })
        );

        expect(postMessageSpy).toHaveBeenCalledWith(
            {
                action: DotCMSUVEAction.SECTION_OFFSET,
                payload: { sectionIndex: 3, offsetTop: 800 }
            },
            '*'
        );
    });

    it('does not invoke the callback when neither selector matches', () => {
        window.dispatchEvent(
            new MessageEvent('message', {
                data: { name: __DOTCMS_UVE_EVENT__.UVE_SCROLL_TO_SECTION, sectionIndex: 99 }
            })
        );

        expect(postMessageSpy).not.toHaveBeenCalled();
    });

    it('stops listening after unsubscribe', () => {
        unsubscribe();

        const el = document.createElement('div');
        el.id = `${DOT_SECTION_ID_PREFIX}1`;
        Object.defineProperty(el, 'offsetTop', { value: 100, configurable: true });
        document.body.appendChild(el);

        window.dispatchEvent(
            new MessageEvent('message', {
                data: { name: __DOTCMS_UVE_EVENT__.UVE_SCROLL_TO_SECTION, sectionIndex: 1 }
            })
        );

        expect(postMessageSpy).not.toHaveBeenCalled();
    });
});
