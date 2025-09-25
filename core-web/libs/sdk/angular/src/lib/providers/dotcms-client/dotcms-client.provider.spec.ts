import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { createDotCMSClient } from '@dotcms/client';

import {
    provideDotCMSClient,
    AngularDotCMSClient,
    DotCMSAngularProviderConfig
} from './dotcms-client.provider';

// Mock the createDotCMSClient function since it's not available in test environment
jest.mock('@dotcms/client', () => ({
    createDotCMSClient: jest.fn()
}));

// Get the mocked function
const mockedCreateDotCMSClient = jest.mocked(createDotCMSClient);

describe('provideDotCMSClient', () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let mockClient: any;

    const validConfig: DotCMSAngularProviderConfig = {
        dotcmsUrl: 'https://demo.dotcms.com',
        authToken: 'test-auth-token-123',
        siteId: 'test-site-id',
        requestOptions: {
            headers: {
                'Content-Type': 'application/json'
            }
        }
    };

    const minimalConfig: DotCMSAngularProviderConfig = {
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

        it('should call createDotCMSClient with provided config when factory is executed', () => {
            TestBed.configureTestingModule({
                providers: [
                    provideHttpClient(),
                    provideHttpClientTesting(),
                    provideDotCMSClient(validConfig)
                ]
            });

            TestBed.inject(AngularDotCMSClient);

            expect(mockedCreateDotCMSClient).toHaveBeenCalledTimes(1);
            expect(mockedCreateDotCMSClient).toHaveBeenCalledWith({
                dotcmsUrl: validConfig.dotcmsUrl,
                authToken: validConfig.authToken,
                siteId: validConfig.siteId,
                httpClient: undefined
            });
        });

        it('should create providers with minimal config', () => {
            const providers = provideDotCMSClient(minimalConfig);

            expect(providers).toBeDefined();
        });

        it('should call createDotCMSClient with minimal config when factory is executed', () => {
            TestBed.configureTestingModule({
                providers: [
                    provideHttpClient(),
                    provideHttpClientTesting(),
                    provideDotCMSClient(minimalConfig)
                ]
            });

            TestBed.inject(AngularDotCMSClient);

            expect(mockedCreateDotCMSClient).toHaveBeenCalledWith({
                dotcmsUrl: minimalConfig.dotcmsUrl,
                authToken: minimalConfig.authToken,
                siteId: undefined,
                httpClient: undefined
            });
        });

        it('should handle config with only required fields', () => {
            const basicConfig: DotCMSAngularProviderConfig = {
                dotcmsUrl: 'https://test.dotcms.com',
                authToken: 'basic-token'
            };

            const providers = provideDotCMSClient(basicConfig);

            expect(providers).toBeDefined();
        });

        it('should call createDotCMSClient with basic config when factory is executed', () => {
            const basicConfig: DotCMSAngularProviderConfig = {
                dotcmsUrl: 'https://test.dotcms.com',
                authToken: 'basic-token'
            };

            TestBed.configureTestingModule({
                providers: [
                    provideHttpClient(),
                    provideHttpClientTesting(),
                    provideDotCMSClient(basicConfig)
                ]
            });

            TestBed.inject(AngularDotCMSClient);

            expect(mockedCreateDotCMSClient).toHaveBeenCalledWith({
                dotcmsUrl: basicConfig.dotcmsUrl,
                authToken: basicConfig.authToken,
                siteId: undefined,
                httpClient: undefined
            });
        });

        it('should handle config with custom request options', () => {
            const configWithCustomOptions: DotCMSAngularProviderConfig = {
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
        });

        it('should call createDotCMSClient with custom request options when factory is executed', () => {
            const configWithCustomOptions: DotCMSAngularProviderConfig = {
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

            TestBed.configureTestingModule({
                providers: [
                    provideHttpClient(),
                    provideHttpClientTesting(),
                    provideDotCMSClient(configWithCustomOptions)
                ]
            });

            TestBed.inject(AngularDotCMSClient);

            expect(mockedCreateDotCMSClient).toHaveBeenCalledWith({
                dotcmsUrl: configWithCustomOptions.dotcmsUrl,
                authToken: configWithCustomOptions.authToken,
                siteId: undefined,
                httpClient: undefined
            });
        });
    });

    describe('Provider Integration', () => {
        beforeEach(() => {
            TestBed.configureTestingModule({
                providers: [
                    provideHttpClient(),
                    provideHttpClientTesting(),
                    provideDotCMSClient(validConfig)
                ]
            });
        });

        it('should provide DotCMSClient instance through dependency injection', () => {
            const client = TestBed.inject(AngularDotCMSClient);

            expect(client).toBeDefined();
            expect(client).toBe(mockClient);
        });

        it('should return same instance when injected multiple times', () => {
            const client1 = TestBed.inject(AngularDotCMSClient);
            const client2 = TestBed.inject(AngularDotCMSClient);

            expect(client1).toBe(client2);
        });

        it('should provide client with expected structure', () => {
            const client = TestBed.inject(AngularDotCMSClient);

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

            TestBed.configureTestingModule({
                providers: [
                    provideHttpClient(),
                    provideHttpClientTesting(),
                    provideDotCMSClient(validConfig)
                ]
            });

            expect(() => {
                TestBed.inject(AngularDotCMSClient);
            }).toThrow(errorMessage);
        });

        it('should handle TypeError from createDotCMSClient', () => {
            const typeError = new TypeError('Invalid URL format');
            mockedCreateDotCMSClient.mockImplementation(() => {
                throw typeError;
            });

            TestBed.configureTestingModule({
                providers: [
                    provideHttpClient(),
                    provideHttpClientTesting(),
                    provideDotCMSClient(validConfig)
                ]
            });

            expect(() => {
                TestBed.inject(AngularDotCMSClient);
            }).toThrow(TypeError);

            // Reset TestBed for second test
            TestBed.resetTestingModule();
            TestBed.configureTestingModule({
                providers: [
                    provideHttpClient(),
                    provideHttpClientTesting(),
                    provideDotCMSClient(validConfig)
                ]
            });

            expect(() => {
                TestBed.inject(AngularDotCMSClient);
            }).toThrow('Invalid URL format');
        });
    });
});
