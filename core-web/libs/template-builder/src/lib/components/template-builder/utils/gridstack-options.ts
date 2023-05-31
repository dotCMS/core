import { GridStackOptions } from 'gridstack';

export const WIDGET_TYPE_ATTRIBUTE = 'data-widget-type';

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
    cellHeight: 224,
    column: 'auto',
    margin: 16,
    minRow: 1,
    maxRow: 1,
    acceptWidgets: isAColumnWidget
};

export const gridOptions: GridStackOptions = {
    disableResize: true,
    cellHeight: 264, // 8px more so it overflows and we can see the 8px of space between rows
    margin: 8,
    minRow: 1,
    acceptWidgets: isARowWidget,
    draggable: {
        handle: '.row__drag-container'
    }
};
