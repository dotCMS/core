import { renderHook, act } from '@testing-library/react-hooks';

import * as dotcmsClient from '@dotcms/client';

import { useDotcmsEditorPageAsset } from './useDotcmsEditorPageAsset';

import { DotCMSPageContext } from '../models';

const mockPageData = {
    name: 'Updated Page',
    url: '/updated-page'
};

const mockCurrentPageAsset = {
    name: 'Current Page',
    url: '/current-page'
} as unknown as DotCMSPageContext['pageAsset'];

describe('useDotcmsEditorPageAsset', () => {
    let onFetchPageAssetFromUVESpy: jest.SpyInstance<() => void>;

    // Mock the onFetchPageAssetFromUVE function
    const mockOnFetchPageAssetFromUVE = jest.fn((callback) => {
        const messageCallback = (event: MessageEvent) => {
            if (event.data.name === 'SET_PAGE_DATA') {
                callback(event.data.payload);
            }
        };

        window.addEventListener('message', messageCallback);

        return () => {
            window.removeEventListener('message', messageCallback);
        };
    });

    beforeEach(() => {
        onFetchPageAssetFromUVESpy = jest.spyOn(dotcmsClient, 'onFetchPageAssetFromUVE');
    });

    test('should update pageAsset state on receiving SET_PAGE_DATA message', async () => {
        // Override the actual implementation with the mock
        onFetchPageAssetFromUVESpy.mockImplementation(mockOnFetchPageAssetFromUVE);

        const { result } = renderHook(() => useDotcmsEditorPageAsset(mockCurrentPageAsset));

        expect(result.current).toEqual(mockCurrentPageAsset);

        act(() => {
            const messageEvent = new MessageEvent('message', {
                data: { name: 'SET_PAGE_DATA', payload: mockPageData }
            });
            window.dispatchEvent(messageEvent);
        });

        expect(result.current).toEqual(mockPageData);
    });

    test('should clean up event listener on unmount', () => {
        const removeEventListenerSpy = jest.spyOn(window, 'removeEventListener');

        const { unmount } = renderHook(() => useDotcmsEditorPageAsset(mockCurrentPageAsset));

        unmount();

        expect(removeEventListenerSpy).toHaveBeenCalledTimes(1);
    });
});
