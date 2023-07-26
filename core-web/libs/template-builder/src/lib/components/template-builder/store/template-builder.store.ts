import { ComponentStore } from '@ngrx/component-store';
import { GridStackElement, GridStackNode } from 'gridstack';
import { v4 as uuid } from 'uuid';

import { Injectable } from '@angular/core';

import { DotContainer } from '@dotcms/dotcms-models';

import {
    BOX_WIDTH,
    DotGridStackNode,
    DotGridStackWidget,
    DotTemplateBuilderState,
    DotTemplateLayoutProperties,
    SYSTEM_CONTAINER_IDENTIFIER
} from '../models/models';
import {
    getIndexRowInItems,
    createDotGridStackWidgets,
    getColumnByID,
    removeColumnByID,
    createDotGridStackWidgetFromNode,
    parseMovedNodeToWidget,
    willBoxFitInRow,
    getRemainingSpaceForBox
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
    public rows$ = this.select((state) => state.rows);
    public layoutProperties$ = this.select((state) => state.layoutProperties);

    public vm$ = this.select((state) => ({
        ...state,
        rows: state.rows.map((row) => ({
            ...row,
            willBoxFit: willBoxFitInRow(row.subGridOpts?.children)
        }))
    }));

    // Init store

    readonly init = this.updater(
        (state, { rows, layoutProperties, containerMap }: DotTemplateBuilderState) => ({
            ...state,
            rows,
            layoutProperties,
            containerMap
        })
    );

    // Rows Updaters

    /**
     * @description This Method adds a new row to the grid
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly addRow = this.updater((state, newRow: DotGridStackWidget) => {
        const { rows } = state;

        return {
            ...state,
            rows: [
                ...rows,
                {
                    ...newRow,
                    h: 1,
                    w: 12,
                    x: 0,
                    id: uuid(),
                    subGridOpts: {
                        children: [
                            {
                                id: uuid(),
                                w: 3,
                                h: 1,
                                x: 0,
                                y: 0,
                                containers: [
                                    {
                                        identifier: SYSTEM_CONTAINER_IDENTIFIER
                                    }
                                ],
                                parentId: newRow.id,
                                styleClass: null
                            }
                        ]
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
        const { rows } = state;
        const itemsCopy = structuredClone(rows) as DotGridStackWidget[];

        affectedRows.forEach(({ y, id }) => {
            const rowIndex = getIndexRowInItems(itemsCopy, id as string);
            // So here I update the positions of the changed ones
            if (rowIndex > -1) itemsCopy[rowIndex] = { ...itemsCopy[rowIndex], y };
        });

        return { ...state, rows: itemsCopy };
    });

    /**
     * @description This Method removes a row from the grid
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly removeRow = this.updater((state, rowID: string) => {
        const { rows } = state;

        return { ...state, rows: rows.filter((item: DotGridStackWidget) => item.id !== rowID) };
    });

    /**
     * @description This Method updates the row with the new data, as new styleclasses
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly updateRow = this.updater((state, updatedRow: DotGridStackWidget) => {
        const { rows } = state;

        const itemsCopy = structuredClone(rows) as DotGridStackWidget[];
        const rowIndex = getIndexRowInItems(itemsCopy, updatedRow.id as string);
        if (rowIndex > -1) itemsCopy[rowIndex] = { ...itemsCopy[rowIndex], ...updatedRow };

        return { ...state, rows: itemsCopy };
    });

    /**
     * @description This Method updates the resizing rowID
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly setResizingRowID = this.updater((state, resizingRowID: string = null) => ({
        ...state,
        resizingRowID
    }));

    // Columns Updaters

    /**
     * @description This Method adds a column to a row
     *
     * @memberof DotTemplateBuilderStore
     */
    readonly addColumn = this.updater((state, column: DotGridStackNode) => {
        const { rows } = state;
        const newColumn = createDotGridStackWidgetFromNode(column);

        return {
            ...state,
            rows: rows.map((row) => {
                if (row.id === newColumn.parentId) {
                    const resizedColumn = {
                        ...newColumn,
                        w:
                            getRemainingSpaceForBox(
                                [...(row.subGridOpts?.children || [])],
                                newColumn
                            ) + BOX_WIDTH
                    };

                    if (row.subGridOpts) row.subGridOpts.children.push(resizedColumn);
                    else row.subGridOpts = { children: [resizedColumn] };
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
        const { rows } = state;

        const [columnToDelete, columnToAdd] = parseMovedNodeToWidget(oldNode, newNode);

        const deleteColParentIndex = getIndexRowInItems(rows, columnToDelete.parentId ?? '');

        // In theory, the children should exist because it had one before the removal, but this is a safety check
        const parentRow = rows[deleteColParentIndex] ?? {}; // Empty object so it doesn't break the template builder
        const parentRowChildren = parentRow.subGridOpts
            ? parentRow.subGridOpts.children
            : undefined;

        // To maintain the data of the node, as styleClass and containers
        const oldColumn = getColumnByID(parentRowChildren ?? [], columnToDelete.id as string);

        // We merge the new GridStack data with the old properties
        const updatedColumn = { ...oldColumn, ...columnToAdd };

        return {
            ...state,
            rows: rows.map((row) => {
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
            const { rows } = state;
            affectedColumns = createDotGridStackWidgets(affectedColumns);

            return {
                ...state,
                rows: rows.map((row) => {
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
            const { rows } = state;

            return {
                ...state,
                rows: rows.map((row) => {
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
        const { rows } = state;

        return {
            ...state,
            rows: rows.map((row) => {
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
            },
            containerMap: { ...state.containerMap, [container.identifier]: container }
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
            const { rows } = state;

            const updatedItems = rows.map((row) => {
                if (row.id != affectedColumn.parentId) {
                    return row;
                }

                const updatedChildren = row.subGridOpts.children.map((child) => {
                    if (affectedColumn.id === child.id) {
                        if (!child.containers) child.containers = [];

                        child.containers.push({
                            identifier: container.identifier
                        });
                    }

                    return child;
                });

                return { ...row, subGridOpts: { ...row.subGridOpts, children: updatedChildren } };
            });

            return {
                ...state,
                rows: updatedItems,
                containerMap: { ...state.containerMap, [container.identifier]: container }
            };
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
            const { rows } = state;

            const updatedItems = rows.map((row) => {
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

            return { ...state, rows: updatedItems };
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
