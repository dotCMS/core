import { vi } from 'vitest';

import { LOOKUP_ENDPOINTS, lookupContent } from './lookup';

import { HttpError, ValidationError, type DotCMSRuntime, type RequestOptions } from '../../runtime';
import { unlistedCalls } from '../toolkit/endpoints';

// Every request the fake below sees. After each test, all of them must be endpoints the
// lookup tool owns — otherwise the operation works here and is refused in production.
const seen: RequestOptions[] = [];
afterEach(() => {
    expect(unlistedCalls(LOOKUP_ENDPOINTS, seen.splice(0))).toEqual([]);
});

const LANGUAGES = [
    {
        id: 1,
        languageCode: 'en',
        countryCode: 'US',
        language: 'English',
        country: 'US',
        isoCode: 'en-us'
    },
    {
        id: 2,
        languageCode: 'es',
        countryCode: 'ES',
        language: 'Spanish',
        country: 'Spain',
        isoCode: 'es-es'
    }
];

const AUTHOR_TYPE = {
    id: 'author-type-id',
    variable: 'Author',
    name: 'Author',
    fields: [
        { clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField', variable: 'fields-0' },
        { clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField', variable: 'fields-1' },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
            variable: 'title',
            name: 'Title',
            required: true
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
            variable: 'role',
            name: 'Role'
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableRelationshipField',
            variable: 'team',
            name: 'Team',
            relationships: { velocityVar: 'Team', cardinality: 3 }
        }
    ]
};

function contentlet(identifier: string, languageId: number, over: Record<string, unknown> = {}) {
    return {
        identifier,
        inode: `${identifier}-${languageId}`,
        contentType: 'Author',
        languageId,
        title: `Author ${identifier}`,
        live: false,
        working: true,
        archived: false,
        ...over
    };
}

function fakeRuntime(
    over: {
        contentlets?: unknown[];
        relationships?: unknown[];
        typeStatus?: number;
        relationshipsThrow?: boolean;
    } = {}
) {
    const calls: RequestOptions[] = [];
    const request = vi.fn(async (options: RequestOptions) => {
        seen.push(options);
        calls.push(options);
        if (options.path.startsWith('/api/v1/contenttype/id/')) {
            if (over.typeStatus) {
                throw new HttpError(over.typeStatus, 'Not Found', '');
            }
            return { entity: AUTHOR_TYPE };
        }
        if (options.path === '/api/content/_search') {
            return { entity: { jsonObjectView: { contentlets: over.contentlets ?? [] } } };
        }
        if (options.path === '/api/v1/relationships') {
            if (over.relationshipsThrow) {
                throw new HttpError(500, 'Server Error', '');
            }
            return { entity: over.relationships ?? [] };
        }
        return {};
    });
    const loadContext = vi.fn(async () => ({
        contentTypes: [
            { id: 'author-type-id', name: 'Author', variable: 'Author', baseType: 'CONTENT' }
        ],
        sites: [],
        languages: LANGUAGES,
        currentUser: null
    }));

    return { runtime: { request, loadContext } as unknown as DotCMSRuntime, calls };
}

describe('lookupContent', () => {
    it('includes unpublished content and groups language versions per contentlet', async () => {
        const { runtime } = fakeRuntime({
            contentlets: [
                contentlet('a1', 2),
                contentlet('a2', 1, { live: true }),
                contentlet('a2', 2, { live: true })
            ]
        });

        const manifest = await lookupContent({ dotcms: runtime, contentType: 'Author' });

        expect(manifest.count).toBe(2);
        expect(manifest.results[0]).toMatchObject({
            identifier: 'a1',
            live: false,
            languages: ['es-es'],
            missingLanguages: ['en-us']
        });
        expect(manifest.results[1]).toMatchObject({
            identifier: 'a2',
            live: true,
            languages: ['en-us', 'es-es'],
            missingLanguages: []
        });
        expect(manifest.guidance.join(' ')).toMatch(/unpublished/);
        expect(manifest.guidance.join(' ')).toMatch(/missingLanguages/);
    });

    it('queries the type without a live filter and excludes archived content by default', async () => {
        const { runtime, calls } = fakeRuntime();

        const manifest = await lookupContent({ dotcms: runtime, contentType: 'Author' });

        expect(manifest.query).toBe('+contentType:Author +deleted:false');
        expect(manifest.query).not.toMatch(/live/);
        const search = calls.find((call) => call.path === '/api/content/_search');
        expect(search?.method).toBe('POST');
        expect((search?.body as { query: string }).query).toBe(manifest.query);
    });

    it('prefixes the matched field with the type variable and escapes the phrase', async () => {
        const { runtime } = fakeRuntime();

        const manifest = await lookupContent({
            dotcms: runtime,
            contentType: 'Author',
            match: { value: 'Jane "JD" Doe' },
            includeArchived: true
        });

        expect(manifest.query).toBe('+contentType:Author +Author.title:"Jane \\"JD\\" Doe"');
    });

    it('rejects a match on a field the type does not have, listing the real fields', async () => {
        const { runtime } = fakeRuntime();

        const error = await lookupContent({
            dotcms: runtime,
            contentType: 'Author',
            match: { field: 'Role', value: 'editor' }
        }).catch((e) => e);

        expect(error).toBeInstanceOf(ValidationError);
        expect(error.message).toMatch(/case-sensitive/);
        expect(error.message).toMatch(/title, role/);
    });

    it('reports a missing content type as a validation error with suggestions', async () => {
        const { runtime } = fakeRuntime({ typeStatus: 404 });

        const error = await lookupContent({ dotcms: runtime, contentType: 'author' }).catch(
            (e) => e
        );

        expect(error).toBeInstanceOf(ValidationError);
        expect(error.message).toMatch(/Similar content types: Author/);
    });

    it('lists its own relationship fields and the fields on other types that point to it', async () => {
        const { runtime } = fakeRuntime({
            relationships: [
                { relationTypeValue: 'Blog.author' },
                { relationTypeValue: 'Event.speakers' },
                { relationTypeValue: 'Author.team' }
            ]
        });

        const manifest = await lookupContent({ dotcms: runtime, contentType: 'Author' });

        expect(manifest.relationshipFields).toEqual([
            { field: 'team', target: 'Team', cardinality: 'many-to-one', required: false }
        ]);
        expect(manifest.referencedBy).toEqual(['Blog.author', 'Event.speakers']);
        expect(manifest.guidance.join(' ')).toMatch(/shared/);
    });

    it('still answers when the relationships read fails', async () => {
        const { runtime } = fakeRuntime({
            relationshipsThrow: true,
            contentlets: [contentlet('a1', 1)]
        });

        const manifest = await lookupContent({ dotcms: runtime, contentType: 'Author' });

        expect(manifest.referencedBy).toEqual([]);
        expect(manifest.count).toBe(1);
    });

    it('caps the results at limit and says so', async () => {
        const { runtime } = fakeRuntime({
            contentlets: [contentlet('a1', 1), contentlet('a2', 1), contentlet('a3', 1)]
        });

        const manifest = await lookupContent({ dotcms: runtime, contentType: 'Author', limit: 2 });

        expect(manifest.count).toBe(2);
        expect(manifest.truncated).toBe(true);
    });

    it('never writes', async () => {
        const { runtime, calls } = fakeRuntime({ contentlets: [contentlet('a1', 1)] });

        await lookupContent({ dotcms: runtime, contentType: 'Author', match: { value: 'x' } });

        const writes = calls.filter(
            (call) => call.method && call.method !== 'GET' && call.path !== '/api/content/_search'
        );
        expect(writes).toEqual([]);
    });
});
