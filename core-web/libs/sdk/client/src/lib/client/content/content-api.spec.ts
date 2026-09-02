/// <reference types="jest" />

import {
    DotCMSClientConfig,
    DotErrorContent,
    DotHttpError,
    DotRequestOptions
} from '@dotcms/types';

import { CollectionBuilder } from './builders/collection/collection';
import { RawQueryBuilder } from './builders/raw-query/raw-query.builder';
import { Content } from './content-api';

import { FetchHttpClient } from '../adapters/fetch-http-client';

jest.mock('../adapters/fetch-http-client');

describe('Content', () => {
    const mockRequest = jest.fn();
    const MockedFetchHttpClient = FetchHttpClient as jest.MockedClass<typeof FetchHttpClient>;

    const config: DotCMSClientConfig = {
        dotcmsUrl: 'http://localhost:8080',
        authToken: 'test-token',
        siteId: 'test-site'
    };

    const requestOptions: DotRequestOptions = {
        cache: 'no-cache'
    };

    const mockResponseData = {
        entity: {
            jsonObjectView: { contentlets: [{ title: 'Post 1' }, { title: 'Post 2' }] },
            resultsSize: 2
        }
    };

    beforeEach(() => {
        mockRequest.mockReset();
        MockedFetchHttpClient.mockImplementation(
            () => ({ request: mockRequest }) as Partial<FetchHttpClient> as FetchHttpClient
        );
        mockRequest.mockResolvedValue(mockResponseData);
    });

    describe('getCollection()', () => {
        it('returns a CollectionBuilder', () => {
            const content = new Content(config, requestOptions, new FetchHttpClient());
            expect(content.getCollection('Blog')).toBeInstanceOf(CollectionBuilder);
        });

        it('executes an HTTP request when awaited', async () => {
            const content = new Content(config, requestOptions, new FetchHttpClient());
            await content.getCollection('Blog');
            expect(mockRequest).toHaveBeenCalledTimes(1);
        });

        it('sends the content type in the query body', async () => {
            const content = new Content(config, requestOptions, new FetchHttpClient());
            await content.getCollection('Blog');
            const body = JSON.parse(mockRequest.mock.calls[0][1].body);
            expect(body.query).toContain('+contentType:Blog');
        });

        it('returns mapped contentlets from the response', async () => {
            const content = new Content(config, requestOptions, new FetchHttpClient());
            const result = await content.getCollection('Blog');
            expect(result.contentlets).toEqual([{ title: 'Post 1' }, { title: 'Post 2' }]);
        });

        it('returns total from resultsSize', async () => {
            const content = new Content(config, requestOptions, new FetchHttpClient());
            const result = await content.getCollection('Blog');
            expect(result.total).toBe(2);
        });

        it('applies limit() and page() on the builder', async () => {
            const content = new Content(config, requestOptions, new FetchHttpClient());
            await content.getCollection('Blog').limit(5).page(3);
            const body = JSON.parse(mockRequest.mock.calls[0][1].body);
            expect(body.limit).toBe(5);
            expect(body.offset).toBe(10);
        });

        it('applies sortBy() on the builder', async () => {
            const content = new Content(config, requestOptions, new FetchHttpClient());
            await content.getCollection('Blog').sortBy([{ field: 'title', order: 'asc' }]);
            const body = JSON.parse(mockRequest.mock.calls[0][1].body);
            expect(body.sort).toBe('title asc');
        });

        it('throws DotErrorContent when the HTTP request fails', async () => {
            const httpError = new DotHttpError({
                status: 500,
                statusText: 'Internal Server Error',
                message: 'Server error',
                data: {}
            });
            mockRequest.mockRejectedValue(httpError);
            const content = new Content(config, requestOptions, new FetchHttpClient());
            await expect(content.getCollection('Blog')).rejects.toBeInstanceOf(DotErrorContent);
        });

        it('DotErrorContent carries the original query on 404', async () => {
            const httpError = new DotHttpError({
                status: 404,
                statusText: 'Not Found',
                message: 'Content not found',
                data: {}
            });
            mockRequest.mockRejectedValue(httpError);
            const content = new Content(config, requestOptions, new FetchHttpClient());

            try {
                await content.getCollection('Blog');
            } catch (e: unknown) {
                expect(e).toBeInstanceOf(DotErrorContent);
                if (e instanceof DotErrorContent) {
                    expect(e.query).toContain('+contentType:Blog');
                }
            }
        });

        it('passes siteId from config into the query', async () => {
            const content = new Content(config, requestOptions, new FetchHttpClient());
            await content.getCollection('Blog');
            const body = JSON.parse(mockRequest.mock.calls[0][1].body);
            expect(body.query).toContain(`+conhost:${config.siteId}`);
        });

        it('each getCollection() call produces an independent builder', async () => {
            const content = new Content(config, requestOptions, new FetchHttpClient());
            const b1 = content.getCollection('Blog').limit(5);
            const b2 = content.getCollection('News').limit(20);
            await b1;
            await b2;
            const body1 = JSON.parse(mockRequest.mock.calls[0][1].body);
            const body2 = JSON.parse(mockRequest.mock.calls[1][1].body);
            expect(body1.query).toContain('+contentType:Blog');
            expect(body1.limit).toBe(5);
            expect(body2.query).toContain('+contentType:News');
            expect(body2.limit).toBe(20);
        });
    });

    describe('query()', () => {
        it('returns a RawQueryBuilder', () => {
            const content = new Content(config, requestOptions, new FetchHttpClient());
            expect(content.query('+contentType:Blog')).toBeInstanceOf(RawQueryBuilder);
        });

        it('executes an HTTP request when awaited', async () => {
            const content = new Content(config, requestOptions, new FetchHttpClient());
            await content.query('+contentType:Blog');
            expect(mockRequest).toHaveBeenCalledTimes(1);
        });

        it('sends the raw query as-is without adding implicit constraints', async () => {
            const content = new Content(config, requestOptions, new FetchHttpClient());
            await content.query('+contentType:Blog +languageId:1');
            const body = JSON.parse(mockRequest.mock.calls[0][1].body);
            expect(body.query).toBe('+contentType:Blog +languageId:1');
        });

        it('returns mapped contentlets from the response', async () => {
            const content = new Content(config, requestOptions, new FetchHttpClient());
            const result = await content.query('+contentType:Blog');
            expect(result.contentlets).toEqual([{ title: 'Post 1' }, { title: 'Post 2' }]);
        });

        it('throws DotErrorContent when the HTTP request fails', async () => {
            const httpError = new DotHttpError({
                status: 403,
                statusText: 'Forbidden',
                message: 'Access denied',
                data: {}
            });
            mockRequest.mockRejectedValue(httpError);
            const content = new Content(config, requestOptions, new FetchHttpClient());
            await expect(content.query('+contentType:Blog')).rejects.toBeInstanceOf(
                DotErrorContent
            );
        });

        it('does NOT inject siteId into a raw query', async () => {
            const content = new Content(config, requestOptions, new FetchHttpClient());
            await content.query('+contentType:Blog');
            const body = JSON.parse(mockRequest.mock.calls[0][1].body);
            expect(body.query).not.toContain('conhost');
        });
    });
});
