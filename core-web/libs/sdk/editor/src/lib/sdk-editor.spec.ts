/// <reference types="jest" />
import { DotCMSPageEditor } from './sdk-editor';

import { postMessageToEditor } from './models/client.model';

jest.mock('./models/client.model', () => ({
    postMessageToEditor: jest.fn(),
    CUSTOMER_ACTIONS: {
        NAVIGATION_UPDATE: 'set-url',
        SET_BOUNDS: 'set-bounds',
        SET_CONTENTLET: 'set-contentlet',
        IFRAME_SCROLL: 'scroll',
        PING_EDITOR: 'ping-editor',
        CONTENT_CHANGE: 'content-change',
        NOOP: 'noop'
    }
}));

import { CUSTOMER_ACTIONS } from './models/client.model';

describe('DotCMSPageEditor', () => {
    describe('is NOT inside editor', () => {
        let dotCMSPageEditor: DotCMSPageEditor;

        beforeEach(() => {
            const mockWindow = {
                ...window,
                parent: window
            };

            const spy = jest.spyOn(global, 'window', 'get');
            spy.mockReturnValueOnce(mockWindow as unknown as Window & typeof globalThis);
            dotCMSPageEditor = new DotCMSPageEditor();
        });

        afterEach(() => {
            jest.clearAllMocks();
        });

        it('should initialize without any listener', () => {
            const addEventListenerSpy = jest.spyOn(window, 'addEventListener');

            dotCMSPageEditor.init();

            expect(dotCMSPageEditor.isInsideEditor).toBe(false);
            expect(addEventListenerSpy).not.toHaveBeenCalled();
        });
    });

    describe('is inside editor', () => {
        let dotCMSPageEditor: DotCMSPageEditor;

        beforeEach(() => {
            const mockWindow = {
                ...window,
                parent: null
            };

            const spy = jest.spyOn(global, 'window', 'get');
            spy.mockReturnValue(mockWindow as unknown as Window & typeof globalThis);
            dotCMSPageEditor = new DotCMSPageEditor();
        });

        afterEach(() => {
            jest.clearAllMocks();
        });

        it('should initialize properly', () => {
            dotCMSPageEditor.init();
            expect(dotCMSPageEditor.isInsideEditor).toBe(true);
        });

        it('should update navigation', () => {
            dotCMSPageEditor.updateNavigation('/');
            expect(postMessageToEditor).toHaveBeenCalledWith({
                action: CUSTOMER_ACTIONS.NAVIGATION_UPDATE,
                payload: {
                    url: 'index'
                }
            });
        });

        it('should listen to editor messages', () => {
            const addEventListenerSpy = jest.spyOn(window, 'addEventListener');
            dotCMSPageEditor.init();
            expect(addEventListenerSpy).toHaveBeenCalledWith('message', expect.any(Function));
        });

        it('should listen to hovered contentlet', () => {
            const addEventListenerSpy = jest.spyOn(document, 'addEventListener');
            dotCMSPageEditor.init();
            expect(addEventListenerSpy).toHaveBeenCalledWith('pointermove', expect.any(Function));
        });

        it('should handle scroll', () => {
            const addEventListenerSpy = jest.spyOn(window, 'addEventListener');
            dotCMSPageEditor.init();
            expect(addEventListenerSpy).toHaveBeenCalledWith('scroll', expect.any(Function));
        });

        it('should check if inside editor', () => {
            dotCMSPageEditor.init();
            expect(postMessageToEditor).toHaveBeenCalledWith({
                action: CUSTOMER_ACTIONS.PING_EDITOR
            });
        });

        it('should listen to content change', () => {
            const observeSpy = jest.spyOn(MutationObserver.prototype, 'observe');
            dotCMSPageEditor.init();
            expect(observeSpy).toHaveBeenCalledWith(document, { childList: true, subtree: true });
        });

        it('should listen to editor messages', () => {
            const addEventListenerSpy = jest.spyOn(window, 'addEventListener');
            const dotCMSPageEditor = new DotCMSPageEditor();
            dotCMSPageEditor.init();

            expect(addEventListenerSpy).toHaveBeenCalledWith('message', expect.any(Function));
        });
    });
});
