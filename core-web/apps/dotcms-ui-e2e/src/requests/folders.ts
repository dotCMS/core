import { APIRequestContext, expect } from '@playwright/test';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

function authHeaders() {
    return {
        Authorization: generateBase64Credentials(admin1.username, admin1.password),
        'Content-Type': 'application/json'
    };
}

/**
 * Creates folders under a given site and flushes the server-side
 * FolderCache so the tree browsing API returns fresh data.
 */
export async function createFolders(
    request: APIRequestContext,
    siteName: string,
    paths: string[]
): Promise<void> {
    const response = await request.post(
        `/api/v1/folder/createfolders/${siteName}?indexPolicy=WAIT_FOR`,
        { data: paths, headers: authHeaders() }
    );
    expect(response.status()).toBe(200);

    await request.delete('/api/v1/caches/region/FolderCache', {
        headers: authHeaders()
    });
}
