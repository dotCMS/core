import { DotCMSUVEAction } from '@dotcms/types';

import {
    addClassToEmptyContentlets,
    injectEmptyStateStyles,
    listenBlockEditorInlineEvent,
    reportIframeHeight,
    scrollHandler,
    setClientIsReady
} from './utils';

import * as documentHeightObserver from '../lib/dom/document-height-observer';

describe('reportIframeHeight', () => {
    let postMessageSpy: jest.SpyInstance;
    let destroySpy: jest.Mock;
    let observeDocumentHeightSpy: jest.SpyInstance;

    beforeEach(() => {
        postMessageSpy = jest.spyOn(window.parent, 'postMessage').mockImplementation(jest.fn());
        destroySpy = jest.fn();
        observeDocumentHeightSpy = jest
            .spyOn(documentHeightObserver, 'observeDocumentHeight')
            .mockReturnValue({ destroy: destroySpy });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it('delegates observation to the shared height observer', () => {
        reportIframeHeight();

        expect(observeDocumentHeightSpy).toHaveBeenCalledWith(
            expect.objectContaining({
                onHeightChange: expect.any(Function)
            })
        );
    });

    it('sends IFRAME_HEIGHT when the shared observer reports a height', () => {
        reportIframeHeight();

        const onHeightChange = observeDocumentHeightSpy.mock.calls[0][0].onHeightChange;
        onHeightChange(1000);

        expect(postMessageSpy).toHaveBeenCalledWith(
            { action: DotCMSUVEAction.IFRAME_HEIGHT, payload: { height: 1000 } },
            '*'
        );
    });

    it('destroys the shared observer when destroyHeightReporter is called', () => {
        const { destroyHeightReporter } = reportIframeHeight();
        destroyHeightReporter();

        expect(destroySpy).toHaveBeenCalled();
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
