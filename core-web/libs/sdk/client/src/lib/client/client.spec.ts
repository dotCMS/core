import { DotCMSClientConfig, RequestOptions } from '@dotcms/types';

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
            expectedRequestOptions
        );

        expect(Content).toHaveBeenCalledWith(
            expect.objectContaining({
                dotcmsUrl: 'https://demo.dotcms.com',
                authToken: 'test-token',
                siteId: 'test-site'
            }),
            expectedRequestOptions
        );

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

        createDotCMSClient(configWithoutHeaders);

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
        createDotCMSClient(validConfig);

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
                expect.objectContaining({
                    dotcmsUrl: 'https://demo.dotcms.com'
                }),
                expect.anything()
            );
        });
    });
});
