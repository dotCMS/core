import { GridStackOptions } from 'gridstack';

export const widgetTypeAttribute = 'data-widget-type';

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
        el.getAttribute(widgetTypeAttribute) === widgetType.COLUMN || el.classList.contains('sub')
    );
}

/**
 * Check if the element is a row widget by checking the data-widget-type attribute
 *
 * @param {Element} el
 * @return {*}  {boolean}
 */
function isARowWidget(el: Element): boolean {
    return el.getAttribute(widgetTypeAttribute) === widgetType.ROW;
}

export const subGridOptions: GridStackOptions = {
    cellHeight: 85,
    column: 'auto',
    margin: 10,
    minRow: 1,
    maxRow: 1,
    acceptWidgets: isAColumnWidget
};

export const gridOptions: GridStackOptions = {
    disableResize: true,
    cellHeight: 100,
    margin: 10,
    minRow: 1,
    acceptWidgets: isARowWidget
};
