import { dotCMSCreateClient, DotCMSClientConfig, RequestOptions } from './client';
import { Content } from './content/content-api';
import { NavigationClient } from './navigation/navigation-api';
import { PageClient } from './page/page-api';

// Mock the dependencies
jest.mock('./content/content-api');
jest.mock('./navigation/navigation-api');
jest.mock('./page/page-api');

describe('DotCMSClient', () => {
    const originalConsoleWarn = console.warn;

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
        console.warn = jest.fn();
    });

    afterAll(() => {
        console.warn = originalConsoleWarn;
    });

    it('should initialize sub-clients with correct parameters', () => {
        dotCMSCreateClient(validConfig);

        const expectedRequestOptions: RequestOptions = {
            headers: {
                'Content-Type': 'application/json',
                Authorization: 'Bearer test-token'
            }
        };

        // Verify PageClient was initialized correctly
        expect(PageClient).toHaveBeenCalledWith(
            expect.objectContaining({
                dotcmsUrl: 'https://demo.dotcms.com',
                authToken: 'test-token',
                siteId: 'test-site'
            }),
            expectedRequestOptions
        );

        expect(Content).toHaveBeenCalledWith(expectedRequestOptions, 'https://demo.dotcms.com');

        expect(NavigationClient).toHaveBeenCalledWith(
            expect.objectContaining({
                dotcmsUrl: 'https://demo.dotcms.com',
                authToken: 'test-token',
                siteId: 'test-site'
            }),
            expectedRequestOptions
        );
    });

    it('should add authorization header to request options', () => {
        const configWithoutHeaders: DotCMSClientConfig = {
            dotcmsUrl: 'https://demo.dotcms.com',
            authToken: 'test-token'
        };

        dotCMSCreateClient(configWithoutHeaders);

        expect(PageClient).toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({
                headers: {
                    Authorization: 'Bearer test-token'
                }
            })
        );
    });

    it('should preserve existing headers when adding authorization', () => {
        dotCMSCreateClient(validConfig);

        expect(PageClient).toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: 'Bearer test-token'
                }
            })
        );
    });

    describe('validation and normalization', () => {
        it('should warn when dotcmsUrl is invalid', () => {
            const invalidConfig = {
                ...validConfig,
                dotcmsUrl: 'invalid-url'
            };

            dotCMSCreateClient(invalidConfig);

            expect(console.warn).toHaveBeenCalledWith(
                "Invalid configuration - 'dotcmsUrl' must be a valid URL"
            );
        });

        it('should warn when authToken is missing', () => {
            const invalidConfig = {
                ...validConfig,
                authToken: ''
            };

            dotCMSCreateClient(invalidConfig);

            expect(console.warn).toHaveBeenCalledWith(
                "Invalid configuration - 'authToken' is required"
            );
        });

        it('should extract origin from dotcmsUrl', () => {
            const configWithPath = {
                ...validConfig,
                dotcmsUrl: 'https://demo.dotcms.com/some/path'
            };

            dotCMSCreateClient(configWithPath);

            expect(Content).toHaveBeenCalledWith(expect.anything(), 'https://demo.dotcms.com');
        });

        it('should use window.location.origin as fallback for invalid URL', () => {
            const originalLocationOrigin = window.location.origin;
            Object.defineProperty(window, 'location', {
                value: { origin: 'https://fallback-origin.com' },
                writable: true
            });

            const invalidConfig = {
                ...validConfig,
                dotcmsUrl: 'invalid-url'
            };

            dotCMSCreateClient(invalidConfig);

            expect(Content).toHaveBeenCalledWith(expect.anything(), 'https://fallback-origin.com');

            Object.defineProperty(window, 'location', {
                value: { origin: originalLocationOrigin },
                writable: true
            });
        });
    });
});
