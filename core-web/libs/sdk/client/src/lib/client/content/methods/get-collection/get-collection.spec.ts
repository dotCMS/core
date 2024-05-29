/// <reference types="jest" />

import { GetCollection } from './get-collection';

import { Equals } from '../../../../query-builder/lucene-syntax';
import { ClientOptions } from '../../../sdk-js-client';
import { CONTENT_API_URL } from '../../shared/const';
import { SortBy } from '../../shared/types';

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

describe('GetCollection', () => {
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

    it('should initialize with valid configuration', () => {
        const contentType = 'my-content-type';
        const collectionBuilder = new GetCollection(requestOptions, serverUrl, contentType);
        expect(collectionBuilder).toBeDefined();
    });

    it('should build a query for a basic collection', async () => {
        const contentType = 'song';
        const collectionBuilder = new GetCollection(requestOptions, serverUrl, contentType);

        await collectionBuilder.fetch();

        expect(fetch).toHaveBeenCalledWith(requestURL, {
            ...baseRequest,
            body: JSON.stringify({
                query: '+contentType:song',
                render: false,
                limit: 10,
                offset: 0,
                depth: 0
            })
        });
    });

    it('should return the contentlets in the mapped response', async () => {
        const contentType = 'song';
        const collectionBuilder = new GetCollection(requestOptions, serverUrl, contentType);

        const response = await collectionBuilder.fetch();

        expect(response).toEqual({
            contentlets: [],
            page: 1,
            size: 0,
            total: 0
        });
    });

    it('should return the contentlets in the mapped response with sort', async () => {
        const contentType = 'song';
        const collectionBuilder = new GetCollection(requestOptions, serverUrl, contentType);

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

        const response = await collectionBuilder.sortBy(sortBy).fetch();

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
        const collectionBuilder = new GetCollection(requestOptions, serverUrl, contentType);

        await collectionBuilder.language(13).fetch();

        expect(fetch).toHaveBeenCalledWith(requestURL, {
            ...baseRequest,
            body: JSON.stringify({
                query: '+contentType:ringsOfPower +languageId:13',
                render: false,
                limit: 10,
                offset: 0,
                depth: 0
            })
        });
    });

    it('should build a query for a collection with render on true', async () => {
        const contentType = 'boringContentType';
        const collectionBuilder = new GetCollection(requestOptions, serverUrl, contentType);

        await collectionBuilder.render(true).fetch();

        expect(fetch).toHaveBeenCalledWith(requestURL, {
            ...baseRequest,
            body: JSON.stringify({
                query: '+contentType:boringContentType',
                render: true,
                limit: 10,
                offset: 0,
                depth: 0
            })
        });
    });

    it("should build a query with multiply sortBy's", async () => {
        const contentType = 'jedi';
        const collectionBuilder = new GetCollection(requestOptions, serverUrl, contentType);

        await collectionBuilder
            .sortBy([
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
            ])
            .fetch();

        expect(fetch).toHaveBeenCalledWith(requestURL, {
            ...baseRequest,
            body: JSON.stringify({
                query: '+contentType:jedi',
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
        const collectionBuilder = new GetCollection(requestOptions, serverUrl, contentType);

        await collectionBuilder.depth(2).fetch();

        expect(fetch).toHaveBeenCalledWith(requestURL, {
            ...baseRequest,
            body: JSON.stringify({
                query: '+contentType:droid',
                render: false,
                limit: 10,
                offset: 0,
                depth: 2
            })
        });
    });

    it('should build a query with a specific limit and page', async () => {
        const contentType = 'ship';
        const collectionBuilder = new GetCollection(requestOptions, serverUrl, contentType);

        await collectionBuilder.limit(20).page(3).fetch();

        expect(fetch).toHaveBeenCalledWith(requestURL, {
            ...baseRequest,
            body: JSON.stringify({
                query: '+contentType:ship',
                render: false,
                limit: 20,
                offset: 40,
                depth: 0
            })
        });
    });

    it('should build a query with an specific query with main fields and custom fields of the content type', async () => {
        const contentType = 'lightsaber';
        const collectionBuilder = new GetCollection(requestOptions, serverUrl, contentType);

        await collectionBuilder
            .query(
                (
                    qb // kyberCrystal is a custom field
                ) => qb.field('kyberCrystal').equals('red')
            )
            .rawQuery('+modDate:2024-05-28') // modDate is a main field so it doesn't need to specify the content type
            .fetch();

        expect(fetch).toHaveBeenCalledWith(requestURL, {
            ...baseRequest,
            body: JSON.stringify({
                query: '+contentType:lightsaber +lightsaber.kyberCrystal:red +modDate:2024-05-28',
                render: false,
                limit: 10,
                offset: 0,
                depth: 0
            })
        });
    });

    it("should throw an error if the query doesn't end in an instance of Equals", async () => {
        const contentType = 'jedi';
        const collectionBuilder = new GetCollection(requestOptions, serverUrl, contentType);

        try {
            // Force the error
            await collectionBuilder.query((qb) => qb.field('name') as unknown as Equals).fetch();
        } catch (error) {
            expect(error).toEqual(
                new Error('The query builder callback should return an Equals instance')
            );
        }

        expect(fetch).not.toHaveBeenCalled();
    });

    it('should build a query for draft content', async () => {
        const contentType = 'draftContent';
        const collectionBuilder = new GetCollection(requestOptions, serverUrl, contentType);

        await collectionBuilder.draft(true).fetch();

        expect(fetch).toHaveBeenCalledWith(requestURL, {
            ...baseRequest,
            body: JSON.stringify({
                query: '+contentType:draftContent +live:false',
                render: false,
                limit: 10,
                offset: 0,
                depth: 0
            })
        });
    });

    it('should build a query for a collection with a specific variant', async () => {
        const contentType = 'adventure';
        const collectionBuilder = new GetCollection(requestOptions, serverUrl, contentType);

        await collectionBuilder.variant('dimension-1334-adventure').fetch();

        expect(fetch).toHaveBeenCalledWith(requestURL, {
            ...baseRequest,
            body: JSON.stringify({
                query: '+contentType:adventure +variant:dimension-1334-adventure',
                render: false,
                limit: 10,
                offset: 0,
                depth: 0
            })
        });
    });

    it('should handle all the query methods on GetCollection', async () => {
        const contentType = 'forceSensitive';
        const collectionBuilder = new GetCollection(requestOptions, serverUrl, contentType);

        // be sure that this test is updated when new methods are added
        let methods = Object.getOwnPropertyNames(Object.getPrototypeOf(collectionBuilder)) as Array<
            keyof GetCollection
        >;

        // Remove the constructor, fetch and the methods that are not part of the query builder.
        // Fetch method is removed because it is the one that makes the request and we already test that
        // For example: ["constructor", "fetch", "thisMethodIsPrivate", "thisMethodIsNotAQueryMethod", "formatQuery"]
        const methodsToIgnore = ['constructor', 'fetch'];

        // Filter to take only the methods that are part of the query builder
        methods = methods.filter((method) => {
            return (
                !methodsToIgnore.includes(method) && typeof collectionBuilder[method] === 'function'
            );
        });

        // Spy on all the methods
        methods.forEach((method) => {
            jest.spyOn(collectionBuilder, method as keyof GetCollection);
        });

        // Start of the test

        // Call all the methods and fetch the content
        await collectionBuilder
            .language(13) // Language Id
            .render(true) // To retrieve the content with the render
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
            .draft(true) // To retrieve the draft content
            .variant('legends-forceSensitive') // Variant of the content
            .rawQuery('+modDate:2024-05-28 +conhost:MyCoolSite') // Raw query to append to the main query
            .fetch(); // Fetch the content

        // Check that the request was made with the correct query
        expect(fetch).toHaveBeenCalledWith(requestURL, {
            ...baseRequest,
            body: JSON.stringify({
                query: '+contentType:forceSensitive +languageId:13 +forceSensitive.kyberCrystal:red AND blue +forceSensitive.master:Yoda OR Obi-Wan +live:false +variant:legends-forceSensitive +modDate:2024-05-28 +conhost:MyCoolSite',
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
