import { DotCMSContentlet, DotContentDriveFolder, DotLanguage } from '@dotcms/dotcms-models';

/** Languages the grid specs resolve `languageId` against. */
export const GRID_LANGUAGES = new Map<number, DotLanguage>([
    [
        1,
        {
            id: 1,
            language: 'English',
            languageCode: 'en',
            countryCode: 'US',
            country: 'USA',
            isoCode: 'en-US'
        }
    ],
    [
        2,
        {
            id: 2,
            language: 'Spanish',
            languageCode: 'es',
            countryCode: 'ES',
            country: 'Spain',
            isoCode: 'es-ES'
        }
    ]
]);

/** A published image, unlocked. */
export const GRID_IMAGE = {
    identifier: 'id-image',
    inode: 'inode-image',
    title: 'A sloth hanging from a branch in the Costa Rica rainforest',
    contentType: 'Image',
    baseType: 'DOTASSET',
    languageId: 1,
    live: true,
    working: true,
    archived: false,
    hasLiveVersion: true,
    locked: false,
    modUserName: 'Luca Romano',
    host: 'host-1'
} as unknown as DotCMSContentlet;

/** A draft blog post, locked by Maya. */
export const GRID_LOCKED_BLOG = {
    identifier: 'id-blog',
    inode: 'inode-blog',
    title: 'Ecotourism in Costa Rica',
    contentType: 'Blog',
    baseType: 'CONTENT',
    languageId: 2,
    live: false,
    working: true,
    archived: false,
    hasLiveVersion: false,
    locked: true,
    lockedBy: { userId: 'maya', firstName: 'Maya', lastName: 'Chen' },
    modUserName: 'Maya Chen',
    host: 'host-1'
} as unknown as DotCMSContentlet;

/** A page with no editor name on it (deleted user). */
export const GRID_ORPHAN_PAGE = {
    identifier: 'id-page',
    inode: 'inode-page',
    title: 'Home',
    contentType: 'htmlpageasset',
    baseType: 'HTMLPAGE',
    languageId: 1,
    live: true,
    working: true,
    archived: false,
    hasLiveVersion: true,
    locked: false,
    host: 'host-1'
} as unknown as DotCMSContentlet;

/** A folder owned by Daniel. */
export const GRID_FOLDER = {
    type: 'folder',
    identifier: 'id-folder',
    inode: 'inode-folder',
    name: 'activities',
    title: 'Activities',
    path: '/activities/',
    owner: 'Daniel Ortiz',
    permissions: []
} as unknown as DotContentDriveFolder;

/** One page as the store holds it: folders first, then content. */
export const GRID_ITEMS = [GRID_FOLDER, GRID_IMAGE, GRID_LOCKED_BLOG, GRID_ORPHAN_PAGE];

/** The messages the grid and card read, with their English wording. */
export const GRID_MESSAGES: Record<string, string> = {
    Folder: 'Folder',
    Published: 'Published',
    Draft: 'Draft',
    Page: 'Page',
    'content-drive.list-view.edited-by.unknown': 'Unknown',
    'content-drive.list-view.row-busy': 'An action is running on this item…',
    'content-drive.grid-view.label': 'Content',
    'content-drive.grid-view.edited-by': 'Edited by: {0}',
    'content-drive.grid-view.locked-by': 'Locked by: {0}',
    'content-drive.grid-view.locked': 'Locked',
    'content-drive.grid-view.unlocked': 'Unlocked',
    'content-drive.grid-view.more-actions': 'More actions',
    'content.drive.empty.state.title': 'This folder is empty',
    'content.drive.empty.state.description': 'Nothing here yet'
};
