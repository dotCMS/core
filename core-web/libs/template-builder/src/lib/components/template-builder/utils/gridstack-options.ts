import { GridStackOptions } from 'gridstack';

export const WIDGET_TYPE_ATTRIBUTE = 'data-widget-type';

export const BOX_MARGIN = 32;

export const ROW_MARGIN = 8;

export enum widgetType {
    ROW = 'row',
    COLUMN = 'col'
}

/**
 * Check if the element is a column widget by checking the data-widget-type attribute
 *
 * @param {Element} el
 * @return {*}  {boolean}
 */
function isAColumnWidget(el: Element): boolean {
    return (
        el.getAttribute(WIDGET_TYPE_ATTRIBUTE) === widgetType.COLUMN || el.classList.contains('sub')
    );
}

/**
 * Check if the element is a row widget by checking the data-widget-type attribute
 *
 * @param {Element} el
 * @return {*}  {boolean}
 */
function isARowWidget(el: Element): boolean {
    return el.getAttribute(WIDGET_TYPE_ATTRIBUTE) === widgetType.ROW;
}

export const subGridOptions: GridStackOptions = {
    cellHeight: 225 + BOX_MARGIN, // 32px more to make the padding
    column: 'auto',
    margin: BOX_MARGIN / 2, // Spacing between columns and the edge of the grid
    minRow: 1,
    maxRow: 1,
    acceptWidgets: isAColumnWidget,
    disableOneColumnMode: true
};

export const gridOptions: GridStackOptions = {
    disableResize: true,
    cellHeight: 256 + ROW_MARGIN, // 8px more so it overflows and we can see the 8px of space between rows
    margin: ROW_MARGIN, // Spacing between rows
    minRow: 1,
    acceptWidgets: isARowWidget,
    draggable: {
        handle: '.row__drag-container'
    },
    disableOneColumnMode: true
};
