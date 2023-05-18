import { ComponentStore } from '@ngrx/component-store';

import { Injectable } from '@angular/core';

import { DotGridStackWidget, DotTemplateBuilderState } from '../utils/types';

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
        return { items: [...items, payload] };
    });

    /**
     * @description This Method updates the position of the rows
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly moveRow = this.updater(({ items }, changes: DotGridStackWidget[]) => {
        const itemsCopy = structuredClone(items);

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
        const itemsCopy = structuredClone(items);
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
    readonly addColumn = this.updater(({ items }, payload: DotGridStackWidget) => {
        const itemsCopy = structuredClone(items);
        const rowIndex = itemsCopy.findIndex(
            (item: DotGridStackWidget) => item.id === payload.parentId
        );
        if (rowIndex > -1)
            if (itemsCopy[rowIndex].subGridOpts)
                itemsCopy[rowIndex].subGridOpts.children.push(payload); // Add the node
            else itemsCopy[rowIndex].subGridOpts = { children: [payload] };

        return { items: itemsCopy };
    });

    /**
     * @description This Method updates the position of the columns in Y axis
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly moveColumnInYAxis = this.updater(
        ({ items }, [nodeToDelete, nodeToAdd]: DotGridStackWidget[]) => {
            const itemsCopy = structuredClone(items);

            const deleteNodeRowIndex = itemsCopy.findIndex(
                (item: DotGridStackWidget) => item.id === nodeToDelete.parentId
            );
            const addNodeRowIndex = itemsCopy.findIndex(
                (item: DotGridStackWidget) => item.id === nodeToAdd.parentId
            );

            if (deleteNodeRowIndex > -1) {
                itemsCopy[deleteNodeRowIndex].subGridOpts.children = itemsCopy[
                    deleteNodeRowIndex
                ].subGridOpts.children.filter(
                    (child: DotGridStackWidget) => child.id !== nodeToDelete.id
                ); // Filter the moved node
            }

            if (addNodeRowIndex > -1) {
                if (itemsCopy[addNodeRowIndex].subGridOpts)
                    itemsCopy[addNodeRowIndex].subGridOpts.children.push(nodeToAdd); // Add the node
                else itemsCopy[addNodeRowIndex].subGridOpts = { children: [nodeToAdd] };
            }

            return {
                items: itemsCopy
            };
        }
    );

    /**
     * @description This method updates the columns when changes are made
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly updateColumn = this.updater(({ items }, payload: DotGridStackWidget[]) => {
        const itemsCopy = structuredClone(items);

        const rowIndex = itemsCopy.findIndex(
            (item: DotGridStackWidget) => item.id === payload[0].parentId // They all have the same parent
        );

        if (rowIndex > -1) {
            itemsCopy[rowIndex].subGridOpts.children = itemsCopy[rowIndex].subGridOpts.children.map(
                (child: DotGridStackWidget) => {
                    const changedChild = payload.find(
                        (changedChild: DotGridStackWidget) => changedChild.id === child.id
                    );
                    if (changedChild) {
                        return { ...child, ...changedChild };
                    }

                    return child;
                }
            );
        }

        return { items: itemsCopy };
    });

    /**
     * @description This method removes a column from the grid
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly removeColumn = this.updater(({ items }, payload: DotGridStackWidget) => {
        const itemsCopy = structuredClone(items);
        const rowIndex = itemsCopy.findIndex(
            (item: DotGridStackWidget) => item.id === payload.parentId
        );
        if (rowIndex > -1) {
            itemsCopy[rowIndex].subGridOpts.children = itemsCopy[
                rowIndex
            ].subGridOpts.children.filter((child: DotGridStackWidget) => child.id !== payload.id);
        }

        return { items: itemsCopy };
    });
}
