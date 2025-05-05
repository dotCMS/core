import { DotCMSClientConfig, RequestOptions } from '@dotcms/types';

import { NavigationClient } from './navigation-api';

describe('NavigationClient', () => {
    const mockFetch = jest.fn();
    const originalFetch = global.fetch;

    const validConfig: DotCMSClientConfig = {
        dotcmsUrl: 'https://demo.dotcms.com',
        authToken: 'test-token',
        siteId: 'test-site'
    };

    const requestOptions: RequestOptions = {
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
        mockFetch.mockReset();
        global.fetch = mockFetch;
        mockFetch.mockResolvedValue({
            ok: true,
            json: async () => mockNavigationData
        });
    });

    afterAll(() => {
        global.fetch = originalFetch;
    });

    it('should fetch navigation successfully', async () => {
        const navClient = new NavigationClient(validConfig, requestOptions);
        const result = await navClient.get('/');

        expect(mockFetch).toHaveBeenCalledWith(
            'https://demo.dotcms.com/api/v1/nav/',
            requestOptions
        );

        expect(result).toEqual(mockNavigationData.entity);
    });

    it('should fetch navigation with custom path', async () => {
        const navClient = new NavigationClient(validConfig, requestOptions);
        const path = '/products';

        await navClient.get(path);

        expect(mockFetch).toHaveBeenCalledWith(
            'https://demo.dotcms.com/api/v1/nav/products',
            expect.anything()
        );
    });

    it('should fetch navigation with custom depth', async () => {
        const navClient = new NavigationClient(validConfig, requestOptions);
        const depth = 3;

        await navClient.get('/', { depth });

        expect(mockFetch).toHaveBeenCalledWith(
            'https://demo.dotcms.com/api/v1/nav/?depth=3',
            expect.anything()
        );
    });

    it('should normalize path by removing leading slash', async () => {
        const navClient = new NavigationClient(validConfig, requestOptions);

        await navClient.get('/about/');

        expect(mockFetch).toHaveBeenCalledWith(
            'https://demo.dotcms.com/api/v1/nav/about/',
            expect.anything()
        );
    });

    it('should handle root path correctly', async () => {
        const navClient = new NavigationClient(validConfig, requestOptions);

        await navClient.get('/');

        expect(mockFetch).toHaveBeenCalledWith(
            'https://demo.dotcms.com/api/v1/nav/',
            expect.anything()
        );
    });

    it('should handle fetch errors', async () => {
        mockFetch.mockRejectedValue(new Error('Network error'));

        const navClient = new NavigationClient(validConfig, requestOptions);

        await expect(navClient.get('/')).rejects.toThrow('Network error');
    });

    it('should handle non-OK responses', async () => {
        mockFetch.mockResolvedValue({
            ok: false,
            status: 404,
            statusText: 'Not Found'
        });

        const navClient = new NavigationClient(validConfig, requestOptions);

        await expect(navClient.get('/')).rejects.toThrow(
            `Failed to fetch navigation data: Not Found - 404`
        );
    });

    it('should include authorization headers in request', async () => {
        const navClient = new NavigationClient(validConfig, requestOptions);

        await navClient.get('/');

        expect(mockFetch).toHaveBeenCalledWith(
            expect.any(String),
            expect.objectContaining({
                headers: expect.objectContaining({
                    Authorization: 'Bearer test-token'
                })
            })
        );
    });

    it('should merge additional request options', async () => {
        const optionsWithCache: RequestOptions = {
            ...requestOptions,
            cache: 'no-cache',
            credentials: 'include'
        };

        const navClient = new NavigationClient(validConfig, optionsWithCache);

        await navClient.get('/');

        expect(mockFetch).toHaveBeenCalledWith(
            'https://demo.dotcms.com/api/v1/nav/',
            optionsWithCache
        );
    });

    it('should fetch navigation with multiple options', async () => {
        const navClient = new NavigationClient(validConfig, requestOptions);

        await navClient.get('/', { depth: 3, languageId: 2 });

        expect(mockFetch).toHaveBeenCalledWith(
            'https://demo.dotcms.com/api/v1/nav/?depth=3&language_id=2',
            requestOptions
        );
    });
});
