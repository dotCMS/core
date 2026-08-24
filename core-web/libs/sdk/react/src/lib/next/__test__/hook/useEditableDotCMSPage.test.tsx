import { renderHook, act } from '@testing-library/react-hooks';

import { DotCMSPageResponse, UVEEventType } from '@dotcms/types';
import { getUVEState, initUVE, createUVESubscription, updateNavigation } from '@dotcms/uve';

import { useEditableDotCMSPage } from '../../hooks/useEditableDotCMSPage';

jest.mock('@dotcms/uve', () => ({
    updateNavigation: jest.fn(),
    getUVEState: jest.fn(),
    initUVE: jest.fn(),
    createUVESubscription: jest.fn()
}));

describe('useEditableDotCMSPage', () => {
    const getUVEStateMock = getUVEState as jest.Mock;
    const initUVEMock = initUVE as jest.Mock;
    const createUVESubscriptionMock = createUVESubscription as jest.Mock;
    const updateNavigationMock = updateNavigation as jest.Mock;

    const mockUnsubscribe = jest.fn();
    const mockDestroyUVESubscriptions = jest.fn();

    // Use unknown as intermediate type to avoid type checking issues
    const mockPageResponse = {
        pageAsset: {
            page: {
                pageURI: '/test-page',
                title: 'Test Page',
                metadata: {}
            }
        },
        content: {
            testContent: [{ title: 'Test Item' }]
        },
        graphql: {}
    } as DotCMSPageResponse;

    beforeEach(() => {
        jest.clearAllMocks();
        initUVEMock.mockReturnValue({ destroyUVESubscriptions: mockDestroyUVESubscriptions });
        createUVESubscriptionMock.mockReturnValue({ unsubscribe: mockUnsubscribe });
    });

    test('should initialize with the provided editable page', () => {
        getUVEStateMock.mockReturnValue({ mode: 'EDIT' });

        const { result } = renderHook(() => useEditableDotCMSPage(mockPageResponse));

        expect(result.current).toEqual(mockPageResponse);
    });

    test('should initialize UVE and update navigation when UVE state exists', () => {
        getUVEStateMock.mockReturnValue({ mode: 'EDIT' });

        renderHook(() => useEditableDotCMSPage(mockPageResponse));

        expect(initUVEMock).toHaveBeenCalledWith(mockPageResponse);
        expect(updateNavigationMock).toHaveBeenCalledWith('/test-page');
    });

    test('should not initialize UVE when UVE state does not exist', () => {
        getUVEStateMock.mockReturnValue(undefined);

        renderHook(() => useEditableDotCMSPage(mockPageResponse));

        expect(initUVEMock).not.toHaveBeenCalled();
        expect(updateNavigationMock).not.toHaveBeenCalled();
    });

    test('should still initialize UVE and subscribe to content changes when pageResponse is undefined', () => {
        getUVEStateMock.mockReturnValue({ mode: 'EDIT' });

        renderHook(() => useEditableDotCMSPage(undefined));

        expect(initUVEMock).toHaveBeenCalledWith(undefined);
        expect(updateNavigationMock).not.toHaveBeenCalled();
        expect(createUVESubscriptionMock).toHaveBeenCalledWith(
            UVEEventType.CONTENT_CHANGES,
            expect.any(Function)
        );
    });

    test('should update editable page once content arrives via postMessage after mounting with an undefined pageResponse', () => {
        getUVEStateMock.mockReturnValue({ mode: 'EDIT' });

        let contentChangesCallback: (payload: DotCMSPageResponse) => void;

        createUVESubscriptionMock.mockImplementation((eventType, callback) => {
            if (eventType === UVEEventType.CONTENT_CHANGES) {
                contentChangesCallback = callback;
            }

            return { unsubscribe: mockUnsubscribe };
        });

        const { result } = renderHook(() => useEditableDotCMSPage(undefined));

        expect(result.current).toBeUndefined();

        act(() => {
            contentChangesCallback(mockPageResponse);
        });

        expect(result.current).toEqual(mockPageResponse);
    });

    test('should forward a `{ graphql }`-only bootstrap (e.g. from a caught DotErrorPage) to initUVE, so the editor can retry the fetch', () => {
        getUVEStateMock.mockReturnValue({ mode: 'EDIT' });

        const graphql = {
            query: 'query { page(url: "/draft-page") { ... } }',
            variables: { url: '/draft-page' }
        };

        const { result } = renderHook(() => useEditableDotCMSPage({ graphql }));

        expect(result.current).toBeUndefined();
        expect(initUVEMock).toHaveBeenCalledWith({ graphql });
        expect(updateNavigationMock).not.toHaveBeenCalled();
    });

    test('should update editable page once the editor delivers the real content after a `{ graphql }`-only bootstrap was passed in', () => {
        getUVEStateMock.mockReturnValue({ mode: 'EDIT' });

        let contentChangesCallback: (payload: DotCMSPageResponse) => void;

        createUVESubscriptionMock.mockImplementation((eventType, callback) => {
            if (eventType === UVEEventType.CONTENT_CHANGES) {
                contentChangesCallback = callback;
            }

            return { unsubscribe: mockUnsubscribe };
        });

        const graphql = {
            query: 'query { page(url: "/draft-page") { ... } }',
            variables: { url: '/draft-page' }
        };

        const { result } = renderHook(() => useEditableDotCMSPage({ graphql }));

        expect(result.current).toBeUndefined();

        act(() => {
            contentChangesCallback(mockPageResponse);
        });

        expect(result.current).toEqual(mockPageResponse);
    });

    test('should cleanup subscriptions on unmount', () => {
        getUVEStateMock.mockReturnValue({ mode: 'EDIT' });

        const { unmount } = renderHook(() => useEditableDotCMSPage(mockPageResponse));

        unmount();

        expect(mockDestroyUVESubscriptions).toHaveBeenCalled();
        expect(mockUnsubscribe).toHaveBeenCalled();
    });

    test('should sync to a new pageResponse prop outside UVE (e.g. after client-side navigation)', () => {
        getUVEStateMock.mockReturnValue(undefined);

        const { result, rerender } = renderHook(
            ({ pageResponse }) => useEditableDotCMSPage(pageResponse),
            { initialProps: { pageResponse: mockPageResponse } }
        );

        expect(result.current).toEqual(mockPageResponse);

        const navigatedPageResponse = {
            pageAsset: {
                page: {
                    pageURI: '/test-page',
                    title: 'Test Page',
                    metadata: {}
                }
            },
            content: {
                testContent: [{ title: 'Navigated Item' }]
            },
            graphql: {}
        } as DotCMSPageResponse;

        rerender({ pageResponse: navigatedPageResponse });

        expect(result.current).toEqual(navigatedPageResponse);
    });

    test('should update editable page when content changes are received', () => {
        getUVEStateMock.mockReturnValue({ mode: 'EDIT' });

        let contentChangesCallback: (payload: DotCMSPageResponse) => void;

        createUVESubscriptionMock.mockImplementation((eventType, callback) => {
            if (eventType === UVEEventType.CONTENT_CHANGES) {
                contentChangesCallback = callback;
            }

            return { unsubscribe: mockUnsubscribe };
        });

        const { result } = renderHook(() => useEditableDotCMSPage(mockPageResponse));

        const updatedPage = {
            pageAsset: {
                page: {
                    pageURI: '/test-page',
                    title: 'Test Page',
                    metadata: {}
                }
            },
            content: {
                testContent: [{ title: 'Updated Item' }]
            },
            graphql: {}
        } as DotCMSPageResponse;

        act(() => {
            contentChangesCallback(updatedPage);
        });

        expect(result.current).toEqual(updatedPage);
    });
});
