/* eslint-disable @typescript-eslint/no-explicit-any */
import { dotcmsClient } from './sdk-js-client';
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
    const client = dotcmsClient.init({
        dotcmsUrl: 'http://localhost',
        host_id: '123456',
        authToken: 'ABC'
    });

    beforeEach(() => {
        (fetch as jest.Mock).mockClear();
    });

    describe('init', () => {
        it('should initialize with valid configuration', () => {
            const config = {
                dotcmsUrl: 'https://example.com',
                host_id: '123456',
                authToken: 'ABC'
            };

            const client = dotcmsClient.init(config);
            expect(client).toBeDefined();
        });

        it('should throw error on missing dotcmsUrl', () => {
            const config = {
                dotcmsUrl: '',
                host_id: '123456',
                authToken: 'ABC'
            };

            expect(() => {
                dotcmsClient.init(config);
            }).toThrow("Invalid configuration - 'dotcmsUrl' is required");
        });

        it('should throw error if dotcmsUrl is not a valid URL', () => {
            const config = {
                dotcmsUrl: '//example.com',
                host_id: '123456',
                authToken: 'ABC'
            };

            expect(() => {
                dotcmsClient.init(config);
            }).toThrow("Invalid configuration - 'dotcmsUrl' must be a valid URL");
        });

        it('should throw error on missing host_id', () => {
            const config = {
                dotcmsUrl: 'https://example.com',
                host_id: '',
                authToken: 'ABC'
            };

            expect(() => {
                dotcmsClient.init(config);
            }).toThrow("Invalid configuration - 'host_id' is required");
        });

        it('should throw error on missing authToken', () => {
            const config = {
                dotcmsUrl: 'https://example.com',
                host_id: '123456',
                authToken: ''
            };

            expect(() => {
                dotcmsClient.init(config);
            }).toThrow("Invalid configuration - 'authToken' is required");
        });
    });

    describe('getPage', () => {
        it('should fetch page data successfully', async () => {
            const mockResponse = { content: 'Page data' };
            mockFetchResponse(mockResponse);

            const data = await client.getPage({ path: '/home' });

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
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            await expect(client.getPage({} as any)).rejects.toThrowError(
                `The 'path' parameter is required for the Page API`
            );
        });

        it('should get the page for specified persona', () => {
            const mockResponse = { content: 'Page data' };
            mockFetchResponse(mockResponse);

            client.getPage({ path: '/home', personaId: 'doe123' });

            expect(fetch).toHaveBeenCalledWith(
                'http://localhost/api/v1/page/json/home?com.dotmarketing.persona.id=doe123&host_id=123456',
                {
                    headers: { Authorization: 'Bearer ABC' }
                }
            );
        });

        it('should get the page for specified host_id', () => {
            const mockResponse = { content: 'Page data' };
            mockFetchResponse(mockResponse);

            client.getPage({ path: '/home', host_id: 'host-123' });

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

            client.getPage({ path: '/home', language_id: 1 });

            expect(fetch).toHaveBeenCalledWith(
                'http://localhost/api/v1/page/json/home?language_id=1&host_id=123456',
                {
                    headers: { Authorization: 'Bearer ABC' }
                }
            );
        });
    });

    describe('getNav', () => {
        it('should fetch navigation data successfully', async () => {
            const mockResponse = { nav: 'Navigation data' };
            mockFetchResponse(mockResponse);

            const data = await client.getNav({ path: '/', depth: 1 });

            expect(fetch).toHaveBeenCalledTimes(1);
            expect(fetch).toHaveBeenCalledWith('http://localhost/api/v1/nav/?depth=1', {
                headers: { Authorization: 'Bearer ABC' }
            });
            expect(data).toEqual(mockResponse);
        });

        it('should correctly handle the root path', async () => {
            const mockResponse = { nav: 'Root nav data' };
            mockFetchResponse(mockResponse);

            const data = await client.getNav({ path: '/' });

            expect(fetch).toHaveBeenCalledWith('http://localhost/api/v1/nav/', {
                headers: { Authorization: 'Bearer ABC' }
            });
            expect(data).toEqual(mockResponse);
        });

        it('should throw an error if the path is not provided', async () => {
            await expect(client.getNav({} as any)).rejects.toThrowError(
                `The 'path' parameter is required for the Nav API`
            );
        });
    });
});
