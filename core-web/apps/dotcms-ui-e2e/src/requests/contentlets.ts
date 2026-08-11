import { APIRequestContext, expect } from '@playwright/test';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

/**
 * Represents a dotCMS contentlet returned from the API.
 */
export interface Contentlet {
    identifier: string;
    inode: string;
    title: string;
    contentType: string;
    languageId: number;
    [key: string]: unknown;
}

/**
 * Creates a contentlet via the workflow fire/publish API.
 *
 * @param request - Playwright APIRequestContext
 * @param data - Contentlet fields including contentType
 * @returns The created contentlet
 */
export async function createContentlet(
    request: APIRequestContext,
    data: Record<string, unknown>
): Promise<Contentlet> {
    const endpoint = `/api/v1/workflow/actions/default/fire/PUBLISH?indexPolicy=WAIT_FOR`;
    const response = await request.post(endpoint, {
        data: {
            contentlet: data
        },
        headers: {
            Authorization: generateBase64Credentials(admin1.username, admin1.password)
        }
    });

    expect(response.status()).toBe(200);

    const responseData = await response.json();
    const entity = responseData.entity;

    return entity as Contentlet;
}

/**
 * Creates a dotAsset contentlet from an in-memory file, in one multipart call.
 *
 * Mirrors what the product itself does (`DotUploadFileService.uploadDotAsset` →
 * `DotWorkflowActionsFireService.newContentlet`): a `PUT .../fire/NEW` whose body carries the binary
 * as the `file` part and the contentlet as a `json` part. Going through `/api/v1/temp` first would
 * work too, but that endpoint fingerprints the caller (session + origin), so a single call is one
 * less thing to get wrong from a test runner.
 *
 * `indexPolicy=WAIT_FOR` is what makes this usable for seeding — the asset is searchable by the time
 * the request returns, so a test can open a picker and expect to find it.
 *
 * @param request - Playwright APIRequestContext
 * @param file - The file to store, as `{ name, mimeType, buffer }`
 * @param hostFolder - Site identifier or folder id the asset is created under
 * @returns The created contentlet
 */
export async function createDotAsset(
    request: APIRequestContext,
    file: { name: string; mimeType: string; buffer: Buffer },
    hostFolder: string
): Promise<Contentlet> {
    const endpoint = `/api/v1/workflow/actions/default/fire/NEW?indexPolicy=WAIT_FOR`;
    const response = await request.put(endpoint, {
        multipart: {
            file: { name: file.name, mimeType: file.mimeType, buffer: file.buffer },
            json: JSON.stringify({
                contentlet: { contentType: 'dotAsset', file: file.name, hostFolder }
            })
        },
        headers: {
            Authorization: generateBase64Credentials(admin1.username, admin1.password)
        }
    });

    expect(response.status()).toBe(200);

    const responseData = await response.json();

    return responseData.entity as Contentlet;
}

/**
 * Relates content via the relationship API.
 * Uses the PUBLISH workflow action to save content with relationship data.
 *
 * @param request - Playwright APIRequestContext
 * @param contentletIdentifier - The identifier of the parent contentlet
 * @param relationshipVariable - The variable name of the relationship field
 * @param relatedIdentifiers - Array of identifiers to relate
 */
export async function relateContent(
    request: APIRequestContext,
    contentletIdentifier: string,
    relationshipVariable: string,
    relatedIdentifiers: string[]
): Promise<Contentlet> {
    const endpoint = `/api/v1/workflow/actions/default/fire/PUBLISH?indexPolicy=WAIT_FOR`;
    const response = await request.put(endpoint, {
        data: {
            contentlet: {
                identifier: contentletIdentifier,
                [relationshipVariable]: relatedIdentifiers.join(',')
            }
        },
        headers: {
            Authorization: generateBase64Credentials(admin1.username, admin1.password)
        }
    });

    expect(response.status()).toBe(200);
    const responseData = await response.json();
    return responseData.entity as Contentlet;
}

/**
 * Deletes contentlets by their identifiers.
 *
 * @param request - Playwright APIRequestContext
 * @param identifiers - Array of contentlet identifiers to delete
 */
export async function deleteContentlets(
    request: APIRequestContext,
    identifiers: string[]
): Promise<void> {
    for (const identifier of identifiers) {
        const endpoint = `/api/v1/content/actions/default/fire/DESTROY?identifier=${identifier}`;
        const response = await request.put(endpoint, {
            headers: {
                Authorization: generateBase64Credentials(admin1.username, admin1.password)
            },
            data: {}
        });
        // Accept 200 or 404 (already deleted)
        expect([200, 404]).toContain(response.status());
    }
}
