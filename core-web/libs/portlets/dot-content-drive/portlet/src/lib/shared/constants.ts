import { SiteEntity } from '@dotcms/dotcms-models';

import { DotContentDrivePagination, DotContentDriveSortOrder } from './models';

// We only need the host from this, the other properties are mostly to comply with SiteEntity interface
export const SYSTEM_HOST: SiteEntity = {
    aliases: '',
    archived: false,
    categoryId: '',
    contentTypeId: '',
    default: false,
    dotAsset: false,
    fileAsset: false,
    folder: '/',
    form: false,
    host: 'SYSTEM_HOST',
    hostThumbnail: null,
    hostname: 'SYSTEM_HOST',
    htmlpage: false,
    identifier: 'SYSTEM_HOST',
    indexPolicyDependencies: '',
    inode: 'SYSTEM_HOST',
    keyValue: false,
    languageId: 1,
    languageVariable: false,
    live: true,
    locked: false,
    lowIndexPriority: false,
    modDate: 0,
    modUser: '',
    name: 'System Host',
    new: false,
    owner: '',
    parent: false,
    permissionId: '',
    permissionType: 'INDIVIDUAL',
    persona: false,
    sortOrder: 0,
    structureInode: '',
    systemHost: true,
    tagStorage: 'SCHEMA',
    title: 'System Host',
    titleImage: null,
    type: 'HOST',
    vanityUrl: false,
    variantId: '',
    versionId: '',
    working: true
};

// We want to exclude forms and Hosts, and only show contentlets that are not deleted
export const BASE_QUERY = '+systemType:false -contentType:forms -contentType:Host +deleted:false';

export const DEFAULT_PAGINATION: DotContentDrivePagination = {
    limit: 20,
    offset: 0
};

export const SORT_ORDER = {
    1: DotContentDriveSortOrder.ASC,
    '-1': DotContentDriveSortOrder.DESC
};

export const DEFAULT_TREE_EXPANDED = true;

export const DEFAULT_PATH = '/';
