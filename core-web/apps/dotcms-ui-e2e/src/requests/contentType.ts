import { APIRequestContext, expect } from '@playwright/test';
import { admin1 } from '@utils/credentials';
import { createFakePayloadTextField } from '@utils/dot-content-types.mock';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

export const SYSTEM_WORKFLOW_ID = 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2';

export interface ContentTypeField {
    id?: string;
    variable: string;
    clazz: string;
    name: string;
    sortOrder: number;
    [key: string]: unknown;
}

/**
 * Field payload accepted when creating a content type via POST /api/v1/contenttype.
 * Only the properties needed by the API are required; extra props (e.g. `relationships`,
 * `required`) are allowed via the index signature.
 */
export type ContentTypeFieldInput = ContentTypeField;

export interface ContentType {
    id: string;
    name: string;
    variable: string;
    clazz: string;
    description: string;
    host: string;
    folder: string;
    metadata: Record<string, unknown>;
    workflow: string[];
    fields: ContentTypeField[];
    defaultType: boolean;
    icon: string | null;
    fixed: boolean;
    system: boolean;
    systemActionMappings: Record<string, string>;
}

export type CreateContentTypePayload = Partial<Omit<ContentType, 'id' | 'fields'>> & {
    fields?: ContentTypeFieldInput[];
};

function authHeaders() {
    return {
        Authorization: generateBase64Credentials(admin1.username, admin1.password)
    };
}

/**
 * Creates a content type with sensible defaults (SystemWorkflow, SYSTEM_HOST, new editor enabled).
 * Override any default by passing it in `data`.
 */
export async function createFakeContentType(
    request: APIRequestContext,
    data: CreateContentTypePayload
): Promise<ContentType> {
    const defaults: CreateContentTypePayload = {
        clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
        host: 'SYSTEM_HOST',
        folder: 'SYSTEM_FOLDER',
        name: 'New content type',
        metadata: { CONTENT_EDITOR2_ENABLED: true },
        workflow: [SYSTEM_WORKFLOW_ID],
        fields: [
            createFakePayloadTextField({
                name: 'Title',
                variable: 'title',
                sortOrder: 1
            })
        ]
    };

    return createContentType(request, { ...defaults, ...data });
}

/**
 * Low-level content type creation — sends the payload directly to `/api/v1/contenttype`.
 */
export async function createContentType(
    request: APIRequestContext,
    data: CreateContentTypePayload
): Promise<ContentType> {
    const response = await request.post('/api/v1/contenttype', {
        data,
        headers: authHeaders()
    });

    if (response.status() !== 200) {
        const errorBody = await response.json().catch(() => response.statusText());
        console.error('createContentType failed:', JSON.stringify(errorBody, null, 2));
    }
    expect(response.status()).toBe(200);

    const responseData = await response.json();
    const entity = responseData.entity[0] ?? responseData.entity;

    return entity as ContentType;
}

/**
 * Creates a page content type (base type HTMLPAGE) of the test's own, on System Workflow. The server
 * gives it the fields every page has (title, URL, host or folder, template, ...). A test that needs
 * pages in a given editor uses its own type rather than changing `htmlpageasset`, which every other
 * test running beside it uses.
 *
 * @param request The API context.
 * @param name The type's name.
 * @param newContentEditor Whether its pages open in the new editor.
 */
export async function createPageContentType(
    request: APIRequestContext,
    name: string,
    newContentEditor: boolean
): Promise<ContentType> {
    return createContentType(request, {
        clazz: 'com.dotcms.contenttype.model.type.ImmutablePageContentType',
        name,
        host: 'SYSTEM_HOST',
        folder: 'SYSTEM_FOLDER',
        metadata: { CONTENT_EDITOR2_ENABLED: newContentEditor },
        workflow: [SYSTEM_WORKFLOW_ID]
    });
}

/**
 * Turns a content type's "new edit content screen" (`metadata.CONTENT_EDITOR2_ENABLED`) on or off,
 * as the content type form's toggle does: reads the full type and sends it back with the flag set.
 * The endpoint returns `workflows` (objects) but takes `workflow` (ids).
 *
 * @param request The API context.
 * @param id The content type's id.
 * @param enabled Whether the type opens in the new editor.
 */
export async function setNewContentEditor(
    request: APIRequestContext,
    id: string,
    enabled: boolean
): Promise<void> {
    const getResponse = await request.get(`/api/v1/contenttype/id/${id}`, {
        headers: authHeaders()
    });
    expect(getResponse.status()).toBe(200);
    const { entity } = await getResponse.json();

    const putResponse = await request.put(`/api/v1/contenttype/id/${id}`, {
        data: {
            ...entity,
            metadata: { ...entity.metadata, CONTENT_EDITOR2_ENABLED: enabled },
            workflow: (entity.workflows ?? []).map((workflow: { id: string }) => workflow.id)
        },
        headers: authHeaders()
    });
    expect(putResponse.status()).toBe(200);
}

export async function deleteContentType(request: APIRequestContext, id: string) {
    const response = await request.delete(`/api/v1/contenttype/id/${id}`, {
        headers: authHeaders()
    });
    expect(response.status()).toBe(200);
}
