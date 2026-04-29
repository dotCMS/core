import { DotHttpError } from '@dotcms/types';

import { buildPageQuery, buildQuery, fetchStyleEditorSchemas, mapContentResponse } from './utils';

describe('buildPageQuery()', () => {
    it('generates a query containing the PageContent operation', () => {
        const query = buildPageQuery({});
        expect(query).toContain('query PageContent(');
    });

    it('uses _map for contentlets when no page fragment is provided', () => {
        const query = buildPageQuery({});
        expect(query).toContain('_map');
        expect(query).not.toContain('fragment ClientPage');
    });

    it('includes the ClientPage fragment when page is provided', () => {
        const query = buildPageQuery({ page: 'title url' });
        expect(query).toContain('fragment ClientPage on DotPage');
        expect(query).toContain('title url');
        expect(query).toContain('...ClientPage');
    });

    it('does NOT include ClientPage fragment when page is omitted', () => {
        const query = buildPageQuery({});
        expect(query).not.toContain('...ClientPage');
    });

    it('includes additional queries when provided', () => {
        const query = buildPageQuery({ additionalQueries: 'blogs: BlogCollection { title }' });
        expect(query).toContain('blogs: BlogCollection { title }');
    });

    it('joins multiple fragments when provided', () => {
        const query = buildPageQuery({
            fragments: ['fragment A on Foo { id }', 'fragment B on Bar { name }']
        });
        expect(query).toContain('fragment A on Foo { id }');
        expect(query).toContain('fragment B on Bar { name }');
    });

    it('does not warn when verbose is false and no page provided', () => {
        const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
        buildPageQuery({ verbose: false });
        expect(warnSpy).not.toHaveBeenCalled();
        warnSpy.mockRestore();
    });

    it('does not warn when page is provided even with verbose=true', () => {
        const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
        buildPageQuery({ page: 'title', verbose: true });
        expect(warnSpy).not.toHaveBeenCalled();
        warnSpy.mockRestore();
    });

    it('warns when verbose=true and no page fragment is provided', () => {
        const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
        buildPageQuery({ verbose: true });
        expect(warnSpy).toHaveBeenCalledWith(expect.stringContaining('No page query was found'));
        warnSpy.mockRestore();
    });
});

describe('buildQuery()', () => {
    it('returns empty string when called with falsy input', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        expect(buildQuery(null as any)).toBe('');
    });

    it('returns empty string for an empty object', () => {
        expect(buildQuery({})).toBe('');
    });

    it('maps a single entry to "key: value"', () => {
        expect(buildQuery({ blogs: 'BlogCollection { title }' })).toBe(
            'blogs: BlogCollection { title }'
        );
    });

    it('joins multiple entries with a space', () => {
        const result = buildQuery({
            blogs: 'BlogCollection { title }',
            nav: 'Navigation { href }'
        });
        expect(result).toBe('blogs: BlogCollection { title } nav: Navigation { href }');
    });
});

describe('fetchStyleEditorSchemas()', () => {
    const config = { dotcmsUrl: 'https://demo.dotcms.com' } as Parameters<
        typeof fetchStyleEditorSchemas
    >[1];
    const requestOptions = {} as Parameters<typeof fetchStyleEditorSchemas>[2];

    const makeHttpClient = (impl: () => unknown) =>
        ({ request: jest.fn().mockImplementation(impl) }) as Parameters<
            typeof fetchStyleEditorSchemas
        >[3];

    it('warns and returns [] when pageId is undefined', async () => {
        const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
        const result = await fetchStyleEditorSchemas(
            undefined,
            config,
            requestOptions,
            makeHttpClient(() => undefined)
        );
        expect(warnSpy).toHaveBeenCalledWith(
            expect.stringContaining('fetchStyleEditorSchemas called without a pageId')
        );
        expect(result).toEqual([]);
        warnSpy.mockRestore();
    });

    it('returns entity array on success', async () => {
        const schemas = [{ id: '1' }];
        const httpClient = makeHttpClient(() => Promise.resolve({ entity: schemas }));
        const result = await fetchStyleEditorSchemas('page-id', config, requestOptions, httpClient);
        expect(result).toEqual(schemas);
    });

    it('returns [] when entity is not an array', async () => {
        const httpClient = makeHttpClient(() => Promise.resolve({ entity: null }));
        const result = await fetchStyleEditorSchemas('page-id', config, requestOptions, httpClient);
        expect(result).toEqual([]);
    });

    it('warns with auth message on 401 error', async () => {
        const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
        const error = new DotHttpError({
            status: 401,
            statusText: 'Unauthorized',
            message: 'Unauthorized'
        });
        const httpClient = makeHttpClient(() => Promise.reject(error));
        const result = await fetchStyleEditorSchemas('page-id', config, requestOptions, httpClient);
        expect(warnSpy).toHaveBeenCalledWith(
            expect.stringContaining('Style editor schemas request failed with 401')
        );
        expect(result).toEqual([]);
        warnSpy.mockRestore();
    });

    it('warns with auth message on 403 error', async () => {
        const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
        const error = new DotHttpError({
            status: 403,
            statusText: 'Forbidden',
            message: 'Forbidden'
        });
        const httpClient = makeHttpClient(() => Promise.reject(error));
        const result = await fetchStyleEditorSchemas('page-id', config, requestOptions, httpClient);
        expect(warnSpy).toHaveBeenCalledWith(
            expect.stringContaining('Style editor schemas request failed with 403')
        );
        expect(result).toEqual([]);
        warnSpy.mockRestore();
    });

    it('uses console warn for non-auth errors', async () => {
        const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
        const httpClient = makeHttpClient(() => Promise.reject(new Error('Network error')));
        const result = await fetchStyleEditorSchemas('page-id', config, requestOptions, httpClient);
        expect(warnSpy).toHaveBeenCalledWith(
            expect.stringContaining('Skipping style editor schemas'),
            expect.any(Error)
        );
        expect(result).toEqual([]);
        warnSpy.mockRestore();
    });
});

describe('mapContentResponse()', () => {
    it('returns undefined when responseData is undefined', () => {
        expect(mapContentResponse(undefined, ['blogs'])).toBeUndefined();
    });

    it('returns undefined when responseData is null (cast)', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        expect(mapContentResponse(null as any, ['blogs'])).toBeUndefined();
    });

    it('returns an object with only the requested keys', () => {
        const data = { blogs: [1, 2], nav: { href: '/' }, extra: 'ignore' };
        expect(mapContentResponse(data, ['blogs', 'nav'])).toEqual({
            blogs: [1, 2],
            nav: { href: '/' }
        });
    });

    it('omits keys that are not present in the response', () => {
        const data = { blogs: [1, 2] };
        const result = mapContentResponse(data, ['blogs', 'missing']);
        expect(result).toEqual({ blogs: [1, 2] });
        expect(result).not.toHaveProperty('missing');
    });

    it('returns an empty object when no requested keys exist in data', () => {
        expect(mapContentResponse({ blogs: [] }, ['nav', 'footer'])).toEqual({});
    });

    it('returns an empty object when keys array is empty', () => {
        expect(mapContentResponse({ blogs: [] }, [])).toEqual({});
    });
});
