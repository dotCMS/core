/// <reference types="jest" />

import { GetCollection } from './get-collection';

import { CONTENT_API_URL } from '../../shared/const';
import { SortByArray } from '../../shared/types';

global.fetch = jest.fn().mockReturnValue(
    Promise.resolve({
        ok: true,
        json: () =>
            Promise.resolve({
                entity: {
                    jsonObjectView: {
                        contentlets: []
                    }
                }
            })
    })
);

describe('GetCollection', () => {
    const requestOptions: Omit<RequestInit, 'body' | 'method'> = {
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
        const client = new GetCollection(requestOptions, serverUrl, contentType);
        expect(client).toBeDefined();
    });

    it('should build a query for a basic collection', async () => {
        const contentType = 'song';
        const client = new GetCollection(requestOptions, serverUrl, contentType);

        await client.fetch();

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
        const client = new GetCollection(requestOptions, serverUrl, contentType);

        const response = await client.fetch();

        expect(response).toEqual({
            contentlets: [],
            page: 1,
            size: 0,
            total: 0
        });
    });

    it('should return the contentlets in the mapped response with sort', async () => {
        const contentType = 'song';
        const client = new GetCollection(requestOptions, serverUrl, contentType);

        const sortBy: SortByArray = [
            {
                field: 'title',
                order: 'asc'
            },
            {
                field: 'duration',
                order: 'desc'
            }
        ];

        const response = await client.sortBy(sortBy).fetch();

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
        const client = new GetCollection(requestOptions, serverUrl, contentType);

        await client.language(13).fetch();

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
        const client = new GetCollection(requestOptions, serverUrl, contentType);

        await client.render(true).fetch();

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
        const client = new GetCollection(requestOptions, serverUrl, contentType);

        await client
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
        const client = new GetCollection(requestOptions, serverUrl, contentType);

        await client.depth(2).fetch();

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
        const client = new GetCollection(requestOptions, serverUrl, contentType);

        await client.limit(20).page(3).fetch();

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
        const client = new GetCollection(requestOptions, serverUrl, contentType);

        await client
            .query(
                (
                    qb // modDate is a main field and kyberCrystal is a custom field
                ) => qb.field('modDate').equals('2024-05-28').field('kyberCrystal').equals('red')
            )
            .fetch();

        expect(fetch).toHaveBeenCalledWith(requestURL, {
            ...baseRequest,
            body: JSON.stringify({
                query: '+contentType:lightsaber +modDate:2024-05-28 +lightsaber.kyberCrystal:red',
                render: false,
                limit: 10,
                offset: 0,
                depth: 0
            })
        });
    });

    it('should build a query for draft content', async () => {
        const contentType = 'draftContent';
        const client = new GetCollection(requestOptions, serverUrl, contentType);

        await client.draft(true).fetch();

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
        const client = new GetCollection(requestOptions, serverUrl, contentType);

        await client.variant('dimension-1334-adventure').fetch();

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

    it('should handle all the query methods', async () => {
        const contentType = 'forceSensitive';
        const client = new GetCollection(requestOptions, serverUrl, contentType);

        await client
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
                        .field('modDate')
                        .equals('2024-05-28')
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
            .fetch(); // Fetch the content

        expect(fetch).toHaveBeenCalledWith(requestURL, {
            ...baseRequest,
            body: JSON.stringify({
                query: '+contentType:forceSensitive +languageId:13 +modDate:2024-05-28 +forceSensitive.kyberCrystal:red AND blue +forceSensitive.master:Yoda OR Obi-Wan +live:false +variant:legends-forceSensitive',
                render: true,
                sort: 'name asc,midichlorians desc',
                limit: 20,
                offset: 40,
                depth: 2
            })
        });
    });
});
