import { type APIRequestContext } from '@playwright/test';
import { createContentlet } from '@requests/contentlets';
import { type ContentType, createFakeContentType, deleteContentType } from '@requests/contentType';
import { createFolder, deleteFolders } from '@requests/folders';
import {
    createFakePayloadHostFolderField,
    createFakePayloadTextField
} from '@utils/dot-content-types.mock';

/** A folder of the test's own and a content type whose content can be filed in it. */
export interface DriveSeed {
    siteName: string;
    folderName: string;
    folderInode: string;
    contentType: ContentType;
}

/**
 * Creates a folder at the site root and a content type, in the new editor, with a Host/Folder
 * field. The field is what lets content be filed in the folder: without one, the server ignores
 * `hostFolder` and files the content under System Host.
 *
 * @param prefix Starts the folder's name, so leftovers from a failed run are easy to spot.
 */
export async function seedDrive(
    request: APIRequestContext,
    siteName: string,
    prefix: string,
    suffix: string
): Promise<DriveSeed> {
    const folderName = `${prefix}-${suffix}`;
    const folderInode = await createFolder(request, siteName, `/${folderName}`);
    // The folder is made first, so a failed type create must take it back: teardown only sees
    // what this returns, and a folder leaked at the site root can push later seeds off the
    // tree's first page.
    const contentType = await createFakeContentType(request, {
        name: `E2E ${prefix} ${suffix}`,
        fields: [
            createFakePayloadTextField({ name: 'Title', variable: 'title', sortOrder: 1 }),
            createFakePayloadHostFolderField({
                name: 'Site Or Folder',
                variable: 'siteOrFolder',
                sortOrder: 2
            })
        ]
    }).catch(async (error: unknown) => {
        await deleteFolders(request, siteName, [`/${folderName}`]).catch(() => undefined);
        throw error;
    });

    return { siteName, folderName, folderInode, contentType };
}

/** Creates and publishes a contentlet of the seed's type in a folder (the seed's by default). */
export async function seedContent(
    request: APIRequestContext,
    seed: DriveSeed,
    title: string,
    folderInode = seed.folderInode
) {
    return createContentlet(request, {
        contentType: seed.contentType.variable,
        title,
        siteOrFolder: folderInode
    });
}

/**
 * Removes what {@link seedDrive} made, and any folder paths the test added. Deleting the type
 * deletes its contentlets. Best effort, so a failing test reports its own failure.
 */
export async function removeSeed(
    request: APIRequestContext,
    seed: DriveSeed | null,
    extraFolders: string[] = []
) {
    if (!seed) {
        return;
    }

    await deleteContentType(request, seed.contentType.id).catch(() => undefined);
    await deleteFolders(request, seed.siteName, [`/${seed.folderName}`, ...extraFolders]);
}
