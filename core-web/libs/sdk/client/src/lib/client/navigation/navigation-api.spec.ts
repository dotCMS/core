import { DotCMSClientConfig, DotRequestOptions, DotHttpError } from '@dotcms/types';

import { NavigationClient, DotCMSNavigationError } from './navigation-api';

import { FetchHttpClient } from '../adapters/fetch-http-client';

// Mock the FetchHttpClient
jest.mock('../adapters/fetch-http-client');

describe('NavigationClient', () => {
    const mockRequest = jest.fn();
    const MockedFetchHttpClient = FetchHttpClient as jest.MockedClass<typeof FetchHttpClient>;

    const validConfig: DotCMSClientConfig = {
        dotcmsUrl: 'https://demo.dotcms.com',
        authToken: 'test-token',
        siteId: 'test-site'
    };

    const requestOptions: DotRequestOptions = {
        headers: {
            Authorization: 'Bearer test-token'
        }
    };

    const mockNavigationData = {
        entity: {
            title: 'Main Navigation',
            items: [
                { label: 'Home', url: '/' },
                { label: 'About', url: '/about' },
                { label: 'Contact', url: '/contact' }
            ]
        }
    };

    beforeEach(() => {
        mockRequest.mockReset();
        MockedFetchHttpClient.mockImplementation(
            () =>
                ({
                    request: mockRequest
                }) as Partial<FetchHttpClient> as FetchHttpClient
        );

        mockRequest.mockResolvedValue(mockNavigationData);
    });

    it('should fetch navigation successfully', async () => {
        const navClient = new NavigationClient(validConfig, requestOptions, new FetchHttpClient());
        const result = await navClient.get('/');

        expect(mockRequest).toHaveBeenCalledWith(
            'https://demo.dotcms.com/api/v1/nav/',
            requestOptions
        );

        expect(result).toEqual(mockNavigationData.entity);
    });

    it('should fetch navigation with custom path', async () => {
        const navClient = new NavigationClient(validConfig, requestOptions, new FetchHttpClient());
        const path = '/products';

        await navClient.get(path);

        expect(mockRequest).toHaveBeenCalledWith(
            'https://demo.dotcms.com/api/v1/nav/products',
            expect.anything()
        );
    });

    it('should fetch navigation with custom depth', async () => {
        const navClient = new NavigationClient(validConfig, requestOptions, new FetchHttpClient());
        const depth = 3;

        await navClient.get('/', { depth });

        expect(mockRequest).toHaveBeenCalledWith(
            'https://demo.dotcms.com/api/v1/nav/?depth=3',
            expect.anything()
        );
    });

    it('should normalize path by removing leading slash', async () => {
        const navClient = new NavigationClient(validConfig, requestOptions, new FetchHttpClient());

        await navClient.get('/about/');

        expect(mockRequest).toHaveBeenCalledWith(
            'https://demo.dotcms.com/api/v1/nav/about/',
            expect.anything()
        );
    });

    it('should handle root path correctly', async () => {
        const navClient = new NavigationClient(validConfig, requestOptions, new FetchHttpClient());

        await navClient.get('/');

        expect(mockRequest).toHaveBeenCalledWith(
            'https://demo.dotcms.com/api/v1/nav/',
            expect.anything()
        );
    });

    it('should handle fetch errors', async () => {
        mockRequest.mockRejectedValue(new Error('Network error'));

        const navClient = new NavigationClient(validConfig, requestOptions, new FetchHttpClient());

        await expect(navClient.get('/')).rejects.toThrow(DotCMSNavigationError);
        await expect(navClient.get('/')).rejects.toThrow('Navigation API failed for path \'/\': Network error');
    });

    it('should handle HTTP errors', async () => {
        const httpError = new DotHttpError({
            status: 404,
            statusText: 'Not Found',
            message: 'Navigation not found',
            data: { error: 'Path not found' }
        });
        mockRequest.mockRejectedValue(httpError);

        const navClient = new NavigationClient(validConfig, requestOptions, new FetchHttpClient());

        await expect(navClient.get('/')).rejects.toThrow(DotCMSNavigationError);
        await expect(navClient.get('/')).rejects.toThrow('Navigation API failed for path \'/\': Navigation not found');
    });

    it('should include HTTP error details in navigation error', async () => {
        const httpError = new DotHttpError({
            status: 500,
            statusText: 'Internal Server Error',
            message: 'Server error',
            data: { error: 'Internal error' }
        });
        mockRequest.mockRejectedValue(httpError);

        const navClient = new NavigationClient(validConfig, requestOptions, new FetchHttpClient());

        try {
            await navClient.get('/');
        } catch (error) {
            expect(error).toBeInstanceOf(DotCMSNavigationError);
            if (error instanceof DotCMSNavigationError) {
                expect(error.path).toBe('/');
                expect(error.httpError).toBe(httpError);
                expect(error.httpError?.status).toBe(500);
            }
        }
    });

    it('should throw navigation error for missing path parameter', async () => {
        const navClient = new NavigationClient(validConfig, requestOptions, new FetchHttpClient());

        await expect(navClient.get('')).rejects.toThrow(DotCMSNavigationError);
        await expect(navClient.get('')).rejects.toThrow("The 'path' parameter is required for the Navigation API");
    });

    it('should include authorization headers in request', async () => {
        const navClient = new NavigationClient(validConfig, requestOptions, new FetchHttpClient());

        await navClient.get('/');

        expect(mockRequest).toHaveBeenCalledWith(
            expect.any(String),
            expect.objectContaining({
                headers: expect.objectContaining({
                    Authorization: 'Bearer test-token'
                })
            })
        );
    });

    it('should merge additional request options', async () => {
        const optionsWithCache: DotRequestOptions = {
            ...requestOptions,
            cache: 'no-cache',
            credentials: 'include'
        };

        const navClient = new NavigationClient(
            validConfig,
            optionsWithCache,
            new FetchHttpClient()
        );

        await navClient.get('/');

        expect(mockRequest).toHaveBeenCalledWith(
            'https://demo.dotcms.com/api/v1/nav/',
            optionsWithCache
        );
    });

    it('should fetch navigation with multiple options', async () => {
        const navClient = new NavigationClient(validConfig, requestOptions, new FetchHttpClient());

        await navClient.get('/', { depth: 3, languageId: 2 });

        expect(mockRequest).toHaveBeenCalledWith(
            'https://demo.dotcms.com/api/v1/nav/?depth=3&language_id=2',
            requestOptions
        );
    });
});
