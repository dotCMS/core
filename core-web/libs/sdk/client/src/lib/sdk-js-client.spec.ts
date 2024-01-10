/* eslint-disable @typescript-eslint/no-explicit-any */
import { DotCmsClient, dotcmsClient } from './sdk-js-client';
global.fetch = jest.fn();

// Utility function to mock fetch responses
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const mockFetchResponse = (body: any, ok = true, status = 200) => {
    (fetch as jest.Mock).mockImplementationOnce(() =>
        Promise.resolve({
            ok,
            status,
            json: () => Promise.resolve(body)
        })
    );
};

describe('DotCmsClient', () => {
    describe('with full configuration', () => {
        let client: DotCmsClient;

        beforeEach(() => {
            (fetch as jest.Mock).mockClear();

            client = dotcmsClient.init({
                dotcmsUrl: 'http://localhost',
                siteId: '123456',
                authToken: 'ABC'
            });
        });

        describe('init', () => {
            it('should initialize with valid configuration', () => {
                const config = {
                    dotcmsUrl: 'https://example.com',
                    siteId: '123456',
                    authToken: 'ABC'
                };

                const client = dotcmsClient.init(config);
                expect(client).toBeDefined();
            });

            it('should throw error on missing dotcmsUrl', () => {
                const config = {
                    dotcmsUrl: '',
                    siteId: '123456',
                    authToken: 'ABC'
                };

                expect(() => {
                    dotcmsClient.init(config);
                }).toThrow("Invalid configuration - 'dotcmsUrl' is required");
            });

            it('should throw error if dotcmsUrl is not a valid URL', () => {
                const config = {
                    dotcmsUrl: '//example.com',
                    siteId: '123456',
                    authToken: 'ABC'
                };

                expect(() => {
                    dotcmsClient.init(config);
                }).toThrow("Invalid configuration - 'dotcmsUrl' must be a valid URL");
            });

            it('should throw error on missing authToken', () => {
                const config = {
                    dotcmsUrl: 'https://example.com',
                    siteId: '123456',
                    authToken: ''
                };

                expect(() => {
                    dotcmsClient.init(config);
                }).toThrow("Invalid configuration - 'authToken' is required");
            });
        });

        describe('page.get', () => {
            it('should fetch page data successfully', async () => {
                const mockResponse = { content: 'Page data' };
                mockFetchResponse(mockResponse);

                const data = await client.page.get({ path: '/home' });

                expect(fetch).toHaveBeenCalledTimes(1);
                expect(fetch).toHaveBeenCalledWith(
                    'http://localhost/api/v1/page/json/home?host_id=123456',
                    {
                        headers: { Authorization: 'Bearer ABC' }
                    }
                );
                expect(data).toEqual(mockResponse);
            });

            it('should throw an error if the path is not provided', async () => {
                await expect(client.page.get({} as any)).rejects.toThrowError(
                    `The 'path' parameter is required for the Page API`
                );
            });

            it('should get the page for specified persona', () => {
                const mockResponse = { content: 'Page data' };
                mockFetchResponse(mockResponse);

                client.page.get({ path: '/home', personaId: 'doe123' });

                expect(fetch).toHaveBeenCalledWith(
                    'http://localhost/api/v1/page/json/home?com.dotmarketing.persona.id=doe123&host_id=123456',
                    {
                        headers: { Authorization: 'Bearer ABC' }
                    }
                );
            });

            it('should get the page for specified siteId', () => {
                const mockResponse = { content: 'Page data' };
                mockFetchResponse(mockResponse);

                client.page.get({ path: '/home', siteId: 'host-123' });

                expect(fetch).toHaveBeenCalledWith(
                    'http://localhost/api/v1/page/json/home?host_id=host-123',
                    {
                        headers: { Authorization: 'Bearer ABC' }
                    }
                );
            });

            it('should get the page for specified language', () => {
                const mockResponse = { content: 'Page data' };
                mockFetchResponse(mockResponse);

                client.page.get({ path: '/home', language_id: 99 });

                expect(fetch).toHaveBeenCalledWith(
                    'http://localhost/api/v1/page/json/home?language_id=99&host_id=123456',
                    {
                        headers: { Authorization: 'Bearer ABC' }
                    }
                );
            });
        });

        describe('nav.get', () => {
            it('should fetch navigation data successfully', async () => {
                const mockResponse = { nav: 'Navigation data' };
                mockFetchResponse(mockResponse);

                const data = await client.nav.get({ path: '/', depth: 1 });

                expect(fetch).toHaveBeenCalledTimes(1);
                expect(fetch).toHaveBeenCalledWith('http://localhost/api/v1/nav/?depth=1', {
                    headers: { Authorization: 'Bearer ABC' }
                });
                expect(data).toEqual(mockResponse);
            });

            it('should correctly handle the root path', async () => {
                const mockResponse = { nav: 'Root nav data' };
                mockFetchResponse(mockResponse);

                const data = await client.nav.get({ path: '/' });

                expect(fetch).toHaveBeenCalledWith('http://localhost/api/v1/nav/', {
                    headers: { Authorization: 'Bearer ABC' }
                });
                expect(data).toEqual(mockResponse);
            });

            it('should throw an error if the path is not provided', async () => {
                await expect(client.nav.get({} as any)).rejects.toThrowError(
                    `The 'path' parameter is required for the Nav API`
                );
            });
        });
    });

    describe('with minimal configuration', () => {
        let client: DotCmsClient;

        beforeEach(() => {
            (fetch as jest.Mock).mockClear();

            client = dotcmsClient.init({
                dotcmsUrl: 'http://localhost',
                authToken: 'ABC'
            });
        });

        it('should get the page without siteId', () => {
            const mockResponse = { content: 'Page data' };
            mockFetchResponse(mockResponse);

            client.page.get({ path: '/home' });

            expect(fetch).toHaveBeenCalledWith('http://localhost/api/v1/page/json/home', {
                headers: { Authorization: 'Bearer ABC' }
            });
        });
    });

    describe('with requestOptions', () => {
        let client: DotCmsClient;

        beforeEach(() => {
            (fetch as jest.Mock).mockClear();

            client = dotcmsClient.init({
                dotcmsUrl: 'http://localhost',
                siteId: '123456',
                authToken: 'ABC',
                requestOptions: {
                    headers: {
                        'X-My-Header': 'my-value'
                    },
                    cache: 'no-cache'
                }
            });
        });

        it('should fetch page data with extra headers and cache', async () => {
            const mockResponse = { content: 'Page data' };
            mockFetchResponse(mockResponse);

            const data = await client.page.get({ path: '/home' });

            expect(fetch).toHaveBeenCalledTimes(1);
            expect(fetch).toHaveBeenCalledWith(
                'http://localhost/api/v1/page/json/home?host_id=123456',
                {
                    headers: { Authorization: 'Bearer ABC', 'X-My-Header': 'my-value' },
                    cache: 'no-cache'
                }
            );
            expect(data).toEqual(mockResponse);
        });

        it('should fetch bav data with extra headers and cache', async () => {
            const mockResponse = { content: 'Page data' };
            mockFetchResponse(mockResponse);

            const data = await client.nav.get();

            expect(fetch).toHaveBeenCalledTimes(1);
            expect(fetch).toHaveBeenCalledWith(
                'http://localhost/api/v1/nav/?depth=0&languageId=1',
                {
                    headers: { Authorization: 'Bearer ABC', 'X-My-Header': 'my-value' },
                    cache: 'no-cache'
                }
            );
            expect(data).toEqual(mockResponse);
        });
    });
});
