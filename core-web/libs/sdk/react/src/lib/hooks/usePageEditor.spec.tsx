import { waitFor } from '@testing-library/react';
import { renderHook } from '@testing-library/react-hooks';

import { postMessageToEditor, CUSTOMER_ACTIONS } from '@dotcms/client';

import { usePageEditor } from './usePageEditor';

const mockBounds = { top: 0, left: 0, right: 100, bottom: 100 };
jest.mock('../utils/utils', () => ({
    getPageElementBound: jest.fn(() => mockBounds)
}));

jest.mock('@dotcms/client', () => ({
    postMessageToEditor: jest.fn(),
    CUSTOMER_ACTIONS: {
        SET_BOUNDS: 'SET_BOUNDS',
        IFRAME_SCROLL: 'IFRAME_SCROLL',
        SET_URL: 'SET_URL'
    }
}));

let reloadSpy: jest.SpyInstance;

afterEach(() => {
    jest.clearAllMocks();
});

describe('usePageEditor', () => {
    beforeAll(() => {
        // Mocking window functions
        delete window.location;
        window.location = { reload: jest.fn() };
        reloadSpy = jest.spyOn(window.location, 'reload');

        // Provide mock implementations if necessary
        reloadSpy.mockImplementation(jest.fn());
    });

    afterEach(() => {
        // Restore the original methods
        reloadSpy.mockRestore();
    });

    it('should throw an error when no pathname is provided', () => {
        expect(() => {
            usePageEditor({});
        }).toThrow('Dotcms page editor required the pathname of your webapp');
    });

    describe('postUrlToEditor', () => {
        it('should call post to parent window with the correct pathname', () => {
            renderHook(() => usePageEditor({ pathname: '/' }));

            expect(postMessageToEditor).toHaveBeenCalledWith({
                action: CUSTOMER_ACTIONS.SET_URL,
                payload: { url: 'index' }
            });
        });
    });

    describe('scrollEvent', () => {
        it('should post to parent window on scroll', async () => {
            renderHook(() => usePageEditor({ pathname: '/test' }));

            window.dispatchEvent(new Event('scroll'));

            await waitFor(() => {
                expect(postMessageToEditor).toHaveBeenCalledWith({
                    action: CUSTOMER_ACTIONS.IFRAME_SCROLL
                });
            });
        });
    });

    describe('reloadFunction', () => {
        it('should reload the page when receiving the reload message', async () => {
            const mockReloadFunction = jest.fn();
            const testPathname = '/test';
            // Act: Render the hook with the mock reload function and dispatch the relevant message event
            renderHook(() =>
                usePageEditor({ pathname: testPathname, reloadFunction: mockReloadFunction })
            );

            // Simulate the message event that should trigger a reload
            const reloadEvent = new MessageEvent('message', {
                data: 'ema-reload-page'
            });
            window.dispatchEvent(reloadEvent);

            // Assert: Check that the mock reload function was called
            await waitFor(() => {
                expect(mockReloadFunction).toHaveBeenCalledTimes(1);
            });
        });

        it('should call the default window.location.reload no custom reload is set', async () => {
            const testPathname = '/test';

            // Arrange: Spy on window.location.reload
            delete window.location;
            window.location = { reload: jest.fn() };
            const reloadSpy = jest.spyOn(window.location, 'reload');

            // Act: Render the hook without a custom reload function and dispatch the relevant message event
            renderHook(() => usePageEditor({ pathname: testPathname }));

            // Simulate the message event that should trigger a reload
            const reloadEvent = new MessageEvent('message', {
                data: 'ema-reload-page'
            });
            window.dispatchEvent(reloadEvent);

            // Assert: Check that window.location.reload was called
            await waitFor(() => {
                expect(reloadSpy).toHaveBeenCalledTimes(1);
            });

            // Cleanup: Remove the spy
            reloadSpy.mockRestore();
        });
    });

    describe('request bounds', () => {
        it('should call postMessageToEditor with SET_BOUNDS action and position data', async () => {
            // Act: Render the hook and dispatch the relevant message event
            renderHook(() => usePageEditor({ pathname: '/test' }));

            // Simulate the message event that should trigger the bounds request
            const boundsEvent = new MessageEvent('message', {
                data: 'ema-request-bounds'
            });
            window.dispatchEvent(boundsEvent);

            // Assert: Check that postMessageToEditor was called with the correct action and payload
            await waitFor(() => {
                expect(postMessageToEditor).toHaveBeenNthCalledWith(2, {
                    action: CUSTOMER_ACTIONS.SET_BOUNDS,
                    payload: mockBounds
                });
            });
        });
    });
});
