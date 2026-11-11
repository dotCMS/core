import { DotContentDriveBrowseItem, DotLanguage } from '@dotcms/dotcms-models';

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

/** How {@link DotFolderListViewComponent} draws the listing: rows in a table, or a grid of cards. */
export const DOT_FOLDER_LIST_VIEW_LAYOUT = {
    TABLE: 'table',
    GRID: 'grid'
} as const;

export type DotFolderListViewLayout =
    (typeof DOT_FOLDER_LIST_VIEW_LAYOUT)[keyof typeof DOT_FOLDER_LIST_VIEW_LAYOUT];

/**
 * What the grid layout hands the caller's card template (`<ng-template #card>`).
 *
 * The listing owns everything around the card: selection and ranges, the keyboard, the tab stop,
 * drag and drop, paging, the skeleton and the empty state. It also draws the element each card sits
 * in, which carries the option role, the tab stop, `aria-selected` and the drag handlers. The
 * template draws only the card's body and reports the three gestures that start inside it.
 *
 * Two conventions tie the body to the element around it:
 * - The element is labelled by `{id}-label` and described by `{id}-description`, so the body gives
 *   its title the first id and a summary of everything else the second.
 * - The drag picture is copied from the body's element marked `data-drag-preview`.
 */
export interface DotFolderListViewCardContext {
    /** The item the card shows. */
    $implicit: DotContentDriveBrowseItem;
    /** Unique to this card on the page; the prefix of the two ids above. */
    id: string;
    /** Whether the item is selected. */
    selected: boolean;
    /** Whether an operation is running on the item: it cannot be selected, opened or dragged. */
    busy: boolean;
    /** Whether dragged items would be dropped into this item. */
    dragOver: boolean;
    /** Whether the card offers no actions: no ⋮ button. */
    readOnly: boolean;
    /** Languages by id, for the item's locale tag. */
    languagesMap: Map<number, DotLanguage>;
    /** Call from a click on the card's checkbox: adds or removes the item, Shift selects a range. */
    toggle: (event: MouseEvent) => void;
    /** Call from a plain click on the card: opens the item. */
    open: (event: MouseEvent) => void;
    /** Call from a click on the card's ⋮ button: opens the item's actions menu. */
    menu: (event: MouseEvent) => void;
}
