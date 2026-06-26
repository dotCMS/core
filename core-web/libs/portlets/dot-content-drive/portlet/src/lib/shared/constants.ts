import { DotCMSBaseTypesContentTypes, DotSite } from '@dotcms/dotcms-models';

import { DotContentDrivePage, DotContentDrivePagination, DotContentDriveSortOrder } from './models';

export const HIDE_MESSAGE_BANNER_LOCALSTORAGE_KEY = 'content-drive-hide-message-banner';

// We only need the host and the identifier from this, the other properties are mostly to comply with SiteEntity interface
export const SYSTEM_HOST: DotSite = {
    aliases: '',
    archived: false,
    hostname: 'SYSTEM_HOST',
    identifier: 'SYSTEM_HOST'
};

// Default pagination
export const DEFAULT_PAGINATION: DotContentDrivePagination = {
    limit: 20,
    page: 1,
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

export const DEFAULT_PAGE: DotContentDrivePage = {
    hasMoreContent: true,
    hasMoreFolders: true,
    folderCursor: 0,
    contentCursor: 0,
    offset: 0
};

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

/**
 * Inverse of `MAP_NUMBERS_TO_BASE_TYPES` — base type variable → numeric key.
 * Avoids `Object.entries(...).find(...)` linear scans when persisting filters.
 *
 * Typed as `Partial<...>` because the map only covers the 9 entries above; if
 * `DotCMSBaseTypesContentTypes` ever gains a new variant, callers will need
 * to handle a possible `undefined` (the existing `.filter(Boolean)` guards do).
 */
export const MAP_BASE_TYPES_TO_NUMBERS: Partial<Record<DotCMSBaseTypesContentTypes, string>> =
    Object.fromEntries(
        Object.entries(MAP_NUMBERS_TO_BASE_TYPES).map(([key, value]) => [value, key])
    );

// Debounce time for requests
export const DEBOUNCE_TIME = 500;

export const PANEL_SCROLL_HEIGHT = '25rem';

// Dialog type
export const DIALOG_TYPE = {
    FOLDER: 'FOLDER',
    CONTENT_TYPE_SELECTOR: 'CONTENT_TYPE_SELECTOR',
    UPLOAD_SELECTOR: 'UPLOAD_SELECTOR'
} as const;

export const DEFAULT_FILE_ASSET_TYPES = [{ id: 'FileAsset', name: 'File' }];

/**
 * Options shown in the upload-type selector dialog. `baseType` is the base type fired to the
 * upload endpoint, which the backend resolves to the matching content type: `DOTASSET` for Assets,
 * `FILEASSET` for Files.
 */
export const UPLOAD_SELECTOR_OPTIONS = [
    {
        baseType: DotCMSBaseTypesContentTypes.DOTASSET,
        icon: 'image',
        labelKey: 'content-drive.dialog.upload-selector.asset',
        descriptionKey: 'content-drive.dialog.upload-selector.asset.description',
        recommended: true
    },
    {
        baseType: DotCMSBaseTypesContentTypes.FILEASSET,
        icon: 'code_blocks',
        labelKey: 'content-drive.dialog.upload-selector.file',
        descriptionKey: 'content-drive.dialog.upload-selector.file.description',
        recommended: false
    }
] as const;

export const SUGGESTED_ALLOWED_FILE_EXTENSIONS = [
    '*.jpg',
    '*.jpeg',
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
export const MOVE_TO_FOLDER_WORKFLOW_ACTION_ID = 'dd4c4b7c-e9d3-4dc0-8fbf-36102f9c6324';

// Dropzone state
export const DROPZONE_STATE = {
    INTERNAL_DRAG: 'internal-drag',
    ACTIVE: 'active',
    INACTIVE: 'inactive'
} as const;
