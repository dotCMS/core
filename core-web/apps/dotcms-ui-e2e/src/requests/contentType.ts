import { APIRequestContext, expect } from '@playwright/test';
import { admin1 } from '@utils/credentials';
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

export type CreateContentTypePayload = Partial<Omit<ContentType, 'id'>>;

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
        workflow: [SYSTEM_WORKFLOW_ID]
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

export async function deleteContentType(request: APIRequestContext, id: string) {
    const response = await request.delete(`/api/v1/contenttype/id/${id}`, {
        headers: authHeaders()
    });
    expect(response.status()).toBe(200);
}
