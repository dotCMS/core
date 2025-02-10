import { DotPageAssetLayoutColumn } from '../../types';

const endClassMap: Record<number, string> = {
    1: 'col-end-1',
    2: 'col-end-2',
    3: 'col-end-3',
    4: 'col-end-4',
    5: 'col-end-5',
    6: 'col-end-6',
    7: 'col-end-7',
    8: 'col-end-8',
    9: 'col-end-9',
    10: 'col-end-10',
    11: 'col-end-11',
    12: 'col-end-12',
    13: 'col-end-13'
};

const startClassMap: Record<number, string> = {
    1: 'col-start-1',
    2: 'col-start-2',
    3: 'col-start-3',
    4: 'col-start-4',
    5: 'col-start-5',
    6: 'col-start-6',
    7: 'col-start-7',
    8: 'col-start-8',
    9: 'col-start-9',
    10: 'col-start-10',
    11: 'col-start-11',
    12: 'col-start-12'
};

/**
 * Calculates and returns the CSS Grid positioning classes for a column based on its configuration.
 * Uses a 12-column grid system where columns are positioned using grid-column-start and grid-column-end.
 *
 * @example
 * ```typescript
 * const classes = getColumnPositionClasses({
 *   leftOffset: 0, // Starts at the first column
 *   width: 6      // Spans 6 columns
 * });
 * // Returns: { startClass: 'col-start-1', endClass: 'col-end-7' }
 * ```
 *
 * @param {DotPageAssetLayoutColumn} column - Column configuration object
 * @param {number} column.leftOffset - Starting position (0-based) in the grid
 * @param {number} column.width - Number of columns to span
 * @returns {{ startClass: string, endClass: string }} Object containing CSS class names for grid positioning
 */
export const getColumnPositionClasses = (column: DotPageAssetLayoutColumn) => {
    const { leftOffset, width } = column;
    const startClass = startClassMap[leftOffset];
    const endClass = endClassMap[leftOffset + width];

    return {
        startClass,
        endClass
    };
};
