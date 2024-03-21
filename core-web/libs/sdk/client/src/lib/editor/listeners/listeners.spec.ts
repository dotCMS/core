import {
    listenContentChange,
    listenEditorMessages,
    listenHoveredContentlet,
    pingEditor,
    preserveScrollOnIframe,
    scrollHandler
} from './listeners';

import { CUSTOMER_ACTIONS, postMessageToEditor } from '../models/client.model';

jest.mock('../models/client.model', () => ({
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

/**
 * Observation: We must test the execution of methods as well.
 */
describe('listeners', () => {
    it('should listen editor messages', () => {
        const addEventListenerSpy = jest.spyOn(window, 'addEventListener');
        listenEditorMessages();
        expect(addEventListenerSpy).toHaveBeenCalledWith('message', expect.any(Function));
    });

    it('should listen to hovered contentlet', () => {
        const addEventListenerSpy = jest.spyOn(document, 'addEventListener');
        listenHoveredContentlet();
        expect(addEventListenerSpy).toHaveBeenCalledWith('pointermove', expect.any(Function));
    });

    it('should handle scroll', () => {
        const addEventListenerSpy = jest.spyOn(window, 'addEventListener');
        scrollHandler();
        expect(addEventListenerSpy).toHaveBeenCalledWith('scroll', expect.any(Function));
    });

    it('should preserve scroll on iframe', () => {
        const addEventListenerSpy = jest.spyOn(window, 'addEventListener');
        preserveScrollOnIframe();
        expect(addEventListenerSpy).toHaveBeenCalledWith('load', expect.any(Function));
    });

    it('should listen to content change', () => {
        const observeSpy = jest.spyOn(MutationObserver.prototype, 'observe');
        listenContentChange();
        expect(observeSpy).toHaveBeenCalledWith(document, { childList: true, subtree: true });
    });

    it('should send ping to editor', () => {
        pingEditor();
        expect(postMessageToEditor).toHaveBeenCalledWith({
            action: CUSTOMER_ACTIONS.PING_EDITOR
        });
    });
});
