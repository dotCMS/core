import { vi } from 'vitest';

import { CREATE_CONTENT_TYPE_ENDPOINTS, createContentType } from './content-type-create';
import { UPDATE_CONTENT_TYPE_ENDPOINTS, updateContentType } from './content-type-update';
import { deriveVariable, prepareFields, variableProblem } from './shared/content-type-fields';

import { HttpError, ValidationError, type DotCMSRuntime, type RequestOptions } from '../../runtime';
import { unlistedCalls } from '../toolkit/endpoints';

// Every request the fake sees; checked against the endpoint list of the operation under test.
const seen: RequestOptions[] = [];
let endpoints = CREATE_CONTENT_TYPE_ENDPOINTS;
afterEach(() => {
    expect(unlistedCalls(endpoints, seen.splice(0))).toEqual([]);
});

const FIELD = 'com.dotcms.contenttype.model.field.Immutable';
const WORKFLOWS = [
    { id: 'wf-system', name: 'System Workflow', archived: false },
    { id: 'wf-review', name: 'Editorial Review', archived: false },
    { id: 'wf-old', name: 'Old Flow', archived: true }
];

type StoredType = {
    id: string;
    variable: string;
    name: string;
    fields: Record<string, unknown>[];
} & Record<string, unknown>;

/**
 * An in-memory content type store. A POST/PUT stores the body as dotCMS would (ids assigned to
 * new fields); relationship fields register a record unless `skipRelationshipRecords`.
 */
function fakeStore(
    initial: StoredType[] = [],
    opts: { skipRelationshipRecords?: boolean; postError?: HttpError } = {}
) {
    const types = new Map(initial.map((type) => [type.variable, structuredClone(type)]));
    const records = new Map<string, Set<string>>();
    const writes: Array<{ method: string; body: Record<string, unknown> }> = [];
    let counter = 0;

    const find = (idOrVar: string) =>
        [...types.values()].find((type) => type.id === idOrVar || type.variable === idOrVar);

    const register = (type: StoredType) => {
        for (const field of type.fields) {
            if (
                String(field['clazz']).endsWith('RelationshipField') &&
                !opts.skipRelationshipRecords
            ) {
                const target = find(String(field['relationType'])) ?? type;
                const set = records.get(target.id) ?? new Set<string>();
                set.add(`${type.variable}.${field['variable']}`);
                records.set(target.id, set);
            }
        }
    };

    const store = (body: Record<string, unknown>, id: string) => {
        const variable = String(body['variable'] ?? String(body['name']).replace(/\W/g, ''));
        const fields = (body['fields'] as Record<string, unknown>[]).map((field) => ({
            id: field['id'] ?? `field-${++counter}`,
            variable: field['variable'] ?? String(field['name']).toLowerCase().replace(/\W/g, ''),
            ...field
        }));
        const type: StoredType = { ...body, id, variable, name: String(body['name']), fields };
        delete type['workflow'];
        type['workflows'] = ((body['workflow'] as string[]) ?? []).map((wid) => ({ id: wid }));
        types.set(variable, type);
        register(type);
        return type;
    };

    const request = vi.fn(async (options: RequestOptions) => {
        seen.push(options);
        const method = options.method ?? 'GET';
        if (method === 'GET' && options.path.startsWith('/api/v1/contenttype/id/')) {
            const type = find(decodeURIComponent(options.path.split('/').pop() ?? ''));
            if (!type) throw new HttpError(404, 'Not Found', '');
            return { entity: structuredClone(type) };
        }
        if (options.path === '/api/v1/workflow/schemes') {
            return { entity: WORKFLOWS };
        }
        if (options.path === '/api/v1/relationships') {
            const id = String(options.query?.['contentTypeId']);
            return {
                entity: [...(records.get(id) ?? [])].map((relationTypeValue) => ({
                    relationTypeValue
                }))
            };
        }
        if (method === 'POST' && options.path === '/api/v1/contenttype') {
            const body = options.body as Record<string, unknown>;
            writes.push({ method, body });
            if (opts.postError) throw opts.postError;
            return { entity: [store(body, `type-${++counter}`)] };
        }
        if (method === 'PUT' && options.path.startsWith('/api/v1/contenttype/id/')) {
            const body = options.body as Record<string, unknown>;
            writes.push({ method, body });
            return { entity: store(body, String(body['id'])) };
        }
        return {};
    });

    const loadContext = vi.fn(async () => ({
        contentTypes: [...types.values()].map((type) => ({
            id: type.id,
            name: type.name,
            variable: type.variable,
            baseType: 'CONTENT'
        })),
        sites: [
            {
                identifier: 'site-1',
                hostname: 'demo.example.com',
                isDefault: true,
                archived: false,
                live: true
            }
        ],
        languages: [],
        currentUser: null
    }));

    return { runtime: { request, loadContext } as unknown as DotCMSRuntime, writes, types };
}

const AUTHOR: StoredType = {
    id: 'author-id',
    variable: 'Author',
    name: 'Author',
    fields: [
        { id: 'r1', clazz: `${FIELD}RowField`, variable: 'fields0' },
        { id: 'c1', clazz: `${FIELD}ColumnField`, variable: 'fields1' },
        { id: 'f1', clazz: `${FIELD}TextField`, variable: 'title', name: 'Title', required: true },
        { id: 'f2', clazz: `${FIELD}TextField`, variable: 'role', name: 'Role' }
    ],
    workflows: [{ id: 'wf-system', name: 'System Workflow' }]
};

describe('deriveVariable', () => {
    it.each([
        ['Sort Order', 'sortOrder'],
        ['Folder', 'folder'],
        ['Reading time (min)', 'readingTimeMin'],
        ['2nd Title', 'ndTitle'],
        ['alreadyCamel', 'alreadyCamel']
    ])('derives %s → %s, as dotCMS does', (name, variable) => {
        expect(deriveVariable(name)).toBe(variable);
    });
});

describe('prepareFields — variables derived from the name', () => {
    const prepare = (fields: Parameters<typeof prepareFields>[1]) => {
        const warnings: string[] = [];
        const prepared = prepareFields('Event', fields, {
            relationshipTargets: new Map(),
            warnings
        });
        return { bodies: prepared.map((field) => field.body), warnings };
    };

    it('replaces a derived variable that is a system property, and says so', () => {
        const { bodies, warnings } = prepare([
            { name: 'Sort Order', type: 'integer' },
            { name: 'Folder', type: 'text' }
        ]);

        expect(bodies.map((body) => body['variable'])).toEqual(['eventSortOrder', 'eventFolder']);
        expect(warnings).toEqual([
            expect.stringMatching(
                /Sort Order: "sortOrder" is a contentlet system property.*"eventSortOrder"/
            ),
            expect.stringMatching(/Folder: "folder" .*"eventFolder"/)
        ]);
    });

    it('leaves other names for dotCMS to derive', () => {
        const { bodies, warnings } = prepare([{ name: 'Starts', type: 'date-time' }]);

        expect(bodies[0]).not.toHaveProperty('variable');
        expect(warnings).toEqual([]);
    });

    it('refuses a replacement that collides with an existing variable', () => {
        expect(() =>
            prepareFields('Event', [{ name: 'Sort Order', type: 'integer' }], {
                relationshipTargets: new Map(),
                takenVariables: ['eventSortOrder'],
                warnings: []
            })
        ).toThrow(/"eventSortOrder" already exists on Event/);
    });
});

describe('variableProblem', () => {
    it.each(['sortOrder', 'host', 'identifier', 'folder', 'LanguageId'])(
        'refuses %s',
        (variable) => {
            expect(variableProblem('Blog', variable)).toMatch(/system property/);
        }
    );

    it('suggests a type-prefixed alternative', () => {
        expect(variableProblem('Blog', 'sortOrder')).toMatch(/"blogSortOrder"/);
    });

    it('refuses invalid characters and accepts normal variables', () => {
        expect(variableProblem('Blog', '1st-field')).toMatch(/must start with a letter/);
        expect(variableProblem('Blog', 'readingTime')).toBeUndefined();
    });
});

describe('createContentType', () => {
    beforeEach(() => {
        endpoints = CREATE_CONTENT_TYPE_ENDPOINTS;
    });

    it('maps friendly types, resolves the workflow by name, and builds relationship fields correctly', async () => {
        const { runtime, writes } = fakeStore([AUTHOR]);

        const manifest = await createContentType({
            dotcms: runtime,
            name: 'Blog',
            variable: 'Blog',
            site: 'demo.example.com',
            urlMapPattern: '/blog/{urlTitle}',
            fields: [
                { name: 'Title', variable: 'title', type: 'text', required: true },
                { name: 'Reading Time', variable: 'readingTime', type: 'integer' },
                {
                    name: 'Level',
                    variable: 'level',
                    type: 'select',
                    options: ['basic', { label: 'Advanced', value: 'adv' }]
                },
                {
                    name: 'Author',
                    variable: 'author',
                    type: 'relationship',
                    listed: true,
                    relationship: { target: 'Author', cardinality: 'many-to-one' }
                },
                {
                    name: 'Parent',
                    variable: 'parent',
                    type: 'relationship',
                    relationship: { target: 'self', cardinality: 'many-to-one' }
                }
            ]
        });

        const body = writes[0].body;
        expect(body).toMatchObject({
            name: 'Blog',
            variable: 'Blog',
            host: 'site-1',
            workflow: ['wf-system'],
            urlMapPattern: '/blog/{urlTitle}'
        });
        const fields = body['fields'] as Record<string, unknown>[];
        expect(fields[1]).toMatchObject({ clazz: `${FIELD}TextField`, dataType: 'INTEGER' });
        expect(fields[2]).toMatchObject({
            clazz: `${FIELD}SelectField`,
            values: 'basic|basic\r\nAdvanced|adv'
        });
        expect(fields[3]).toMatchObject({
            clazz: `${FIELD}RelationshipField`,
            relationType: 'Author',
            values: '3',
            indexed: true,
            listed: false
        });
        expect(fields[4]).toMatchObject({ relationType: 'Blog', values: '3' });
        expect(fields[3]).not.toHaveProperty('relationshipType');
        expect(fields[3]).not.toHaveProperty('cardinality');

        expect(manifest.dryRun).toBe(false);
        expect(manifest.variable).toBe('Blog');
        expect(manifest.warnings).toEqual([expect.stringMatching(/cannot be listed/)]);
    });

    it('reports every invalid field at once and writes nothing', async () => {
        const { runtime, writes } = fakeStore([AUTHOR]);

        const error = await createContentType({
            dotcms: runtime,
            name: 'Blog',
            variable: 'Blog',
            fields: [
                { name: 'Order', variable: 'sortOrder', type: 'text' },
                { name: 'Kind', variable: 'kind', type: 'select' },
                {
                    name: 'Team',
                    variable: 'team',
                    type: 'relationship',
                    relationship: { target: 'Team', cardinality: 'many-to-many' }
                },
                { name: 'Dup', variable: 'kind', type: 'text' }
            ]
        }).catch((e) => e);

        expect(error).toBeInstanceOf(ValidationError);
        expect(error.message).toMatch(/sortOrder: .*system property/);
        expect(error.message).toMatch(/kind: a select field needs options/);
        expect(error.message).toMatch(/"Team" is not a content type/);
        expect(error.message).toMatch(/used twice/);
        expect(writes).toEqual([]);
    });

    it('refuses an existing variable and points to update_content_type', async () => {
        const { runtime, writes } = fakeStore([AUTHOR]);

        const error = await createContentType({
            dotcms: runtime,
            name: 'Author',
            variable: 'Author',
            fields: [{ name: 'Title', type: 'text' }]
        }).catch((e) => e);

        expect(error.message).toMatch(/already exists.*update_content_type/);
        expect(writes).toEqual([]);
    });

    it('explains the "Invalid content type variable" response', async () => {
        const { runtime } = fakeStore([], {
            postError: new HttpError(
                400,
                'Bad Request',
                '{"message":"Invalid content type variable: Blog"}'
            )
        });

        const error = await createContentType({
            dotcms: runtime,
            name: 'Blog',
            variable: 'Blog',
            fields: [{ name: 'Title', type: 'text' }]
        }).catch((e) => e);

        expect(error).toBeInstanceOf(ValidationError);
        expect(error.message).toMatch(/deleted recently/);
    });

    it('rejects an unknown workflow, listing the active ones', async () => {
        const { runtime } = fakeStore();

        const error = await createContentType({
            dotcms: runtime,
            name: 'Blog',
            workflows: ['Old Flow'],
            fields: [{ name: 'Title', type: 'text' }]
        }).catch((e) => e);

        expect(error.message).toMatch(/Available: System Workflow, Editorial Review\./);
    });

    it('returns the body without writing on a dry run', async () => {
        const { runtime, writes } = fakeStore();

        const manifest = await createContentType({
            dotcms: runtime,
            name: 'Blog',
            dryRun: true,
            fields: [{ name: 'Title', type: 'text' }]
        });

        expect(manifest.dryRun).toBe(true);
        expect(manifest.body).toMatchObject({ name: 'Blog', workflow: ['wf-system'] });
        expect(writes).toEqual([]);
    });

    it('warns when a relationship field was saved without its relationship record', async () => {
        const { runtime } = fakeStore([AUTHOR], { skipRelationshipRecords: true });

        const manifest = await createContentType({
            dotcms: runtime,
            name: 'Blog',
            variable: 'Blog',
            fields: [
                { name: 'Title', variable: 'title', type: 'text' },
                {
                    name: 'Author',
                    variable: 'author',
                    type: 'relationship',
                    relationship: { target: 'Author', cardinality: 'many-to-one' }
                }
            ]
        });

        expect(manifest.warnings).toEqual([
            expect.stringMatching(/Blog\.author: .*no relationship record/)
        ]);
    });
});

describe('updateContentType', () => {
    beforeEach(() => {
        endpoints = UPDATE_CONTENT_TYPE_ENDPOINTS;
    });

    it('sends the full type back with field ids kept, workflows as ids, and new fields appended', async () => {
        const { runtime, writes, types } = fakeStore([AUTHOR]);

        const manifest = await updateContentType({
            dotcms: runtime,
            contentType: 'Author',
            addFields: [{ name: 'Bio', variable: 'bio', type: 'textarea' }],
            updateFields: [{ variable: 'role', required: true }]
        });

        const body = writes[0].body;
        expect(writes[0].method).toBe('PUT');
        expect(body).not.toHaveProperty('workflows');
        expect(body['workflow']).toEqual(['wf-system']);
        const fields = body['fields'] as Record<string, unknown>[];
        expect(fields.map((field) => field['id'])).toEqual(['r1', 'c1', 'f1', 'f2', undefined]);
        expect(fields[3]).toMatchObject({ variable: 'role', required: true });
        expect(fields[4]).toMatchObject({
            variable: 'bio',
            clazz: `${FIELD}TextAreaField`,
            contentTypeId: 'author-id'
        });
        expect(manifest.changes).toEqual({
            type: [],
            added: ['bio'],
            updated: [{ variable: 'role', properties: ['required'] }],
            removed: []
        });
        expect(types.get('Author')?.fields.map((field) => field['variable'])).toContain('bio');
    });

    it('refuses to remove fields without allowDataLoss, and removes them with it', async () => {
        const { runtime, writes } = fakeStore([AUTHOR]);

        const refused = await updateContentType({
            dotcms: runtime,
            contentType: 'Author',
            removeFields: ['role']
        }).catch((e) => e);
        expect(refused.message).toMatch(/allowDataLoss/);
        expect(writes).toEqual([]);

        const manifest = await updateContentType({
            dotcms: runtime,
            contentType: 'Author',
            removeFields: ['role'],
            allowDataLoss: true
        });
        expect(manifest.changes.removed).toEqual(['role']);
        expect(manifest.fields).not.toContain('role');
    });

    it('validates new fields against the existing ones and the reserved variables', async () => {
        const { runtime, writes } = fakeStore([AUTHOR]);

        const error = await updateContentType({
            dotcms: runtime,
            contentType: 'Author',
            addFields: [
                { name: 'Role', variable: 'role', type: 'text' },
                { name: 'Host', variable: 'host', type: 'text' }
            ]
        }).catch((e) => e);

        expect(error.message).toMatch(/"role" already exists on Author/);
        expect(error.message).toMatch(/host: .*system property/);
        expect(writes).toEqual([]);
    });

    it('reports the diff without writing on a dry run, and says when nothing changes', async () => {
        const { runtime, writes } = fakeStore([AUTHOR]);

        const dry = await updateContentType({
            dotcms: runtime,
            contentType: 'Author',
            name: 'Writer',
            workflows: ['Editorial Review'],
            dryRun: true
        });
        expect(dry.changes.type).toEqual(['workflows → Editorial Review', 'name']);

        const same = await updateContentType({
            dotcms: runtime,
            contentType: 'Author',
            updateFields: [{ variable: 'title', required: true }]
        });
        expect(same.warnings).toEqual([expect.stringMatching(/Nothing to change/)]);
        expect(writes).toEqual([]);
    });

    it('rejects updates to fields the type does not have', async () => {
        const { runtime } = fakeStore([AUTHOR]);

        const error = await updateContentType({
            dotcms: runtime,
            contentType: 'Author',
            updateFields: [{ variable: 'Role', hint: 'x' }]
        }).catch((e) => e);

        expect(error.message).toMatch(/"Role" is not a field of Author/);
    });
});
