import { DotCMSUVEAction } from '@dotcms/types';

import {
    addClassToEmptyContentlets,
    injectEmptyStateStyles,
    listenBlockEditorInlineEvent,
    reportIframeHeight,
    scrollHandler,
    setClientIsReady
} from './utils';

describe('reportIframeHeight', () => {
    let postMessageSpy: jest.SpyInstance;
    let roObserveSpy: jest.Mock;
    let roDisconnectSpy: jest.Mock;
    let roCallback: ResizeObserverCallback;
    let moObserveSpy: jest.Mock;
    let moDisconnectSpy: jest.Mock;
    let moCallback: MutationCallback;

    beforeEach(() => {
        jest.useFakeTimers();

        postMessageSpy = jest.spyOn(window.parent, 'postMessage').mockImplementation(jest.fn());

        roObserveSpy = jest.fn();
        roDisconnectSpy = jest.fn();
        global.ResizeObserver = jest.fn((cb: ResizeObserverCallback) => {
            roCallback = cb;

            return { observe: roObserveSpy, disconnect: roDisconnectSpy, unobserve: jest.fn() };
        }) as unknown as typeof ResizeObserver;

        moObserveSpy = jest.fn();
        moDisconnectSpy = jest.fn();
        global.MutationObserver = jest.fn((cb: MutationCallback) => {
            moCallback = cb;

            return {
                observe: moObserveSpy,
                disconnect: moDisconnectSpy,
                takeRecords: jest.fn()
            };
        }) as unknown as typeof MutationObserver;

        Object.defineProperty(document, 'readyState', {
            configurable: true,
            get: jest.fn().mockReturnValue('complete')
        });

        Object.defineProperty(document.documentElement, 'offsetHeight', {
            configurable: true,
            get: jest.fn().mockReturnValue(1000)
        });
    });

    afterEach(() => {
        jest.useRealTimers();
        jest.clearAllMocks();
    });

    /** Flush debounce (50 ms) then both animation frames. */
    const flushAll = () => {
        jest.advanceTimersByTime(50);
        jest.runAllTimers();
    };

    describe('measurement', () => {
        it('sends IFRAME_HEIGHT with document.documentElement.offsetHeight after debounce and double rAF', () => {
            reportIframeHeight();
            flushAll();

            expect(postMessageSpy).toHaveBeenCalledWith(
                { action: DotCMSUVEAction.IFRAME_HEIGHT, payload: { height: 1000 } },
                '*'
            );
        });

        it('does not send when offsetHeight is 0 (layout not yet settled)', () => {
            Object.defineProperty(document.documentElement, 'offsetHeight', {
                configurable: true,
                get: jest.fn().mockReturnValue(0)
            });

            reportIframeHeight();
            flushAll();

            expect(postMessageSpy).not.toHaveBeenCalledWith(
                expect.objectContaining({ action: DotCMSUVEAction.IFRAME_HEIGHT }),
                expect.anything()
            );
        });
    });

    describe('scheduling', () => {
        it('does not send before the debounce timer fires', () => {
            reportIframeHeight();

            // Advance less than the 50 ms debounce — the timer is still pending
            jest.advanceTimersByTime(49);

            expect(postMessageSpy).not.toHaveBeenCalled();
        });

        it('collapses rapid-fire observer callbacks into a single send', () => {
            reportIframeHeight();

            // Simulate MO + RO firing multiple times before the debounce expires
            roCallback([], {} as ResizeObserver);
            moCallback([], {} as MutationObserver);
            roCallback([], {} as ResizeObserver);

            flushAll();

            expect(postMessageSpy).toHaveBeenCalledTimes(1);
        });

        it('schedules a new send after the debounce settles when observers fire again', () => {
            reportIframeHeight();
            flushAll();
            postMessageSpy.mockClear();

            roCallback([], {} as ResizeObserver);
            flushAll();

            expect(postMessageSpy).toHaveBeenCalledTimes(1);
        });
    });

    describe('initial readyState handling', () => {
        it('schedules immediately when readyState is "complete"', () => {
            Object.defineProperty(document, 'readyState', {
                configurable: true,
                get: jest.fn().mockReturnValue('complete')
            });

            reportIframeHeight();
            flushAll();

            expect(postMessageSpy).toHaveBeenCalled();
        });

        it('schedules immediately when readyState is "interactive"', () => {
            Object.defineProperty(document, 'readyState', {
                configurable: true,
                get: jest.fn().mockReturnValue('interactive')
            });

            reportIframeHeight();
            flushAll();

            expect(postMessageSpy).toHaveBeenCalled();
        });

        it('waits for the load event when readyState is "loading"', () => {
            Object.defineProperty(document, 'readyState', {
                configurable: true,
                get: jest.fn().mockReturnValue('loading')
            });

            const addEventListenerSpy = jest.spyOn(window, 'addEventListener');
            reportIframeHeight();

            expect(addEventListenerSpy).toHaveBeenCalledWith('load', expect.any(Function));
            // Nothing sent yet — load hasn't fired
            jest.runAllTimers();
            expect(postMessageSpy).not.toHaveBeenCalled();
        });
    });

    describe('observers', () => {
        it('observes document.documentElement with ResizeObserver', () => {
            reportIframeHeight();
            expect(roObserveSpy).toHaveBeenCalledWith(document.documentElement);
        });

        it('observes document.body with MutationObserver (childList + subtree)', () => {
            reportIframeHeight();
            expect(moObserveSpy).toHaveBeenCalledWith(document.body, {
                childList: true,
                subtree: true
            });
        });

        it('sends when ResizeObserver fires (e.g. image load changes html height)', () => {
            reportIframeHeight();
            flushAll();
            postMessageSpy.mockClear();

            roCallback([], {} as ResizeObserver);
            flushAll();

            expect(postMessageSpy).toHaveBeenCalledWith(
                { action: DotCMSUVEAction.IFRAME_HEIGHT, payload: { height: 1000 } },
                '*'
            );
        });

        it('sends when MutationObserver fires (e.g. contentlet removed from DOM)', () => {
            reportIframeHeight();
            flushAll();
            postMessageSpy.mockClear();

            moCallback([], {} as MutationObserver);
            flushAll();

            expect(postMessageSpy).toHaveBeenCalledWith(
                { action: DotCMSUVEAction.IFRAME_HEIGHT, payload: { height: 1000 } },
                '*'
            );
        });
    });

    describe('destroyHeightReporter', () => {
        it('stops sending after destroy is called', () => {
            const { destroyHeightReporter } = reportIframeHeight();
            destroyHeightReporter();
            flushAll();

            expect(postMessageSpy).not.toHaveBeenCalled();
        });

        it('cancels a pending debounce timer', () => {
            const clearTimeoutSpy = jest.spyOn(global, 'clearTimeout');
            const { destroyHeightReporter } = reportIframeHeight();

            // Debounce timer is pending; destroy should cancel it
            destroyHeightReporter();

            expect(clearTimeoutSpy).toHaveBeenCalled();
        });

        it('disconnects the ResizeObserver', () => {
            const { destroyHeightReporter } = reportIframeHeight();
            destroyHeightReporter();
            expect(roDisconnectSpy).toHaveBeenCalled();
        });

        it('disconnects the MutationObserver', () => {
            const { destroyHeightReporter } = reportIframeHeight();
            destroyHeightReporter();
            expect(moDisconnectSpy).toHaveBeenCalled();
        });

        it('removes the load event listener', () => {
            Object.defineProperty(document, 'readyState', {
                configurable: true,
                get: jest.fn().mockReturnValue('loading')
            });

            const removeEventListenerSpy = jest.spyOn(window, 'removeEventListener');
            const { destroyHeightReporter } = reportIframeHeight();
            destroyHeightReporter();

            expect(removeEventListenerSpy).toHaveBeenCalledWith('load', expect.any(Function));
        });

        it('ignores observer callbacks fired after destroy', () => {
            const { destroyHeightReporter } = reportIframeHeight();
            destroyHeightReporter();

            roCallback([], {} as ResizeObserver);
            moCallback([], {} as MutationObserver);
            flushAll();

            expect(postMessageSpy).not.toHaveBeenCalled();
        });
    });
});

describe('scrollHandler', () => {
    let postMessageSpy: jest.SpyInstance;
    let destroyScrollHandler: () => void;

    beforeEach(() => {
        postMessageSpy = jest.spyOn(window.parent, 'postMessage').mockImplementation(jest.fn());
    });

    afterEach(() => {
        destroyScrollHandler?.();
        jest.clearAllMocks();
    });

    it('sends IFRAME_SCROLL on window scroll', () => {
        ({ destroyScrollHandler } = scrollHandler());
        window.dispatchEvent(new Event('scroll'));

        expect(postMessageSpy).toHaveBeenCalledWith({ action: DotCMSUVEAction.IFRAME_SCROLL }, '*');
    });

    it('sends IFRAME_SCROLL_END on window scrollend', () => {
        ({ destroyScrollHandler } = scrollHandler());
        window.dispatchEvent(new Event('scrollend'));

        expect(postMessageSpy).toHaveBeenCalledWith(
            { action: DotCMSUVEAction.IFRAME_SCROLL_END },
            '*'
        );
    });

    it('removes both listeners after destroyScrollHandler is called', () => {
        ({ destroyScrollHandler } = scrollHandler());
        destroyScrollHandler();

        window.dispatchEvent(new Event('scroll'));
        window.dispatchEvent(new Event('scrollend'));

        expect(postMessageSpy).not.toHaveBeenCalled();
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
    let postMessageSpy: jest.SpyInstance;

    beforeEach(() => {
        postMessageSpy = jest.spyOn(window.parent, 'postMessage').mockImplementation(jest.fn());
    });

    afterEach(() => {
        jest.clearAllMocks();
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
        jest.clearAllMocks();
        document.body.innerHTML = '';
    });

    it('adds a DOMContentLoaded listener when readyState is not "complete"', () => {
        Object.defineProperty(document, 'readyState', {
            configurable: true,
            get: jest.fn().mockReturnValue('loading')
        });

        const addEventListenerSpy = jest.spyOn(document, 'addEventListener');
        listenBlockEditorInlineEvent();

        expect(addEventListenerSpy).toHaveBeenCalledWith('DOMContentLoaded', expect.any(Function));
    });

    it('removes the DOMContentLoaded listener via destroyListenBlockEditorInlineEvent', () => {
        Object.defineProperty(document, 'readyState', {
            configurable: true,
            get: jest.fn().mockReturnValue('loading')
        });

        const removeEventListenerSpy = jest.spyOn(document, 'removeEventListener');
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
            get: jest.fn().mockReturnValue('complete')
        });

        const addEventListenerSpy = jest.spyOn(document, 'addEventListener');
        listenBlockEditorInlineEvent();

        expect(addEventListenerSpy).not.toHaveBeenCalledWith(
            'DOMContentLoaded',
            expect.any(Function)
        );
    });
});

describe('injectEmptyStateStyles', () => {
    afterEach(() => {
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
            'dotMessagesKeys',
            JSON.stringify({ 'editpage.container.is.empty': 'Contenedor vacío' })
        );

        injectEmptyStateStyles();

        const style = document.head.querySelector(
            '[data-dot-styles="uve-empty-state"]'
        ) as HTMLStyleElement;

        expect(style.textContent).toContain("content: 'Contenedor vacío'");
    });

    it('falls back to the default label when localStorage JSON is malformed', () => {
        jest.spyOn(Storage.prototype, 'getItem').mockReturnValue('not-valid-json');

        injectEmptyStateStyles();

        const style = document.head.querySelector(
            '[data-dot-styles="uve-empty-state"]'
        ) as HTMLStyleElement;

        expect(style.textContent).toContain("content: 'Empty container'");
    });
});
