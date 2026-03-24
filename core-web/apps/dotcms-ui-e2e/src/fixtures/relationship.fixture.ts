import { test as base, type Page, type APIRequestContext, expect } from '@playwright/test';
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
async function deleteContentTypeById(
    request: APIRequestContext,
    idOrVar: string
): Promise<void> {
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
        console.error('fireContentletWithRelationship failed:', JSON.stringify({ contentType, fields, relationships, error: errorBody }, null, 2));
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
    await request.post('/api/v1/system-table/', {
        data: { key: 'DOT_CONTENT_EDITOR2_ENABLED', value: true },
        headers: authHeaders()
    });
    // Set the content type pattern
    await request.post('/api/v1/system-table/', {
        data: { key: 'DOT_CONTENT_EDITOR2_CONTENT_TYPE', value: '*' },
        headers: authHeaders()
    });
    // Enable the new editor in the content type's metadata
    const response = await request.put(
        `/api/v1/contenttype/id/${contentTypeVariable}`,
        {
            data: {
                contentType: {
                    variable: contentTypeVariable,
                    metadata: { CONTENT_EDITOR2_ENABLED: true }
                }
            },
            headers: authHeaders()
        }
    );
    // Might be 200 or 400 depending on API version; the system-table flags are the critical ones
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
                name: relationshipFieldVariable.charAt(0).toUpperCase() +
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
        createContentletWithRelationship: (ct: string, fields: Record<string, unknown>, rels: Record<string, string>) => Promise<TestContentlet>;
        enableNewEditor: (ctVar: string) => Promise<void>;
        authorPayload: (suffix: string) => Record<string, unknown>;
        tagPayload: (suffix: string) => Record<string, unknown>;
        blogPayload: (suffix: string, name: string, variable: string, relatedCt: string, relField: string, cardinality: number) => Record<string, unknown>;
        addFieldVariable: (contentTypeId: string, fieldId: string, key: string, value: string) => Promise<void>;
        CARDINALITY: typeof CARDINALITY;
    };
}>({
    adminPage: async ({ page }, use) => {
        // Auth cookies loaded automatically via storageState in playwright.config.ts
        // No login needed — the setup project handles it once for all workers.
        await use(page);
    },

    testSuffix: async ({}, use) => {
        await use(`${Date.now()}`);
    },

    apiHelpers: async ({ request }, use) => {
        const workflowId = SYSTEM_WORKFLOW_ID;

        await use({
            createContentType: (payload) => createContentTypeWithFields(request, payload),
            deleteContentType: (id) => deleteContentTypeById(request, id),
            createContentlet: (ct, fields) => fireContentlet(request, ct, fields),
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
            ) => blogContentTypePayload(suffix, name, variable, relatedCt, relField, cardinality, workflowId),
            addFieldVariable: (contentTypeId, fieldId, key, value) =>
                createFieldVariable(request, contentTypeId, fieldId, key, value),
            CARDINALITY
        });
    }
});

export { expect } from '@playwright/test';
