import { DotCMSBaseTypesContentTypes, SiteEntity } from '@dotcms/dotcms-models';

import { DotContentDrivePagination, DotContentDriveSortOrder } from './models';

export const HIDE_MESSAGE_BANNER_LOCALSTORAGE_KEY = 'content-drive-hide-message-banner';

// We only need the host and the identifier from this, the other properties are mostly to comply with SiteEntity interface
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

// Default pagination
export const DEFAULT_PAGINATION: DotContentDrivePagination = {
    limit: 20,
    offset: 0
};

export const DEFAULT_SORT = {
    field: 'modDate',
    order: DotContentDriveSortOrder.DESC
};

// Sort order from PrimeNG to dotCMS
export const SORT_ORDER = {
    1: DotContentDriveSortOrder.ASC,
    '-1': DotContentDriveSortOrder.DESC
};

// Default tree expanded
export const DEFAULT_TREE_EXPANDED = true;

// Default path, it needs to be undefined to show the root folder
export const DEFAULT_PATH = undefined;

// Map numbers to base types, ticket: https://github.com/dotCMS/core/issues/32991
export const MAP_NUMBERS_TO_BASE_TYPES = {
    1: DotCMSBaseTypesContentTypes.CONTENT,
    2: DotCMSBaseTypesContentTypes.WIDGET,
    3: DotCMSBaseTypesContentTypes.FORM,
    4: DotCMSBaseTypesContentTypes.FILEASSET,
    5: DotCMSBaseTypesContentTypes.HTMLPAGE,
    6: DotCMSBaseTypesContentTypes.PERSONA,
    7: DotCMSBaseTypesContentTypes.VANITY_URL,
    8: DotCMSBaseTypesContentTypes.KEY_VALUE,
    9: DotCMSBaseTypesContentTypes.DOTASSET
};

// Debounce time for requests
export const DEBOUNCE_TIME = 500;

export const PANEL_SCROLL_HEIGHT = '25rem';

// Dialog type
export const DIALOG_TYPE = {
    FOLDER: 'FOLDER'
} as const;

export const DEFAULT_FILE_ASSET_TYPES = [{ id: 'FileAsset', name: 'File' }];

export const SUGGESTED_ALLOWED_FILE_EXTENSIONS = [
    '*.jpg',
    '*.jpeg',
    '*.png',
    '*.gif',
    '*.png',
    '*.csv',
    '*.xls',
    '*.xlsx',
    '*.pdf',
    '*.doc',
    '*.docx',
    '*.txt',
    '*.zip',
    '*.rar',
    '*.tar',
    '*.gz'
];

export const SUCCESS_MESSAGE_LIFE = 4500;
export const WARNING_MESSAGE_LIFE = 4200;
export const ERROR_MESSAGE_LIFE = 4500;

// Dropzone state
export const DROPZONE_STATE = {
    INTERNAL_DRAG: 'internal-drag',
    ACTIVE: 'active',
    INACTIVE: 'inactive'
} as const;
