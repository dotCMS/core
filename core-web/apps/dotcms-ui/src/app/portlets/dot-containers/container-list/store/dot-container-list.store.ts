import { Injectable } from '@angular/core';
import { ComponentStore } from '@ngrx/component-store';
import { MenuItem } from 'primeng/api';
import { DotContainer } from '@models/container/dot-container.model';
import { ActionHeaderOptions } from '@models/action-header';
import { DataTableColumn } from '@models/data-table';

export interface DotContainerListState {
    containerBulkActions: MenuItem[];
    selectedContainers: DotContainer[];
    addToBundleIdentifier: string;
    actionHeaderOptions: ActionHeaderOptions;
    tableColumns: DataTableColumn[];
}

const defaultState: DotContainerListState = {
    containerBulkActions: [],
    selectedContainers: [],
    addToBundleIdentifier: '',
    actionHeaderOptions: {},
    tableColumns: []
};

@Injectable()
export class DotContainerListStore extends ComponentStore<DotContainerListState> {
    constructor() {
        super(defaultState);
    }

    readonly vm$ = this.select(
        ({
            containerBulkActions,
            selectedContainers,
            addToBundleIdentifier,
            actionHeaderOptions,
            tableColumns
        }: DotContainerListState) => {
            return {
                containerBulkActions,
                selectedContainers,
                addToBundleIdentifier,
                actionHeaderOptions,
                tableColumns
            };
        }
    );

    readonly updateContainerBulkActions = this.updater<MenuItem[]>(
        (state: DotContainerListState, bulkActions: MenuItem[]) => {
            return {
                ...state,
                containerBulkActions: bulkActions
            };
        }
    );

    readonly updateTableColumns = this.updater<DataTableColumn[]>(
        (state: DotContainerListState, tableColumns: DataTableColumn[]) => {
            return {
                ...state,
                tableColumns
            };
        }
    );

    readonly updateBundleIdentifier = this.updater<string>(
        (state: DotContainerListState, addToBundleIdentifier: string) => {
            return {
                ...state,
                addToBundleIdentifier
            };
        }
    );

    readonly updateSelectedContainers = this.updater<DotContainer[]>(
        (state: DotContainerListState, selectedContainers: DotContainer[]) => {
            return {
                ...state,
                selectedContainers
            };
        }
    );

    readonly updateActionHeaderOptions = this.updater<ActionHeaderOptions>(
        (state: DotContainerListState, actionHeaderOptions: ActionHeaderOptions) => {
            return {
                ...state,
                actionHeaderOptions
            };
        }
    );
}
