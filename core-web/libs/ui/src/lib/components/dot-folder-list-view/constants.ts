import { DotFolderListViewColumn, DotFolderListViewColumnField } from './models';

export type DotFolderListViewFixedColumn = DotFolderListViewColumn & {
    field: DotFolderListViewColumnField;
};

/**
 * Column widths, in the order the header renders them.
 *
 * `title` deliberately has NO width: the table also renders a fixed `3rem` selection column that is
 * outside this budget, so a set of percentages adding up to 100% makes the `table-layout: fixed`
 * table `3rem` wider than its container — a horizontal scrollbar that scrolls nothing. Leaving
 * `title` unsized lets it absorb whatever is left after the selection column and the sized ones,
 * whichever subset of these is actually rendered.
 */
const FIXED_COLUMNS: DotFolderListViewFixedColumn[] = [
    { field: 'title', header: 'name', order: 1, sortable: true },
    { field: 'live', header: 'status', width: '10%', order: 2 },
    { field: 'languageId', header: 'locale', width: '10%', order: 3, sortable: true },
    { field: 'contentType', header: 'type', sortable: true, width: '15%', order: 4 },
    { field: 'modUser', header: 'Edited-By', width: '15%', order: 5, sortable: true },
    { field: 'modDate', header: 'Last-Edited', sortable: true, width: '13%', order: 6 },
    { field: 'actions', header: '', width: '5%', order: 7 }
];

export const HEADER_COLUMNS: DotFolderListViewFixedColumn[] = [...FIXED_COLUMNS].sort(
    (a, b) => a.order - b.order
);

/**
 * MIME type used to mark internal Content Drive / AssetPicker row drags.
 */
export const DOT_DRAG_ITEM = 'dotcms/item';
