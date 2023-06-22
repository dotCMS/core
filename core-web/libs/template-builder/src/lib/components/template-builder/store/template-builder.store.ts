import { ComponentStore } from '@ngrx/component-store';
import { GridStackElement, GridStackNode } from 'gridstack';
import { v4 as uuid } from 'uuid';

import { Injectable } from '@angular/core';

import { DotContainer } from '@dotcms/dotcms-models';

import {
    DotGridStackNode,
    DotGridStackWidget,
    DotTemplateBuilderState,
    DotTemplateLayoutProperties
} from '../models/models';
import {
    getIndexRowInItems,
    createDotGridStackWidgets,
    getColumnByID,
    removeColumnByID,
    createDotGridStackWidgetFromNode,
    parseMovedNodeToWidget
} from '../utils/gridstack-utils';

/**
 *
 *
 * @export
 * @class DotTemplateBuilderStore
 * @extends {ComponentStore<DotTemplateBuilderState>}
 */
@Injectable()
export class DotTemplateBuilderStore extends ComponentStore<DotTemplateBuilderState> {
    // We need to discuss how we will save this to not trigger the parse every time

    public items$ = this.select((state) => state.items);
    public layoutProperties$ = this.select((state) => state.layoutProperties);

    public vm$ = this.select(this.items$, this.layoutProperties$, (items, layoutProperties) => ({
        items,
        layoutProperties
    }));

    constructor() {
        super({ items: [], layoutProperties: { header: true, footer: true, sidebar: {} } });
    }

    // Init store

    readonly init = this.updater((state, { items, layoutProperties }: DotTemplateBuilderState) => ({
        ...state,
        items,
        layoutProperties
    }));

    // Rows Updaters

    /**
     * @description This Method adds a new row to the grid
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly addRow = this.updater((state, newRow: DotGridStackWidget) => {
        const { items } = state;

        return {
            ...state,
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
    readonly moveRow = this.updater((state, affectedRows: DotGridStackWidget[]) => {
        const { items } = state;
        const itemsCopy = structuredClone(items) as DotGridStackWidget[];

        affectedRows.forEach(({ y, id }) => {
            const rowIndex = getIndexRowInItems(itemsCopy, id as string);
            // So here I update the positions of the changed ones
            if (rowIndex > -1) itemsCopy[rowIndex] = { ...itemsCopy[rowIndex], y };
        });

        return { ...state, items: itemsCopy };
    });

    /**
     * @description This Method removes a row from the grid
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly removeRow = this.updater((state, rowID: string) => {
        const { items } = state;

        return { ...state, items: items.filter((item: DotGridStackWidget) => item.id !== rowID) };
    });

    /**
     * @description This Method updates the row with the new data, as new styleclasses
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly updateRow = this.updater((state, updatedRow: DotGridStackWidget) => {
        const { items } = state;

        const itemsCopy = structuredClone(items) as DotGridStackWidget[];
        const rowIndex = getIndexRowInItems(itemsCopy, updatedRow.id as string);
        if (rowIndex > -1) itemsCopy[rowIndex] = { ...itemsCopy[rowIndex], ...updatedRow };

        return { ...state, items: itemsCopy };
    });

    // Columns Updaters

    /**
     * @description This Method adds a column to a row
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly addColumn = this.updater((state, column: DotGridStackNode) => {
        const { items } = state;
        const newColumn = createDotGridStackWidgetFromNode(column);

        return {
            ...state,
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
    readonly moveColumnInYAxis = this.updater((state, [oldNode, newNode]: DotGridStackNode[]) => {
        const { items } = state;

        const [columnToDelete, columnToAdd] = parseMovedNodeToWidget(oldNode, newNode);

        const deleteColParentIndex = getIndexRowInItems(items, columnToDelete.parentId ?? '');

        // In theory, the children should exist because it had one before the removal, but this is a safety check
        const parentRow = items[deleteColParentIndex] ?? {}; // Empty object so it doesn't break the template builder
        const parentRowChildren = parentRow.subGridOpts
            ? parentRow.subGridOpts.children
            : undefined;

        // To maintain the data of the node, as styleClass and containers
        const oldColumn = getColumnByID(parentRowChildren ?? [], columnToDelete.id as string);

        // We merge the new GridStack data with the old properties
        const updatedColumn = { ...oldColumn, ...columnToAdd };

        return {
            ...state,
            items: items.map((row) => {
                if (row.id === columnToDelete.parentId) {
                    row.subGridOpts = {
                        ...row.subGridOpts,
                        children: removeColumnByID(row, columnToDelete.id as string)
                    };
                } else if (row.id === columnToAdd.parentId) {
                    row.subGridOpts = {
                        ...row.subGridOpts,
                        children: [...(row.subGridOpts?.children ?? []), updatedColumn]
                    };
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
    readonly updateColumnGridStackData = this.updater(
        (state, affectedColumns: DotGridStackNode[]) => {
            const { items } = state;
            affectedColumns = createDotGridStackWidgets(affectedColumns);

            return {
                ...state,
                items: items.map((row) => {
                    if (row.id != affectedColumns[0].parentId || !row.subGridOpts) {
                        return row;
                    }

                    row.subGridOpts.children = row.subGridOpts.children.map((child) => {
                        const column = getColumnByID(affectedColumns, child.id as string);
                        if (column) return { ...child, x: column.x, y: column.y, w: column.w };

                        return child;
                    });

                    return row;
                })
            };
        }
    );

    readonly updateColumnStyleClasses = this.updater(
        (state, affectedColumn: DotGridStackWidget) => {
            const { items } = state;

            return {
                ...state,
                items: items.map((row) => {
                    if (row.id != affectedColumn.parentId || !row.subGridOpts) {
                        return row;
                    }

                    row.subGridOpts.children = row.subGridOpts.children.map((child) => {
                        if (affectedColumn.id === child.id)
                            return { ...child, styleClass: affectedColumn.styleClass };

                        return child;
                    });

                    return row;
                })
            };
        }
    );

    /**
     * @description This method removes a column from the grid
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly removeColumn = this.updater((state, columnToDelete: DotGridStackWidget) => {
        const { items } = state;

        return {
            ...state,
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

    /**
     * @description This method updates the layout properties with new data
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly updateLayoutProperties = this.updater(
        (state, layoutProperties: DotTemplateLayoutProperties) => {
            return {
                ...state,
                layoutProperties: {
                    ...state.layoutProperties,
                    ...layoutProperties,
                    // This is meant to just change the location of the sidebar
                    sidebar: {
                        ...state.layoutProperties.sidebar,
                        location: layoutProperties.sidebar.location
                    }
                }
            };
        }
    );

    /**
     * @description This method updates the sidebar width
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly updateSidebarWidth = this.updater((state, width: string) => {
        const { layoutProperties } = state;

        return {
            ...state,
            layoutProperties: {
                ...layoutProperties,
                sidebar: {
                    ...layoutProperties.sidebar,
                    width
                }
            }
        };
    });

    /**
     * @description This method adds a container to the sidebar
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly addSidebarContainer = this.updater((state, container: DotContainer) => {
        const { layoutProperties } = state;

        if (!container) return state;

        return {
            ...state,
            layoutProperties: {
                ...layoutProperties,
                sidebar: {
                    ...layoutProperties.sidebar,
                    containers: [
                        ...(layoutProperties.sidebar.containers ?? []),
                        {
                            identifier: container.identifier
                        }
                    ]
                }
            }
        };
    });

    /**
     * @description This method deletes a container from the sidebar
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly deleteSidebarContainer = this.updater((state, index: number) => {
        const { layoutProperties } = state;

        return {
            ...state,
            layoutProperties: {
                ...layoutProperties,
                sidebar: {
                    ...layoutProperties.sidebar,
                    containers: (layoutProperties.sidebar.containers ?? []).filter(
                        (_, i) => i !== index
                    )
                }
            }
        };
    });

    /**
     * @description This method adds a container to a box
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly addContainer = this.updater(
        (
            state,
            {
                affectedColumn,
                container
            }: { affectedColumn: DotGridStackWidget; container: DotContainer }
        ) => {
            const { items } = state;

            const updatedItems = items.map((row) => {
                if (row.id != affectedColumn.parentId) {
                    return row;
                }

                const updatedChildren = row.subGridOpts.children.map((child) => {
                    if (affectedColumn.id === child.id)
                        child.containers.push({
                            identifier: container.identifier
                        });

                    return child;
                });

                return { ...row, subGridOpts: { ...row.subGridOpts, children: updatedChildren } };
            });

            return { ...state, items: updatedItems };
        }
    );

    /**
     * @description This method deletes a container from a box
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly deleteContainer = this.updater(
        (
            state,
            {
                affectedColumn,
                containerIndex
            }: { affectedColumn: DotGridStackWidget; containerIndex: number }
        ) => {
            const { items } = state;

            const updatedItems = items.map((row) => {
                if (row.id != affectedColumn.parentId) {
                    return row;
                }

                const updatedChildren = row.subGridOpts.children.map((child) => {
                    if (affectedColumn.id !== child.id) return child;
                    child.containers = child.containers.filter((_, i) => i !== containerIndex);

                    return child;
                });

                return { ...row, subGridOpts: { ...row.subGridOpts, children: updatedChildren } };
            });

            return { ...state, items: updatedItems };
        }
    );

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
