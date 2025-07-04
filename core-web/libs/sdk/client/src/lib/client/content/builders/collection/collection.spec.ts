/// <reference types="jest" />

import { ClientOptions, CollectionBuilder } from './collection';

import { CONTENT_API_URL } from '../../shared/const';
import { SortBy } from '../../shared/types';
import { Equals } from '../query/lucene-syntax';

global.fetch = jest.fn().mockReturnValue(
    Promise.resolve({
        ok: true,
        json: () =>
            Promise.resolve({
                entity: {
                    jsonObjectView: {
                        contentlets: []
                    },
                    resultsSize: 0
                }
            })
    })
);

describe('CollectionBuilder', () => {
    const requestOptions: ClientOptions = {
        cache: 'no-cache' // To simulate a valid request
    };

    const serverUrl = 'http://localhost:8080';

    const baseRequest = {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        ...requestOptions
    };

    const requestURL = `${serverUrl}${CONTENT_API_URL}`;

    beforeEach(() => {
        (fetch as jest.Mock).mockClear();
    });

    it('should initialize with valid configuration', async () => {
        const contentType = 'my-content-type';
        const collectionBuilder = new CollectionBuilder(requestOptions, serverUrl, contentType);
        expect(collectionBuilder).toBeDefined();
    });

    describe('successful requests', () => {
        it('should build a query for a basic collection', async () => {
            const contentType = 'song';
            const collectionBuilder = new CollectionBuilder(requestOptions, serverUrl, contentType);

            await collectionBuilder;

            expect(fetch).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+contentType:song +languageId:1 +live:true',
                    render: false,
                    limit: 10,
                    offset: 0,
                    depth: 0
                })
            });
        });

        it('should return the contentlets in the mapped response', async () => {
            const contentType = 'song';
            const collectionBuilder = new CollectionBuilder(requestOptions, serverUrl, contentType);

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
            const collectionBuilder = new CollectionBuilder(requestOptions, serverUrl, contentType);

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
            const collectionBuilder = new CollectionBuilder(requestOptions, serverUrl, contentType);

            await collectionBuilder.language(13);

            expect(fetch).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+contentType:ringsOfPower +languageId:13 +live:true',
                    render: false,
                    limit: 10,
                    offset: 0,
                    depth: 0
                })
            });
        });

        it('should build a query for a collection with render on true', async () => {
            const contentType = 'boringContentType';
            const collectionBuilder = new CollectionBuilder(requestOptions, serverUrl, contentType);

            await collectionBuilder.render();

            expect(fetch).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+contentType:boringContentType +languageId:1 +live:true',
                    render: true,
                    limit: 10,
                    offset: 0,
                    depth: 0
                })
            });
        });

        it("should build a query with multiply sortBy's", async () => {
            const contentType = 'jedi';
            const collectionBuilder = new CollectionBuilder(requestOptions, serverUrl, contentType);

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

            expect(fetch).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+contentType:jedi +languageId:1 +live:true',
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
            const collectionBuilder = new CollectionBuilder(requestOptions, serverUrl, contentType);

            await collectionBuilder.depth(2);

            expect(fetch).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+contentType:droid +languageId:1 +live:true',
                    render: false,
                    limit: 10,
                    offset: 0,
                    depth: 2
                })
            });
        });

        it('should build a query with a specific limit and page', async () => {
            const contentType = 'ship';
            const collectionBuilder = new CollectionBuilder(requestOptions, serverUrl, contentType);

            await collectionBuilder.limit(20).page(3);

            expect(fetch).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+contentType:ship +languageId:1 +live:true',
                    render: false,
                    limit: 20,
                    offset: 40,
                    depth: 0
                })
            });
        });

        it('should build a query with an specific query with main fields and custom fields of the content type', async () => {
            const contentType = 'lightsaber';
            const collectionBuilder = new CollectionBuilder(requestOptions, serverUrl, contentType);

            await collectionBuilder
                .query(
                    (
                        qb // kyberCrystal is a custom field
                    ) => qb.field('kyberCrystal').equals('red')
                )
                .query('+modDate:2024-05-28'); // modDate is a main field so it doesn't need to specify the content type

            expect(fetch).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+lightsaber.kyberCrystal:red +contentType:lightsaber +languageId:1 +live:true +modDate:2024-05-28',
                    render: false,
                    limit: 10,
                    offset: 0,
                    depth: 0
                })
            });
        });

        it("should throw an error if the query doesn't end in an instance of Equals", async () => {
            const contentType = 'jedi';
            const collectionBuilder = new CollectionBuilder(requestOptions, serverUrl, contentType);

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

            expect(fetch).not.toHaveBeenCalled();
        });

        it('should throw an error if the parameter for query is not a function or string', async () => {
            const contentType = 'jedi';
            const collectionBuilder = new CollectionBuilder(requestOptions, serverUrl, contentType);

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

            expect(fetch).not.toHaveBeenCalled();
        });

        it('should throw an error if the depth is out of range (positive value)', async () => {
            const contentType = 'jedi';
            const collectionBuilder = new CollectionBuilder(requestOptions, serverUrl, contentType);

            try {
                // Force the error
                await collectionBuilder.depth(5);
            } catch (error) {
                expect(error).toEqual(new Error('Depth value must be between 0 and 3'));
            }

            expect(fetch).not.toHaveBeenCalled();
        });

        it('should throw an error if the depth is out of range (negative value)', async () => {
            const contentType = 'jedi';
            const collectionBuilder = new CollectionBuilder(requestOptions, serverUrl, contentType);

            try {
                // Force the error
                await collectionBuilder.depth(-5);
            } catch (error) {
                expect(error).toEqual(new Error('Depth value must be between 0 and 3'));
            }

            expect(fetch).not.toHaveBeenCalled();
        });

        it('should build a query for draft content', async () => {
            const contentType = 'draftContent';
            const collectionBuilder = new CollectionBuilder(requestOptions, serverUrl, contentType);

            await collectionBuilder.draft();

            expect(fetch).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+contentType:draftContent +languageId:1 +live:false',
                    render: false,
                    limit: 10,
                    offset: 0,
                    depth: 0
                })
            });
        });

        it('should build a query for a collection with a specific variant', async () => {
            const contentType = 'adventure';
            const collectionBuilder = new CollectionBuilder(requestOptions, serverUrl, contentType);

            await collectionBuilder.variant('dimension-1334-adventure');

            expect(fetch).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+contentType:adventure +variant:dimension-1334-adventure +languageId:1 +live:true',
                    render: false,
                    limit: 10,
                    offset: 0,
                    depth: 0
                })
            });
        });

        it('should handle all the query methods on GetCollection', async () => {
            const contentType = 'forceSensitive';
            const collectionBuilder = new CollectionBuilder(requestOptions, serverUrl, contentType);

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
            expect(fetch).toHaveBeenCalledWith(requestURL, {
                ...baseRequest,
                body: JSON.stringify({
                    query: '+forceSensitive.kyberCrystal:red AND blue +forceSensitive.master:Yoda OR Obi-Wan +contentType:forceSensitive +variant:legends-forceSensitive +languageId:13 +live:false +modDate:2024-05-28 +conhost:MyCoolSite',
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

    describe('fetch is rejected', () => {
        it('should trigger onrejected callback', (done) => {
            const contentType = 'song';
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                serverUrl,
                contentType
            ).language(13);

            // Mock the fetch to return a rejected promise
            (fetch as jest.Mock).mockRejectedValue(new Error('URL is invalid'));

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
                serverUrl,
                contentType
            ).query((dotQuery) => dotQuery.field('author').equals('Linkin Park'));

            // Mock the fetch to return a rejected promise
            (fetch as jest.Mock).mockRejectedValue(new Error('DNS are not resolving'));

            collectionBuilder.then().catch((error) => {
                expect(error).toEqual(new Error('DNS are not resolving'));
                done();
            });
        });

        it('should trigger catch of try catch block', async () => {
            const contentType = 'song';
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                serverUrl,
                contentType
            ).query((dotQuery) => dotQuery.field('author').equals('Linkin Park'));

            // Mock a network error
            (fetch as jest.Mock).mockRejectedValue(new Error('Network error'));

            try {
                await collectionBuilder;
            } catch (e) {
                expect(e).toEqual(new Error('Network error'));
            }
        });
    });

    describe('fetch resolves on error', () => {
        it('should have the error content on then', async () => {
            const contentType = 'song';
            const collectionBuilder = new CollectionBuilder(
                requestOptions,
                serverUrl,
                contentType
            ).limit(10);

            const error = {
                message: 'Internal server error',
                buffer: {
                    stacktrace: 'Some really long server stacktrace'
                }
            };

            // Mock the fetch to return a rejected promise
            (fetch as jest.Mock).mockReturnValue(
                Promise.resolve({
                    status: 500,
                    json: () => Promise.resolve(error)
                })
            );

            collectionBuilder.then((response) => {
                expect(response).toEqual({
                    status: 500,
                    ...error
                });
            });
        });
    });
});
