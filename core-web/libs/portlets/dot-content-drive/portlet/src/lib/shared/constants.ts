import { DotCMSBaseTypesContentTypes, DotSite } from '@dotcms/dotcms-models';

import { DotContentDrivePage, DotContentDrivePagination, DotContentDriveSortOrder } from './models';

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

/**
 * Prefix that marks a filter-bag key as a per-field "user searchable" criterion, e.g. `us.title`.
 * Keeping these entries in the flat `filters` bag lets them ride the existing URL encode/decode and
 * be cleared alongside every other filter. The prefix avoids colliding with known filter keys or a
 * field whose variable happens to be `title`, `workflow`, etc.
 */
export const USER_SEARCHABLE_PREFIX = 'us.';

/**
 * Content-type field types offered as Content Drive field filters (phase 1 — simple fields only).
 * The string values match the backend field-type contract (edit-content `FIELD_TYPES`). Grouped by
 * the control rendered and the value shape stored/sent:
 * - text  → single string (contains)
 * - single-select (Select/Radio) → single string (equals), options from `field.values`
 * - multi-select (Multi-Select/Checkbox) → string[], options from `field.values`
 * - date  → `{ from, to }` ISO range
 */
export const FIELD_FILTER_TEXT_TYPES = ['Text', 'Textarea', 'WYSIWYG'] as const;
/** Singular field-type names, matched to their native widget in the filter chip. */
export const FIELD_FILTER_SELECT_TYPE = 'Select';
export const FIELD_FILTER_RADIO_TYPE = 'Radio';
export const FIELD_FILTER_MULTISELECT_TYPE = 'Multi-Select';
export const FIELD_FILTER_CHECKBOX_TYPE = 'Checkbox';
/** Single-value option fields (stored as one string). */
export const FIELD_FILTER_SINGLE_SELECT_TYPES = [
    FIELD_FILTER_SELECT_TYPE,
    FIELD_FILTER_RADIO_TYPE
] as const;
/** Multi-value option fields (stored as a comma-joined list). */
export const FIELD_FILTER_MULTI_SELECT_TYPES = [
    FIELD_FILTER_MULTISELECT_TYPE,
    FIELD_FILTER_CHECKBOX_TYPE
] as const;
/** Complex field types (own picker + fetched options), added in phase 2. */
export const FIELD_FILTER_TAG_TYPE = 'Tag';
export const FIELD_FILTER_CATEGORY_TYPE = 'Category';
export const FIELD_FILTER_RELATIONSHIP_TYPE = 'Relationship';
/**
 * Every field type whose value is a list stored comma-joined (multi-select, checkbox, tag,
 * category). Relationship is intentionally excluded — the backend only supports a single related
 * value, so it's stored as one identifier string.
 */
export const FIELD_FILTER_MULTI_VALUE_TYPES: readonly string[] = [
    ...FIELD_FILTER_MULTI_SELECT_TYPES,
    FIELD_FILTER_TAG_TYPE,
    FIELD_FILTER_CATEGORY_TYPE
];
export const FIELD_FILTER_DATE_TYPES = ['Date', 'Date-and-Time', 'Time'] as const;
/** Date field type showing time; `Time` is time-only, `Date-and-Time` shows date + time. */
export const FIELD_FILTER_TIME_ONLY_TYPE = 'Time';
export const FIELD_FILTER_DATE_TIME_TYPE = 'Date-and-Time';

/** Every field type eligible to become a filter (excludes Host-Folder + out-of-scope types). */
export const USER_SEARCHABLE_FIELD_TYPES: readonly string[] = [
    ...FIELD_FILTER_TEXT_TYPES,
    ...FIELD_FILTER_SINGLE_SELECT_TYPES,
    ...FIELD_FILTER_MULTI_SELECT_TYPES,
    ...FIELD_FILTER_DATE_TYPES,
    FIELD_FILTER_TAG_TYPE,
    FIELD_FILTER_CATEGORY_TYPE,
    FIELD_FILTER_RELATIONSHIP_TYPE
];

/**
 * Field variable of the content type's title field. It's already covered by the toolbar's keyword
 * search (which queries the contentlet title), so it's not offered as a redundant field filter.
 */
export const TITLE_FIELD_VARIABLE = 'title';

/** Separator joining multi-select values and date-range `from,to` in the flat filter string. */
export const USER_SEARCHABLE_VALUE_SEPARATOR = ',';

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
