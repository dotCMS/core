import { ComponentStore } from '@ngrx/component-store';
import { GridStackElement, GridStackNode } from 'gridstack';

import { Injectable } from '@angular/core';

import {
    DotGridStackNode,
    DotGridStackOptions,
    DotGridStackWidget,
    DotTemplateBuilderState
} from '../models/models';

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
        const itemsCopy = structuredClone(items) as DotGridStackWidget[];

        const newColumn = {
            parentId: payload.grid?.parentGridItem?.id as string,
            w: payload.w,
            h: payload.h,
            x: payload.x,
            y: payload.y,
            id: String(ids++)
        };

        const rowIndex = itemsCopy.findIndex(
            (item: DotGridStackWidget) => item.id === newColumn.parentId
        );
        if (rowIndex > -1)
            if (itemsCopy[rowIndex].subGridOpts)
                (itemsCopy[rowIndex].subGridOpts as DotGridStackOptions).children.push(newColumn);
            // Add the node
            else itemsCopy[rowIndex].subGridOpts = { children: [newColumn] };

        return { items: itemsCopy };
    });

    /**
     * @description This Method updates the position of the columns in Y axis
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly moveColumnInYAxis = this.updater(({ items }, payload: DotGridStackNode[]) => {
        const itemsCopy = structuredClone(items) as DotGridStackWidget[];

        const [nodeToDelete, nodeToAdd] = payload.map((node, i) => ({
            parentId: node.grid?.parentGridItem?.id as string,
            w: node.w,
            h: node.h,
            x: node.x,
            y: node.y,
            id: i ? String(ids++) : node.id
        }));

        const deleteNodeRowIndex = itemsCopy.findIndex(
            (item: DotGridStackWidget) => item.id === nodeToDelete.parentId
        );

        const addNodeRowIndex = itemsCopy.findIndex(
            (item: DotGridStackWidget) => item.id === nodeToAdd.parentId
        );

        // To maintain the data of the node, as styleClass and containers
        let updatedNode = itemsCopy[deleteNodeRowIndex].subGridOpts?.children.find((child) => {
            return child.id === nodeToDelete.id;
        }) as DotGridStackNode;

        updatedNode = { ...updatedNode, ...nodeToAdd };

        if (deleteNodeRowIndex > -1 && itemsCopy[deleteNodeRowIndex].subGridOpts) {
            (itemsCopy[deleteNodeRowIndex].subGridOpts as DotGridStackOptions).children = itemsCopy[
                deleteNodeRowIndex
            ].subGridOpts?.children.filter(
                (child: DotGridStackWidget) => child.id !== nodeToDelete.id
            ) as DotGridStackWidget[]; // Filter the moved node
        }

        if (addNodeRowIndex > -1) {
            if (itemsCopy[addNodeRowIndex].subGridOpts) {
                (itemsCopy[addNodeRowIndex].subGridOpts as DotGridStackOptions).children.push(
                    updatedNode
                );
            }
            // Add the node
            else itemsCopy[addNodeRowIndex].subGridOpts = { children: [updatedNode] };
        }

        return {
            items: itemsCopy
        };
    });

    /**
     * @description This method updates the columns when changes are made
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly updateColumn = this.updater(({ items }, payload: DotGridStackNode[]) => {
        const itemsCopy = structuredClone(items) as DotGridStackWidget[];

        payload = payload.map((node) => ({
            x: node.x,
            id: node.id as string,
            parentId: node.grid?.parentGridItem?.id as string,
            w: node.w,
            styleClass: node.styleClass,
            containers: node.containers
        })) as DotGridStackWidget[];

        const rowIndex = itemsCopy.findIndex(
            (item: DotGridStackWidget) => item.id === payload[0].parentId // They all have the same parent
        );

        if (
            rowIndex > -1 &&
            itemsCopy[rowIndex].subGridOpts &&
            itemsCopy[rowIndex].subGridOpts?.children
        ) {
            (itemsCopy[rowIndex].subGridOpts as DotGridStackOptions).children = itemsCopy[
                rowIndex
            ].subGridOpts?.children.map((child: DotGridStackWidget) => {
                const changedChild = payload.find(
                    (changedChild: DotGridStackWidget) => changedChild.id === child.id
                );
                if (changedChild) {
                    return { ...child, ...changedChild };
                }

                return child;
            }) as DotGridStackWidget[];
        }

        return { items: itemsCopy };
    });

    /**
     * @description This method removes a column from the grid
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly removeColumn = this.updater(({ items }, payload: DotGridStackWidget) => {
        const itemsCopy = structuredClone(items) as DotGridStackWidget[];
        const rowIndex = itemsCopy.findIndex(
            (item: DotGridStackWidget) => item.id === payload.parentId
        );
        if (rowIndex > -1 && itemsCopy[rowIndex].subGridOpts) {
            (itemsCopy[rowIndex].subGridOpts as DotGridStackOptions).children = itemsCopy[
                rowIndex
            ].subGridOpts?.children.filter(
                (child: DotGridStackWidget) => child.id !== payload.id
            ) as DotGridStackWidget[]; // Filter the moved node
        }

        return { items: itemsCopy };
    });

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
