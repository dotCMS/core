import {
    fetchPageDataFromInsideUVE,
    listenEditorMessages,
    listenHoveredContentlet,
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
        GET_PAGE_DATA: 'get-page-data',
        NOOP: 'noop'
    }
}));

const CONTAINER_MOCK = {
    acceptTypes: 'text/html',
    identifier: '123',
    maxContentlets: '1',
    uuid: '123-456'
};

const getContentletTestElement = () => {
    const div = document.createElement('div');

    div.setAttribute('data-dot-object', 'contentlet');
    div.setAttribute('data-dot-identifier', '123');
    div.setAttribute('data-dot-title', 'title');
    div.setAttribute('data-dot-inode', '123-456');
    div.setAttribute('data-dot-type', 'CONTENT');
    div.setAttribute('data-dot-basetype', 'WIDGET');
    div.setAttribute('data-dot-widget-title', 'widgetTitle');
    div.setAttribute('data-dot-on-number-of-pages', '1');
    div.setAttribute('data-dot-container', JSON.stringify(CONTAINER_MOCK));
    div.innerHTML = 'contentlet';

    return div;
};

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

        const target = getContentletTestElement();
        const event = new MouseEvent('pointermove', {
            bubbles: true,
            cancelable: true
        });

        document.body.appendChild(target);
        target.dispatchEvent(event);

        expect(addEventListenerSpy).toHaveBeenCalledWith('pointermove', expect.any(Function));
        expect(postMessageToEditor).toHaveBeenCalledWith({
            action: CUSTOMER_ACTIONS.SET_CONTENTLET,
            payload: {
                x: expect.any(Number),
                y: expect.any(Number),
                width: expect.any(Number),
                height: expect.any(Number),
                payload: {
                    container: CONTAINER_MOCK,
                    contentlet: {
                        baseType: 'WIDGET',
                        identifier: '123',
                        inode: '123-456',
                        contentType: 'CONTENT',
                        title: 'title',
                        widgetTitle: 'widgetTitle',
                        onNumberOfPages: '1'
                    },
                    vtlFiles: null
                }
            }
        });
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

    it('should get page data post message to editor', () => {
        fetchPageDataFromInsideUVE('some-url');
        expect(postMessageToEditor).toHaveBeenCalledWith({
            action: CUSTOMER_ACTIONS.GET_PAGE_DATA,
            payload: {
                pathname: 'some-url'
            }
        });
    });
});
