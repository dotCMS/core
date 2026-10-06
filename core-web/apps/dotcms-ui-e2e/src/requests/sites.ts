import { APIRequestContext, expect } from '@playwright/test';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

export interface Site {
    aliases: string | null;
    archived: boolean;
    categoryId: string;
    contentTypeId: string;
    default: boolean;
    dotAsset: boolean;
    fileAsset: boolean;
    folder: string;
    form: boolean;
    host: string;
    hostThumbnail: string | null;
    hostname: string;
    htmlpage: boolean;
    identifier: string;
    indexPolicyDependencies: string;
    inode: string;
    keyValue: boolean;
    languageId: number;
    languageVariable: boolean;
    live: boolean;
    locked: boolean;
    lowIndexPriority: boolean;
    modDate: number;
    modUser: string;
    name: string;
    new: boolean;
    owner: string;
    parent: boolean;
    permissionId: string;
    permissionType: string;
    persona: boolean;
    sortOrder: number;
    structureInode: string;
    systemHost: boolean;
    tagStorage: string;
    title: string;
    titleImage: string | null;
    type: string;
    vanityUrl: boolean;
    variantId: string;
    versionId: string;
    working: boolean;
}

/**
 * Creates a site and publishes it, so it is browsable and shows up in the site selectors.
 *
 * Two calls, because a freshly created site comes back `live: false` and an unpublished site is
 * not offered anywhere the user can browse to it:
 *
 *   POST /api/v1/site            { "siteName": "<hostname>" }   -> 200, entity.identifier
 *   PUT  /api/v1/site/{id}/_publish                             -> 200
 *
 * Note the field name asymmetry, verified against a running instance: the **create** response
 * carries `siteName` and has no `hostname` at all, while `GET /api/v1/site` (and therefore
 * {@link Site}) carries `hostname`. Reading `hostname` off the create response yields `undefined`,
 * so the hostname is threaded through from the caller's argument instead.
 */
export async function createSite(
    request: APIRequestContext,
    hostname: string
): Promise<{ identifier: string; hostname: string }> {
    const created = await request.post('/api/v1/site', {
        data: { siteName: hostname },
        headers: {
            Authorization: generateBase64Credentials(admin1.username, admin1.password),
            'Content-Type': 'application/json'
        }
    });
    expect(created.status()).toBe(200);
    const { entity } = await created.json();
    const identifier = entity.identifier as string;

    const published = await request.put(`/api/v1/site/${identifier}/_publish`, {
        headers: {
            Authorization: generateBase64Credentials(admin1.username, admin1.password)
        }
    });
    expect(published.status()).toBe(200);

    return { identifier, hostname };
}

/**
 * Removes a site created by {@link createSite}. Unpublish -> archive -> delete, in that order:
 * a live or unarchived site cannot be deleted.
 */
export async function deleteSite(request: APIRequestContext, identifier: string): Promise<void> {
    const headers = {
        Authorization: generateBase64Credentials(admin1.username, admin1.password)
    };

    await request.put(`/api/v1/site/${identifier}/_unpublish`, { headers });
    await request.put(`/api/v1/site/${identifier}/_archive`, { headers });
    await request.delete(`/api/v1/site/${identifier}`, { headers });
}

export async function getSites(request: APIRequestContext) {
    const endpoint = `/api/v1/site?filter=*&per_page=15&system=true`;
    const response = await request.get(endpoint, {
        headers: {
            Authorization: generateBase64Credentials(admin1.username, admin1.password)
        }
    });
    expect(response.status()).toBe(200);
    const responseData = await response.json();
    return responseData.entity as Site[];
}

export async function getCurrentSite(request: APIRequestContext) {
    const response = await request.get('/api/v1/site/currentSite', {
        headers: {
            Authorization: generateBase64Credentials(admin1.username, admin1.password)
        }
    });
    expect(response.status()).toBe(200);
    const responseData = await response.json();
    return responseData.entity as Site;
}

/**
 * Returns the default site from the site catalog, or throws if none is marked default.
 */
export async function getDefaultSite(request: APIRequestContext): Promise<Site> {
    const sites = await getSites(request);
    const site = sites.find((s) => s.default);

    if (!site) {
        throw new Error('No default site found');
    }

    return site;
}
