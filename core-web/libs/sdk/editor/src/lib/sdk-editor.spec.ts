import { DotCMSPageEditor } from './sdk-editor';

import { CUSTOMER_ACTIONS, postMessageToEditor } from './models/client.model';

jest.mock('./postMessageToEditor', () => ({
    postMessageToEditor: jest.fn()
}));

describe('DotCMSPageEditor', () => {
    let dotCMSPageEditor: DotCMSPageEditor;

    beforeEach(() => {
        dotCMSPageEditor = new DotCMSPageEditor();
    });

    afterEach(() => {
        dotCMSPageEditor.destroy();
    });

    it('should initialize properly', () => {
        dotCMSPageEditor.init();
        expect(dotCMSPageEditor.isInsideEditor).toBe(false);
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
        const addEventListenerSpy = spyOn(window, 'addEventListener');
        dotCMSPageEditor.init();
        expect(addEventListenerSpy).toHaveBeenCalledWith('message', jasmine.any(Function));
    });

    it('should listen to hovered contentlet', () => {
        const addEventListenerSpy = spyOn(document, 'addEventListener');
        dotCMSPageEditor.init();
        expect(addEventListenerSpy).toHaveBeenCalledWith('pointermove', jasmine.any(Function));
    });

    it('should handle scroll', () => {
        const addEventListenerSpy = spyOn(window, 'addEventListener');
        dotCMSPageEditor.init();
        expect(addEventListenerSpy).toHaveBeenCalledWith('scroll', jasmine.any(Function));
    });

    it('should check if inside editor', () => {
        dotCMSPageEditor.init();
        expect(postMessageToEditor).toHaveBeenCalledWith({
            action: CUSTOMER_ACTIONS.PING_EDITOR
        });
    });

    it('should listen to content change', () => {
        const observeSpy = spyOn(window.MutationObserver.prototype, 'observe');
        dotCMSPageEditor.init();
        expect(observeSpy).toHaveBeenCalledWith(document, { childList: true, subtree: true });
    });

    it('should set bounds', () => {
        const querySelectorAllSpy = spyOn(document, 'querySelectorAll').and.returnValue([]);

        // dotCMSPageEditor.setBounds();
        expect(querySelectorAllSpy).toHaveBeenCalledWith('[data-dot-object="container"]');
        expect(postMessageToEditor).toHaveBeenCalledWith({
            action: CUSTOMER_ACTIONS.SET_BOUNDS,
            payload: []
        });
    });

    // it('should reload page', () => {
    //     const onReloadSpy = spyOn(dotCMSPageEditor.config, 'onReload');
    //     dotCMSPageEditor.reloadPage();
    //     expect(onReloadSpy).toHaveBeenCalled();
    // });
});
