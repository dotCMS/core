import { DotCMSUVEAction } from '@dotcms/types';
import { __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';

import {
    addClassToEmptyContentlets,
    injectEmptyStateStyles,
    listenBlockEditorInlineEvent,
    scrollHandler,
    setClientIsReady
} from './utils';

import { DOT_SECTION_ID_PREFIX } from '../internal/constants';
import { onScrollToSection } from '../internal/events';

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

    it('binds through Zone.js native listeners when Zone is present, not the patched ones', () => {
        // Simulate a Zone.js-loaded page: expose the unpatched native methods
        // under the __zone_symbol__ keys the way Zone.js stashes them.
        const nativeAdd = jest.fn();
        const nativeRemove = jest.fn();
        const win = window as unknown as Record<string, unknown>;

        try {
            win['__zone_symbol__addEventListener'] = nativeAdd;
            win['__zone_symbol__removeEventListener'] = nativeRemove;
            (globalThis as unknown as { Zone: unknown }).Zone = {
                __symbol__: (name: string) => `__zone_symbol__${name}`
            };
            const patchedAddSpy = jest.spyOn(window, 'addEventListener');

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
        jest.restoreAllMocks();
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

    it('escapes CSS-breaking characters in the empty container label', () => {
        localStorage.setItem(
            'dotMessagesKeys',
            JSON.stringify({
                'editpage.container.is.empty': "Ain't \\ broken\nlabel"
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
    let postMessageSpy: jest.SpyInstance;
    let unsubscribe: () => void;

    beforeEach(() => {
        postMessageSpy = jest.spyOn(window.parent, 'postMessage').mockImplementation(jest.fn());
        ({ unsubscribe } = onScrollToSection((payload) => {
            window.parent.postMessage({ action: DotCMSUVEAction.SECTION_OFFSET, payload }, '*');
        }));
    });

    afterEach(() => {
        unsubscribe();
        document.body.innerHTML = '';
        jest.clearAllMocks();
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
