/// <reference types="jest" />

import {
    DotCMSClientConfig,
    DotErrorContent,
    DotHttpClient,
    DotHttpError,
    DotRequestOptions
} from '@dotcms/types';

import { BaseBuilder } from './base-builder';

import { CONTENT_API_URL } from '../../shared/const';
import { SortBy } from '../../shared/types';

type MockRawResponse<T = unknown> = {
    entity: {
        jsonObjectView: {
            contentlets: T[];
        };
        resultsSize: number;
    };
};

class TestBuilder<T = unknown> extends BaseBuilder<T> {
    #query: string;
    #languageId?: number | string;

    constructor(params: {
        requestOptions: DotRequestOptions;
        config: DotCMSClientConfig;
        httpClient: { request: jest.Mock };
        query?: string;
        languageId?: number | string;
    }) {
        super({
            requestOptions: params.requestOptions,
            config: params.config,
            httpClient: params.httpClient as DotHttpClient
        });
        this.#query = params.query ?? '+contentType:Test';
        this.#languageId = params.languageId;
    }

    protected wrapError(error: unknown): DotErrorContent {
        if (error instanceof DotHttpError) {
            return new DotErrorContent(
                `BaseBuilder failed (fetch): ${error.message}`,
                'test',
                'fetch',
                error,
                this.buildFinalQuery()
            );
        }

        const errorMessage = error instanceof Error ? error.message : 'Unknown error';
        return new DotErrorContent(
            `BaseBuilder failed (fetch): ${errorMessage}`,
            'test',
            'fetch',
            undefined,
            this.buildFinalQuery()
        );
    }

    protected buildFinalQuery(): string {
        return this.#query;
    }

    protected getLanguageId(): number | string | undefined {
        return this.#languageId;
    }
}

describe('BaseBuilder', () => {
    const mockRequest = jest.fn();

    const requestOptions: DotRequestOptions = {
        cache: 'no-cache'
    };

    const config: DotCMSClientConfig = {
        dotcmsUrl: 'http://localhost:8080',
        authToken: 'test-token',
        siteId: 'test-site'
    };

    const createBuilder = (opts?: { languageId?: number | string }) =>
        new TestBuilder({
            requestOptions,
            config,
            httpClient: { request: mockRequest },
            query: '+contentType:Test',
            languageId: opts?.languageId
        });

    const requestURL = `${config.dotcmsUrl}${CONTENT_API_URL}`;

    const baseRequest = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        ...requestOptions
    };

    const mockResponseData: MockRawResponse = {
        entity: {
            jsonObjectView: {
                contentlets: []
            },
            resultsSize: 0
        }
    };

    beforeEach(() => {
        mockRequest.mockReset();
        mockRequest.mockResolvedValue(mockResponseData);
    });

    describe('request formation', () => {
        it('should build a request with default pagination and no languageId when not provided', async () => {
            const builder = createBuilder();

            await builder;

            const body = JSON.parse(mockRequest.mock.calls[0][1].body);
            expect(body).toEqual({
                query: '+contentType:Test',
                render: false,
                limit: 10,
                offset: 0,
                depth: 0
            });

            expect(mockRequest).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify(body)
            });
        });

        it('should include languageId in request body when provided', async () => {
            const builder = createBuilder({ languageId: 2 });

            await builder;

            const body = JSON.parse(mockRequest.mock.calls[0][1].body);
            expect(body).toEqual({
                query: '+contentType:Test',
                render: false,
                limit: 10,
                offset: 0,
                depth: 0,
                languageId: 2
            });
        });

        it('should apply limit/page and compute offset', async () => {
            const builder = createBuilder().limit(20).page(3);

            await builder;

            const body = JSON.parse(mockRequest.mock.calls[0][1].body);
            expect(body.limit).toBe(20);
            expect(body.offset).toBe(40);
        });

        it('should include sort string when sortBy is set', async () => {
            const builder = createBuilder();
            const sortBy: SortBy[] = [
                { field: 'name', order: 'asc' },
                { field: 'modDate', order: 'desc' }
            ];

            await builder.sortBy(sortBy);

            const body = JSON.parse(mockRequest.mock.calls[0][1].body);
            expect(body.sort).toBe('name asc,modDate desc');
        });

        it('should set render=true when render() is called', async () => {
            const builder = createBuilder().render();

            await builder;

            const body = JSON.parse(mockRequest.mock.calls[0][1].body);
            expect(body.render).toBe(true);
        });

        it('should set depth when depth() is called', async () => {
            const builder = createBuilder().depth(2);

            await builder;

            const body = JSON.parse(mockRequest.mock.calls[0][1].body);
            expect(body.depth).toBe(2);
        });
    });

    describe('response mapping', () => {
        it('should map contentlets/total/page/size', async () => {
            mockRequest.mockResolvedValue({
                entity: {
                    jsonObjectView: {
                        contentlets: [{ title: 'a' }, { title: 'b' }]
                    },
                    resultsSize: 200
                }
            } satisfies MockRawResponse<{ title: string }>);

            const response = await createBuilder().page(4);

            expect(response).toEqual({
                contentlets: [{ title: 'a' }, { title: 'b' }],
                total: 200,
                page: 4,
                size: 2
            });
        });

        it('should include sortedBy in mapped response when sortBy is set', async () => {
            const sortBy: SortBy[] = [{ field: 'title', order: 'asc' }];
            const response = await createBuilder().sortBy(sortBy);

            expect(response).toEqual({
                contentlets: [],
                total: 0,
                page: 1,
                size: 0,
                sortedBy: sortBy
            });
        });
    });

    describe('then() behavior', () => {
        it('should pass formatted response to onfulfilled and return the callback result when defined', async () => {
            const builder = createBuilder();
            const onfulfilled = jest.fn((_data) => ({
                contentlets: [],
                total: 0,
                page: 1,
                size: 0
            }));

            const result = await builder.then(onfulfilled);

            expect(onfulfilled).toHaveBeenCalled();
            expect(result).toEqual({
                contentlets: [],
                total: 0,
                page: 1,
                size: 0
            });
        });

        it('should return formatted response when onfulfilled returns undefined', async () => {
            const builder = createBuilder();
            const onfulfilled = jest.fn((_data) => undefined);

            const result = await builder.then(onfulfilled);

            expect(result).toEqual({
                contentlets: [],
                total: 0,
                page: 1,
                size: 0
            });
        });

        it('should wrap errors and throw DotErrorContent when no onrejected is provided', async () => {
            const builder = createBuilder();
            mockRequest.mockRejectedValue(new Error('Network error'));

            await expect(builder).rejects.toBeInstanceOf(DotErrorContent);
        });

        it('should pass wrapped error to onrejected and return callback result when defined', async () => {
            const builder = createBuilder();
            mockRequest.mockRejectedValue(new Error('Boom'));
            const onrejected = jest.fn((err) => err);

            const result = await builder.then(undefined, onrejected);

            expect(onrejected).toHaveBeenCalled();
            expect(result).toBeInstanceOf(DotErrorContent);
        });

        it('should return wrapped error when onrejected returns undefined', async () => {
            const builder = createBuilder();
            mockRequest.mockRejectedValue(new Error('Boom'));
            const onrejected = jest.fn((_err) => undefined);

            const result = await builder.then(undefined, onrejected);

            expect(result).toBeInstanceOf(DotErrorContent);
        });
    });

    describe('depth validation', () => {
        it('should throw when depth is out of range', async () => {
            const builder = createBuilder();
            expect(() => builder.depth(5)).toThrow(
                new Error('Depth value must be between 0 and 3')
            );
        });
    });
});
