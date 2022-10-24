import { Injectable } from '@angular/core';
import { ComponentStore } from '@ngrx/component-store';
import { MenuItem } from 'primeng/api';
import { DataTableColumn } from '@models/data-table';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotCategory } from '@dotcms/app/shared/models/dot-categories/dot-categories.model';
import { PaginatorService } from '@dotcms/app/api/services/paginator';
import { take } from 'rxjs/operators';
import { DotActionMenuItem } from '@dotcms/app/shared/models/dot-action-menu/dot-action-menu-item.model';

export interface DotCategoriescTableState {
    categoriesBulkActions: MenuItem[];
    categoriesActions: DotActionMenuItem[];
    selectedCategories: DotCategory[];
    addToBundleIdentifier: string;
    tableColumns: DataTableColumn[];
    categories: DotCategory[];
    paginationPerPage: number;
    currentPage: number;
    totalRecords: number;
}

@Injectable()
export class DotCategoriesTableStore extends ComponentStore<DotCategoriescTableState> {
    constructor(
        private dotMessageService: DotMessageService,
        public paginatorService: PaginatorService
    ) {
        super(null);
        this.paginatorService.url = 'v1/categories';
        this.paginatorService
            .get()
            .pipe(take(1))
            .subscribe((items: DotCategory[]) => {
                this.setState({
                    categoriesBulkActions: this.getCategoriesBulkActions(),
                    categoriesActions: this.getCategoriesActions(),
                    tableColumns: this.getCategoriesColumns(),
                    addToBundleIdentifier: '',
                    selectedCategories: [],
                    categories: items,
                    currentPage: this.paginatorService.currentPage,
                    paginationPerPage: this.paginatorService.paginationPerPage,
                    totalRecords: this.paginatorService.totalRecords
                });
            });
    }
    readonly vm$ = this.select((state: DotCategoriescTableState) => state);

    /**
     * A function that updates the state of the store.
     * @param state DotCategoryListState
     * @param selectedCategories DotCategory[]
     */
    readonly updateSelectedCategories = this.updater<DotCategory[]>(
        (state: DotCategoriescTableState, selectedCategories: DotCategory[]) => {
            return {
                ...state,
                selectedCategories
            };
        }
    );

    /** A function that updates the state of the store.
     * @param state DotCategoryListState
     * @param categories DotCategory[]
     */
    readonly setCategories = this.updater<DotCategory[]>(
        (state: DotCategoriescTableState, categories: DotCategory[]) => {
            return {
                ...state,
                categories
            };
        }
    );

    /**
     * > This function returns an observable of an array of DotCategory objects
     * @returns Observable<DotCategory[]>
     */
    public getCategories(filter?: string, currentPage?: number): void {
        this.paginatorService.filter = filter || '';
        if (currentPage) this.paginatorService.setExtraParams('page', currentPage);
        this.paginatorService
            .get()
            .pipe(take(1))
            .subscribe((items: DotCategory[]) => {
                this.setCategories(items);
            });
    }

    /**
     * It returns an array of objects with a label property
     * @returns An array of objects with a label property.
     */
    private getCategoriesBulkActions(): { label: string }[] {
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

    private getCategoriesActions(): DotActionMenuItem[] {
        return [
            {
                menuItem: {
                    label: this.dotMessageService.get('Edit')
                }
            },
            {
                menuItem: {
                    label: this.dotMessageService.get('View Children')
                }
            },
            {
                menuItem: {
                    label: this.dotMessageService.get('Permissions')
                }
            },
            {
                menuItem: {
                    label: this.dotMessageService.get('Add To Bundle')
                }
            },
            {
                menuItem: {
                    label: this.dotMessageService.get('Delete')
                }
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
                fieldName: 'categoryName',
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
                    'message.categories.fieldName.CategoryVelocityVarName'
                ),
                width: '20%',
                sortable: true
            },
            {
                fieldName: 'childrens',
                header: this.dotMessageService.get('message.categories.fieldName.childrens'),
                width: '20%',
                sortable: true
            },
            {
                fieldName: 'sortOrder',
                width: '5%',
                header: this.dotMessageService.get('message.categories.fieldName.SortOrder'),
                sortable: true
            },
            {
                fieldName: 'Actions',
                width: '5%',
                header: '',
                sortable: true
            }
        ];
    }
}
