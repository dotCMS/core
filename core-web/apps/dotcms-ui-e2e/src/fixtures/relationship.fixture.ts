import { type APIRequestContext, test as base, expect, type Page } from '@playwright/test';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

import {
    type ContentType,
    type CreateContentTypePayload,
    createFakeContentType
} from '../requests/contentType';
import { createFieldVariable } from '../requests/field-variables';

export type TestContentType = ContentType;

/**
 * Represents a contentlet created for relationship tests.
 */
export interface TestContentlet {
    identifier: string;
    inode: string;
    title: string;
    [key: string]: unknown;
}

// ─── Contentlet API Helpers ──────────────────────────────────────

function authHeaders() {
    return {
        Authorization: generateBase64Credentials(admin1.username, admin1.password)
    };
}

async function fireContentlet(
    request: APIRequestContext,
    contentType: string,
    fields: Record<string, unknown>
): Promise<TestContentlet> {
    const response = await request.put(
        '/api/v1/workflow/actions/default/fire/PUBLISH?indexPolicy=WAIT_FOR',
        {
            data: { contentlet: { contentType, ...fields } },
            headers: authHeaders()
        }
    );
    expect(response.status()).toBe(200);
    const data = await response.json();
    return data.entity as TestContentlet;
}

interface BulkPublishResponseBody {
    entity?: {
        results?: Array<Record<string, unknown>>;
        summary?: { failCount?: number; successCount?: number };
    };
}

/**
 * Parses bulk PUBLISH response. Results are ordered by `title` because the API
 * completes futures in non-deterministic order.
 */
function parseBulkPublishContentlets(
    data: BulkPublishResponseBody,
    expectedCount: number
): TestContentlet[] {
    const summary = data.entity?.summary;
    expect(summary?.failCount ?? 0, `bulk publish failures: ${JSON.stringify(summary)}`).toBe(0);
    expect(summary?.successCount).toBe(expectedCount);
    const results = data.entity?.results ?? [];
    expect(results).toHaveLength(expectedCount);
    const contentlets: TestContentlet[] = [];
    for (const entry of results) {
        const payload = Object.values(entry)[0];
        expect(
            payload != null && typeof payload === 'object',
            'bulk result entry missing contentlet'
        ).toBe(true);
        contentlets.push(payload as TestContentlet);
    }
    contentlets.sort((a, b) =>
        String(a.title).localeCompare(String(b.title), undefined, { numeric: true })
    );
    return contentlets;
}

async function fireContentlets(
    request: APIRequestContext,
    contentType: string,
    fieldsList: Record<string, unknown>[]
): Promise<TestContentlet[]> {
    const items = fieldsList.map((fields) => ({ contentType, ...fields }));
    const response = await request.post(
        '/api/v1/workflow/actions/default/fire/PUBLISH?indexPolicy=WAIT_FOR',
        {
            data: { contentlets: items },
            headers: { ...authHeaders(), 'Content-Type': 'application/json' }
        }
    );
    const bodyText = await response.text();
    expect(response.status(), bodyText).toBe(200);
    const data = JSON.parse(bodyText) as BulkPublishResponseBody;
    return parseBulkPublishContentlets(data, items.length);
}

async function fireContentletWithRelationship(
    request: APIRequestContext,
    contentType: string,
    fields: Record<string, unknown>,
    relationships: Record<string, string>
): Promise<TestContentlet> {
    const response = await request.put(
        '/api/v1/workflow/actions/default/fire/PUBLISH?indexPolicy=WAIT_FOR',
        {
            data: {
                contentlet: { contentType, ...fields, ...relationships }
            },
            headers: authHeaders()
        }
    );
    if (response.status() !== 200) {
        const errorBody = await response.json().catch(() => response.statusText());
        console.error(
            'fireContentletWithRelationship failed:',
            JSON.stringify({ contentType, fields, relationships, error: errorBody }, null, 2)
        );
    }
    expect(response.status()).toBe(200);
    const data = await response.json();
    return data.entity as TestContentlet;
}

async function enableNewEditor(
    request: APIRequestContext,
    contentTypeVariable: string
): Promise<void> {
    const flagResponse = await request.post('/api/v1/system-table/', {
        data: { key: 'DOT_CONTENT_EDITOR2_ENABLED', value: true },
        headers: authHeaders()
    });
    expect(
        flagResponse.ok(),
        `enableNewEditor: failed to set DOT_CONTENT_EDITOR2_ENABLED (status ${flagResponse.status()})`
    ).toBeTruthy();

    const patternResponse = await request.post('/api/v1/system-table/', {
        data: { key: 'DOT_CONTENT_EDITOR2_CONTENT_TYPE', value: contentTypeVariable },
        headers: authHeaders()
    });
    expect(
        patternResponse.ok(),
        `enableNewEditor: failed to set DOT_CONTENT_EDITOR2_CONTENT_TYPE (status ${patternResponse.status()})`
    ).toBeTruthy();

    const putResponse = await request.put(`/api/v1/contenttype/id/${contentTypeVariable}`, {
        data: {
            contentType: {
                variable: contentTypeVariable,
                metadata: { CONTENT_EDITOR2_ENABLED: true }
            }
        },
        headers: authHeaders()
    });
    expect(
        putResponse.status(),
        `enableNewEditor: content type metadata PUT failed with status ${putResponse.status()}`
    ).toBe(200);
}

// ─── Content Type Payload Builders ───────────────────────────────
// Defaults (clazz, host, folder, workflow, metadata) are provided by createFakeContentType.

function authorContentTypePayload(suffix: string): CreateContentTypePayload {
    return {
        name: `E2E_Author_${suffix}`,
        variable: `E2EAuthor${suffix}`,
        fields: [
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                name: 'Title',
                variable: 'title',
                sortOrder: 1
            },
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                name: 'Bio',
                variable: 'bio',
                sortOrder: 2
            }
        ]
    };
}

function tagContentTypePayload(suffix: string): CreateContentTypePayload {
    return {
        name: `E2E_Tag_${suffix}`,
        variable: `E2ETag${suffix}`,
        metadata: {},
        fields: [
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                name: 'Name',
                variable: 'name',
                sortOrder: 1
            }
        ]
    };
}

function blogContentTypePayload(
    suffix: string,
    name: string,
    variable: string,
    relatedContentTypeVariable: string,
    relationshipFieldVariable: string,
    cardinality: number
): CreateContentTypePayload {
    return {
        name: `${name}_${suffix}`,
        variable: `${variable}${suffix}`,
        fields: [
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                name: 'Title',
                variable: 'title',
                sortOrder: 1
            },
            {
                clazz: 'com.dotcms.contenttype.model.field.ImmutableRelationshipField',
                name:
                    relationshipFieldVariable.charAt(0).toUpperCase() +
                    relationshipFieldVariable.slice(1),
                variable: relationshipFieldVariable,
                sortOrder: 2,
                relationships: {
                    velocityVar: relatedContentTypeVariable,
                    cardinality
                }
            }
        ]
    };
}

// ─── Cardinality Constants ───────────────────────────────────────

/**
 * Cardinality values as defined in RELATIONSHIP_OPTIONS:
 *   0 -> ONE_TO_MANY, 1 -> MANY_TO_MANY, 2 -> ONE_TO_ONE, 3 -> MANY_TO_ONE
 */
export const CARDINALITY = {
    ONE_TO_MANY: 0,
    MANY_TO_MANY: 1,
    ONE_TO_ONE: 2,
    MANY_TO_ONE: 3
} as const;

// ─── Test Data Interface ─────────────────────────────────────────

export interface RelationshipTestData {
    suffix: string;
    authorType: TestContentType;
    authors: TestContentlet[];
}

// ─── Fixture ─────────────────────────────────────────────────────

export const test = base.extend<{
    adminPage: Page;
    testSuffix: string;
    apiHelpers: {
        createContentType: (payload: CreateContentTypePayload) => Promise<ContentType>;
        createContentlet: (ct: string, fields: Record<string, unknown>) => Promise<TestContentlet>;
        /** Bulk create; returned array sorted by `title` (API completion order is not guaranteed). */
        createContentlets: (
            ct: string,
            fieldsList: Record<string, unknown>[]
        ) => Promise<TestContentlet[]>;
        createContentletWithRelationship: (
            ct: string,
            fields: Record<string, unknown>,
            rels: Record<string, string>
        ) => Promise<TestContentlet>;
        enableNewEditor: (ctVar: string) => Promise<void>;
        authorPayload: (suffix: string) => CreateContentTypePayload;
        tagPayload: (suffix: string) => CreateContentTypePayload;
        blogPayload: (
            suffix: string,
            name: string,
            variable: string,
            relatedCt: string,
            relField: string,
            cardinality: number
        ) => CreateContentTypePayload;
        addFieldVariable: (
            contentTypeId: string,
            fieldId: string,
            key: string,
            value: string
        ) => Promise<void>;
        CARDINALITY: typeof CARDINALITY;
    };
}>({
    adminPage: async ({ page }, use) => {
        await use(page);
    },

    // eslint-disable-next-line no-empty-pattern
    testSuffix: async ({}, use) => {
        await use(crypto.randomUUID().slice(0, 8));
    },

    apiHelpers: async ({ request }, use) => {
        await use({
            createContentType: (payload) => createFakeContentType(request, payload),
            createContentlet: (ct, fields) => fireContentlet(request, ct, fields),
            createContentlets: (ct, fieldsList) => fireContentlets(request, ct, fieldsList),
            createContentletWithRelationship: (ct, fields, rels) =>
                fireContentletWithRelationship(request, ct, fields, rels),
            enableNewEditor: (ctVar) => enableNewEditor(request, ctVar),
            authorPayload: (suffix: string) => authorContentTypePayload(suffix),
            tagPayload: (suffix: string) => tagContentTypePayload(suffix),
            blogPayload: (
                suffix: string,
                name: string,
                variable: string,
                relatedCt: string,
                relField: string,
                cardinality: number
            ) => blogContentTypePayload(suffix, name, variable, relatedCt, relField, cardinality),
            addFieldVariable: (contentTypeId, fieldId, key, value) =>
                createFieldVariable(request, contentTypeId, fieldId, key, value),
            CARDINALITY
        });
    }
});

export { expect } from '@playwright/test';
export { SYSTEM_WORKFLOW_ID } from '../requests/contentType';
