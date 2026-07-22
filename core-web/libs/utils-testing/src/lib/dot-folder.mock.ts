import { faker } from '@faker-js/faker';

import { DotFolder, FolderSearchView } from '@dotcms/dotcms-models';

export function createFakeFolder(overrides: Partial<DotFolder> = {}): DotFolder {
    return {
        id: faker.string.uuid(),
        hostName: faker.internet.domainName(),
        path: faker.string.alphanumeric(10),
        addChildrenAllowed: faker.datatype.boolean(),
        ...overrides
    };
}

/**
 * Create a fake `FolderSearchView` as returned by `GET /api/v1/folder/search`.
 * Note: `path` is the folder's parent path, `name` is the folder's own name.
 */
export function createFakeFolderSearchView(
    overrides: Partial<FolderSearchView> = {}
): FolderSearchView {
    return {
        id: faker.string.uuid(),
        inode: faker.string.uuid(),
        name: faker.string.alphanumeric(8),
        path: '/',
        addChildrenAllowed: faker.datatype.boolean(),
        hasChildren: faker.datatype.boolean(),
        ...overrides
    };
}
