import { postMessageToEditor, CUSTOMER_ACTIONS } from './models/client.model';
import { initEditor, isInsideEditor, pingEditor, updateNavigation } from './sdk-editor';

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

describe('DotCMSPageEditor', () => {
    describe('is NOT inside editor', () => {
        beforeEach(() => {
            const mockWindow = {
                ...window,
                parent: window
            };

            const spy = jest.spyOn(global, 'window', 'get');
            spy.mockReturnValueOnce(mockWindow as unknown as Window & typeof globalThis);
        });

        afterEach(() => {
            jest.clearAllMocks();
        });

        it('should initialize without any listener', () => {
            const addEventListenerSpy = jest.spyOn(window, 'addEventListener');

            expect(isInsideEditor()).toBe(false);
            expect(addEventListenerSpy).not.toHaveBeenCalled();
        });
    });

    describe('is inside editor', () => {
        beforeEach(() => {
            const mockWindow = {
                ...window,
                parent: null
            };

            const spy = jest.spyOn(global, 'window', 'get');
            spy.mockReturnValue(mockWindow as unknown as Window & typeof globalThis);
        });

        afterEach(() => {
            jest.clearAllMocks();
        });

        it('should initialize properly', () => {
            expect(isInsideEditor()).toBe(true);
        });

        it('should update navigation', () => {
            updateNavigation('/');
            expect(postMessageToEditor).toHaveBeenCalledWith({
                action: CUSTOMER_ACTIONS.NAVIGATION_UPDATE,
                payload: {
                    url: 'index'
                }
            });
        });

        it('should listen to editor messages', () => {
            const addEventListenerSpy = jest.spyOn(window, 'addEventListener');
            initEditor();
            expect(addEventListenerSpy).toHaveBeenCalledWith('message', expect.any(Function));
        });

        it('should listen to hovered contentlet', () => {
            const addEventListenerSpy = jest.spyOn(document, 'addEventListener');
            initEditor();
            expect(addEventListenerSpy).toHaveBeenCalledWith('pointermove', expect.any(Function));
        });

        it('should handle scroll', () => {
            const addEventListenerSpy = jest.spyOn(window, 'addEventListener');
            initEditor();
            expect(addEventListenerSpy).toHaveBeenCalledWith('scroll', expect.any(Function));
        });

        it('should send ping to editor', () => {
            pingEditor();
            expect(postMessageToEditor).toHaveBeenCalledWith({
                action: CUSTOMER_ACTIONS.PING_EDITOR
            });
        });

        it('should listen to content change', () => {
            const observeSpy = jest.spyOn(MutationObserver.prototype, 'observe');
            initEditor();

            expect(observeSpy).toHaveBeenCalledWith(document, { childList: true, subtree: true });
        });

        it('should listen to editor messages', () => {
            const addEventListenerSpy = jest.spyOn(window, 'addEventListener');
            initEditor();

            expect(addEventListenerSpy).toHaveBeenCalledWith('message', expect.any(Function));
        });
    });
});
