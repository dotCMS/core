import { v4 as uuid } from 'uuid';

import { DotLayoutBody } from '@dotcms/dotcms-models';

import { DotGridStackNode, DotGridStackWidget, TemplateBuilderBoxSize } from '../models/models';

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
        id: node.id || uuid(),
        parentId: node.grid?.parentGridItem?.id as string,
        w: node.w,
        styleClass: node.styleClass,
        containers: node.containers ?? [],
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

export const parseFromGridStackToDotObject = (gridData: DotGridStackWidget[]): DotLayoutBody => {
    if (!gridData) {
        return { rows: [] };
    }

    // Clone the data so we don't mutate the original
    const clone = structuredClone(gridData);
    const ordered = clone.sort((a, b) => a.y - b.y);

    const rows = ordered.map((row) => {
        const { x: colWidth, subGridOpts, styleClass: styles } = row;
        const { children = [] } = subGridOpts || {};
        const styleClass = styles?.join(' ') || null;

        if (!subGridOpts) {
            return {
                columns: [],
                styleClass
            };
        }

        const columns = children.map(({ x, w, containers = [], styleClass: styles }) => {
            const leftOffset = x + colWidth + 1;
            const styleClass = styles?.join(' ') || '';

            return {
                containers,
                leftOffset,
                width: w,
                styleClass
            };
        });

        return {
            columns,
            styleClass
        };
    });

    return { rows };
};

/**
 * @description This function returns the variant of a box based on the number of width
 *
 * @param {number} width
 * @return {*}  {TemplateBuilderBoxSize}
 */
export function getBoxVariantByWidth(width: number): TemplateBuilderBoxSize {
    if (width <= 1) return TemplateBuilderBoxSize.small;

    if (width <= 3) return TemplateBuilderBoxSize.medium;

    return TemplateBuilderBoxSize.large;
}
