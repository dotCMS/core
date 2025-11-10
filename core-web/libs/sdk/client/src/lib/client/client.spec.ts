import {
    DotCMSClientConfig,
    DotRequestOptions,
    DotHttpClient,
    DotCMSNavigationItem
} from '@dotcms/types';

import { FetchHttpClient } from './adapters/fetch-http-client';
import { createDotCMSClient } from './client';
import { Content } from './content/content-api';
import { NavigationClient } from './navigation/navigation-api';
import { PageClient } from './page/page-api';

// Mock the dependencies
jest.mock('./content/content-api');
jest.mock('./navigation/navigation-api');
jest.mock('./page/page-api');
jest.mock('./ai/ai-api');

describe('DotCMSClient', () => {
    const originalTypeError = global.TypeError;
    const mockTypeError = jest.fn().mockImplementation((...args) => new originalTypeError(...args));
    const validConfig: DotCMSClientConfig = {
        dotcmsUrl: 'https://demo.dotcms.com',
        authToken: 'test-token',
        siteId: 'test-site',
        requestOptions: {
            headers: {
                'Content-Type': 'application/json'
            }
        }
    };

    beforeEach(() => {
        jest.clearAllMocks();
        global.TypeError = mockTypeError as unknown as ErrorConstructor;
    });

    afterAll(() => {
        global.TypeError = originalTypeError;
    });

    it('should initialize sub-clients with correct parameters', () => {
        createDotCMSClient(validConfig);

        const expectedRequestOptions: DotRequestOptions = {
            headers: {
                'Content-Type': 'application/json',
                Authorization: 'Bearer test-token'
            }
        };

        expect(PageClient).toHaveBeenCalledWith(
            expect.objectContaining(validConfig),
            expectedRequestOptions,
            expect.any(FetchHttpClient)
        );

        expect(Content).toHaveBeenCalledWith(
            validConfig,
            expectedRequestOptions,
            expect.any(FetchHttpClient)
        );

        expect(NavigationClient).toHaveBeenCalledWith(
            expect.objectContaining({
                dotcmsUrl: 'https://demo.dotcms.com',
                authToken: 'test-token',
                siteId: 'test-site'
            }),
            expectedRequestOptions,
            expect.any(FetchHttpClient) // httpClient
        );
    });

    it('should add authorization header to request options', () => {
        const configWithoutHeaders: DotCMSClientConfig = {
            dotcmsUrl: 'https://demo.dotcms.com',
            authToken: 'test-token'
        };

        createDotCMSClient(configWithoutHeaders);

        expect(PageClient).toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({
                headers: {
                    Authorization: 'Bearer test-token'
                }
            }),
            expect.any(FetchHttpClient) // httpClient
        );
    });

    it('should preserve existing headers when adding authorization', () => {
        createDotCMSClient(validConfig);

        expect(PageClient).toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: 'Bearer test-token'
                }
            }),
            expect.any(FetchHttpClient) // httpClient
        );
    });

    describe('validation and normalization', () => {
        it('should throw TypeError when dotcmsUrl is invalid', () => {
            const invalidConfig = {
                ...validConfig,
                dotcmsUrl: 'invalid-url'
            };

            try {
                createDotCMSClient(invalidConfig);
                fail('Expected TypeError to be thrown');
            } catch {
                // This is expected, verify the error
            }

            expect(mockTypeError).toHaveBeenCalledWith(
                "Invalid configuration - 'dotcmsUrl' must be a valid URL"
            );
        });

        it('should throw TypeError when authToken is missing', () => {
            const invalidConfig = {
                ...validConfig,
                authToken: ''
            };

            try {
                createDotCMSClient(invalidConfig);
                fail('Expected TypeError to be thrown');
            } catch {
                // This is expected, verify the error
            }

            expect(mockTypeError).toHaveBeenCalledWith(
                "Invalid configuration - 'authToken' is required"
            );
        });

        it('should extract origin from dotcmsUrl', () => {
            const configWithPath = {
                ...validConfig,
                dotcmsUrl: 'https://demo.dotcms.com/some/path'
            };

            createDotCMSClient(configWithPath);

            expect(Content).toHaveBeenCalledWith(
                validConfig,
                expect.anything(),
                expect.any(FetchHttpClient)
            );
        });
    });
});

describe('DotCMSClient with custom HTTP client', () => {
    it('should use custom HTTP client when provided', async () => {
        const mockHttpClient: DotHttpClient = {
            request: jest.fn().mockResolvedValue({ entity: [{ name: 'test' }] })
        };

        // Create a spy on the NavigationClient prototype to intercept the get method
        const getSpy = jest
            .spyOn(NavigationClient.prototype, 'get')
            .mockImplementation(async (path: string) => {
                // Call the real HTTP client to verify it's being used
                await mockHttpClient.request(`${path}-test`, {});

                return [] as DotCMSNavigationItem[];
            });

        const client = createDotCMSClient({
            dotcmsUrl: 'https://demo.dotcms.com',
            authToken: 'token',
            httpClient: mockHttpClient
        });

        await client.nav.get('/test');

        expect(mockHttpClient.request).toHaveBeenCalledWith('/test-test', {});

        getSpy.mockRestore();
    });
});
