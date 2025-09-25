/// <reference types="jest" />

import { DotRequestOptions, DotHttpError, DotCMSClientConfig } from '@dotcms/types';

import { CollectionBuilder } from './collection';

import { FetchHttpClient } from '../../../adapters/fetch-http-client';
import { CONTENT_API_URL } from '../../shared/const';
import { SortBy } from '../../shared/types';
import { Equals } from '../query/lucene-syntax';

// Mock the FetchHttpClient
jest.mock('../../../adapters/fetch-http-client');

describe('CollectionBuilder', () => {
    const mockRequest = jest.fn();
    const MockedFetchHttpClient = FetchHttpClient as jest.MockedClass<typeof FetchHttpClient>;

    const requestOptions: DotRequestOptions = {
        cache: 'no-cache' // To simulate a valid request
    };

    const config: DotCMSClientConfig = {
        dotcmsUrl: 'http://localhost:8080',
        authToken: 'test-token',
        siteId: 'test-site'
    };

    const baseRequest = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        ...requestOptions
    };

    const requestURL = `${config.dotcmsUrl}${CONTENT_API_URL}`;

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

    it('should initialize with valid configuration', async () => {
        const contentType = 'my-content-type';
        const collectionBuilder = new CollectionBuilder(
            requestOptions,
            config,
            contentType,
            new FetchHttpClient()
        );
        expect(collectionBuilder).toBeDefined();
    });

    describe('successful requests', () => {
        it('should build a query for a basic collection', async () => {
            const contentType = 'song';
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                config,
                contentType,
                new FetchHttpClient()
            );

            await collectionBuilder;

            expect(mockRequest).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+contentType:song +languageId:1 +live:true +conhost:test-site',
                    render: false,
                    limit: 10,
                    offset: 0,
                    depth: 0
                })
            });
        });

        it('should return the contentlets in the mapped response', async () => {
            const contentType = 'song';
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                config,
                contentType,
                new FetchHttpClient()
            );

            const response = await collectionBuilder;

            expect(response).toEqual({
                contentlets: [],
                page: 1,
                size: 0,
                total: 0
            });
        });

        it('should return the contentlets in the mapped response with sort', async () => {
            const contentType = 'song';
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                config,
                contentType,
                new FetchHttpClient()
            );

            const sortBy: SortBy[] = [
                {
                    field: 'title',
                    order: 'asc'
                },
                {
                    field: 'duration',
                    order: 'desc'
                }
            ];

            const response = await collectionBuilder.sortBy(sortBy);

            expect(response).toEqual({
                contentlets: [],
                page: 1,
                size: 0,
                total: 0,
                sortedBy: sortBy
            });
        });

        it('should build a query for a collection with a specific language', async () => {
            const contentType = 'ringsOfPower';
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                config,
                contentType,
                new FetchHttpClient()
            );

            await collectionBuilder.language(13);

            expect(mockRequest).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+contentType:ringsOfPower +languageId:13 +live:true +conhost:test-site',
                    render: false,
                    limit: 10,
                    offset: 0,
                    depth: 0
                })
            });
        });

        it('should build a query for a collection with render on true', async () => {
            const contentType = 'boringContentType';
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                config,
                contentType,
                new FetchHttpClient()
            );

            await collectionBuilder.render();

            expect(mockRequest).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+contentType:boringContentType +languageId:1 +live:true +conhost:test-site',
                    render: true,
                    limit: 10,
                    offset: 0,
                    depth: 0
                })
            });
        });

        it("should build a query with multiply sortBy's", async () => {
            const contentType = 'jedi';
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                config,
                contentType,
                new FetchHttpClient()
            );

            await collectionBuilder.sortBy([
                {
                    field: 'name',
                    order: 'asc'
                },
                {
                    field: 'force',
                    order: 'desc'
                },
                {
                    field: 'midichlorians',
                    order: 'desc'
                }
            ]);

            expect(mockRequest).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+contentType:jedi +languageId:1 +live:true +conhost:test-site',
                    render: false,
                    sort: 'name asc,force desc,midichlorians desc',
                    limit: 10,
                    offset: 0,
                    depth: 0
                })
            });
        });

        it('should build a query with a specific depth', async () => {
            const contentType = 'droid';
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                config,
                contentType,
                new FetchHttpClient()
            );

            await collectionBuilder.depth(2);

            expect(mockRequest).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+contentType:droid +languageId:1 +live:true +conhost:test-site',
                    render: false,
                    limit: 10,
                    offset: 0,
                    depth: 2
                })
            });
        });

        it('should build a query with a specific limit and page', async () => {
            const contentType = 'ship';
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                config,
                contentType,
                new FetchHttpClient()
            );

            await collectionBuilder.limit(20).page(3);

            expect(mockRequest).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+contentType:ship +languageId:1 +live:true +conhost:test-site',
                    render: false,
                    limit: 20,
                    offset: 40,
                    depth: 0
                })
            });
        });

        it('should build a query with an specific query with main fields and custom fields of the content type', async () => {
            const contentType = 'lightsaber';
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                config,
                contentType,
                new FetchHttpClient()
            );

            await collectionBuilder
                .query(
                    (
                        qb // kyberCrystal is a custom field
                    ) => qb.field('kyberCrystal').equals('red')
                )
                .query('+modDate:2024-05-28'); // modDate is a main field so it doesn't need to specify the content type

            expect(mockRequest).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+lightsaber.kyberCrystal:red +contentType:lightsaber +languageId:1 +live:true +conhost:test-site +modDate:2024-05-28',
                    render: false,
                    limit: 10,
                    offset: 0,
                    depth: 0
                })
            });
        });

        it("should throw an error if the query doesn't end in an instance of Equals", async () => {
            const contentType = 'jedi';
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                config,
                contentType,
                new FetchHttpClient()
            );

            try {
                // Force the error
                await collectionBuilder.query((qb) => qb.field('name') as unknown as Equals);
            } catch (error) {
                expect(error).toEqual(
                    new Error(
                        'Provided query is not valid. A query should end in an equals method call.\nExample:\n(queryBuilder) => queryBuilder.field("title").equals("Hello World")\nSee documentation for more information.'
                    )
                );
            }

            expect(mockRequest).not.toHaveBeenCalled();
        });

        it('should throw an error if the parameter for query is not a function or string', async () => {
            const contentType = 'jedi';
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                config,
                contentType,
                new FetchHttpClient()
            );

            try {
                // Force the error
                await collectionBuilder.query({} as string);
            } catch (error) {
                expect(error).toEqual(
                    new Error(
                        `Parameter for query method should be a buildQuery function or a string.\nExample:\nclient.content.getCollection('Activity').query((queryBuilder) => queryBuilder.field('title').equals('Hello World'))\nor\nclient.content.getCollection('Activity').query('+Activity.title:"Hello World"') \nSee documentation for more information.`
                    )
                );
            }

            expect(mockRequest).not.toHaveBeenCalled();
        });

        it('should throw an error if the depth is out of range (positive value)', async () => {
            const contentType = 'jedi';
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                config,
                contentType,
                new FetchHttpClient()
            );

            try {
                // Force the error
                await collectionBuilder.depth(5);
            } catch (error) {
                expect(error).toEqual(new Error('Depth value must be between 0 and 3'));
            }

            expect(mockRequest).not.toHaveBeenCalled();
        });

        it('should throw an error if the depth is out of range (negative value)', async () => {
            const contentType = 'jedi';
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                config,
                contentType,
                new FetchHttpClient()
            );

            try {
                // Force the error
                await collectionBuilder.depth(-5);
            } catch (error) {
                expect(error).toEqual(new Error('Depth value must be between 0 and 3'));
            }

            expect(mockRequest).not.toHaveBeenCalled();
        });

        it('should build a query for draft content', async () => {
            const contentType = 'draftContent';
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                config,
                contentType,
                new FetchHttpClient()
            );

            await collectionBuilder.draft();

            expect(mockRequest).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+contentType:draftContent +languageId:1 +live:false +conhost:test-site',
                    render: false,
                    limit: 10,
                    offset: 0,
                    depth: 0
                })
            });
        });

        it('should build a query for a collection with a specific variant', async () => {
            const contentType = 'adventure';
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                config,
                contentType,
                new FetchHttpClient()
            );

            await collectionBuilder.variant('dimension-1334-adventure');

            expect(mockRequest).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+contentType:adventure +variant:dimension-1334-adventure +languageId:1 +live:true +conhost:test-site',
                    render: false,
                    limit: 10,
                    offset: 0,
                    depth: 0
                })
            });
        });

        it('should handle all the query methods on GetCollection', async () => {
            const contentType = 'forceSensitive';
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                config,
                contentType,
                new FetchHttpClient()
            );

            // be sure that this test is updated when new methods are added
            let methods = Object.getOwnPropertyNames(
                Object.getPrototypeOf(collectionBuilder)
            ) as Array<keyof CollectionBuilder>;

            // Remove the constructor and the methods that are not part of the query builder.
            // Fetch method is removed because it is the one that makes the request and we already test that
            // For example: ["constructor", "thisMethodIsPrivate", "thisMethodIsNotAQueryMethod", "formatQuery"]
            const methodsToIgnore = ['constructor', 'formatResponse', 'fetchContentApi'];

            // Filter to take only the methods that are part of the query builder
            methods = methods.filter((method) => {
                return (
                    !methodsToIgnore.includes(method) &&
                    typeof collectionBuilder[method] === 'function'
                );
            });

            // Spy on all the methods
            methods.forEach((method) => {
                jest.spyOn(collectionBuilder, method as keyof CollectionBuilder);
            });

            // Start of the test

            // Call all the methods and fetch the content
            await collectionBuilder
                .language(13) // Language Id
                .render() // To retrieve the content with the render
                .sortBy([
                    // Sort by multiple fields
                    {
                        field: 'name',
                        order: 'asc'
                    },
                    {
                        field: 'midichlorians',
                        order: 'desc'
                    }
                ])
                .depth(2) // Depth of the content for relationships
                .limit(20) // Limit of content per page
                .page(3) // Page to fetch
                .query(
                    (
                        qb // Lucene query to append to the main query for more complex queries
                    ) =>
                        qb
                            .field('kyberCrystal')
                            .equals('red')
                            .and()
                            .equals('blue')
                            .field('master')
                            .equals('Yoda')
                            .or()
                            .equals('Obi-Wan')
                )
                .draft() // To retrieve the draft content
                .variant('legends-forceSensitive') // Variant of the content
                .query('+modDate:2024-05-28 +conhost:MyCoolSite'); // Raw query to append to the main query // Fetch the content

            // Check that the request was made with the correct query
            expect(mockRequest).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+forceSensitive.kyberCrystal:red AND blue +forceSensitive.master:Yoda OR Obi-Wan +contentType:forceSensitive +variant:legends-forceSensitive +languageId:13 +live:false +conhost:test-site +modDate:2024-05-28 +conhost:MyCoolSite',
                    render: true,
                    sort: 'name asc,midichlorians desc',
                    limit: 20,
                    offset: 40,
                    depth: 2
                })
            });

            // Chech that all functions for the queryBuilder were called
            methods.forEach((method) => {
                expect(collectionBuilder[method]).toHaveBeenCalled();
            });
        });
    });

    describe('host/site constraint handling', () => {
        it('should add default host to query when siteId is configured', async () => {
            const contentType = 'blog';
            const configWithSite: DotCMSClientConfig = {
                dotcmsUrl: 'http://localhost:8080',
                authToken: 'test-token',
                siteId: 'my-default-site'
            };
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                configWithSite,
                contentType,
                new FetchHttpClient()
            );

            await collectionBuilder;

            expect(mockRequest).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+contentType:blog +languageId:1 +live:true +conhost:my-default-site',
                    render: false,
                    limit: 10,
                    offset: 0,
                    depth: 0
                })
            });
        });

        it('should use user-set host in addition to default when user specifies conhost in raw query', async () => {
            const contentType = 'blog';
            const configWithSite: DotCMSClientConfig = {
                dotcmsUrl: 'http://localhost:8080',
                authToken: 'test-token',
                siteId: 'my-default-site'
            };
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                configWithSite,
                contentType,
                new FetchHttpClient()
            );

            await collectionBuilder.query('+conhost:user-specified-site');

            // The current implementation adds both the default site constraint and the user-specified one
            // because the raw query is appended after the site constraint decision is made
            expect(mockRequest).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+contentType:blog +languageId:1 +live:true +conhost:my-default-site +conhost:user-specified-site',
                    render: false,
                    limit: 10,
                    offset: 0,
                    depth: 0
                })
            });
        });

        it('should not add any host constraint when no siteId is configured and user does not specify one', async () => {
            const contentType = 'blog';
            const configWithoutSite: DotCMSClientConfig = {
                dotcmsUrl: 'http://localhost:8080',
                authToken: 'test-token'
                // No siteId configured
            };
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                configWithoutSite,
                contentType,
                new FetchHttpClient()
            );

            await collectionBuilder;

            expect(mockRequest).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+contentType:blog +languageId:1 +live:true',
                    render: false,
                    limit: 10,
                    offset: 0,
                    depth: 0
                })
            });
        });

        it('should not add default host when user specifies conhost via query builder', async () => {
            const contentType = 'blog';
            const configWithSite: DotCMSClientConfig = {
                dotcmsUrl: 'http://localhost:8080',
                authToken: 'test-token',
                siteId: 'my-default-site'
            };
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                configWithSite,
                contentType,
                new FetchHttpClient()
            );

            await collectionBuilder.query((qb) =>
                qb.field('conhost').equals('user-specified-site')
            );

            // When using the query builder, the conhost constraint is part of the base query
            // so the shouldAddSiteIdConstraint function will detect it and not add the default
            expect(mockRequest).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+conhost:user-specified-site +contentType:blog +languageId:1 +live:true',
                    render: false,
                    limit: 10,
                    offset: 0,
                    depth: 0
                })
            });
        });

        it('should still add default host even when it is explicitly excluded in raw query', async () => {
            const contentType = 'blog';
            const configWithSite: DotCMSClientConfig = {
                dotcmsUrl: 'http://localhost:8080',
                authToken: 'test-token',
                siteId: 'my-default-site'
            };
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                configWithSite,
                contentType,
                new FetchHttpClient()
            );

            await collectionBuilder.query('-conhost:my-default-site');

            // The current implementation still adds the default site constraint because
            // the exclusion is in the raw query which is processed after the site constraint decision
            expect(mockRequest).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+contentType:blog +languageId:1 +live:true +conhost:my-default-site -conhost:my-default-site',
                    render: false,
                    limit: 10,
                    offset: 0,
                    depth: 0
                })
            });
        });
    });

    describe('fetch is rejected', () => {
        it('should trigger onrejected callback', (done) => {
            const contentType = 'song';
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                config,
                contentType,
                new FetchHttpClient()
            ).language(13);

            // Mock the request to return a rejected promise
            mockRequest.mockRejectedValue(new Error('URL is invalid'));

            collectionBuilder.then(
                () => {
                    /* */
                },
                (error) => {
                    expect(error).toEqual(new Error('URL is invalid'));
                    done();
                }
            );
        });

        it('should trigger catch method', (done) => {
            const contentType = 'song';
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                config,
                contentType,
                new FetchHttpClient()
            ).query((dotQuery) => dotQuery.field('author').equals('Linkin Park'));

            // Mock the request to return a rejected promise
            mockRequest.mockRejectedValue(new Error('DNS are not resolving'));

            collectionBuilder.then().catch((error) => {
                expect(error).toEqual(new Error('DNS are not resolving'));
                done();
            });
        });

        it('should trigger catch of try catch block', async () => {
            const contentType = 'song';
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                config,
                contentType,
                new FetchHttpClient()
            ).query((dotQuery) => dotQuery.field('author').equals('Linkin Park'));

            // Mock a network error
            mockRequest.mockRejectedValue(new Error('Network error'));

            try {
                await collectionBuilder;
            } catch (e) {
                expect(e).toEqual(new Error('Network error'));
            }
        });

        it('should throw HttpError when HTTP request fails', async () => {
            const contentType = 'song';
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                config,
                contentType,
                new FetchHttpClient()
            ).limit(10);

            const httpError = new DotHttpError({
                status: 404,
                statusText: 'Not Found',
                message: 'Content not found',
                data: { error: 'Content type does not exist' }
            });

            // Mock the request to throw an HttpError
            mockRequest.mockRejectedValue(httpError);

            try {
                await collectionBuilder;
                fail('Expected HttpError to be thrown');
            } catch (error) {
                expect(error).toBeInstanceOf(DotHttpError);
                expect(error).toEqual(httpError);
                expect((error as DotHttpError).status).toBe(404);
                expect((error as DotHttpError).statusText).toBe('Not Found');
                expect((error as DotHttpError).message).toBe('Content not found');
                expect((error as DotHttpError).data).toEqual({
                    error: 'Content type does not exist'
                });
            }
        });

        it('should handle HttpError in onrejected callback', (done) => {
            const contentType = 'song';
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                config,
                contentType,
                new FetchHttpClient()
            ).language(13);

            const httpError = new DotHttpError({
                status: 500,
                statusText: 'Internal Server Error',
                message: 'Server error occurred',
                data: { error: 'Internal server error' }
            });

            // Mock the request to throw an HttpError
            mockRequest.mockRejectedValue(httpError);

            collectionBuilder.then(
                () => {
                    fail('Expected onrejected callback to be called');
                },
                (error) => {
                    expect(error).toBeInstanceOf(DotHttpError);
                    expect(error).toEqual(httpError);
                    expect((error as DotHttpError).status).toBe(500);
                    expect((error as DotHttpError).statusText).toBe('Internal Server Error');
                    done();
                }
            );
        });
    });
});
