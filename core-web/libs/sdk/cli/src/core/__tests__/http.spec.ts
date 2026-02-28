/* eslint-disable @typescript-eslint/no-explicit-any */
import { ofetch } from 'ofetch';

jest.mock('ofetch', () => {
    const mockFetch: any = jest.fn();
    mockFetch.create = jest.fn().mockReturnValue(mockFetch);
    mockFetch.raw = jest.fn();
    mockFetch.native = jest.fn();
    return { ofetch: mockFetch };
});

import { createHttpClient, get, graphql, post, put } from '../http';

describe('http', () => {
    const mockClient = ofetch as any;

    beforeEach(() => {
        jest.clearAllMocks();
    });

    describe('createHttpClient', () => {
        it('should create an ofetch instance with auth headers', () => {
            const createSpy = (ofetch as any).create as jest.Mock;

            createHttpClient({
                baseURL: 'https://demo.dotcms.com',
                token: 'test-token-123'
            });

            expect(createSpy).toHaveBeenCalledWith(
                expect.objectContaining({
                    baseURL: 'https://demo.dotcms.com',
                    headers: expect.objectContaining({
                        Authorization: 'Bearer test-token-123'
                    })
                })
            );
        });

        it('should use custom timeout when provided', () => {
            const createSpy = (ofetch as any).create as jest.Mock;

            createHttpClient({
                baseURL: 'https://demo.dotcms.com',
                token: 'tok',
                timeout: 60000
            });

            expect(createSpy).toHaveBeenCalledWith(
                expect.objectContaining({
                    timeout: 60000
                })
            );
        });

        it('should default timeout to 30 seconds', () => {
            const createSpy = (ofetch as any).create as jest.Mock;

            createHttpClient({
                baseURL: 'https://demo.dotcms.com',
                token: 'tok'
            });

            expect(createSpy).toHaveBeenCalledWith(
                expect.objectContaining({
                    timeout: 30000
                })
            );
        });
    });

    describe('graphql', () => {
        it('should POST to /api/v1/graphql and return data', async () => {
            const mockData = { contentTypes: [{ variable: 'Blog' }] };
            mockClient.mockResolvedValueOnce({ data: mockData });

            const result = await graphql(mockClient, '{ contentTypes { variable } }');

            expect(mockClient).toHaveBeenCalledWith('/api/v1/graphql', {
                method: 'POST',
                body: { query: '{ contentTypes { variable } }' }
            });
            expect(result).toEqual(mockData);
        });

        it('should include variables when provided', async () => {
            mockClient.mockResolvedValueOnce({ data: { result: true } });

            await graphql(mockClient, 'query($id: ID!) { node(id: $id) { id } }', {
                id: '123'
            });

            expect(mockClient).toHaveBeenCalledWith('/api/v1/graphql', {
                method: 'POST',
                body: {
                    query: 'query($id: ID!) { node(id: $id) { id } }',
                    variables: { id: '123' }
                }
            });
        });

        it('should throw on GraphQL errors', async () => {
            mockClient.mockResolvedValueOnce({
                data: null,
                errors: [{ message: 'Field not found' }, { message: 'Type mismatch' }]
            });

            await expect(graphql(mockClient, '{ bad }')).rejects.toThrow(
                'GraphQL error: Field not found; Type mismatch'
            );
        });

        it('should not include variables key when not provided', async () => {
            mockClient.mockResolvedValueOnce({ data: {} });

            await graphql(mockClient, '{ ping }');

            expect(mockClient).toHaveBeenCalledWith('/api/v1/graphql', {
                method: 'POST',
                body: { query: '{ ping }' }
            });
        });
    });

    describe('get', () => {
        it('should perform a GET request', async () => {
            mockClient.mockResolvedValueOnce({ items: [] });

            const result = await get(mockClient, '/api/v1/content');

            expect(mockClient).toHaveBeenCalledWith('/api/v1/content', {
                method: 'GET'
            });
            expect(result).toEqual({ items: [] });
        });

        it('should pass additional options', async () => {
            mockClient.mockResolvedValueOnce([]);

            await get(mockClient, '/api/v1/data', {
                query: { page: 1, perPage: 10 }
            });

            expect(mockClient).toHaveBeenCalledWith('/api/v1/data', {
                method: 'GET',
                query: { page: 1, perPage: 10 }
            });
        });
    });

    describe('post', () => {
        it('should perform a POST request with body', async () => {
            const body = { title: 'Hello' };
            mockClient.mockResolvedValueOnce({ id: '1' });

            const result = await post(mockClient, '/api/v1/content', body);

            expect(mockClient).toHaveBeenCalledWith('/api/v1/content', {
                method: 'POST',
                body: { title: 'Hello' }
            });
            expect(result).toEqual({ id: '1' });
        });
    });

    describe('put', () => {
        it('should perform a PUT request with body', async () => {
            const body = { title: 'Updated' };
            mockClient.mockResolvedValueOnce({ id: '1' });

            const result = await put(mockClient, '/api/v1/content/1', body);

            expect(mockClient).toHaveBeenCalledWith('/api/v1/content/1', {
                method: 'PUT',
                body: { title: 'Updated' }
            });
            expect(result).toEqual({ id: '1' });
        });
    });
});
