import { ComponentStore } from '@ngrx/component-store';
import { GridStackElement, GridStackNode } from 'gridstack';

import { Injectable } from '@angular/core';

import { DotGridStackNode, DotGridStackWidget, DotTemplateBuilderState } from '../models/models';

let ids = 5;

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
    readonly init = this.updater((_, payload: DotGridStackWidget[]) => ({ items: payload }));

    // Rows Updaters

    /**
     * @description This Method adds a new row to the grid
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly addRow = this.updater(({ items }, payload: DotGridStackWidget) => {
        return {
            items: [
                ...items,
                {
                    h: 1,
                    w: 12,
                    x: 0,
                    id: String(ids++),
                    subGridOpts: {
                        children: []
                    },
                    ...payload
                }
            ]
        };
    });

    /**
     * @description This Method updates the position of the rows
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly moveRow = this.updater(({ items }, changes: DotGridStackWidget[]) => {
        const itemsCopy = structuredClone(items) as DotGridStackWidget[];

        changes.forEach(({ y, id }) => {
            const rowIndex = itemsCopy.findIndex((item: DotGridStackWidget) => item.id === id);
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
    readonly removeRow = this.updater(({ items }, id: string) => {
        return { items: items.filter((item: DotGridStackWidget) => item.id !== id) };
    });

    /**
     * @description This Method updates the row with the new data, as new styleclasses
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly updateRow = this.updater(({ items }, payload: DotGridStackWidget) => {
        const itemsCopy = structuredClone(items) as DotGridStackWidget[];
        const rowIndex = itemsCopy.findIndex((item: DotGridStackWidget) => item.id === payload.id);
        if (rowIndex > -1) itemsCopy[rowIndex] = { ...itemsCopy[rowIndex], ...payload };

        return { items: itemsCopy };
    });

    // Columns Updaters

    /**
     * @description This Method adds a column to a row
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly addColumn = this.updater(({ items }, payload: DotGridStackNode) => {
        const newColumn = {
            parentId: payload.grid?.parentGridItem?.id as string,
            w: payload.w,
            h: payload.h,
            x: payload.x,
            y: payload.y,
            id: String(ids++)
        };

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
    readonly moveColumnInYAxis = this.updater(({ items }, payload: DotGridStackNode[]) => {
        const [nodeToDelete, nodeToAdd] = payload.map((node, i) => ({
            parentId: node.grid?.parentGridItem?.id as string,
            w: node.w,
            h: node.h,
            x: node.x,
            y: node.y,
            id: i ? String(ids++) : node.id
        }));

        const deleteNodeRowIndex = items.findIndex(
            (item: DotGridStackWidget) => item.id === nodeToDelete.parentId
        );
        // To maintain the data of the node, as styleClass and containers
        let updatedNode = items[deleteNodeRowIndex].subGridOpts?.children.find((child) => {
            return child.id === nodeToDelete.id;
        }) as DotGridStackNode;

        updatedNode = { ...updatedNode, ...nodeToAdd };

        return {
            items: items.map((row) => {
                if (row.id === nodeToDelete.parentId) {
                    if (row.subGridOpts)
                        row.subGridOpts.children = row.subGridOpts.children.filter(
                            (child) => child.id !== nodeToDelete.id
                        );
                } else if (row.id === nodeToAdd.parentId) {
                    if (row.subGridOpts) row.subGridOpts.children.push(updatedNode);
                    else row.subGridOpts = { children: [updatedNode] };
                }

                return row;
            })
        };
    });

    /**
     * @description This method updates the columns when changes are made
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly updateColumn = this.updater(({ items }, payload: DotGridStackNode[]) => {
        payload = payload.map((node) => ({
            x: node.x,
            id: node.id as string,
            parentId: node.grid?.parentGridItem?.id as string,
            w: node.w,
            styleClass: node.styleClass,
            containers: node.containers
        })) as DotGridStackWidget[];

        return {
            items: items.map((row) => {
                if (row.id === payload[0].parentId) {
                    if (row.subGridOpts) {
                        row.subGridOpts.children = row.subGridOpts.children.map((child) => {
                            const node = payload.find((node) => node.id === child.id);
                            if (node) return { ...child, ...node };

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
    readonly removeColumn = this.updater(({ items }, payload: DotGridStackWidget) => {
        return {
            items: items.map((row) => {
                if (row.id === payload.parentId) {
                    if (row.subGridOpts) {
                        row.subGridOpts.children = row.subGridOpts.children.filter(
                            (child) => child.id !== payload.id
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
    subGridOnDropped(oldNode: GridStackNode, newNode: GridStackNode) {
        // If the oldNode exists, then the widget was dropped from another subgrid
        if (oldNode && newNode) {
            this.moveColumnInYAxis([oldNode, newNode] as DotGridStackNode[]);
        } else {
            this.addColumn(newNode as DotGridStackNode);

            newNode.grid?.removeWidget(newNode.el as GridStackElement, true);
        }
    }
}
