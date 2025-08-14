import { SiteEntity } from '@dotcms/dotcms-models';

/**
 * Mock SiteEntity for testing purposes
 */
export const mockSiteEntity: SiteEntity = {
    aliases: '',
    archived: false,
    categoryId: 'cat123',
    contentTypeId: 'content123',
    default: true,
    dotAsset: false,
    fileAsset: false,
    folder: '/folder',
    form: false,
    host: 'localhost',
    hostThumbnail: null,
    hostname: 'demo.dotcms.com',
    htmlpage: false,
    identifier: 'site-123',
    indexPolicyDependencies: '',
    inode: 'inode-123',
    keyValue: false,
    languageId: 1,
    languageVariable: false,
    live: true,
    locked: false,
    lowIndexPriority: false,
    modDate: 1234567890,
    modUser: 'user123',
    name: 'Demo Site',
    new: false,
    owner: 'owner123',
    parent: false,
    permissionId: 'perm123',
    permissionType: 'INDIVIDUAL',
    persona: false,
    sortOrder: 0,
    structureInode: 'struct123',
    systemHost: false,
    tagStorage: 'SCHEMA',
    title: 'Demo Site',
    titleImage: null,
    type: 'host',
    vanityUrl: false,
    variantId: 'variant123',
    versionId: 'version123',
    working: true
};

/**
 * Mock user data for testing purposes
 */
export const mockUserData = {
    name: 'John Doe',
    email: 'john@example.com'
};

/**
 * Alternative mock user data for testing purposes
 */
export const mockUserDataAlt = {
    name: 'Jane Smith',
    email: 'jane@example.com'
};
