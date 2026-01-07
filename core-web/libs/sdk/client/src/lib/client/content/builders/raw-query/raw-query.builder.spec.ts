/// <reference types="jest" />

import {
    DotRequestOptions,
    DotCMSClientConfig,
    DotHttpError,
    DotErrorContent
} from '@dotcms/types';

import { RawQueryBuilder } from './raw-query.builder';

import { FetchHttpClient } from '../../../adapters/fetch-http-client';
import { CONTENT_API_URL } from '../../shared/const';
import { SortBy } from '../../shared/types';

jest.mock('../../../adapters/fetch-http-client');

describe('RawQueryBuilder', () => {
    const mockRequest = jest.fn();
    const MockedFetchHttpClient = FetchHttpClient as jest.MockedClass<typeof FetchHttpClient>;

    const requestOptions: DotRequestOptions = {
        cache: 'no-cache'
    };

    const config: DotCMSClientConfig = {
        dotcmsUrl: 'http://localhost:8080',
        authToken: 'test-token',
        siteId: 'test-site'
    };

    const createRawQueryBuilder = (rawQuery: string) =>
        new RawQueryBuilder({
            requestOptions,
            config,
            rawQuery,
            httpClient: new FetchHttpClient()
        });

    const requestURL = `${config.dotcmsUrl}${CONTENT_API_URL}`;

    const baseRequest = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        ...requestOptions
    };

    const mockResponseData = {
        entity: {
            jsonObjectView: {
                contentlets: []
            },
            resultsSize: 0
        }
    };

    beforeEach(() => {
        mockRequest.mockReset();
        MockedFetchHttpClient.mockImplementation(
            () =>
                ({
                    request: mockRequest
                }) as Partial<FetchHttpClient> as FetchHttpClient
        );
        mockRequest.mockResolvedValue(mockResponseData);
    });

    describe('request formation', () => {
        it('should send raw query with minimal sanitization and no implicit constraints', async () => {
            const builder = createRawQueryBuilder('   +contentType:Blog    +title:Hello   ');

            await builder;

            expect(mockRequest).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    // sanitizeQuery collapses repeated spaces and trims
                    query: '+contentType:Blog +title:Hello',
                    render: false,
                    limit: 10,
                    offset: 0,
                    depth: 0
                    // NOTE: no languageId, no live/draft, no site/variant constraints are injected
                })
            });
        });

        it('should include languageId in request body when language() is called', async () => {
            const builder = createRawQueryBuilder('+contentType:Blog').language(13);

            await builder;

            expect(mockRequest).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+contentType:Blog',
                    render: false,
                    limit: 10,
                    offset: 0,
                    depth: 0,
                    languageId: 13
                })
            });
        });

        it('should include sort string when sortBy is set', async () => {
            const builder = createRawQueryBuilder('+contentType:Blog');
            const sortBy: SortBy[] = [
                { field: 'title', order: 'asc' },
                { field: 'modDate', order: 'desc' }
            ];

            await builder.sortBy(sortBy);

            const body = JSON.parse(mockRequest.mock.calls[0][1].body);
            expect(body.sort).toBe('title asc,modDate desc');
        });

        it('should apply limit/page and compute offset', async () => {
            const builder = createRawQueryBuilder('+contentType:Blog').limit(20).page(3);

            await builder;

            const body = JSON.parse(mockRequest.mock.calls[0][1].body);
            expect(body.limit).toBe(20);
            expect(body.offset).toBe(40);
        });
    });

    describe('response mapping', () => {
        it('should map the response to GetCollectionResponse format', async () => {
            const response = await createRawQueryBuilder('+contentType:Blog');

            expect(response).toEqual({
                contentlets: [],
                page: 1,
                size: 0,
                total: 0
            });
        });

        it('should include sortedBy in mapped response when sortBy is set', async () => {
            const sortBy: SortBy[] = [{ field: 'title', order: 'asc' }];

            const response = await createRawQueryBuilder('+contentType:Blog').sortBy(sortBy);

            expect(response).toEqual({
                contentlets: [],
                total: 0,
                page: 1,
                size: 0,
                sortedBy: sortBy
            });
        });
    });

    describe('errors', () => {
        it('should wrap DotHttpError as DotErrorContent', async () => {
            const httpError = new DotHttpError({
                status: 404,
                statusText: 'Not Found',
                message: 'Content not found',
                data: { error: 'Missing' }
            });

            mockRequest.mockRejectedValue(httpError);

            await expect(createRawQueryBuilder('+contentType:Blog')).rejects.toBeInstanceOf(
                DotErrorContent
            );
        });

        it('should call onrejected with DotErrorContent and return fallback when undefined', async () => {
            mockRequest.mockRejectedValue(new Error('Boom'));
            const onrejected = jest.fn((_err) => undefined);

            const result = await createRawQueryBuilder('+contentType:Blog').then(
                undefined,
                onrejected
            );

            expect(onrejected).toHaveBeenCalled();
            expect(result).toBeInstanceOf(DotErrorContent);
        });
    });
});
