/// <reference types="jest" />

import { DotCMSClientConfig, DotRequestOptions } from '@dotcms/types';

import { CollectionBuilder } from './builders/collection/collection';
import { Content } from './content-api';

import { FetchHttpClient } from '../adapters/fetch-http-client';

// Mock dependencies
jest.mock('../adapters/fetch-http-client');
jest.mock('./builders/collection/collection');

describe('Content', () => {
    const MockedFetchHttpClient = FetchHttpClient as jest.MockedClass<typeof FetchHttpClient>;
    const MockedCollectionBuilder = CollectionBuilder as jest.MockedClass<typeof CollectionBuilder>;

    const validConfig: DotCMSClientConfig = {
        dotcmsUrl: 'https://demo.dotcms.com',
        authToken: 'test-token',
        siteId: 'test-site'
    };

    const requestOptions: DotRequestOptions = {
        headers: {
            Authorization: 'Bearer test-token'
        }
    };

    beforeEach(() => {
        jest.clearAllMocks();
        MockedFetchHttpClient.mockImplementation(
            () =>
                ({
                    request: jest.fn()
                }) as Partial<FetchHttpClient> as FetchHttpClient
        );
    });

    describe('initialization', () => {
        it('should create a Content instance with valid configuration', () => {
            const content = new Content(validConfig, requestOptions, new FetchHttpClient());

            expect(content).toBeDefined();
            expect(content).toBeInstanceOf(Content);
        });

        it('should create a Content instance without optional siteId', () => {
            const configWithoutSite: DotCMSClientConfig = {
                dotcmsUrl: 'https://demo.dotcms.com',
                authToken: 'test-token'
            };

            const content = new Content(configWithoutSite, requestOptions, new FetchHttpClient());

            expect(content).toBeDefined();
            expect(content).toBeInstanceOf(Content);
        });

        it('should create a Content instance with empty request options', () => {
            const content = new Content(validConfig, {}, new FetchHttpClient());

            expect(content).toBeDefined();
            expect(content).toBeInstanceOf(Content);
        });
    });

    describe('getCollection', () => {
        it('should return a CollectionBuilder instance', () => {
            const httpClient = new FetchHttpClient();
            const content = new Content(validConfig, requestOptions, httpClient);

            const result = content.getCollection('Blog');

            expect(result).toBeInstanceOf(CollectionBuilder);
        });

        it('should pass correct parameters to CollectionBuilder', () => {
            const httpClient = new FetchHttpClient();
            const content = new Content(validConfig, requestOptions, httpClient);
            const contentType = 'Article';

            content.getCollection(contentType);

            expect(MockedCollectionBuilder).toHaveBeenCalledWith(
                requestOptions,
                expect.objectContaining({
                    dotcmsUrl: validConfig.dotcmsUrl,
                    authToken: validConfig.authToken,
                    siteId: validConfig.siteId
                }),
                contentType,
                httpClient
            );
        });

        it('should create different CollectionBuilder instances for different content types', () => {
            const httpClient = new FetchHttpClient();
            const content = new Content(validConfig, requestOptions, httpClient);

            content.getCollection('Blog');
            content.getCollection('News');
            content.getCollection('Product');

            expect(MockedCollectionBuilder).toHaveBeenCalledTimes(3);
            expect(MockedCollectionBuilder).toHaveBeenNthCalledWith(
                1,
                expect.anything(),
                expect.anything(),
                'Blog',
                expect.anything()
            );
            expect(MockedCollectionBuilder).toHaveBeenNthCalledWith(
                2,
                expect.anything(),
                expect.anything(),
                'News',
                expect.anything()
            );
            expect(MockedCollectionBuilder).toHaveBeenNthCalledWith(
                3,
                expect.anything(),
                expect.anything(),
                'Product',
                expect.anything()
            );
        });

        it('should support generic type parameter for content type', () => {
            interface BlogPost {
                title: string;
                author: string;
                summary: string;
            }

            const httpClient = new FetchHttpClient();
            const content = new Content(validConfig, requestOptions, httpClient);

            // This test mainly verifies TypeScript compilation works correctly
            const result = content.getCollection<BlogPost>('Blog');

            expect(result).toBeInstanceOf(CollectionBuilder);
            expect(MockedCollectionBuilder).toHaveBeenCalledWith(
                requestOptions,
                expect.anything(),
                'Blog',
                httpClient
            );
        });

        it('should handle content type with special characters', () => {
            const httpClient = new FetchHttpClient();
            const content = new Content(validConfig, requestOptions, httpClient);
            const contentType = 'My-Custom_ContentType.v2';

            content.getCollection(contentType);

            expect(MockedCollectionBuilder).toHaveBeenCalledWith(
                expect.anything(),
                expect.anything(),
                contentType,
                expect.anything()
            );
        });

        it('should handle empty content type string', () => {
            const httpClient = new FetchHttpClient();
            const content = new Content(validConfig, requestOptions, httpClient);

            content.getCollection('');

            expect(MockedCollectionBuilder).toHaveBeenCalledWith(
                expect.anything(),
                expect.anything(),
                '',
                expect.anything()
            );
        });
    });
});
