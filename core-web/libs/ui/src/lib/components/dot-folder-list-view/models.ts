/**
 * Generic display types for a column's cell value. Kept agnostic of any domain field system so the
 * table can format and size values (dates, booleans, numbers) without knowing where the column came
 * from. Callers map their own field/data types onto these.
 */
export const DOT_FOLDER_LIST_VIEW_COLUMN_TYPE = {
    TEXT: 'text',
    NUMBER: 'number',
    BOOLEAN: 'boolean',
    DATE: 'date',
    DATETIME: 'datetime',
    TIME: 'time',
    /** Image/binary/file field: renders the field's own asset as a thumbnail. */
    IMAGE: 'image'
} as const;

export type DotFolderListViewColumnType =
    (typeof DOT_FOLDER_LIST_VIEW_COLUMN_TYPE)[keyof typeof DOT_FOLDER_LIST_VIEW_COLUMN_TYPE];

/**
 * Column configuration for the folder list view.
 */
export interface DotFolderListViewColumn {
    field: string;
    header: string;
    /**
     * Explicit width (any CSS length). Optional: when omitted the column takes whatever the sized
     * ones leave over. Caller-provided extra columns are sized per type when they omit it; among the
     * fixed columns, `title` omits it on purpose so it can absorb the remainder.
     */
    width?: string;
    sortable?: boolean;
    order: number;
    /** How the cell value is rendered and sized. Defaults to `text` when omitted. */
    type?: DotFolderListViewColumnType;
}

/**
 * The table's fixed columns, by field. A closed set so `visibleColumns` and the body's per-cell
 * checks are compiler-checked against `HEADER_COLUMNS`.
 */
export type DotFolderListViewColumnField =
    | 'title'
    | 'live'
    | 'languageId'
    | 'contentType'
    | 'modUser'
    | 'modDate'
    | 'actions';

/** Selection mode for {@link DotFolderListViewComponent}. */
export type DotFolderListViewSelectionMode = 'single' | 'multiple';
