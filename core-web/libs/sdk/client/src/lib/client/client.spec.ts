import { DotCMSClientConfig, RequestOptions, HttpClient } from '@dotcms/types';

import { createDotCMSClient } from './client';
import { Content } from './content/content-api';
import { NavigationClient } from './navigation/navigation-api';
import { PageClient } from './page/page-api';

// Mock the dependencies
jest.mock('./content/content-api');
jest.mock('./navigation/navigation-api');
jest.mock('./page/page-api');

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

        const expectedRequestOptions: RequestOptions = {
            headers: {
                'Content-Type': 'application/json',
                Authorization: 'Bearer test-token'
            }
        };

        expect(PageClient).toHaveBeenCalledWith(
            expect.objectContaining({
                dotcmsUrl: 'https://demo.dotcms.com',
                authToken: 'test-token',
                siteId: 'test-site'
            }),
            expectedRequestOptions,
            expect.any(Object) // httpClient
        );

        expect(Content).toHaveBeenCalledWith(expectedRequestOptions, 'https://demo.dotcms.com', expect.any(Object));

        expect(NavigationClient).toHaveBeenCalledWith(
            expect.objectContaining({
                dotcmsUrl: 'https://demo.dotcms.com',
                authToken: 'test-token',
                siteId: 'test-site'
            }),
            expectedRequestOptions,
            expect.any(Object) // httpClient
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
            expect.any(Object) // httpClient
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
            expect.any(Object) // httpClient
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
            } catch (error) {
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
            } catch (error) {
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

            expect(Content).toHaveBeenCalledWith(expect.anything(), 'https://demo.dotcms.com', expect.any(Object));
        });
    });
});

describe('DotCMSClient with custom HTTP client', () => {
  it('should use custom HTTP client when provided', async () => {
    const mockHttpClient: HttpClient = {
      request: jest.fn().mockResolvedValue({ entity: [{ name: 'test' }] })
    };

    const client = createDotCMSClient({
      dotcmsUrl: 'https://demo.dotcms.com',
      authToken: 'token',
      httpClient: mockHttpClient
    });

    await client.nav.get('/test');

    expect(mockHttpClient.request).toHaveBeenCalled();
  });

  it('should use default FetchHttpClient when no custom client provided', () => {
    const client = createDotCMSClient({
      dotcmsUrl: 'https://demo.dotcms.com',
      authToken: 'token'
    });

    // The client should be created successfully with default HTTP client
    expect(client).toBeDefined();
    expect(client.nav).toBeDefined();
    expect(client.page).toBeDefined();
    expect(client.content).toBeDefined();
  });
});
