import { type APIRequestContext, test as base, expect, type Page } from '@playwright/test';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';
import { createFieldVariable } from '../requests/field-variables';

/**
 * Represents a content type created for relationship tests.
 */
export interface TestContentType {
    id: string;
    name: string;
    variable: string;
    fields: { id: string; variable: string; clazz: string }[];
}

/**
 * Represents a contentlet created for relationship tests.
 */
export interface TestContentlet {
    identifier: string;
    inode: string;
    title: string;
    [key: string]: unknown;
}

// ─── API Helpers ─────────────────────────────────────────────────

function authHeaders() {
    return {
        Authorization: generateBase64Credentials(admin1.username, admin1.password)
    };
}

/**
 * Creates a content type with fields via the v3 content type API.
 */
async function createContentTypeWithFields(
    request: APIRequestContext,
    payload: Record<string, unknown>
): Promise<TestContentType> {
    const response = await request.post('/api/v1/contenttype', {
        data: payload,
        headers: authHeaders()
    });
    if (response.status() !== 200) {
        const errorBody = await response.json().catch(() => response.statusText());
        console.error('createContentType failed:', JSON.stringify(errorBody, null, 2));
    }
    expect(response.status()).toBe(200);
    const data = await response.json();
    const entity = data.entity[0] ?? data.entity;
    return {
        id: entity.id,
        name: entity.name,
        variable: entity.variable,
        fields: entity.fields ?? []
    };
}

/**
 * Deletes a content type by its id or variable name.
 */
async function deleteContentTypeById(request: APIRequestContext, idOrVar: string): Promise<void> {
    const response = await request.delete(`/api/v1/contenttype/id/${idOrVar}`, {
        headers: authHeaders()
    });
    // Accept 200 or 404 (already cleaned up)
    expect([200, 404]).toContain(response.status());
}

/**
 * Creates a contentlet via the workflow fire/PUBLISH endpoint.
 */
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
 * Parses bulk PUBLISH response (`POST .../workflow/actions/default/fire/PUBLISH` with `contentlets` array).
 * Results are ordered by `title` because the API completes futures in non-deterministic order.
 */
function parseBulkPublishContentlets(
    data: BulkPublishResponseBody,
    expectedCount: number
): TestContentlet[] {
    const summary = data.entity?.summary;
    expect(summary?.failCount ?? 0, `bulk publish failures: ${JSON.stringify(summary)}`).toBe(0);
    expect(summary?.successCount).toBe(expectedCount);
    const results = data.entity?.results ?? [];
    expect(results.length).toBe(expectedCount);
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

/**
 * Creates several contentlets in one request (POST bulk PUBLISH with `contentlets` body).
 */
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

/**
 * Saves a contentlet and sets its relationships via the workflow PUBLISH endpoint.
 */
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

/**
 * Enables the new content editor feature flag for a specific content type.
 */
async function enableNewEditor(
    request: APIRequestContext,
    contentTypeVariable: string
): Promise<void> {
    // Enable the global feature flag
    const flagResponse = await request.post('/api/v1/system-table/', {
        data: { key: 'DOT_CONTENT_EDITOR2_ENABLED', value: true },
        headers: authHeaders()
    });
    expect(
        flagResponse.ok(),
        `enableNewEditor: failed to set DOT_CONTENT_EDITOR2_ENABLED (status ${flagResponse.status()})`
    ).toBeTruthy();

    // Set the content type pattern
    const patternResponse = await request.post('/api/v1/system-table/', {
        data: { key: 'DOT_CONTENT_EDITOR2_CONTENT_TYPE', value: '*' },
        headers: authHeaders()
    });
    expect(
        patternResponse.ok(),
        `enableNewEditor: failed to set DOT_CONTENT_EDITOR2_CONTENT_TYPE (status ${patternResponse.status()})`
    ).toBeTruthy();

    // Enable the new editor in the content type's metadata
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
        [200, 400].includes(putResponse.status()),
        `enableNewEditor: content type metadata PUT failed with status ${putResponse.status()}`
    ).toBe(true);
}

/**
 * SystemWorkflow ID — standard across all dotCMS instances.
 */
export const SYSTEM_WORKFLOW_ID = 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2';

// ─── Content Type Payloads ───────────────────────────────────────

function authorContentTypePayload(suffix: string, workflowId?: string) {
    return {
        clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
        name: `E2E_Author_${suffix}`,
        variable: `E2EAuthor${suffix}`,
        host: 'SYSTEM_HOST',
        folder: 'SYSTEM_FOLDER',
        metadata: { CONTENT_EDITOR2_ENABLED: true },
        ...(workflowId && { workflow: [workflowId] }),
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

function tagContentTypePayload(suffix: string, workflowId?: string) {
    return {
        clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
        name: `E2E_Tag_${suffix}`,
        variable: `E2ETag${suffix}`,
        host: 'SYSTEM_HOST',
        folder: 'SYSTEM_FOLDER',
        ...(workflowId && { workflow: [workflowId] }),
        // No new editor metadata — intentionally left out
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
    cardinality: number,
    workflowId?: string
) {
    return {
        clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
        name: `${name}_${suffix}`,
        variable: `${variable}${suffix}`,
        host: 'SYSTEM_HOST',
        folder: 'SYSTEM_FOLDER',
        metadata: { CONTENT_EDITOR2_ENABLED: true },
        ...(workflowId && { workflow: [workflowId] }),
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
 *   0 → ONE_TO_MANY, 1 → MANY_TO_MANY, 2 → ONE_TO_ONE, 3 → MANY_TO_ONE
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
        createContentType: (payload: Record<string, unknown>) => Promise<TestContentType>;
        deleteContentType: (idOrVar: string) => Promise<void>;
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
        authorPayload: (suffix: string) => Record<string, unknown>;
        tagPayload: (suffix: string) => Record<string, unknown>;
        blogPayload: (
            suffix: string,
            name: string,
            variable: string,
            relatedCt: string,
            relField: string,
            cardinality: number
        ) => Record<string, unknown>;
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
        // Auth cookies loaded automatically via storageState in playwright.config.ts
        // No login needed — the setup project handles it once for all workers.
        await use(page);
    },

    testSuffix: async ({}, use) => {
        await use(crypto.randomUUID().slice(0, 8));
    },

    apiHelpers: async ({ request }, use) => {
        const workflowId = SYSTEM_WORKFLOW_ID;

        await use({
            createContentType: (payload) => createContentTypeWithFields(request, payload),
            deleteContentType: (id) => deleteContentTypeById(request, id),
            createContentlet: (ct, fields) => fireContentlet(request, ct, fields),
            createContentlets: (ct, fieldsList) => fireContentlets(request, ct, fieldsList),
            createContentletWithRelationship: (ct, fields, rels) =>
                fireContentletWithRelationship(request, ct, fields, rels),
            enableNewEditor: (ctVar) => enableNewEditor(request, ctVar),
            authorPayload: (suffix: string) => authorContentTypePayload(suffix, workflowId),
            tagPayload: (suffix: string) => tagContentTypePayload(suffix, workflowId),
            blogPayload: (
                suffix: string,
                name: string,
                variable: string,
                relatedCt: string,
                relField: string,
                cardinality: number
            ) =>
                blogContentTypePayload(
                    suffix,
                    name,
                    variable,
                    relatedCt,
                    relField,
                    cardinality,
                    workflowId
                ),
            addFieldVariable: (contentTypeId, fieldId, key, value) =>
                createFieldVariable(request, contentTypeId, fieldId, key, value),
            CARDINALITY
        });
    }
});

export { expect } from '@playwright/test';
