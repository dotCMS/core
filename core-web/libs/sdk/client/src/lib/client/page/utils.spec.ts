import { buildPageQuery, buildQuery, mapContentResponse, removeUndefinedValues } from './utils';

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

    it('always includes styleEditorSchemas in the page fragment regardless of mode', () => {
        // Gating is server-side (EDIT_MODE only); the field is always requested so the
        // server can decide whether to populate it or return null.
        expect(buildPageQuery({})).toContain('styleEditorSchemas');
        expect(buildPageQuery({ page: 'title url' })).toContain('styleEditorSchemas');
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

describe('removeUndefinedValues()', () => {
    it('removes keys whose value is undefined', () => {
        const result = removeUndefinedValues({ a: 1, b: undefined, c: 'x' });
        expect(result).toEqual({ a: 1, c: 'x' });
        expect(result).not.toHaveProperty('b');
    });

    it('preserves null and other falsy values', () => {
        expect(removeUndefinedValues({ a: null, b: 0, c: '', d: false })).toEqual({
            a: null,
            b: 0,
            c: '',
            d: false
        });
    });

    it('returns an empty object when all values are undefined', () => {
        expect(removeUndefinedValues({ a: undefined, b: undefined })).toEqual({});
    });

    it('returns an equivalent object when nothing is undefined', () => {
        const input = { a: 1, b: 'x' };
        expect(removeUndefinedValues(input)).toEqual(input);
    });

    it('produces a JSON-serializable object', () => {
        const result = removeUndefinedValues({ a: 1, b: undefined });
        expect(JSON.parse(JSON.stringify(result))).toEqual(result);
    });
});
