import { vi } from 'vitest';

import { UPSERT_CONTENT_ENDPOINTS, upsertContent } from './upsert-content';

import { ValidationError, type DotCMSRuntime, type RequestOptions } from '../../runtime';
import { unlistedCalls } from '../toolkit/endpoints';

// Every request the fake below sees. After each test, all of them must be endpoints the
// upsert_content tool owns — otherwise the operation works here and is refused in production.
const seen: RequestOptions[] = [];
afterEach(() => {
    expect(unlistedCalls(UPSERT_CONTENT_ENDPOINTS, seen.splice(0))).toEqual([]);
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

const BLOG_TYPE = {
    id: 'blog-type-id',
    variable: 'Blog',
    name: 'Blog',
    fields: [
        { clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField', variable: 'fields-0' },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
            variable: 'title',
            required: true
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableRelationshipField',
            variable: 'author',
            relationships: { velocityVar: 'Author', cardinality: 3 }
        },
        {
            clazz: 'com.dotcms.contenttype.model.field.ImmutableRelationshipField',
            variable: 'tags',
            relationships: { velocityVar: 'Tag', cardinality: 1 }
        }
    ]
};

interface Version {
    identifier: string;
    contentType: string;
    languageId: number;
}

/**
 * A tiny in-memory instance: `versions` is what the search index knows; a fire adds a version
 * and records the relationship values it was sent, which a depth-1 read then resolves —
 * unless `unresolved` names the field, to simulate a relationship that does not resolve yet.
 */
function fakeInstance(initial: Version[] = [], opts: { unresolved?: string[] } = {}) {
    const versions = [...initial];
    const related = new Map<string, Record<string, string[]>>();
    const fires: Array<{ path: string; query: unknown; contentlet: Record<string, unknown> }> = [];
    let counter = 0;

    const request = vi.fn(async (options: RequestOptions) => {
        seen.push(options);
        if (options.path.startsWith('/api/v1/contenttype/id/')) {
            return { entity: BLOG_TYPE };
        }
        if (options.path === '/api/content/_search') {
            const { query, depth } = options.body as { query: string; depth?: number };
            const ids = (query.match(/\+identifier:\(?([^)+]+)\)?/)?.[1] ?? '').trim().split(/\s+/);
            const lang = Number(query.match(/\+languageId:(\d+)/)?.[1] ?? 0);
            const matches = versions.filter(
                (v) => ids.includes(v.identifier) && (!lang || v.languageId === lang)
            );
            return {
                entity: {
                    jsonObjectView: {
                        contentlets: matches.map((v) => {
                            const base: Record<string, unknown> = {
                                ...v,
                                inode: `${v.identifier}-${v.languageId}`
                            };
                            if (depth) {
                                const rel = related.get(`${v.identifier}-${v.languageId}`) ?? {};
                                for (const [field, relIds] of Object.entries(rel)) {
                                    base[field] = opts.unresolved?.includes(field)
                                        ? []
                                        : relIds.map((identifier) => ({ identifier }));
                                }
                            }
                            return base;
                        })
                    }
                }
            };
        }
        if (options.path.startsWith('/api/v1/workflow/actions/default/fire/')) {
            const { contentlet } = options.body as { contentlet: Record<string, unknown> };
            fires.push({ path: options.path, query: options.query, contentlet });
            const identifier = (contentlet['identifier'] as string) ?? `new${++counter}aaaaaaaa`;
            const languageId = contentlet['languageId'] as number;
            if (!versions.some((v) => v.identifier === identifier && v.languageId === languageId)) {
                versions.push({ identifier, contentType: 'Blog', languageId });
            }
            const rel: Record<string, string[]> = {};
            for (const field of ['author', 'tags']) {
                if (typeof contentlet[field] === 'string') {
                    rel[field] = (contentlet[field] as string).split(',').filter(Boolean);
                }
            }
            related.set(`${identifier}-${languageId}`, rel);
            return { entity: { identifier, inode: `${identifier}-${languageId}`, live: true } };
        }
        return {};
    });
    const loadContext = vi.fn(async () => ({
        contentTypes: [],
        sites: [
            { identifier: 'site-1', hostname: 'demo.example.com', isDefault: true, archived: false }
        ],
        languages: LANGUAGES,
        currentUser: null
    }));

    return { runtime: { request, loadContext } as unknown as DotCMSRuntime, fires };
}

const AUTHOR_EN: Version = { identifier: 'author1aaaa', contentType: 'Author', languageId: 1 };
const AUTHOR_ES: Version = { identifier: 'author2aaaa', contentType: 'Author', languageId: 2 };
const TAG_EN: Version = { identifier: 'tag1aaaaaaa', contentType: 'Tag', languageId: 1 };
const FAST = { readBackAttempts: 2, readBackDelayMs: 0 };

describe('upsertContent', () => {
    it('sends relationship arrays as the comma-separated string the fire endpoint accepts', async () => {
        const { runtime, fires } = fakeInstance([
            AUTHOR_EN,
            TAG_EN,
            { ...TAG_EN, identifier: 'tag2aaaaaaa' }
        ]);

        const manifest = await upsertContent({
            dotcms: runtime,
            contentType: 'Blog',
            languageId: 1,
            fields: {
                title: 'Hello',
                author: 'author1aaaa',
                tags: ['tag1aaaaaaa', { identifier: 'tag2aaaaaaa' }]
            },
            ...FAST
        });

        expect(fires).toHaveLength(1);
        expect(fires[0].path).toBe('/api/v1/workflow/actions/default/fire/PUBLISH');
        expect(fires[0].query).toEqual({ indexPolicy: 'WAIT_FOR' });
        expect(fires[0].contentlet).toMatchObject({
            contentType: 'Blog',
            languageId: 1,
            title: 'Hello',
            author: 'author1aaaa',
            tags: 'tag1aaaaaaa,tag2aaaaaaa'
        });
        expect(manifest).toMatchObject({
            outcome: 'created',
            action: 'PUBLISH',
            language: 'en-us',
            warnings: []
        });
        expect(manifest.relationships).toEqual([
            { field: 'author', sent: ['author1aaaa'], resolved: ['author1aaaa'] },
            {
                field: 'tags',
                sent: ['tag1aaaaaaa', 'tag2aaaaaaa'],
                resolved: ['tag1aaaaaaa', 'tag2aaaaaaa']
            }
        ]);
    });

    it('warns BEFORE writing when a related item has no version in the target language', async () => {
        const { runtime, fires } = fakeInstance([AUTHOR_ES]);

        const manifest = await upsertContent({
            dotcms: runtime,
            contentType: 'Blog',
            languageId: 1,
            fields: { title: 'Hello', author: 'author2aaaa' },
            ...FAST
        });

        expect(fires).toHaveLength(1);
        expect(manifest.warnings).toEqual([
            expect.objectContaining({
                code: 'related-content-missing-language',
                field: 'author',
                relatedIdentifier: 'author2aaaa',
                requestedLanguage: 'en-us',
                availableLanguages: ['es-es']
            })
        ]);
    });

    it('writes nothing in "fail" mode when a relationship cannot resolve', async () => {
        const { runtime, fires } = fakeInstance([AUTHOR_ES]);

        const error = await upsertContent({
            dotcms: runtime,
            contentType: 'Blog',
            languageId: 1,
            fields: { title: 'Hello', author: 'author2aaaa' },
            onRelationshipIssue: 'fail',
            ...FAST
        }).catch((e) => e);

        expect(error).toBeInstanceOf(ValidationError);
        expect(error.message).toMatch(/Nothing was written/);
        expect(fires).toHaveLength(0);
    });

    it('flags related ids that do not exist or belong to another type', async () => {
        const { runtime } = fakeInstance([TAG_EN]);

        const manifest = await upsertContent({
            dotcms: runtime,
            contentType: 'Blog',
            languageId: 1,
            fields: { title: 'Hello', author: 'tag1aaaaaaa', tags: 'missing1aaa' },
            ...FAST
        });

        expect(manifest.warnings.map((w) => [w.field, w.code])).toEqual([
            ['author', 'related-content-wrong-type'],
            ['tags', 'related-content-not-found']
        ]);
    });

    it('reports a relationship that was sent but does not resolve after the write', async () => {
        const { runtime } = fakeInstance([AUTHOR_EN], { unresolved: ['author'] });

        const manifest = await upsertContent({
            dotcms: runtime,
            contentType: 'Blog',
            languageId: 1,
            fields: { title: 'Hello', author: 'author1aaaa' },
            ...FAST
        });

        expect(manifest.warnings).toEqual([
            expect.objectContaining({ code: 'relationship-not-resolved', field: 'author' })
        ]);
    });

    it('refuses unknown and wrong-case field keys instead of letting dotCMS drop them', async () => {
        const { runtime, fires } = fakeInstance();

        const error = await upsertContent({
            dotcms: runtime,
            contentType: 'Blog',
            languageId: 1,
            fields: { Title: 'Hello', subtitle: 'x' }
        }).catch((e) => e);

        expect(error).toBeInstanceOf(ValidationError);
        expect(error.message).toMatch(/did you mean "title"/);
        expect(error.message).toMatch(/"subtitle" is not a field of Blog/);
        expect(fires).toHaveLength(0);
    });

    it('refuses system keys inside fields, pointing to the parameter to use', async () => {
        const { runtime } = fakeInstance();

        const error = await upsertContent({
            dotcms: runtime,
            contentType: 'Blog',
            languageId: 1,
            fields: { title: 'Hello', contentHost: 'site-1' }
        }).catch((e) => e);

        expect(error.message).toMatch(/use the site parameter/);
    });

    it('rejects a language the instance does not have', async () => {
        const { runtime, fires } = fakeInstance();

        const error = await upsertContent({
            dotcms: runtime,
            contentType: 'Blog',
            languageId: 99,
            fields: { title: 'Hello' }
        }).catch((e) => e);

        expect(error).toBeInstanceOf(ValidationError);
        expect(fires).toHaveLength(0);
    });

    it('adds a language version to existing content and reports every language it now has', async () => {
        const { runtime, fires } = fakeInstance([
            { identifier: 'blog1aaaaaa', contentType: 'Blog', languageId: 2 }
        ]);

        const manifest = await upsertContent({
            dotcms: runtime,
            contentType: 'Blog',
            languageId: 1,
            identifier: 'blog1aaaaaa',
            fields: { title: 'Hello' }
        });

        expect(fires[0].contentlet).toMatchObject({ identifier: 'blog1aaaaaa', languageId: 1 });
        expect(manifest.outcome).toBe('created-language-version');
        expect(manifest.languages.sort()).toEqual(['en-us', 'es-es']);
    });

    it('updates in place, and saves without publishing when asked', async () => {
        const { runtime, fires } = fakeInstance([
            { identifier: 'blog1aaaaaa', contentType: 'Blog', languageId: 1 }
        ]);

        const manifest = await upsertContent({
            dotcms: runtime,
            contentType: 'Blog',
            languageId: 1,
            identifier: 'blog1aaaaaa',
            fields: { title: 'Hello again' },
            publish: false
        });

        expect(fires[0].path).toBe('/api/v1/workflow/actions/default/fire/EDIT');
        expect(manifest).toMatchObject({ outcome: 'updated', action: 'EDIT' });
    });

    it('refuses an identifier that does not exist or belongs to another type', async () => {
        const { runtime, fires } = fakeInstance([AUTHOR_EN]);

        const missing = await upsertContent({
            dotcms: runtime,
            contentType: 'Blog',
            languageId: 1,
            identifier: 'nothere1aaa',
            fields: { title: 'x' }
        }).catch((e) => e);
        const otherType = await upsertContent({
            dotcms: runtime,
            contentType: 'Blog',
            languageId: 1,
            identifier: 'author1aaaa',
            fields: { title: 'x' }
        }).catch((e) => e);

        expect(missing.message).toMatch(/No content with identifier/);
        expect(otherType.message).toMatch(/belongs to Author/);
        expect(fires).toHaveLength(0);
    });

    it('resolves the site to its identifier for contentHost', async () => {
        const { runtime, fires } = fakeInstance();

        await upsertContent({
            dotcms: runtime,
            contentType: 'Blog',
            languageId: 1,
            site: 'demo.example.com',
            fields: { title: 'Hello' }
        });

        expect(fires[0].contentlet['contentHost']).toBe('site-1');
    });
});
