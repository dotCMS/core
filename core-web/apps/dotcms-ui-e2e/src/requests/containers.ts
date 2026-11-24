import { APIRequestContext, expect } from '@playwright/test';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

import { getDefaultSite } from './sites';

/** A container as `POST /api/v1/containers` answers it, narrowed to what tests use. */
export interface Container {
    identifier: string;
    inode: string;
    title: string;
}

/** What a new container needs. */
export interface CreateContainer {
    title: string;
    /** Content type ids, each rendered with `code`. */
    contentTypeIds: string[];
    /** The Velocity code for each contentlet. Defaults to its title as a heading. */
    code?: string;
    maxContentlets?: number;
}

const headers = () => ({
    Authorization: generateBase64Credentials(admin1.username, admin1.password)
});

/**
 * Creates and publishes a container on the default site that accepts the given content types.
 *
 * For a page that shows, or takes a drop of, content types a test has just created: the server
 * caches `SYSTEM_CONTAINER`'s rendered code, with its list of types, from its first render, so a
 * type created after that is neither rendered there (its contentlets show as empty wrappers) nor
 * accepted at a drop. A container of the test's own is rendered fresh.
 *
 * @param request - Playwright APIRequestContext
 * @param data - The container's title, accepted types and code
 * @returns The container created
 */
export async function createContainer(
    request: APIRequestContext,
    {
        title,
        contentTypeIds,
        code = '<h1>$!{dotContentMap.title}</h1>',
        maxContentlets = 25
    }: CreateContainer
): Promise<Container> {
    const site = await getDefaultSite(request);
    const response = await request.post('/api/v1/containers', {
        data: {
            title,
            friendlyName: title,
            maxContentlets,
            hostId: site.identifier,
            containerStructures: contentTypeIds.map((structureId) => ({ structureId, code }))
        },
        headers: headers()
    });

    expect(response.status(), `failed to create container ${title}: ${await response.text()}`).toBe(
        200
    );

    return (await response.json()).entity as Container;
}

/**
 * Deletes a container; a published one needs no archiving first. Best effort: teardown must not
 * turn a failing test into a second, misleading error.
 *
 * @param request - Playwright APIRequestContext
 * @param identifier - The container's identifier
 */
export async function deleteContainer(
    request: APIRequestContext,
    identifier: string
): Promise<void> {
    await request
        .delete(`/api/v1/containers?containerId=${encodeURIComponent(identifier)}`, {
            headers: headers()
        })
        .catch(() => undefined);
}
