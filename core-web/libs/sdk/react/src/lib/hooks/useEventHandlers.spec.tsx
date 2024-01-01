import { renderHook } from '@testing-library/react-hooks';

import { CUSTOMER_ACTIONS } from '@dotcms/client';

import { useEventHandlers } from './useEventHandlers'; // Adjust the import path based on your file structure.

// Mocking reload function and getPageElementBound utility
const mockReload = jest.fn();
const mockGetPageElementBound = jest.fn();

jest.mock('../utils/utils', () => ({
    getPageElementBound: () => mockGetPageElementBound()
}));

// Spies for addEventListener and removeEventListener
const addEventListenerSpy = jest.spyOn(window, 'addEventListener');
const removeEventListenerSpy = jest.spyOn(window, 'removeEventListener');
// eslint-disable-next-line @typescript-eslint/no-empty-function
const postMessageSpy = jest.spyOn(window.parent, 'postMessage').mockImplementation(() => {});

describe('useEventHandlers', () => {
    afterEach(() => {
        mockReload.mockClear();
        addEventListenerSpy.mockClear();
        removeEventListenerSpy.mockClear();
        postMessageSpy.mockClear();
        mockGetPageElementBound.mockClear();
    });

    it('attaches event listeners to window on mount', () => {
        const { unmount } = renderHook(() => useEventHandlers({ rows: { current: [] } }));

        expect(addEventListenerSpy).toHaveBeenCalledWith('message', expect.any(Function));
        expect(addEventListenerSpy).toHaveBeenCalledWith('scroll', expect.any(Function));

        unmount();

        expect(removeEventListenerSpy).toHaveBeenCalledWith('message', expect.any(Function));
        expect(removeEventListenerSpy).toHaveBeenCalledWith('scroll', expect.any(Function));
    });

    it('handles reload message event correctly', () => {
        const { unmount } = renderHook(() =>
            useEventHandlers({ rows: { current: [] }, reload: mockReload })
        );

        // Simulate receiving a message event for reload
        window.dispatchEvent(new MessageEvent('message', { data: 'ema-reload-page' }));
        expect(mockReload).toHaveBeenCalled();

        unmount();
    });

    it('handles request bounds message event correctly', () => {
        const { unmount } = renderHook(() => useEventHandlers({ rows: { current: [] } }));

        // Simulate receiving a message event for requesting bounds
        window.dispatchEvent(new MessageEvent('message', { data: 'ema-request-bounds' }));
        expect(postMessageSpy).toHaveBeenCalledWith(
            {
                action: CUSTOMER_ACTIONS.SET_BOUNDS,
                payload: mockGetPageElementBound()
            },
            '*'
        );

        unmount();
    });

    it('handles scroll events correctly', () => {
        const { unmount } = renderHook(() => useEventHandlers({ rows: { current: [] } }));

        // Simulate a scroll event
        window.dispatchEvent(new Event('scroll'));
        expect(postMessageSpy).toHaveBeenCalledWith(
            {
                action: 'scroll'
            },
            '*'
        );

        unmount();
    });
});
