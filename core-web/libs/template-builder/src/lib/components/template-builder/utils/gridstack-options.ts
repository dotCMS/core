import { GridStackOptions } from 'gridstack';

import { BOX_WIDTH } from '../models/models';

export const WIDGET_TYPE_ATTRIBUTE = 'data-widget-type';

// Pixel values remember to change if you change the values in the scss
export const BOX_MARGIN = 32;

export const BOX_HEIGHT = 225;

export const ROW_MARGIN = 8;

export const ROW_HEIGHT = 256;

export const GRID_STACK_ROW_HEIGHT = ROW_HEIGHT + ROW_MARGIN;

export const GRID_STACK_BOX_HEIGHT = BOX_HEIGHT + BOX_MARGIN;

// REM values

export const GRID_STACK_UNIT = 'rem';

export const GRID_STACK_MARGIN_HORIZONTAL = 0.5;

export const GRID_STACK_MARGIN_VERTICAL = 1;

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
    cellHeight: GRID_STACK_BOX_HEIGHT,
    column: 'auto',
    margin: `${GRID_STACK_MARGIN_VERTICAL}${GRID_STACK_UNIT} ${GRID_STACK_MARGIN_HORIZONTAL}${GRID_STACK_UNIT}`,
    minRow: 1,
    maxRow: 1,
    acceptWidgets: isAColumnWidget,
    disableOneColumnMode: true,
    resizable: {
        handles: 'e, w'
    }
};

export const gridOptions: GridStackOptions = {
    disableResize: true,
    cellHeight: GRID_STACK_ROW_HEIGHT,
    minRow: 1,
    acceptWidgets: isARowWidget,
    draggable: {
        handle: '.row__drag-container'
    },
    disableOneColumnMode: true
};

export const rowInitialOptions = {
    w: 12,
    h: 1
};

export const boxInitialOptions = {
    w: BOX_WIDTH,
    h: 1
};
