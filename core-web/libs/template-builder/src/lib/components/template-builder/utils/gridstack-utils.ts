import { v4 as uuid } from 'uuid';

import { DotContainer, DotLayoutBody } from '@dotcms/dotcms-models';

import {
    BOX_WIDTH,
    DotGridStackNode,
    DotGridStackWidget,
    SYSTEM_CONTAINER_IDENTIFIER,
    TemplateBuilderBoxSize
} from '../models/models';

const emptyChar = '_';
const boxChar = '#';
const currentBoxChar = '*';

export const EMPTY_ROWS_VALUE = (container?: DotContainer) => {
    return [
        {
            w: 12,
            h: 1,
            x: 0,
            y: 0,
            subGridOpts: {
                children: [
                    {
                        w: 3,
                        h: 1,
                        y: 0,
                        x: 0,
                        id: uuid(),
                        styleClass: [],
                        containers: [
                            {
                                identifier: container?.identifier || SYSTEM_CONTAINER_IDENTIFIER
                            }
                        ]
                    }
                ]
            },
            id: uuid(),
            styleClass: []
        }
    ];
};

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
export function createDotGridStackWidgets(
    columns: DotGridStackNode[],
    defaultContainer?: DotContainer
): DotGridStackWidget[] {
    return columns.map((node) => createDotGridStackWidgetFromNode(node, defaultContainer));
}

/**
 * @description This function creates a DotGridStackWidget from a DotGridStackNode
 *
 * @export
 * @param {DotGridStackNode} node
 * @return {*}  {DotGridStackWidget}
 */
export function createDotGridStackWidgetFromNode(
    node: DotGridStackNode,
    defaultContainer?: DotContainer
): DotGridStackWidget {
    return {
        x: node.x,
        id: node.id || uuid(),
        parentId: node.grid?.parentGridItem?.id as string,
        w: node.w,
        styleClass: node.styleClass,
        containers: node.containers ?? [
            {
                identifier: defaultContainer?.identifier || SYSTEM_CONTAINER_IDENTIFIER
            }
        ],
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
    body: DotLayoutBody | undefined,
    container?: DotContainer
): DotGridStackWidget[] {
    if (!body || !body.rows?.length) {
        return structuredClone(EMPTY_ROWS_VALUE(container));
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

        const columns = children
            .map(({ x, w, containers = [], styleClass: styles }) => {
                const leftOffset = x + colWidth + 1;
                const styleClass = styles?.join(' ') || '';

                return {
                    containers,
                    leftOffset,
                    width: w,
                    styleClass
                };
            })
            .sort(({ leftOffset: a }, { leftOffset: b }) => a - b); // We need to sort by offset so the CSS Grid on Preview Mode works correctly

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
    if (width <= 1) {
        return TemplateBuilderBoxSize.small;
    }

    if (width <= 3) {
        return TemplateBuilderBoxSize.medium;
    }

    return TemplateBuilderBoxSize.large;
}

/**
 * @description This function returns if a box will fit in a row
 *
 * @export
 * @param {DotGridStackWidget[]} boxes
 * @return {*} boolean
 */
export function willBoxFitInRow(boxes: DotGridStackWidget[]): boolean {
    const validSpaceRegex = new RegExp(emptyChar.repeat(BOX_WIDTH), 'g');

    const rowSpace = simulateRowSpace(boxes); // Simulate the row with the current box

    return validSpaceRegex.test(rowSpace.join('')); // If there are sufficient consecutive empty spaces then we can drop one more box
}

/**
 *  @description This function returns the remaining space for a box in a row
 *
 * @export
 * @param {DotGridStackWidget[]} [boxes=[]]
 * @param {DotGridStackWidget} newBox
 * @return {*}  {number}
 */
export function getRemainingSpaceForBox(
    boxes: DotGridStackWidget[] = [],
    newBox: DotGridStackWidget
): number {
    const remainingSpaceRegex = new RegExp(`${'\\' + currentBoxChar}+${emptyChar}{1,2}`, 'g');
    const newBoxRegex = new RegExp(`${'\\' + currentBoxChar}`, 'g');

    const rowSpace = simulateRowSpace(boxes, newBox); // Simulate the row with the current box

    const result = rowSpace.join('').match(remainingSpaceRegex); // Get the section of the currentBox

    return result ? result[0].replace(newBoxRegex, '').length : 0; // If there's space return it
}

/**
 * @description This function returns an array simulating the row pushing the new box in the row
 *
 * @param {DotGridStackWidget} boxes
 * @param {DotGridStackWidget} [newBox]
 * @return {*}
 */
function simulateRowSpace(boxes: DotGridStackWidget[] = [], newBox?: DotGridStackWidget): string[] {
    const rowSpace = Array.from({ length: 12 }, () => emptyChar); // Array with 12 empty spaces

    if (newBox) {
        boxes.push(newBox);
    }

    boxes.forEach(({ x, w, id }) => {
        const currentChar = id === newBox?.id ? currentBoxChar : boxChar;

        rowSpace.splice(x, w, ...currentChar.repeat(w).split('')); // Fill needed empty spaces with # or *
    });

    return rowSpace;
}
