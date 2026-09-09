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
 * Removes folders from a site and flushes the FolderCache, so a suite can undo what it seeded.
 *
 * Without this, every run leaves its folders behind for good. They accumulate across runs until a
 * tree level or a search result set — both paged at 40 and sorted by name — no longer contains the
 * folder the current run just created, and tests start failing while the product works fine.
 *
 * Best-effort by design: teardown must not turn an already-failing test into a confusing second
 * error, and a folder that is already gone is the outcome we wanted anyway.
 */
export async function deleteFolders(
    request: APIRequestContext,
    siteName: string,
    paths: string[]
): Promise<void> {
    await request.delete(`/api/v1/folder/${siteName}`, {
        data: paths,
        headers: authHeaders()
    });

    await request.delete('/api/v1/caches/region/FolderCache', {
        headers: authHeaders()
    });
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

/**
 * Creates one folder that only admits file names matching the given globs.
 *
 * Its own helper because the *filter* is the point: a folder's `fileMasks` is a glob on the file
 * name, checked per folder, and it is the only way to make the server refuse a file for a reason
 * that has nothing to do with its type or its size. `createFolders` cannot express it — that
 * endpoint takes paths and nothing else.
 *
 * @param siteName the site the folder belongs to, e.g. `demo.dotcms.com`
 * @param path the folder path, leading slash included
 * @param fileMasks the globs to admit, e.g. `['*.jpg']`
 */
export async function createFilteredFolder(
    request: APIRequestContext,
    siteName: string,
    path: string,
    fileMasks: string[]
): Promise<void> {
    const response = await request.post('/api/v1/assets/folders', {
        data: {
            assetPath: `//${siteName}${path}`,
            data: {
                title: path.replace(/^\//, ''),
                name: path.replace(/^\//, ''),
                fileMasks
            }
        },
        headers: authHeaders()
    });
    expect(response.ok()).toBeTruthy();

    await request.delete('/api/v1/caches/region/FolderCache', {
        headers: authHeaders()
    });
}
