import { renderHook, act } from '@testing-library/react-hooks';

import { DotCMSAISearchResponse, DotCMSBasicContentlet, DotCMSEntityState } from '@dotcms/types';

import { useAISearch } from '../../hooks/useAISearch';

interface TestContentlet extends DotCMSBasicContentlet {
    customField?: string;
}

const createMockContentlet = (
    id: string,
    title: string,
    distance?: number
): TestContentlet & { matches?: any } => ({
    archived: false,
    baseType: 'CONTENT',
    contentType: 'TestContent',
    folder: 'folder-id',
    hasTitleImage: false,
    host: 'host-id',
    hostName: 'test.com',
    identifier: id,
    inode: `inode-${id}`,
    languageId: 1,
    live: true,
    locked: false,
    modDate: '2024-01-01',
    modUser: 'user-id',
    modUserName: 'Test User',
    owner: 'owner-id',
    sortOrder: 0,
    stInode: 'st-inode',
    title,
    titleImage: '',
    working: true,
    ...(distance !== undefined && {
        matches: [
            {
                distance,
                extractedText: title
            }
        ]
    })
});

describe('useAISearch', () => {
    const mockSearch = jest.fn();
    const mockClient = {
        ai: {
            search: mockSearch
        }
    } as any;

    const mockIndexName = 'test-index';
    const mockParams = {
        query: {
            limit: 10,
            offset: 0
        }
    };

    const mockResponse: DotCMSAISearchResponse<TestContentlet> = {
        results: [
            createMockContentlet('1', 'Test Content 1', 0.45),
            createMockContentlet('2', 'Test Content 2', 0.4)
        ],
        query: 'test query',
        total: 2,
        timeToEmbeddings: 100,
        threshold: 0.5,
        operator: '<=>',
        offset: 0,
        limit: 10,
        count: 2
    };

    beforeEach(() => {
        jest.clearAllMocks();
    });

    describe('Initial state', () => {
        test('should initialize with idle status and null response', () => {
            const { result } = renderHook(() =>
                useAISearch<TestContentlet>({
                    client: mockClient,
                    indexName: mockIndexName,
                    params: mockParams
                })
            );

            expect(result.current.status).toEqual({ state: DotCMSEntityState.IDLE });
            expect(result.current.response).toBeNull();
            expect(result.current.results).toBeUndefined();
        });
    });

    describe('search function', () => {
        test('should set loading status when search is called', async () => {
            mockSearch.mockImplementation(
                () => new Promise((resolve) => setTimeout(() => resolve(mockResponse), 100))
            );

            const { result } = renderHook(() =>
                useAISearch<TestContentlet>({
                    client: mockClient,
                    indexName: mockIndexName,
                    params: mockParams
                })
            );

            act(() => {
                result.current.search('test query');
            });

            expect(result.current.status).toEqual({ state: DotCMSEntityState.LOADING });
        });

        test('should successfully fetch and set response data', async () => {
            mockSearch.mockResolvedValue(mockResponse);

            const { result, waitForNextUpdate } = renderHook(() =>
                useAISearch<TestContentlet>({
                    client: mockClient,
                    indexName: mockIndexName,
                    params: mockParams
                })
            );

            await act(async () => {
                result.current.search('test query');
                await waitForNextUpdate();
            });

            expect(mockSearch).toHaveBeenCalledWith('test query', mockIndexName, mockParams);
            expect(result.current.status).toEqual({ state: DotCMSEntityState.SUCCESS });
            expect(result.current.response).toEqual(mockResponse);
            expect(result.current.results).toEqual(mockResponse.results);
        });

        test('should handle errors and set error status', async () => {
            const mockError = new Error('Search failed');
            mockSearch.mockRejectedValue(mockError);

            const { result, waitForNextUpdate } = renderHook(() =>
                useAISearch<TestContentlet>({
                    client: mockClient,
                    indexName: mockIndexName,
                    params: mockParams
                })
            );

            await act(async () => {
                result.current.search('test query');
                await waitForNextUpdate();
            });

            expect(result.current.status).toEqual({
                state: DotCMSEntityState.ERROR,
                error: mockError
            });
            expect(result.current.response).toBeNull();
        });

        test('should not make API call when prompt is empty', async () => {
            const { result } = renderHook(() =>
                useAISearch<TestContentlet>({
                    client: mockClient,
                    indexName: mockIndexName,
                    params: mockParams
                })
            );

            await act(async () => {
                await result.current.search('');
            });

            expect(mockSearch).not.toHaveBeenCalled();
            expect(result.current.status).toEqual({ state: DotCMSEntityState.IDLE });
        });

        test('should reset to idle state when prompt is empty', async () => {
            const { result } = renderHook(() =>
                useAISearch<TestContentlet>({
                    client: mockClient,
                    indexName: mockIndexName,
                    params: mockParams
                })
            );

            await act(async () => {
                await result.current.search('test query');
            });

            await act(async () => {
                await result.current.search('');
            });

            expect(mockSearch).not.toHaveBeenNthCalledWith(2, '');
            expect(result.current.status).toEqual({ state: DotCMSEntityState.IDLE });
        });

        test('should not make API call when prompt is only whitespace', async () => {
            const { result } = renderHook(() =>
                useAISearch<TestContentlet>({
                    client: mockClient,
                    indexName: mockIndexName,
                    params: mockParams
                })
            );

            await act(async () => {
                await result.current.search('   ');
            });

            expect(mockSearch).not.toHaveBeenCalled();
            expect(result.current.status).toEqual({ state: DotCMSEntityState.IDLE });
        });

        test('should reset to idle when prompt is only whitespace', async () => {
            const { result } = renderHook(() =>
                useAISearch<TestContentlet>({
                    client: mockClient,
                    indexName: mockIndexName,
                    params: mockParams
                })
            );

            await act(async () => {
                await result.current.search('test query');
            });

            await act(async () => {
                await result.current.search('   ');
            });

            expect(mockSearch).not.toHaveBeenNthCalledWith(2, '   ');
            expect(result.current.status).toEqual({ state: DotCMSEntityState.IDLE });
        });

        test('should merge params with search request', async () => {
            mockSearch.mockResolvedValue(mockResponse);

            const customParams = {
                query: {
                    limit: 20,
                    offset: 10
                },
                config: {
                    threshold: 0.8
                }
            };

            const { result, waitForNextUpdate } = renderHook(() =>
                useAISearch<TestContentlet>({
                    client: mockClient,
                    indexName: mockIndexName,
                    params: customParams
                })
            );

            await act(async () => {
                result.current.search('test query');
                await waitForNextUpdate();
            });

            expect(mockSearch).toHaveBeenCalledWith('test query', mockIndexName, customParams);
        });
    });

    describe('reset function', () => {
        test('should reset to idle state and clear response', async () => {
            mockSearch.mockResolvedValue(mockResponse);

            const { result, waitForNextUpdate } = renderHook(() =>
                useAISearch<TestContentlet>({
                    client: mockClient,
                    indexName: mockIndexName,
                    params: mockParams
                })
            );

            // First perform a search
            await act(async () => {
                result.current.search('test query');
                await waitForNextUpdate();
            });

            expect(result.current.response).toEqual(mockResponse);
            expect(result.current.status).toEqual({ state: DotCMSEntityState.SUCCESS });

            // Then reset
            act(() => {
                result.current.reset();
            });

            expect(result.current.response).toBeNull();
            expect(result.current.results).toBeUndefined();
            expect(result.current.status).toEqual({ state: DotCMSEntityState.IDLE });
        });

        test('should reset from error state', async () => {
            const mockError = new Error('Search failed');
            mockSearch.mockRejectedValue(mockError);

            const { result, waitForNextUpdate } = renderHook(() =>
                useAISearch<TestContentlet>({
                    client: mockClient,
                    indexName: mockIndexName,
                    params: mockParams
                })
            );

            // First trigger an error
            await act(async () => {
                result.current.search('test query');
                await waitForNextUpdate();
            });

            expect(result.current.status).toEqual({
                state: DotCMSEntityState.ERROR,
                error: mockError
            });

            // Then reset
            act(() => {
                result.current.reset();
            });

            expect(result.current.status).toEqual({ state: DotCMSEntityState.IDLE });
            expect(result.current.response).toBeNull();
        });
    });

    describe('callback memoization', () => {
        test('search callback should be stable when dependencies do not change', () => {
            const { result, rerender } = renderHook(() =>
                useAISearch<TestContentlet>({
                    client: mockClient,
                    indexName: mockIndexName,
                    params: mockParams
                })
            );

            const firstSearch = result.current.search;

            rerender();

            expect(result.current.search).toBe(firstSearch);
        });

        test('search callback should update when client changes', () => {
            const newMockClient = {
                ai: {
                    search: jest.fn()
                }
            } as any;

            const { result, rerender } = renderHook(
                ({ client }) =>
                    useAISearch<TestContentlet>({
                        client,
                        indexName: mockIndexName,
                        params: mockParams
                    }),
                { initialProps: { client: mockClient } }
            );

            const firstSearch = result.current.search;

            rerender({ client: newMockClient });

            expect(result.current.search).not.toBe(firstSearch);
        });

        test('reset callback should be stable', () => {
            const { result, rerender } = renderHook(() =>
                useAISearch<TestContentlet>({
                    client: mockClient,
                    indexName: mockIndexName,
                    params: mockParams
                })
            );

            const firstReset = result.current.reset;

            rerender();

            expect(result.current.reset).toBe(firstReset);
        });

        test('search callback should remain stable when params change', () => {
            const initialParams = {
                query: {
                    limit: 10,
                    offset: 0
                }
            };

            const newParams = {
                query: {
                    limit: 20,
                    offset: 10
                }
            };

            const { result, rerender } = renderHook(
                ({ params }) =>
                    useAISearch<TestContentlet>({
                        client: mockClient,
                        indexName: mockIndexName,
                        params
                    }),
                { initialProps: { params: initialParams } }
            );

            const firstSearch = result.current.search;

            // Change params - this creates a new object reference
            rerender({ params: newParams });

            // Search callback should remain stable to prevent infinite loops in useEffect
            expect(result.current.search).toBe(firstSearch);
        });

        test('search should use latest params even though callback identity is stable', async () => {
            mockSearch.mockResolvedValue(mockResponse);

            const initialParams = {
                query: {
                    limit: 10,
                    offset: 0
                }
            };

            const newParams = {
                query: {
                    limit: 20,
                    offset: 10
                },
                config: {
                    threshold: 0.8
                }
            };

            const { result, rerender, waitForNextUpdate } = renderHook(
                ({ params }) =>
                    useAISearch<TestContentlet>({
                        client: mockClient,
                        indexName: mockIndexName,
                        params
                    }),
                { initialProps: { params: initialParams } }
            );

            // Change params
            rerender({ params: newParams });

            // Call search with the new params
            await act(async () => {
                result.current.search('test query');
                await waitForNextUpdate();
            });

            // Should be called with the latest params, not the initial ones
            expect(mockSearch).toHaveBeenCalledWith('test query', mockIndexName, newParams);
        });
    });

    describe('multiple searches', () => {
        test('should handle consecutive searches correctly', async () => {
            const firstResponse: DotCMSAISearchResponse<TestContentlet> = {
                results: [createMockContentlet('1', 'First Result', 0.95)],
                query: 'first query',
                total: 1,
                timeToEmbeddings: 100,
                threshold: 0.5,
                operator: '<=>',
                offset: 0,
                limit: 10,
                count: 1
            };

            const secondResponse: DotCMSAISearchResponse<TestContentlet> = {
                results: [createMockContentlet('2', 'Second Result', 0.9)],
                query: 'second query',
                total: 1,
                timeToEmbeddings: 100,
                threshold: 0.5,
                operator: '<=>',
                offset: 0,
                limit: 10,
                count: 1
            };

            mockSearch.mockResolvedValueOnce(firstResponse).mockResolvedValueOnce(secondResponse);

            const { result, waitForNextUpdate } = renderHook(() =>
                useAISearch<TestContentlet>({
                    client: mockClient,
                    indexName: mockIndexName,
                    params: mockParams
                })
            );

            // First search
            await act(async () => {
                result.current.search('first query');
                await waitForNextUpdate();
            });

            expect(result.current.response).toEqual(firstResponse);
            expect(result.current.results).toEqual(firstResponse.results);

            // Second search
            await act(async () => {
                result.current.search('second query');
                await waitForNextUpdate();
            });

            expect(result.current.response).toEqual(secondResponse);
            expect(result.current.results).toEqual(secondResponse.results);
        });
    });
});
