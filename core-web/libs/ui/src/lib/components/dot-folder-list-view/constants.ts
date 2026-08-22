import { DotFolderListViewColumn, DotFolderListViewColumnField } from './models';

export type DotFolderListViewFixedColumn = DotFolderListViewColumn & {
    field: DotFolderListViewColumnField;
};

/**
 * Column widths, in the order the header renders them.
 *
 * `title` carries a width on purpose, and 28% rather than a larger share so the sized columns total
 * 96% — the checkbox column below stays inside the table, which is the invariant the width was
 * originally dropped to protect.
 *
 * Sizing it at all is the point: Leaving it unsized makes it the leftover column, and with
 * `table-layout: fixed` the sized columns plus the `3rem` selection column plus the Show-In-List
 * extras are all satisfied first — so two or three extras drive the leftover to zero and the title
 * collapses under its own status badge, headers overlapping. Measured at a 1200px container: unsized
 * gives 316px with no extras but 50px at two and 0px at three, while 32% holds ~355px throughout.
 *
 * `min-width` is not an alternative: `table-layout: fixed` sizes columns from the first row's `width`
 * values and ignores a cell's min-width, so a floor there has no effect (measured — identical results
 * with and without).
 *
 * The percentages summing to 100% alongside the out-of-budget `3rem` selection column does NOT
 * overflow, which is the reason the width was previously dropped: the browser scales the
 * over-specified widths instead. Measured as 0px of overflow with no extras.
 */
const FIXED_COLUMNS: DotFolderListViewFixedColumn[] = [
    { field: 'title', header: 'name', width: '28%', order: 1, sortable: true },
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
 * The total the authored percentages add up to, and therefore the share a rendered subset should
 * still fill. Derived from the columns rather than written down so the two cannot drift apart.
 */
const PERCENTAGE_WIDTH_BUDGET = FIXED_COLUMNS.reduce(
    (total, column) => total + (column.width?.endsWith('%') ? parseFloat(column.width) : 0),
    0
);

/**
 * Rescales a subset's percentage widths so they still fill {@link PERCENTAGE_WIDTH_BUDGET}.
 *
 * Necessary because `table-layout: fixed` has to put unclaimed width *somewhere*, and Chrome gives
 * it to the leading column — which here is the `3rem` checkbox one. Measured in the Action Center
 * preview, which renders `title, live, contentType` in a 586px table: the authored 28/10/15 total
 * 53%, and the 47% left over went to the checkbox column, rendering it 275px instead of 48. That
 * pushed the name far right of its heading and squeezed Status to 59px, so its badge overflowed into
 * Type. Rescaling put the checkbox column back to 42px and Status to 103px.
 *
 * Proportions between the columns shown are preserved, so a subset reads like the full table does.
 * Non-percentage widths are passed through: only the percentage columns share this budget.
 */
export const rescaleToWidthBudget = (
    columns: DotFolderListViewFixedColumn[]
): DotFolderListViewFixedColumn[] => {
    const total = columns.reduce(
        (sum, column) => sum + (column.width?.endsWith('%') ? parseFloat(column.width) : 0),
        0
    );

    if (!total || total === PERCENTAGE_WIDTH_BUDGET) {
        return columns;
    }

    const factor = PERCENTAGE_WIDTH_BUDGET / total;

    return columns.map((column) =>
        column.width?.endsWith('%')
            ? { ...column, width: `${(parseFloat(column.width) * factor).toFixed(2)}%` }
            : column
    );
};

/**
 * MIME type used to mark internal Content Drive / AssetPicker row drags.
 */
export const DOT_DRAG_ITEM = 'dotcms/item';
