import { v4 as uuid } from 'uuid';

import { DotLayoutBody } from '@dotcms/dotcms-models';

import { DotGridStackNode, DotGridStackWidget } from '../models/models';

/**
 * @description This function parses the oldNode and newNode to a DotGridStackWidget array
 *
 * @export
 * @param {DotGridStackNode} oldNode
 * @param {DotGridStackNode} newNode
 * @return {DotGridStackWidget[]}
 */
export function parseMovedNodeToWidget(
    oldNode: DotGridStackNode,
    newNode: DotGridStackNode
): DotGridStackWidget[] {
    return [oldNode, newNode].map((node, i) => ({
        parentId: node.grid?.parentGridItem?.id as string,
        w: node.w,
        h: node.h,
        x: node.x,
        y: node.y,
        id: i ? uuid() : node.id // I set a new id to the new node so it doesn't get deleted by accident
    }));
}

/**
 * @description This function finds the index of a row in the items array
 *
 * @export
 * @param {DotGridStackWidget[]} items
 * @param {string} rowID
 * @return {*}  {number}
 */
export function getIndexRowInItems(items: DotGridStackWidget[], rowID: string): number {
    return items.findIndex((row: DotGridStackWidget) => row.id === rowID);
}

/**
 * @description This function creates a DotGridStackWidget array from a DotGridStackNode array
 *
 * @export
 * @param {DotGridStackNode[]} columns
 * @return {*}  {DotGridStackWidget[]}
 */
export function createDotGridStackWidgets(columns: DotGridStackNode[]): DotGridStackWidget[] {
    return columns.map((node) => createDotGridStackWidgetFromNode(node));
}

/**
 * @description This function creates a DotGridStackWidget from a DotGridStackNode
 *
 * @export
 * @param {DotGridStackNode} node
 * @return {*}  {DotGridStackWidget}
 */
export function createDotGridStackWidgetFromNode(node: DotGridStackNode): DotGridStackWidget {
    return {
        x: node.x,
        id: uuid(),
        parentId: node.grid?.parentGridItem?.id as string,
        w: node.w,
        styleClass: node.styleClass,
        containers: node.containers,
        y: node.y
    } as DotGridStackWidget;
}

/**
 * @description This function finds a column in a DotGridStackNode array
 *
 * @export
 * @param {DotGridStackNode[]} columns
 * @param {string} columnID
 * @return {*}  {DotGridStackNode}
 */
export function getColumnByID(
    columns: DotGridStackNode[],
    columnID: string
): DotGridStackNode | undefined {
    return columns.find((node) => node.id === columnID);
}

/**
 * @description This function removes a column from a row
 *
 * @export
 * @param {DotGridStackWidget} row
 * @param {string} columnID
 * @return {*}  {DotGridStackWidget[]}
 */
export function removeColumnByID(row: DotGridStackWidget, columnID: string): DotGridStackWidget[] {
    return row.subGridOpts?.children.filter((column) => column.id !== columnID) || [];
}

/**
 * @description This method parse a backend object to gridStack
 * @param body
 * @param gridOptions
 * @returns
 */
export function parseFromDotObjectToGridStack(
    body: DotLayoutBody | undefined
): DotGridStackWidget[] {
    if (!body) {
        return [];
    }

    return body.rows.map((row, i) => ({
        w: 12,
        h: 1,
        x: 0,
        y: i,
        subGridOpts: {
            children: row.columns.map((col) => {
                return {
                    w: col.width,
                    h: 1,
                    y: 0,
                    x: col.leftOffset - 1,
                    id: uuid(),
                    styleClass: col.styleClass ? col.styleClass.split(' ') : [],
                    containers: col.containers
                };
            })
        },
        id: uuid(),
        styleClass: row.styleClass ? row.styleClass.split(' ') : []
    })) as DotGridStackWidget[];
}
