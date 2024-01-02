import { renderHook } from '@testing-library/react-hooks';

import { postMessageToEditor, CUSTOMER_ACTIONS } from '@dotcms/client';

import { usePageEditor } from './usePageEditor';

jest.mock('@dotcms/client', () => ({
    postMessageToEditor: jest.fn(),
    CUSTOMER_ACTIONS: {
        SET_BOUNDS: 'SET_BOUNDS',
        IFRAME_SCROLL: 'IFRAME_SCROLL',
        SET_URL: 'SET_URL'
    }
}));

let reloadSpy: jest.SpyInstance;
let addEventListenerSpy: jest.SpyInstance;
let removeEventListenerSpy: jest.SpyInstance;

afterEach(() => {
    jest.clearAllMocks();
});

describe('usePageEditor', () => {
    beforeAll(() => {
        // Mocking window functions
        delete window.location;
        window.location = { reload: jest.fn() };
        reloadSpy = jest.spyOn(window.location, 'reload');
        addEventListenerSpy = jest.spyOn(window, 'addEventListener');
        removeEventListenerSpy = jest.spyOn(window, 'removeEventListener');

        // Provide mock implementations if necessary
        reloadSpy.mockImplementation(jest.fn());
        addEventListenerSpy.mockImplementation(jest.fn());
        removeEventListenerSpy.mockImplementation(jest.fn());
    });

    afterAll(() => {
        // Restore the original methods
        reloadSpy.mockRestore();
        addEventListenerSpy.mockRestore();
        removeEventListenerSpy.mockRestore();
    });

    it('should call usePostUrlToEditor with the correct pathname', () => {
        renderHook(() => usePageEditor({ pathname: '/test' }));

        expect(postMessageToEditor).toHaveBeenCalledWith({
            action: CUSTOMER_ACTIONS.SET_URL,
            payload: { url: 'test' }
        });
    });

    // Add more tests here for each hook and scenario
});
