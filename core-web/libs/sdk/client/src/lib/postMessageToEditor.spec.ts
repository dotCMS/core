import { postMessageToEditor, CUSTOMER_ACTIONS } from './postMessageToEditor';

describe('postMessageToEditor', () => {
    beforeEach(() => {
        jest.spyOn(window.parent, 'postMessage');
    });

    it('should post the correct message for SET_URL action', () => {
        const message = { action: CUSTOMER_ACTIONS.SET_URL, payload: 'test-url' };
        postMessageToEditor(message);
        expect(window.parent.postMessage).toHaveBeenCalledWith(message, '*');
    });

    it('should post the correct message for SET_BOUNDS action', () => {
        const bounds = { x: 10, y: 20 };
        postMessageToEditor({ action: CUSTOMER_ACTIONS.SET_BOUNDS, payload: bounds });
        expect(window.parent.postMessage).toHaveBeenCalledWith(
            { action: CUSTOMER_ACTIONS.SET_BOUNDS, payload: bounds },
            '*'
        );
    });
});
