/// <reference types="jest" />
/* eslint-disable @typescript-eslint/no-explicit-any */
import { Content } from './content/content-api';
import { ClientConfig, DotCmsClient } from './sdk-js-client';

import * as dotcmsEditor from '../editor/sdk-editor';

global.fetch = jest.fn();

class TestDotCmsClient extends DotCmsClient {
    /**
     * Override the init method to test the static method
     * Allows to test Singleton pattern with different configurations
     *
     * @param {*} config
     * @return {*}
     * @memberof TestDotCmsClient
     */
    static override init(config: ClientConfig) {
        return (this.instance = new TestDotCmsClient(config));
    }
}

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
        let client: TestDotCmsClient;
        let isInsideEditorSpy: jest.SpyInstance<boolean>;

        beforeEach(() => {
            (fetch as jest.Mock).mockClear();

            client = TestDotCmsClient.init({
                dotcmsUrl: 'http://localhost',
                siteId: '123456',
                authToken: 'ABC'
            });

            isInsideEditorSpy = jest.spyOn(dotcmsEditor, 'isInsideEditor');
        });

        describe('init', () => {
            it('should initialize with valid configuration', () => {
                const config = {
                    dotcmsUrl: 'https://example.com',
                    siteId: '123456',
                    authToken: 'ABC'
                };

                const client = TestDotCmsClient.init(config);
                expect(client).toBeDefined();
            });

            it('should throw error on missing dotcmsUrl', () => {
                const config = {
                    dotcmsUrl: '',
                    siteId: '123456',
                    authToken: 'ABC'
                };

                expect(() => {
                    TestDotCmsClient.init(config);
                }).toThrow("Invalid configuration - 'dotcmsUrl' is required");
            });

            it('should throw error if dotcmsUrl is not a valid URL', () => {
                const config = {
                    dotcmsUrl: '//example.com',
                    siteId: '123456',
                    authToken: 'ABC'
                };

                expect(() => {
                    TestDotCmsClient.init(config);
                }).toThrow("Invalid configuration - 'dotcmsUrl' must be a valid URL");
            });

            it('should throw error on missing authToken', () => {
                const config = {
                    dotcmsUrl: 'https://example.com',
                    siteId: '123456',
                    authToken: ''
                };

                expect(() => {
                    TestDotCmsClient.init(config);
                }).toThrow("Invalid configuration - 'authToken' is required");
            });

            it('should use a clean dotcmsUrl when passing an slash at the end', async () => {
                const config = {
                    dotcmsUrl: 'https://example.com/',
                    siteId: '123456',
                    authToken: 'ABC'
                };

                const mockResponse = { content: 'Page data' };
                mockFetchResponse(mockResponse);

                const client = TestDotCmsClient.init(config);

                await client.page.get({ path: '/home' });

                expect(fetch).toHaveBeenCalledWith(
                    'https://example.com/api/v1/page/json/home?host_id=123456',
                    {
                        headers: { Authorization: 'Bearer ABC' }
                    }
                );
            });

            it('should use a clean dotcmsUrl when passing a route at the end', async () => {
                const config = {
                    dotcmsUrl: 'https://example.com/some/cool/route',
                    siteId: '123456',
                    authToken: 'ABC'
                };

                const mockResponse = { content: 'Page data' };
                mockFetchResponse(mockResponse);

                const client = TestDotCmsClient.init(config);

                await client.page.get({ path: '/home' });

                expect(fetch).toHaveBeenCalledWith(
                    'https://example.com/api/v1/page/json/home?host_id=123456',
                    {
                        headers: { Authorization: 'Bearer ABC' }
                    }
                );
            });

            it('should use a clean dotcmsUrl when passing a port and an slash at the end', async () => {
                const config = {
                    dotcmsUrl: 'https://example.com:3434/cool/route',
                    siteId: '123456',
                    authToken: 'ABC'
                };

                const mockResponse = { content: 'Page data' };
                mockFetchResponse(mockResponse);

                const client = TestDotCmsClient.init(config);

                await client.page.get({ path: '/home' });

                expect(fetch).toHaveBeenCalledWith(
                    'https://example.com:3434/api/v1/page/json/home?host_id=123456',
                    {
                        headers: { Authorization: 'Bearer ABC' }
                    }
                );
            });

            it('should use a clean dotcmsUrl when passing a port and an slash at the end', async () => {
                const config = {
                    dotcmsUrl: 'https://example.com:3434/',
                    siteId: '123456',
                    authToken: 'ABC'
                };

                const mockResponse = { content: 'Page data' };
                mockFetchResponse(mockResponse);

                const client = TestDotCmsClient.init(config);

                await client.page.get({ path: '/home' });

                expect(fetch).toHaveBeenCalledWith(
                    'https://example.com:3434/api/v1/page/json/home?host_id=123456',
                    {
                        headers: { Authorization: 'Bearer ABC' }
                    }
                );
            });
        });

        describe('page.get', () => {
            it('should fetch page data successfully', async () => {
                const mockResponse = { content: 'Page data' };
                mockFetchResponse({ entity: mockResponse });

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

            it('should manage error response', () => {
                const mockResponse = {};
                mockFetchResponse(mockResponse, false, 401);

                expect(client.page.get({ path: '/home' })).rejects.toEqual({
                    status: 401,
                    message: 'Unauthorized. Check the token and try again.'
                });
            });
        });

        describe('editor.on', () => {
            it('should listen to FETCH_PAGE_ASSET_FROM_UVE event', () => {
                isInsideEditorSpy.mockReturnValue(true);

                const callback = jest.fn();
                client.editor.on('changes', callback);

                const mockMessageEvent = {
                    data: {
                        name: 'SET_PAGE_DATA',
                        payload: { some: 'test' }
                    }
                };

                window.dispatchEvent(new MessageEvent('message', mockMessageEvent));

                expect(callback).toHaveBeenCalledWith(mockMessageEvent.data.payload);
            });

            it('should do nothing if is outside editor', () => {
                isInsideEditorSpy.mockReturnValue(false);

                const callback = jest.fn();
                client.editor.on('FETCH_PAGE_ASSET_FROM_UVE', callback);

                const mockMessageEvent = {
                    data: {
                        name: 'SET_PAGE_DATA',
                        payload: { some: 'test' }
                    }
                };

                window.dispatchEvent(new MessageEvent('message', mockMessageEvent));

                expect(callback).not.toHaveBeenCalled();
            });
        });

        describe('editor.off', () => {
            it('should remove a page event listener', () => {
                isInsideEditorSpy.mockReturnValue(true);

                const windowSpy = jest.spyOn(window, 'removeEventListener');
                const callback = jest.fn();
                client.editor.on('changes', callback);

                client.editor.off('changes');

                expect(windowSpy).toHaveBeenCalledWith('message', expect.anything());

                const mockMessageEvent = {
                    data: {
                        name: 'SET_PAGE_DATA',
                        payload: { some: 'test' }
                    }
                };

                window.dispatchEvent(new MessageEvent('message', mockMessageEvent));

                expect(callback).not.toHaveBeenCalled();
            });
        });

        describe('content', () => {
            it('should have an instance of the content API', () => {
                expect(client.content instanceof Content).toBeTruthy();
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

            client = TestDotCmsClient.init({
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

            client = TestDotCmsClient.init({
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
            mockFetchResponse({ entity: mockResponse });

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

    describe('Singleton pattern', () => {
        it('should return the same instance and log a warning when calling init multiple times', () => {
            const consoleWarnSpy = jest.spyOn(console, 'warn');
            const expectedWarnMessage =
                'DotCmsClient has already been initialized. Please use the instance to interact with the DotCMS API.';
            const config = {
                dotcmsUrl: 'https://example.com',
                siteId: '123456',
                authToken: 'ABC'
            };

            const config2 = {
                dotcmsUrl: 'https://example.com',
                siteId: '123456',
                authToken: 'DEF'
            };

            const client1 = DotCmsClient.init(config);
            const client2 = DotCmsClient.init(config2);

            expect(client1).toBe(client2);
            expect(consoleWarnSpy).toHaveBeenCalledWith(expectedWarnMessage);
        });
    });

    describe('Properties', () => {
        it('should return he current dotcmsUrl', () => {
            TestDotCmsClient.init({
                dotcmsUrl: 'http://localhost:8080',
                siteId: '123456',
                authToken: 'ABC'
            });

            expect(TestDotCmsClient.dotcmsUrl).toBe('http://localhost:8080');
        });
    });
});
