import { Injectable } from '@angular/core';
import { ComponentStore } from '@ngrx/component-store';
import { MenuItem } from 'primeng/api';
import { DataTableColumn } from '@models/data-table';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotCategory } from '@dotcms/app/shared/models/categories/dot-categories.model';

export interface DotCategoriesListState {
    getCategoryEndPoint: string;
    categoriesBulkActions: MenuItem[];
    selectedCategories: DotCategory[];
    addToBundleIdentifier: string;
    tableColumns: DataTableColumn[];
    categoryBreadCrumb: MenuItem[];
    breadCrumbStarterIcon: MenuItem;
    paginationPerPage: number;
}

@Injectable()
export class DotCategoriesListStore extends ComponentStore<DotCategoriesListState> {
    constructor(private dotMessageService: DotMessageService) {
        super(null);
        this.breadCrumbStarterIcon = { icon: 'pi pi-home', disabled: true };
        this.setState({
            getCategoryEndPoint: 'v1/categories',
            categoriesBulkActions: this.getCategoriesActions(),
            tableColumns: this.getCategoriesColumns(),
            addToBundleIdentifier: '',
            selectedCategories: [],
            categoryBreadCrumb: this.getCategoryBreadCrumbs(),
            breadCrumbStarterIcon: this.breadCrumbStarterIcon,
            paginationPerPage: 25
        });
    }
    breadCrumbStarterIcon: MenuItem;
    readonly vm$ = this.select(
        ({
            getCategoryEndPoint,
            categoriesBulkActions,
            addToBundleIdentifier,
            tableColumns,
            selectedCategories,
            categoryBreadCrumb,
            breadCrumbStarterIcon,
            paginationPerPage
        }: DotCategoriesListState) => {
            return {
                getCategoryEndPoint,
                categoriesBulkActions,
                addToBundleIdentifier,
                tableColumns,
                selectedCategories,
                categoryBreadCrumb,
                breadCrumbStarterIcon,
                paginationPerPage
            };
        }
    );

    /* A selector that returns the categoryBreadCrumb property of the state. */
    readonly categoryBreadCrumbSelector$ = this.select(
        ({ categoryBreadCrumb }: DotCategoriesListState) => {
            return {
                categoryBreadCrumb
            };
        }
    );

    /**
     * A function that updates the state of the store.
     * @param state DotCategoryListState
     * @param categoryBreadCrumb MenuItem
     */
    readonly addCategoriesBreadCrumb = this.updater<MenuItem>(
        (state: DotCategoriesListState, categoryBreadCrumb: MenuItem) => {
            return {
                ...state,
                categoryBreadCrumb: [
                    ...state.categoryBreadCrumb,
                    { ...categoryBreadCrumb, tabindex: state.categoryBreadCrumb.length.toString() }
                ]
            };
        }
    );

    /**
     * A function that updates the state of the store.
     * @param state DotCategoryListState
     * @param categoryBreadCrumb MenuItem[]
     */
    readonly updateCategoriesBreadCrumb = this.updater<MenuItem[]>(
        (state: DotCategoriesListState, categoryBreadCrumb: MenuItem[]) => {
            return {
                ...state,
                categoryBreadCrumb
            };
        }
    );

    /**
     * A function that updates the state of the store.
     * @param state DotCategoryListState
     * @param selectedCategories DotCategory[]
     */
    readonly updateSelectedCategories = this.updater<DotCategory[]>(
        (state: DotCategoriesListState, selectedCategories: DotCategory[]) => {
            return {
                ...state,
                selectedCategories
            };
        }
    );

    /**
     * A function that updates the state of the store.
     * @param state DotCategoryListState
     * @param getCategoryEndPoint string
     */
    readonly updateCategoryEndPoint = this.updater<string>(
        (state: DotCategoriesListState, getCategoryEndPoint: string) => {
            return {
                ...state,
                getCategoryEndPoint
            };
        }
    );

    /**
     * It returns an array of objects with a label property
     * @returns An array of objects with a label property.
     */
    private getCategoriesActions(): { label: string }[] {
        return [
            {
                label: this.dotMessageService.get('Add')
            },
            {
                label: this.dotMessageService.get('Delete')
            },
            {
                label: this.dotMessageService.get('Import')
            },
            {
                label: this.dotMessageService.get('Export')
            }
        ];
    }

    /**
     * It returns an array of MenuItem objects
     * @returns An array of MenuItem objects.
     */
    private getCategoryBreadCrumbs(): MenuItem[] {
        return [
            {
                label: 'Top',
                tabindex: '0'
            }
        ];
    }

    /**
     * It returns an array of objects that describe the columns of the table
     * @returns An array of DataTableColumn objects.
     */
    private getCategoriesColumns(): DataTableColumn[] {
        return [
            {
                fieldName: 'name',
                header: this.dotMessageService.get('message.categories.fieldName.Name'),
                width: '50%',
                sortable: true
            },
            {
                fieldName: 'key',
                header: this.dotMessageService.get('message.categories.fieldName.Key'),
                width: '20%',
                sortable: true
            },
            {
                fieldName: 'categoryVelocityVarName',
                header: this.dotMessageService.get(
                    'message.categories.fieldName.categoryVelocityVarName'
                ),
                width: '20%',
                sortable: true
            },
            {
                fieldName: 'sortOrder',
                width: '5%',
                header: this.dotMessageService.get('message.categories.fieldName.SortOrder'),
                sortable: true
            }
        ];
    }
}
