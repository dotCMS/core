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
     * Explicit width (any CSS length). Optional for caller-provided extra columns: when omitted the
     * table sizes the column itself — by content for text/number, by a sensible default per type
     * otherwise. Fixed columns set it explicitly.
     */
    width?: string;
    sortable?: boolean;
    order: number;
    /** How the cell value is rendered and sized. Defaults to `text` when omitted. */
    type?: DotFolderListViewColumnType;
}

/** Selection mode for {@link DotFolderListViewComponent}. */
export type DotFolderListViewSelectionMode = 'single' | 'multiple';
