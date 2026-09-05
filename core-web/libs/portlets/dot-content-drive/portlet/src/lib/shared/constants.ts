import {
    DOT_FOLDER_TREE_PAGE_SIZE,
    DotCMSBaseTypesContentTypes,
    DotSite
} from '@dotcms/dotcms-models';
import { SYSTEM_HOST_ID } from '@dotcms/ui';

import { DotContentDrivePage, DotContentDrivePagination, DotContentDriveSortOrder } from './models';

// We only need the host and the identifier from this, the other properties are mostly to comply with SiteEntity interface
export const SYSTEM_HOST: DotSite = {
    aliases: '',
    archived: false,
    hostname: SYSTEM_HOST_ID,
    identifier: SYSTEM_HOST_ID
};

// Default pagination
export const DEFAULT_PAGINATION: DotContentDrivePagination = {
    limit: 20,
    page: 1,
    offset: 0
};

/**
 * Page size for interactive folder-tree expand and load-more.
 * Re-exports the shared limit used by Host Folder Field so both stay in sync.
 */
export const FOLDER_TREE_PAGE_SIZE = DOT_FOLDER_TREE_PAGE_SIZE;

/**
 * Page size for the deep-link / initial hierarchy fetch only. One request per ancestor level
 * (parallel). Expand and load-more keep using {@link FOLDER_TREE_PAGE_SIZE}.
 *
 * Pinned to the backend's cap for `includePermissions=true`
 * (`content.drive.folder.search.permissions.max.per.page`, default 200): anything larger is
 * rejected with a 400, and the hierarchy load must carry permissions so every node the tree
 * renders on first paint can gate its context menu without a second round-trip.
 *
 * An ancestor sorting past this page is fetched individually instead of by widening the page,
 * see `getFolderHierarchyByPath`.
 *
 * Deliberately a whole multiple of {@link FOLDER_TREE_PAGE_SIZE}: load-more resumes in
 * 40-sized pages, so the hierarchy's page count has to convert to a clean page boundary.
 */
export const FOLDER_TREE_HIERARCHY_PAGE_SIZE = 200;

/** Minimum length the folder-search `name` filter accepts; shorter values are rejected with a 400. */
export const FOLDER_NAME_FILTER_MIN_LENGTH = 2;

export const DEFAULT_SORT = {
    field: 'modDate',
    order: DotContentDriveSortOrder.DESC
};

// Sort order from PrimeNG to dotCMS
export const SORT_ORDER: Record<number, DotContentDriveSortOrder> = {
    1: DotContentDriveSortOrder.ASC,
    [-1]: DotContentDriveSortOrder.DESC
};

// Default tree expanded
export const DEFAULT_TREE_EXPANDED = true;

// Default path, it needs to be undefined to show the root folder
export const DEFAULT_PATH = undefined;

/**
 * The site root as a browsable path.
 *
 * Distinct from {@link DEFAULT_PATH}: that is the *absence* of a path in the URL, while this is an
 * explicit "go to the root" the drive can be sent to — used when the folder being browsed stops
 * existing.
 */
export const ROOT_PATH = '/';

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

// Also re-exported: the shared chips write these keys, and the portlet's URL layer reads them.
export { USER_SEARCHABLE_PREFIX } from '@dotcms/ui';

// The "Show Shared Assets" key, which drives `includeSystemHost` on the search request. Living in
// the filter bag rather than its own query param is what gives it the URL encode/decode, the
// back/forward guard and the legacy-editor `CD_` round-trip for free, and it is always seeded so
// the applied state is visible in the URL rather than implied by an absent key.
//
// Re-exported rather than redefined: the chip that writes it now lives in `@dotcms/ui` and is
// shared with the AssetPicker, so one definition serves both. Kept exported from here so the
// portlet's existing importers — the URL encode/decode pair, the back/forward guard, the legacy
// `CD_` round-trip and `withFilterDefaults` — are untouched.
export {
    SHARED_ASSETS_DISABLED_VALUE,
    SHARED_ASSETS_ENABLED_VALUE,
    SHARED_ASSETS_FILTER_KEY
} from '@dotcms/ui';

// Re-exported rather than redefined: the field-filter chips that read these now live in
// `@dotcms/ui` and are shared with the AssetPicker, so one definition serves both. Kept exported
// from here so the portlet's own importers — the URL encode/decode pair and the store's request
// builder — are untouched.
export {
    FIELD_FILTER_BINARY_TYPE,
    FIELD_FILTER_CATEGORY_TYPE,
    FIELD_FILTER_CHECKBOX_TYPE,
    FIELD_FILTER_CUSTOM_TYPE,
    FIELD_FILTER_DATE_TIME_TYPE,
    FIELD_FILTER_DATE_TYPES,
    FIELD_FILTER_JSON_TYPE,
    FIELD_FILTER_KEY_VALUE_TYPE,
    FIELD_FILTER_MULTI_SELECT_TYPES,
    FIELD_FILTER_MULTI_VALUE_TYPES,
    FIELD_FILTER_MULTISELECT_TYPE,
    FIELD_FILTER_RADIO_TYPE,
    FIELD_FILTER_RELATIONSHIP_TYPE,
    FIELD_FILTER_SELECT_TYPE,
    FIELD_FILTER_SINGLE_SELECT_TYPES,
    FIELD_FILTER_STORY_BLOCK_TYPE,
    FIELD_FILTER_TEXT_FALLBACK_TYPES,
    FIELD_FILTER_TEXT_TYPES,
    FIELD_FILTER_TIME_ONLY_TYPE,
    TITLE_FIELD_VARIABLE,
    USER_SEARCHABLE_FIELD_TYPES,
    USER_SEARCHABLE_VALUE_SEPARATOR
} from '@dotcms/ui';

// Re-exported rather than redefined: the Status chip moved to `@dotcms/ui` and is shared with the
// AssetPicker, and the portlet's URL decode layer sanitizes against the same three values.
export { CONTENT_STATUS, STATUS_FILTER_KEY, STATUS_FILTER_OPTIONS } from '@dotcms/ui';

export const PANEL_SCROLL_HEIGHT = '25rem';

// Dialog type
export const DIALOG_TYPE = {
    FOLDER: 'FOLDER',
    CONTENT_TYPE_SELECTOR: 'CONTENT_TYPE_SELECTOR',
    ACTION_CENTER: 'ACTION_CENTER'
} as const;

/**
 * Root styles for the Action Center dialog.
 *
 * Fixed height so the content box has something to flex against — without it the column sizes to
 * content and the body never scrolls. `display: flex` / `flex-direction: column` / `overflow: hidden`
 * are required: the theme gives `.p-dialog-content` `flex-grow: 1` and the header/footer
 * `flex-shrink: 0`, but `.p-dialog` itself is not a flex container — without that, `height: 80vh`
 * does not constrain the content and the whole dialog (footer included) grows past the viewport.
 */
export const ACTION_CENTER_DIALOG_STYLE = {
    width: '42rem',
    maxWidth: '92vw',
    height: '80vh',
    maxHeight: '80vh',
    display: 'flex',
    'flex-direction': 'column',
    overflow: 'hidden'
} as const;

/**
 * Content-box styles for the Action Center dialog — the dialog's only scroll container.
 *
 * Applied as inline styles via `[contentStyle]` rather than utility classes: the theme sets
 * `overflow-y: auto` on `.p-dialog-content` and its runtime-injected CSS outranks a Tailwind
 * `overflow-hidden`. Inline styles win without needing `!` overrides. `min-height: 0` lets the box
 * shrink inside the flex column; without it a flex item refuses to go below its content height.
 */
export const ACTION_CENTER_DIALOG_CONTENT_STYLE = {
    display: 'flex',
    'flex-direction': 'column',
    flex: '1',
    'min-height': '0',
    overflow: 'hidden',
    padding: '0'
} as const;

/**
 * Pass-through styling for the Action Center's "these folders can only be bundled" notice, which
 * spans the dialog edge to edge instead of sitting inset like the sections around it.
 *
 * `-mx-6` cancels the dialog body's `px-6`. Because that inset is *padding*, the notice grows into
 * the container's padding box rather than past its border box, so the body's `overflow-y-auto`
 * does not turn into a horizontal scrollbar. The dialog's own content box is `padding: 0` (see
 * {@link ACTION_CENTER_DIALOG_CONTENT_STYLE}), so `px-6` is the only inset to cancel.
 *
 * Both `!` flags are required rather than defensive. `.p-message` sets `border-radius` and
 * `.p-message-content` sets a `padding` shorthand; PrimeNG injects that stylesheet at runtime, so
 * at equal specificity it lands after Tailwind's and wins.
 *
 * The content keeps 24px of its own horizontal padding so the text stays on the same left edge as
 * the dialog header and the sections below it.
 */
export const ACTION_CENTER_FOLDER_NOTICE_PT = {
    root: { class: '-mx-6 rounded-none!' },
    content: { class: 'px-6!' }
} as const;

export const DEFAULT_FILE_ASSET_TYPES = [{ id: 'FileAsset', name: 'File' }];

/**
 * Options for the folder settings "Upload Behavior" radio group. `value` is persisted to the
 * folder's `defaultBaseType`: `null` means "ask each time" (the upload menu is shown on every
 * upload), `DOTASSET`/`FILEASSET` force every upload to that base type. The backend routes the
 * concrete content type by file type.
 */
export const FOLDER_UPLOAD_BEHAVIOR_OPTIONS: {
    value: DotCMSBaseTypesContentTypes | null;
    labelKey: string;
    descriptionKey: string;
}[] = [
    {
        value: null,
        labelKey: 'content-drive.dialog.folder.upload-behavior.ask-each-time',
        descriptionKey: 'content-drive.dialog.folder.upload-behavior.ask-each-time.description'
    },
    {
        value: DotCMSBaseTypesContentTypes.DOTASSET,
        labelKey: 'content-drive.dialog.folder.upload-behavior.always-assets',
        descriptionKey: 'content-drive.dialog.folder.upload-behavior.always-assets.description'
    },
    {
        value: DotCMSBaseTypesContentTypes.FILEASSET,
        labelKey: 'content-drive.dialog.folder.upload-behavior.always-files',
        descriptionKey: 'content-drive.dialog.folder.upload-behavior.always-files.description'
    }
];

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

/**
 * `editContent` value written for a `new`-mode panel: a non-shareable marker (creating has no
 * identifier) whose only job is to give browser Back a history entry to pop, so Back closes the
 * create panel too (AC8). The deep-link reader ignores it; only real identifiers are resolved.
 */
export const NEW_CONTENT_MARKER = 'new';
