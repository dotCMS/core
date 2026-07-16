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
