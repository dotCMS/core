import {
    ContentTypeField,
    ContentTypeSchema,
    ContentletRecord,
    LanguageMap
} from '../../core/types';
import {
    getUserFields,
    getBodyField,
    getBinaryFields,
    buildGraphQLQuery,
    serializeContentlet,
    parseContentFile,
    buildPushPayload,
    validateContentFile
} from '../content';

// ─── Test fixtures ──────────────────────────────────────────────────────────

function makeField(overrides: Partial<ContentTypeField>): ContentTypeField {
    return {
        variable: 'testField',
        name: 'Test Field',
        fieldType: 'Text',
        dataType: 'TEXT',
        sortOrder: 1,
        required: false,
        fixed: false,
        readOnly: false,
        searchable: false,
        listed: false,
        ...overrides
    };
}

function makeSchema(fields: ContentTypeField[]): ContentTypeSchema {
    return {
        variable: 'BlogPost',
        name: 'Blog Post',
        id: 'abc-123',
        fields
    };
}

const defaultLanguageMap: LanguageMap = {
    '1': 'en-US',
    '2': 'es-ES'
};

const baseBlogFields: ContentTypeField[] = [
    makeField({ variable: 'title', name: 'Title', fieldType: 'Text', sortOrder: 1 }),
    makeField({
        variable: 'body',
        name: 'Body',
        fieldType: 'WYSIWYG',
        sortOrder: 2,
        dataType: 'TEXT'
    }),
    makeField({ variable: 'author', name: 'Author', fieldType: 'Text', sortOrder: 3 }),
    makeField({
        variable: 'image',
        name: 'Image',
        fieldType: 'Binary',
        sortOrder: 4,
        dataType: 'BINARY'
    }),
    makeField({
        variable: 'tags',
        name: 'Tags',
        fieldType: 'Tag',
        sortOrder: 5,
        dataType: 'TEXT'
    }),
    // System fields
    makeField({
        variable: 'hostFolder',
        name: 'Host',
        fieldType: 'HostFolder',
        sortOrder: 100,
        fixed: true,
        dataType: 'SYSTEM'
    }),
    makeField({
        variable: 'modUser',
        name: 'Modified By',
        fieldType: 'Text',
        sortOrder: 101,
        readOnly: true,
        dataType: 'SYSTEM'
    })
];

// ─── getUserFields ──────────────────────────────────────────────────────────

describe('getUserFields', () => {
    it('should filter out system fields (fixed, readOnly, SYSTEM dataType)', () => {
        const schema = makeSchema(baseBlogFields);
        const result = getUserFields(schema);
        const variables = result.map((f) => f.variable);

        expect(variables).toContain('title');
        expect(variables).toContain('body');
        expect(variables).toContain('author');
        expect(variables).toContain('image');
        expect(variables).toContain('tags');
        expect(variables).not.toContain('hostFolder');
        expect(variables).not.toContain('modUser');
    });

    it('should return empty array when all fields are system fields', () => {
        const schema = makeSchema([
            makeField({ variable: 'sys1', fixed: true, dataType: 'SYSTEM' }),
            makeField({ variable: 'sys2', readOnly: true })
        ]);
        const result = getUserFields(schema);
        expect(result).toHaveLength(0);
    });

    it('should return all fields when none are system fields', () => {
        const schema = makeSchema([
            makeField({ variable: 'a', sortOrder: 1 }),
            makeField({ variable: 'b', sortOrder: 2 })
        ]);
        const result = getUserFields(schema);
        expect(result).toHaveLength(2);
    });
});

// ─── getBodyField ───────────────────────────────────────────────────────────

describe('getBodyField', () => {
    it('should return the first WYSIWYG field by sortOrder', () => {
        const schema = makeSchema(baseBlogFields);
        expect(getBodyField(schema)).toBe('body');
    });

    it('should pick Textarea if no WYSIWYG', () => {
        const schema = makeSchema([
            makeField({ variable: 'title', fieldType: 'Text', sortOrder: 1 }),
            makeField({ variable: 'description', fieldType: 'Textarea', sortOrder: 2 }),
            makeField({ variable: 'notes', fieldType: 'Textarea', sortOrder: 3 })
        ]);
        expect(getBodyField(schema)).toBe('description');
    });

    it('should pick Story-Block as body field', () => {
        const schema = makeSchema([
            makeField({ variable: 'content', fieldType: 'Story-Block', sortOrder: 1 })
        ]);
        expect(getBodyField(schema)).toBe('content');
    });

    it('should prefer lower sortOrder among body candidates', () => {
        const schema = makeSchema([
            makeField({ variable: 'second', fieldType: 'WYSIWYG', sortOrder: 5 }),
            makeField({ variable: 'first', fieldType: 'Textarea', sortOrder: 2 })
        ]);
        expect(getBodyField(schema)).toBe('first');
    });

    it('should return null when no body-type fields exist', () => {
        const schema = makeSchema([
            makeField({ variable: 'title', fieldType: 'Text', sortOrder: 1 }),
            makeField({ variable: 'count', fieldType: 'Text', sortOrder: 2 })
        ]);
        expect(getBodyField(schema)).toBeNull();
    });

    it('should skip system WYSIWYG fields', () => {
        const schema = makeSchema([
            makeField({
                variable: 'sysBody',
                fieldType: 'WYSIWYG',
                sortOrder: 1,
                fixed: true,
                dataType: 'SYSTEM'
            }),
            makeField({ variable: 'userBody', fieldType: 'WYSIWYG', sortOrder: 2 })
        ]);
        expect(getBodyField(schema)).toBe('userBody');
    });
});

// ─── getBinaryFields ────────────────────────────────────────────────────────

describe('getBinaryFields', () => {
    it('should find Binary, Image, and File fields', () => {
        const schema = makeSchema([
            makeField({ variable: 'photo', fieldType: 'Binary', sortOrder: 1 }),
            makeField({ variable: 'thumbnail', fieldType: 'Image', sortOrder: 2 }),
            makeField({ variable: 'attachment', fieldType: 'File', sortOrder: 3 }),
            makeField({ variable: 'title', fieldType: 'Text', sortOrder: 4 })
        ]);
        const result = getBinaryFields(schema);
        expect(result).toHaveLength(3);
        expect(result.map((f) => f.variable)).toEqual(['photo', 'thumbnail', 'attachment']);
    });

    it('should return empty when no binary fields', () => {
        const schema = makeSchema([
            makeField({ variable: 'title', fieldType: 'Text', sortOrder: 1 })
        ]);
        expect(getBinaryFields(schema)).toHaveLength(0);
    });

    it('should skip system binary fields', () => {
        const schema = makeSchema([
            makeField({
                variable: 'sysImage',
                fieldType: 'Binary',
                sortOrder: 1,
                fixed: true,
                dataType: 'SYSTEM'
            }),
            makeField({ variable: 'userImage', fieldType: 'Binary', sortOrder: 2 })
        ]);
        const result = getBinaryFields(schema);
        expect(result).toHaveLength(1);
        expect(result[0].variable).toBe('userImage');
    });
});

// ─── buildGraphQLQuery ──────────────────────────────────────────────────────

describe('buildGraphQLQuery', () => {
    it('should build basic collection query with user fields and system fields', () => {
        const schema = makeSchema([
            makeField({ variable: 'title', fieldType: 'Text', sortOrder: 1 }),
            makeField({ variable: 'body', fieldType: 'WYSIWYG', sortOrder: 2 })
        ]);
        const query = buildGraphQLQuery(schema);

        expect(query).toContain('BlogPostCollection');
        expect(query).toContain('identifier');
        expect(query).toContain('inode');
        expect(query).toContain('modDate');
        expect(query).toContain('conLanguage { id languageCode countryCode }');
        expect(query).toContain('host { hostName }');
        expect(query).toContain('title');
        expect(query).toContain('body');
    });

    it('should use subfield selections for binary fields', () => {
        const schema = makeSchema([
            makeField({ variable: 'title', fieldType: 'Text', sortOrder: 1 }),
            makeField({ variable: 'photo', fieldType: 'Binary', sortOrder: 2 })
        ]);
        const query = buildGraphQLQuery(schema);

        expect(query).toContain('photo { versionPath name idPath size }');
    });

    it('should omit Constant and Hidden field types', () => {
        const schema = makeSchema([
            makeField({ variable: 'title', fieldType: 'Text', sortOrder: 1 }),
            makeField({ variable: 'hidden1', fieldType: 'Constant', sortOrder: 2 }),
            makeField({ variable: 'hidden2', fieldType: 'Hidden', sortOrder: 3 })
        ]);
        const query = buildGraphQLQuery(schema);

        expect(query).toContain('title');
        expect(query).not.toContain('hidden1');
        expect(query).not.toContain('hidden2');
    });

    it('should include query arguments', () => {
        const schema = makeSchema([
            makeField({ variable: 'title', fieldType: 'Text', sortOrder: 1 })
        ]);
        const query = buildGraphQLQuery(schema, {
            site: 'demo.dotcms.com',
            query: '+live:true',
            limit: 10,
            offset: 20
        });

        expect(query).toContain('query: "+live:true"');
        expect(query).toContain('site: "demo.dotcms.com"');
        expect(query).toContain('limit: 10');
        expect(query).toContain('offset: 20');
    });

    it('should not include args parentheses when no options provided', () => {
        const schema = makeSchema([
            makeField({ variable: 'title', fieldType: 'Text', sortOrder: 1 })
        ]);
        const query = buildGraphQLQuery(schema);

        // Should have `BlogPostCollection {` not `BlogPostCollection(`
        expect(query).toMatch(/BlogPostCollection \{/);
    });
});

// ─── serializeContentlet ────────────────────────────────────────────────────

describe('serializeContentlet', () => {
    const schema = makeSchema(baseBlogFields);

    const sampleRecord: ContentletRecord = {
        identifier: 'abc123def456',
        inode: 'inode-001',
        modDate: '2024-01-15T10:30:00Z',
        languageId: 1,
        hostName: 'demo.dotcms.com',
        title: 'Hello World',
        body: '<p>This is the body content</p>',
        author: 'John Doe',
        image: { idPath: '/dA/abc123/image/photo.jpg', name: 'photo.jpg' },
        tags: 'tech,blog,dotcms'
    };

    it('should generate correct filename for default language', () => {
        const result = serializeContentlet(sampleRecord, schema, defaultLanguageMap);
        expect(result.filename).toBe('abc123.md');
    });

    it('should generate correct filename for non-default language', () => {
        const record = { ...sampleRecord, languageId: 2 };
        const result = serializeContentlet(record, schema, defaultLanguageMap);
        expect(result.filename).toBe('abc123.es-ES.md');
    });

    it('should place body field content after frontmatter', () => {
        const result = serializeContentlet(sampleRecord, schema, defaultLanguageMap);
        expect(result.content).toContain('---\n');
        expect(result.content).toContain('<p>This is the body content</p>');

        // body should be AFTER the closing ---
        const parts = result.content.split('---');
        // parts[0] is empty (before first ---), parts[1] is YAML, parts[2] is body
        expect(parts.length).toBeGreaterThanOrEqual(3);
        expect(parts[2]).toContain('<p>This is the body content</p>');
    });

    it('should include metadata in frontmatter', () => {
        const result = serializeContentlet(sampleRecord, schema, defaultLanguageMap);
        expect(result.content).toContain('contentType: BlogPost');
        expect(result.content).toContain('identifier: abc123def456');
        expect(result.content).toContain('language: en-US');
        expect(result.content).toContain('inode: inode-001');
        expect(result.content).toContain('bodyField: body');
    });

    it('should include user fields in frontmatter (except body)', () => {
        const result = serializeContentlet(sampleRecord, schema, defaultLanguageMap);
        expect(result.content).toContain('title: Hello World');
        expect(result.content).toContain('author: John Doe');
    });

    it('should serialize binary fields as local paths', () => {
        const result = serializeContentlet(sampleRecord, schema, defaultLanguageMap);
        expect(result.content).toContain('./assets/abc123/image.photo.jpg');
    });

    it('should return binary field info for sidecar download', () => {
        const result = serializeContentlet(sampleRecord, schema, defaultLanguageMap);
        expect(result.binaries).toHaveLength(1);
        expect(result.binaries[0]).toEqual({
            fieldVariable: 'image',
            localPath: './assets/abc123/image.photo.jpg',
            fileName: 'photo.jpg'
        });
    });

    it('should serialize tag fields as lists', () => {
        const result = serializeContentlet(sampleRecord, schema, defaultLanguageMap);
        // YAML list format: either inline [tech, blog] or block - tech\n- blog
        expect(result.content).toMatch(/tags:/);
    });

    it('should handle missing optional fields gracefully', () => {
        const record: ContentletRecord = {
            identifier: 'abc123def456',
            inode: 'inode-001',
            modDate: '2024-01-15T10:30:00Z',
            languageId: 1,
            title: 'Minimal Post',
            body: 'Minimal body'
        };
        const result = serializeContentlet(record, schema, defaultLanguageMap);
        expect(result.content).toContain('title: Minimal Post');
        expect(result.binaries).toHaveLength(0);
    });

    it('should handle content type with no body field', () => {
        const noBodySchema = makeSchema([
            makeField({ variable: 'title', fieldType: 'Text', sortOrder: 1 }),
            makeField({ variable: 'count', fieldType: 'Text', sortOrder: 2 })
        ]);
        const record: ContentletRecord = {
            identifier: 'def456abc789',
            inode: 'inode-002',
            modDate: '2024-01-15T10:30:00Z',
            languageId: 1,
            title: 'No Body',
            count: '42'
        };
        const result = serializeContentlet(record, noBodySchema, defaultLanguageMap);
        expect(result.content).not.toContain('bodyField:');
    });
});

// ─── parseContentFile ───────────────────────────────────────────────────────

describe('parseContentFile', () => {
    it('should parse frontmatter and body from valid .md content', () => {
        const content = `---
contentType: BlogPost
identifier: abc123def456
language: en-US
inode: inode-001
modDate: "2024-01-15T10:30:00Z"
bodyField: body
title: Hello World
---
<p>This is the body</p>
`;
        const result = parseContentFile('/path/to/abc123.md', content);

        expect(result.frontmatter.contentType).toBe('BlogPost');
        expect(result.frontmatter.identifier).toBe('abc123def456');
        expect(result.frontmatter.language).toBe('en-US');
        expect(result.frontmatter['title']).toBe('Hello World');
        expect(result.body).toContain('<p>This is the body</p>');
        expect(result.filePath).toBe('/path/to/abc123.md');
    });

    it('should handle empty body', () => {
        const content = `---
contentType: BlogPost
identifier: abc123def456
language: en-US
---
`;
        const result = parseContentFile('/path/to/abc123.md', content);
        expect(result.frontmatter.contentType).toBe('BlogPost');
        expect(result.body).toBe('');
    });

    it('should handle complex frontmatter values', () => {
        const content = `---
contentType: BlogPost
identifier: abc123def456
language: en-US
tags:
  - tech
  - blog
keyValue:
  key1: value1
  key2: value2
---
Body here
`;
        const result = parseContentFile('/path/to/abc123.md', content);
        expect(result.frontmatter['tags']).toEqual(['tech', 'blog']);
        expect(result.frontmatter['keyValue']).toEqual({ key1: 'value1', key2: 'value2' });
    });
});

// ─── buildPushPayload ───────────────────────────────────────────────────────

describe('buildPushPayload', () => {
    const schema = makeSchema(baseBlogFields);

    it('should build correct payload from parsed content', () => {
        const parsed = {
            frontmatter: {
                contentType: 'BlogPost',
                identifier: 'abc123def456',
                language: 'en-US',
                inode: 'inode-001',
                modDate: '2024-01-15T10:30:00Z',
                bodyField: 'body',
                title: 'Hello World',
                author: 'John Doe'
            } as ContentletRecord & {
                contentType: string;
                identifier: string;
                language: string;
                inode: string;
                modDate: string;
                bodyField: string;
            },
            body: '<p>This is the body</p>',
            filePath: '/path/to/abc123.md'
        };

        const result = buildPushPayload(parsed, schema);

        expect(result.contentlet['contentType']).toBe('BlogPost');
        expect(result.contentlet['identifier']).toBe('abc123def456');
        expect(result.contentlet['body']).toBe('<p>This is the body</p>');
        expect(result.contentlet['title']).toBe('Hello World');
        expect(result.contentlet['author']).toBe('John Doe');
        expect(result.contentlet['languageId']).toBe('1');
    });

    it('should detect binary fields with local paths', () => {
        const parsed = {
            frontmatter: {
                contentType: 'BlogPost',
                identifier: 'abc123def456',
                language: 'en-US',
                inode: 'inode-001',
                modDate: '2024-01-15T10:30:00Z',
                bodyField: 'body',
                title: 'Post with Image',
                image: './assets/abc123/image.photo.jpg'
            } as ContentletRecord & {
                contentType: string;
                identifier: string;
                language: string;
                inode: string;
                modDate: string;
                bodyField: string;
            },
            body: '<p>Body</p>',
            filePath: '/path/to/abc123.md'
        };

        const result = buildPushPayload(parsed, schema);

        expect(result.binaries).toHaveLength(1);
        expect(result.binaries[0]).toEqual({
            fieldVariable: 'image',
            localPath: './assets/abc123/image.photo.jpg',
            fileName: 'photo.jpg' // field variable prefix stripped from sidecar name
        });
        // Binary fields should NOT be in contentlet JSON
        expect(result.contentlet['image']).toBeUndefined();
    });

    it('should not include metadata fields as user content', () => {
        const parsed = {
            frontmatter: {
                contentType: 'BlogPost',
                identifier: 'abc123def456',
                language: 'en-US',
                inode: 'inode-001',
                modDate: '2024-01-15T10:30:00Z',
                bodyField: 'body',
                title: 'Test'
            } as ContentletRecord & {
                contentType: string;
                identifier: string;
                language: string;
                inode: string;
                modDate: string;
                bodyField: string;
            },
            body: 'Body content',
            filePath: '/path/to/abc123.md'
        };

        const result = buildPushPayload(parsed, schema);

        // These should be set via system field mapping, not duplicated
        expect(result.contentlet['modDate']).toBeUndefined();
        expect(result.contentlet['bodyField']).toBeUndefined();
    });
});

// ─── validateContentFile ────────────────────────────────────────────────────

describe('validateContentFile', () => {
    const schema = makeSchema(baseBlogFields);

    it('should pass validation for valid content', () => {
        const parsed = {
            frontmatter: {
                contentType: 'BlogPost',
                identifier: 'abc123def456',
                language: 'en-US',
                inode: 'inode-001',
                modDate: '2024-01-15T10:30:00Z',
                bodyField: 'body'
            },
            body: '<p>Valid body</p>',
            filePath: '/path/to/abc123.md'
        };

        const result = validateContentFile(parsed, schema);
        expect(result.valid).toBe(true);
        expect(result.errors).toHaveLength(0);
    });

    it('should fail when contentType is missing', () => {
        const parsed = {
            frontmatter: {
                identifier: 'abc123def456',
                language: 'en-US'
            } as unknown as ContentletRecord & {
                contentType: string;
                identifier: string;
                language: string;
                inode: string;
                modDate: string;
            },
            body: 'Body',
            filePath: '/path/to/abc123.md'
        };

        const result = validateContentFile(parsed, schema);
        expect(result.valid).toBe(false);
        expect(result.errors.some((e) => e.includes('contentType'))).toBe(true);
    });

    it('should fail when language is missing', () => {
        const parsed = {
            frontmatter: {
                contentType: 'BlogPost',
                identifier: 'abc123def456'
            } as unknown as ContentletRecord & {
                contentType: string;
                identifier: string;
                language: string;
                inode: string;
                modDate: string;
            },
            body: 'Body',
            filePath: '/path/to/abc123.md'
        };

        const result = validateContentFile(parsed, schema);
        expect(result.valid).toBe(false);
        expect(result.errors.some((e) => e.includes('language'))).toBe(true);
    });

    it('should fail on contentType mismatch', () => {
        const parsed = {
            frontmatter: {
                contentType: 'WrongType',
                identifier: 'abc123def456',
                language: 'en-US',
                inode: 'inode-001',
                modDate: '2024-01-15T10:30:00Z'
            },
            body: 'Body',
            filePath: '/path/to/abc123.md'
        };

        const result = validateContentFile(parsed, schema);
        expect(result.valid).toBe(false);
        expect(result.errors.some((e) => e.includes('mismatch'))).toBe(true);
    });

    it('should validate required user fields are present', () => {
        const requiredSchema = makeSchema([
            makeField({
                variable: 'title',
                fieldType: 'Text',
                sortOrder: 1,
                required: true
            }),
            makeField({ variable: 'body', fieldType: 'WYSIWYG', sortOrder: 2 })
        ]);

        const parsed = {
            frontmatter: {
                contentType: 'BlogPost',
                identifier: 'abc123def456',
                language: 'en-US',
                inode: 'inode-001',
                modDate: '2024-01-15T10:30:00Z'
                // title is missing!
            },
            body: 'Body content',
            filePath: '/path/to/abc123.md'
        };

        const result = validateContentFile(parsed, requiredSchema);
        expect(result.valid).toBe(false);
        expect(result.errors.some((e) => e.includes('title'))).toBe(true);
    });

    it('should validate required body field is not empty', () => {
        const requiredSchema = makeSchema([
            makeField({ variable: 'title', fieldType: 'Text', sortOrder: 1 }),
            makeField({
                variable: 'body',
                fieldType: 'WYSIWYG',
                sortOrder: 2,
                required: true
            })
        ]);

        const parsed = {
            frontmatter: {
                contentType: 'BlogPost',
                identifier: 'abc123def456',
                language: 'en-US',
                inode: 'inode-001',
                modDate: '2024-01-15T10:30:00Z',
                bodyField: 'body',
                title: 'Test'
            },
            body: '',
            filePath: '/path/to/abc123.md'
        };

        const result = validateContentFile(parsed, requiredSchema);
        expect(result.valid).toBe(false);
        expect(result.errors.some((e) => e.includes('body'))).toBe(true);
    });
});

// ─── Field type serialization matrix ────────────────────────────────────────

describe('field type serialization', () => {
    it('should serialize Select field as string', () => {
        const schema = makeSchema([
            makeField({ variable: 'status', fieldType: 'Select', sortOrder: 1 })
        ]);
        const record: ContentletRecord = {
            identifier: 'abc123def456',
            inode: 'inode-001',
            modDate: '2024-01-15T10:30:00Z',
            languageId: 1,
            status: 'published'
        };
        const result = serializeContentlet(record, schema, defaultLanguageMap);
        expect(result.content).toContain('status: published');
    });

    it('should serialize MultiSelect field as list', () => {
        const schema = makeSchema([
            makeField({ variable: 'categories', fieldType: 'MultiSelect', sortOrder: 1 })
        ]);
        const record: ContentletRecord = {
            identifier: 'abc123def456',
            inode: 'inode-001',
            modDate: '2024-01-15T10:30:00Z',
            languageId: 1,
            categories: 'cat1,cat2,cat3'
        };
        const result = serializeContentlet(record, schema, defaultLanguageMap);
        // Should be serialized as a YAML list
        expect(result.content).toMatch(/categories:/);
    });

    it('should serialize KeyValue field as map', () => {
        const schema = makeSchema([
            makeField({ variable: 'metadata', fieldType: 'KeyValue', sortOrder: 1 })
        ]);
        const record: ContentletRecord = {
            identifier: 'abc123def456',
            inode: 'inode-001',
            modDate: '2024-01-15T10:30:00Z',
            languageId: 1,
            metadata: '{"seoTitle":"Test","seoDesc":"Description"}'
        };
        const result = serializeContentlet(record, schema, defaultLanguageMap);
        expect(result.content).toMatch(/metadata:/);
    });

    it('should serialize Date field as string', () => {
        const schema = makeSchema([
            makeField({
                variable: 'publishDate',
                fieldType: 'Date',
                sortOrder: 1,
                dataType: 'DATE'
            })
        ]);
        const record: ContentletRecord = {
            identifier: 'abc123def456',
            inode: 'inode-001',
            modDate: '2024-01-15T10:30:00Z',
            languageId: 1,
            publishDate: '2024-06-15'
        };
        const result = serializeContentlet(record, schema, defaultLanguageMap);
        expect(result.content).toContain('publishDate:');
    });

    it('should omit Constant and Hidden fields', () => {
        const schema = makeSchema([
            makeField({ variable: 'title', fieldType: 'Text', sortOrder: 1 }),
            makeField({ variable: 'divider', fieldType: 'Constant', sortOrder: 2 }),
            makeField({ variable: 'secret', fieldType: 'Hidden', sortOrder: 3 })
        ]);
        const record: ContentletRecord = {
            identifier: 'abc123def456',
            inode: 'inode-001',
            modDate: '2024-01-15T10:30:00Z',
            languageId: 1,
            title: 'Test',
            divider: 'some value',
            secret: 'hidden value'
        };
        const result = serializeContentlet(record, schema, defaultLanguageMap);
        expect(result.content).not.toContain('divider:');
        expect(result.content).not.toContain('secret:');
    });
});

// ─── Round-trip test ────────────────────────────────────────────────────────

describe('round-trip: serialize then parse', () => {
    it('should preserve data through serialize → parse → payload', () => {
        const schema = makeSchema([
            makeField({ variable: 'title', fieldType: 'Text', sortOrder: 1 }),
            makeField({ variable: 'body', fieldType: 'WYSIWYG', sortOrder: 2 }),
            makeField({ variable: 'author', fieldType: 'Text', sortOrder: 3 })
        ]);
        const record: ContentletRecord = {
            identifier: 'abc123def456',
            inode: 'inode-001',
            modDate: '2024-01-15T10:30:00Z',
            languageId: 1,
            title: 'Round Trip Test',
            body: '<h1>Hello</h1>\n<p>World</p>',
            author: 'Test Author'
        };

        // Serialize
        const serialized = serializeContentlet(record, schema, defaultLanguageMap);

        // Parse
        const parsed = parseContentFile('/tmp/abc123.md', serialized.content);

        // Build payload
        const { contentlet } = buildPushPayload(parsed, schema);

        expect(contentlet['contentType']).toBe('BlogPost');
        expect(contentlet['identifier']).toBe('abc123def456');
        expect(contentlet['title']).toBe('Round Trip Test');
        expect(contentlet['author']).toBe('Test Author');
        expect(contentlet['body']).toContain('<h1>Hello</h1>');
        expect(contentlet['body']).toContain('<p>World</p>');
    });
});

// ─── Multi-language filename ────────────────────────────────────────────────

describe('multi-language filename generation', () => {
    it('should use no suffix for default language (id=1)', () => {
        const schema = makeSchema([
            makeField({ variable: 'title', fieldType: 'Text', sortOrder: 1 })
        ]);
        const record: ContentletRecord = {
            identifier: 'abc123def456',
            inode: 'inode-001',
            modDate: '2024-01-15T10:30:00Z',
            languageId: 1,
            title: 'Test'
        };
        const result = serializeContentlet(record, schema, defaultLanguageMap);
        expect(result.filename).toBe('abc123.md');
    });

    it('should add language suffix for non-default language', () => {
        const schema = makeSchema([
            makeField({ variable: 'title', fieldType: 'Text', sortOrder: 1 })
        ]);
        const record: ContentletRecord = {
            identifier: 'abc123def456',
            inode: 'inode-001',
            modDate: '2024-01-15T10:30:00Z',
            languageId: 2,
            title: 'Test'
        };
        const result = serializeContentlet(record, schema, defaultLanguageMap);
        expect(result.filename).toBe('abc123.es-ES.md');
    });

    it('should use languageId as fallback when not in map', () => {
        const schema = makeSchema([
            makeField({ variable: 'title', fieldType: 'Text', sortOrder: 1 })
        ]);
        const record: ContentletRecord = {
            identifier: 'abc123def456',
            inode: 'inode-001',
            modDate: '2024-01-15T10:30:00Z',
            languageId: 99,
            title: 'Test'
        };
        const result = serializeContentlet(record, schema, defaultLanguageMap);
        expect(result.filename).toBe('abc123.99.md');
    });
});
