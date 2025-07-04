import { TestBed } from '@angular/core/testing';

import { createDotCMSClient } from '@dotcms/client';
import { DotCMSClientConfig } from '@dotcms/types';

import { provideDotCMSClient, DotCMSClient } from './dotcms-client.provider';

// Mock the createDotCMSClient function since it's not available in test environment
jest.mock('@dotcms/client', () => ({
    createDotCMSClient: jest.fn()
}));

// Get the mocked function
const mockedCreateDotCMSClient = jest.mocked(createDotCMSClient);

describe('provideDotCMSClient', () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let mockClient: any;

    const validConfig: DotCMSClientConfig = {
        dotcmsUrl: 'https://demo.dotcms.com',
        authToken: 'test-auth-token-123',
        siteId: 'test-site-id',
        requestOptions: {
            headers: {
                'Content-Type': 'application/json'
            }
        }
    };

    const minimalConfig: DotCMSClientConfig = {
        dotcmsUrl: 'https://demo.dotcms.com',
        authToken: 'test-auth-token'
    };

    beforeEach(() => {
        // Create mock client with all expected methods
        mockClient = {
            page: {
                get: jest.fn(),
                getPageAsset: jest.fn()
            },
            content: {
                get: jest.fn(),
                getCollection: jest.fn()
            },
            nav: {
                get: jest.fn()
            }
        };

        mockedCreateDotCMSClient.mockReturnValue(mockClient);
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('Success Scenarios', () => {
        it('should create environment providers with valid config', () => {
            const providers = provideDotCMSClient(validConfig);

            expect(providers).toBeDefined();
            expect(providers).toBeInstanceOf(Object);
        });

        it('should call createDotCMSClient with provided config', () => {
            provideDotCMSClient(validConfig);

            expect(mockedCreateDotCMSClient).toHaveBeenCalledTimes(1);
            expect(mockedCreateDotCMSClient).toHaveBeenCalledWith(validConfig);
        });

        it('should create providers with minimal config', () => {
            const providers = provideDotCMSClient(minimalConfig);

            expect(providers).toBeDefined();
            expect(mockedCreateDotCMSClient).toHaveBeenCalledWith(minimalConfig);
        });

        it('should handle config with only required fields', () => {
            const basicConfig: DotCMSClientConfig = {
                dotcmsUrl: 'https://test.dotcms.com',
                authToken: 'basic-token'
            };

            const providers = provideDotCMSClient(basicConfig);

            expect(providers).toBeDefined();
            expect(mockedCreateDotCMSClient).toHaveBeenCalledWith(basicConfig);
        });

        it('should handle config with custom request options', () => {
            const configWithCustomOptions: DotCMSClientConfig = {
                dotcmsUrl: 'https://demo.dotcms.com',
                authToken: 'test-token',
                requestOptions: {
                    headers: {
                        'X-Custom-Header': 'custom-value',
                        Accept: 'application/json'
                    },
                    cache: 'no-cache'
                }
            };

            const providers = provideDotCMSClient(configWithCustomOptions);

            expect(providers).toBeDefined();
            expect(mockedCreateDotCMSClient).toHaveBeenCalledWith(configWithCustomOptions);
        });
    });

    describe('Provider Integration', () => {
        beforeEach(() => {
            TestBed.configureTestingModule({
                providers: [provideDotCMSClient(validConfig)]
            });
        });

        it('should provide DotCMSClient instance through dependency injection', () => {
            const client = TestBed.inject(DotCMSClient);

            expect(client).toBeDefined();
            expect(client).toBe(mockClient);
        });

        it('should return same instance when injected multiple times', () => {
            const client1 = TestBed.inject(DotCMSClient);
            const client2 = TestBed.inject(DotCMSClient);

            expect(client1).toBe(client2);
        });

        it('should provide client with expected structure', () => {
            const client = TestBed.inject(DotCMSClient);

            // Verify the mock client structure is injected correctly
            expect(client).toHaveProperty('page');
            expect(client).toHaveProperty('content');
            expect(client).toHaveProperty('nav');
        });
    });

    describe('Error Scenarios', () => {
        it('should propagate error when createDotCMSClient throws', () => {
            const errorMessage = 'Invalid configuration provided';
            mockedCreateDotCMSClient.mockImplementation(() => {
                throw new Error(errorMessage);
            });

            expect(() => {
                provideDotCMSClient(validConfig);
            }).toThrow(errorMessage);
        });

        it('should handle TypeError from createDotCMSClient', () => {
            const typeError = new TypeError('Invalid URL format');
            mockedCreateDotCMSClient.mockImplementation(() => {
                throw typeError;
            });

            expect(() => {
                provideDotCMSClient(validConfig);
            }).toThrow(TypeError);
            expect(() => {
                provideDotCMSClient(validConfig);
            }).toThrow('Invalid URL format');
        });
    });
});
