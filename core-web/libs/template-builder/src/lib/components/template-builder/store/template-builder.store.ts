import { ComponentStore } from '@ngrx/component-store';
import { GridStackElement, GridStackNode } from 'gridstack';
import { v4 as uuid } from 'uuid';

import { Injectable } from '@angular/core';

import { DotGridStackNode, DotGridStackWidget, DotTemplateBuilderState } from '../models/models';
import {
    getIndexRowInItems,
    createDotGridStackWidgets,
    getColumnByID,
    removeColumnByID,
    createDotGridStackWidgetFromNode,
    parseMovedNodeToWidget
} from '../utils/gridstack-utils';

const starter: DotGridStackWidget[] = [
    { x: 0, y: 0, w: 12, id: uuid() },
    { x: 0, y: 1, w: 12, id: uuid() },
    {
        x: 0,
        y: 2,
        w: 12,
        id: uuid(),
        subGridOpts: {
            children: [{ x: 0, y: 0, w: 4, id: uuid() }]
        }
    }
];

/**
 *
 *
 * @export
 * @class DotTemplateBuilderStore
 * @extends {ComponentStore<DotTemplateBuilderState>}
 */
@Injectable()
export class DotTemplateBuilderStore extends ComponentStore<DotTemplateBuilderState> {
    public items$ = this.select((state) => state.items);

    constructor() {
        super({ items: [] });
    }

    // Init store
    readonly init = this.updater(() => ({ items: starter }));

    // Rows Updaters

    /**
     * @description This Method adds a new row to the grid
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly addRow = this.updater(({ items }, newRow: DotGridStackWidget) => {
        return {
            items: [
                ...items,
                {
                    ...newRow,
                    h: 1,
                    w: 12,
                    x: 0,
                    id: uuid(),
                    subGridOpts: {
                        children: []
                    }
                }
            ]
        };
    });

    /**
     * @description This Method updates the position of the rows
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly moveRow = this.updater(({ items }, affectedRows: DotGridStackWidget[]) => {
        const itemsCopy = structuredClone(items) as DotGridStackWidget[];

        affectedRows.forEach(({ y, id }) => {
            const rowIndex = getIndexRowInItems(itemsCopy, id as string);
            // So here I update the positions of the changed ones
            if (rowIndex > -1) itemsCopy[rowIndex] = { ...itemsCopy[rowIndex], y };
        });

        return { items: itemsCopy };
    });

    /**
     * @description This Method removes a row from the grid
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly removeRow = this.updater(({ items }, rowID: string) => {
        return { items: items.filter((item: DotGridStackWidget) => item.id !== rowID) };
    });

    /**
     * @description This Method updates the row with the new data, as new styleclasses
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly updateRow = this.updater(({ items }, updatedRow: DotGridStackWidget) => {
        const itemsCopy = structuredClone(items) as DotGridStackWidget[];
        const rowIndex = getIndexRowInItems(itemsCopy, updatedRow.id as string);
        if (rowIndex > -1) itemsCopy[rowIndex] = { ...itemsCopy[rowIndex], ...updatedRow };

        return { items: itemsCopy };
    });

    // Columns Updaters

    /**
     * @description This Method adds a column to a row
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly addColumn = this.updater(({ items }, column: DotGridStackNode) => {
        const newColumn = createDotGridStackWidgetFromNode(column);

        return {
            items: items.map((row) => {
                if (row.id === newColumn.parentId) {
                    if (row.subGridOpts) row.subGridOpts.children.push(newColumn);
                    else row.subGridOpts = { children: [newColumn] };
                }

                return row;
            })
        };
    });

    /**
     * @description This Method updates the position of the columns in Y axis
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly moveColumnInYAxis = this.updater(
        ({ items }, [oldNode, newNode]: DotGridStackNode[]) => {
            const [columnToDelete, columnToAdd] = parseMovedNodeToWidget(oldNode, newNode);

            const deleteNodeRowIndex = getIndexRowInItems(items, columnToDelete.parentId as string);
            // To maintain the data of the node, as styleClass and containers
            let updatedColumn = getColumnByID(
                items[deleteNodeRowIndex].subGridOpts?.children || [],
                columnToDelete.id as string
            );

            updatedColumn = updatedColumn ? { ...updatedColumn, ...columnToAdd } : columnToAdd;

            return {
                items: items.map((row) => {
                    if (row.id === columnToDelete.parentId) {
                        if (row.subGridOpts)
                            row.subGridOpts.children = removeColumnByID(
                                row,
                                columnToDelete.id as string
                            );
                    } else if (row.id === columnToAdd.parentId) {
                        if (row.subGridOpts) row.subGridOpts.children.push(updatedColumn);
                        else row.subGridOpts = { children: [updatedColumn] };
                    }

                    return row;
                })
            };
        }
    );

    /**
     * @description This method updates the columns when changes are made
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly updateColumn = this.updater(({ items }, affectedColumns: DotGridStackNode[]) => {
        affectedColumns = createDotGridStackWidgets(affectedColumns);

        return {
            items: items.map((row) => {
                if (row.id === affectedColumns[0].parentId) {
                    if (row.subGridOpts) {
                        row.subGridOpts.children = row.subGridOpts.children.map((child) => {
                            const column = getColumnByID(affectedColumns, child.id as string);

                            if (column) return { ...child, ...column };

                            return child;
                        });
                    }
                }

                return row;
            })
        };
    });

    /**
     * @description This method removes a column from the grid
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly removeColumn = this.updater(({ items }, columnToDelete: DotGridStackWidget) => {
        return {
            items: items.map((row) => {
                if (row.id === columnToDelete.parentId) {
                    if (row.subGridOpts) {
                        row.subGridOpts.children = removeColumnByID(
                            row,
                            columnToDelete.id as string
                        );
                    }
                }

                return row;
            })
        };
    });

    // Utils methods

    /**
     * @description This is called when a widget is dropped in a subgrid
     *
     * @private
     * @param {GridStackNode} oldNode This is not undefined when you dropped a widget that was on another subGrid
     * @param {GridStackNode} newNode This is the newNode that was dropped
     * @memberof TemplateBuilderComponent
     */
    subGridOnDropped(oldNode: GridStackNode | undefined, newNode: GridStackNode) {
        // If the oldNode exists, then the widget was dropped from another subgrid
        if (oldNode && newNode) {
            this.moveColumnInYAxis([oldNode, newNode] as DotGridStackNode[]);
        } else {
            this.addColumn(newNode as DotGridStackNode);

            newNode.grid?.removeWidget(newNode.el as GridStackElement, true);
        }
    }
}
