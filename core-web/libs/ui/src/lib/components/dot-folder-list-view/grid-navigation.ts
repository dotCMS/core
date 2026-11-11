/**
 * Keyboard maths for the folder list view's grid layout (#37930, US7). Pure, so the rules can be
 * tested without a layout engine: the grid measures the page and hands the numbers in.
 */

/** Keys that move the grid's tab stop. Everything else is left alone. */
export const GRID_MOVE_KEYS = new Set([
    'ArrowLeft',
    'ArrowRight',
    'ArrowUp',
    'ArrowDown',
    'Home',
    'End'
]);

/**
 * How many cards sit in one row, read from where the cards were laid out.
 *
 * Measured rather than computed from widths so it follows whatever the CSS grid decided, including
 * after a resize or the folder tree opening or closing.
 *
 * @param tops Each card's distance from the top of the grid, in reading order.
 * @returns The number of cards in the first row, never less than one.
 */
export function columnCountFromTops(tops: number[]): number {
    if (!tops.length) {
        return 1;
    }

    const firstRowTop = tops[0];
    const firstRowLength = tops.findIndex((top) => top !== firstRowTop);

    return firstRowLength === -1 ? tops.length : firstRowLength;
}

/**
 * Where the tab stop goes for a key press.
 *
 * Left and Right move one card in reading order, wrapping onto the row before or after. Up and Down
 * move a whole row; when the row below is shorter, Down lands on its last card. Home and End go to
 * the first and last card. Nothing leaves the page: at an edge the tab stop stays where it is.
 * Unselectable cards are passed over, as the table passes over its busy rows.
 *
 * @param options.index The card that has the tab stop.
 * @param options.key The `KeyboardEvent.key` pressed.
 * @param options.columns Cards per row.
 * @param options.count Cards on the page.
 * @param options.isSkipped Whether a card cannot take the tab stop.
 * @returns The card that should have the tab stop; `index` when it should not move.
 */
export function nextGridIndex(options: {
    index: number;
    key: string;
    columns: number;
    count: number;
    isSkipped?: (index: number) => boolean;
}): number {
    const { index, key, count } = options;
    const columns = Math.max(1, options.columns);
    const isSkipped = options.isSkipped ?? (() => false);
    const last = count - 1;

    /** The first card from `start` walking by `step` that can take the tab stop. */
    const firstUsable = (start: number, step: number): number => {
        for (let candidate = start; candidate >= 0 && candidate <= last; candidate += step) {
            if (!isSkipped(candidate)) {
                return candidate;
            }
        }

        return index;
    };

    switch (key) {
        case 'ArrowRight':
            return firstUsable(index + 1, 1);
        case 'ArrowLeft':
            return firstUsable(index - 1, -1);
        case 'Home':
            return firstUsable(0, 1);
        case 'End':
            return firstUsable(last, -1);
        case 'ArrowUp': {
            const target = index - columns;

            return target < 0 ? index : firstUsable(target, -1);
        }
        case 'ArrowDown': {
            const target = index + columns;

            if (target <= last) {
                return firstUsable(target, 1);
            }

            // The row below is shorter than this one. Its last card is the closest one down, as long
            // as there is a row below at all.
            const currentRow = Math.floor(index / columns);
            const lastRow = Math.floor(last / columns);

            return currentRow < lastRow ? firstUsable(last, -1) : index;
        }
        default:
            return index;
    }
}
