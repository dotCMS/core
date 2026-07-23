import { Subject } from 'rxjs';

import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    computed,
    inject,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ConfirmationService, MenuItem } from 'primeng/api';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ContextMenu, ContextMenuModule } from 'primeng/contextmenu';
import { DialogService } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { Menu, MenuModule } from 'primeng/menu';
import { SplitButtonModule } from 'primeng/splitbutton';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';
import { BreadcrumbItemClickEvent } from 'primeng/types/breadcrumb';

import { debounceTime, distinctUntilChanged, take } from 'rxjs/operators';

import {
    DotCategoryForm,
    DotCategoryImportResult,
    DotCategoryUpdateForm,
    DotMessageDisplayService,
    DotMessageService
} from '@dotcms/data-access';
import { DotCategory, DotMessageSeverity, DotMessageType } from '@dotcms/dotcms-models';
import {
    DotAddToBundleComponent,
    DotMessagePipe,
    DotPermissionsIframeDialogComponent,
    DotPermissionsIframeDialogData
} from '@dotcms/ui';

import {
    ALL_CATEGORIES_BUNDLE_IDENTIFIER,
    DotCategoriesListStore
} from './store/dot-categories-list.store';

import { DotCategoriesCreateComponent } from '../dot-categories-create/dot-categories-create.component';
import { DotCategoriesImportComponent } from '../dot-categories-import/dot-categories-import.component';

@Component({
    selector: 'dot-categories-list',
    imports: [
        FormsModule,
        TableModule,
        ButtonModule,
        InputTextModule,
        IconFieldModule,
        InputIconModule,
        ConfirmDialogModule,
        BreadcrumbModule,
        ContextMenuModule,
        InputNumberModule,
        MenuModule,
        SplitButtonModule,
        ToolbarModule,
        DotMessagePipe,
        DotAddToBundleComponent
    ],
    templateUrl: './dot-categories-list.component.html',
    providers: [DotCategoriesListStore, DialogService, ConfirmationService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-col h-full min-h-0' }
})
export class DotCategoriesListComponent {
    readonly store = inject(DotCategoriesListStore);

    readonly #dialogService = inject(DialogService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dotMessageDisplayService = inject(DotMessageDisplayService);
    readonly #destroyRef = inject(DestroyRef);

    #searchSubject = new Subject<string>();

    readonly homeItem = {};
    readonly $rowMenu = viewChild<ContextMenu>('rowMenu');
    readonly $toolbarMenu = viewChild.required<Menu>('toolbarMenu');
    rowMenuItems: MenuItem[] = [];
    readonly #pendingSortOrders = new Map<string, number>();

    /** @see ALL_CATEGORIES_BUNDLE_IDENTIFIER */
    readonly allCategoriesBundleIdentifier = ALL_CATEGORIES_BUNDLE_IDENTIFIER;

    readonly $ptConfig = computed(() => ({
        table: {
            style: {
                'table-layout': 'fixed' as const,
                ...(this.store.categories().length === 0 && {
                    height: '100%',
                    width: '100%'
                })
            }
        }
    }));

    readonly addCategoryMenuItems: MenuItem[] = [
        {
            label: this.#dotMessageService.get('categories.import'),
            command: () => this.openImportDialog()
        }
    ];

    readonly toolbarMenuItems: MenuItem[] = [
        {
            label: this.#dotMessageService.get('categories.add.to.bundle'),
            command: () => this.store.openAddToBundle()
        }
    ];

    onToolbarMenuToggle(event: MouseEvent): void {
        this.$toolbarMenu().toggle(event);
    }

    constructor() {
        this.#searchSubject
            .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.#destroyRef))
            .subscribe((value) => this.store.setFilter(value));
    }

    onSearch(value: string): void {
        this.#searchSubject.next(value);
    }

    onLazyLoad(event: TableLazyLoadEvent): void {
        const rows = (event.rows as number) ?? this.store.rows();
        const first = (event.first as number) ?? 0;
        const page = Math.floor(first / rows) + 1;

        this.store.setPagination(page, rows);

        if (event.sortField) {
            const field = Array.isArray(event.sortField) ? event.sortField[0] : event.sortField;
            const order = event.sortOrder === -1 ? 'DESC' : 'ASC';
            this.store.setSort(field, order);
        }
    }

    onSortOrderInput(category: DotCategory, value: number | null): void {
        if (value !== null) {
            this.#pendingSortOrders.set(category.inode, value);
        }
    }

    onSortOrderBlur(category: DotCategory): void {
        const pending = this.#pendingSortOrders.get(category.inode);
        this.#pendingSortOrders.delete(category.inode);
        if (pending !== undefined && pending !== category.sortOrder) {
            this.store.updateSortOrder(category.inode, pending);
        }
    }

    onBreadcrumbClick(event: BreadcrumbItemClickEvent): void {
        const index = event.item ? this.store.breadcrumbs().indexOf(event.item) : -1;
        this.store.navigateToBreadcrumb(index);
    }

    onHomeClick(): void {
        this.store.navigateToBreadcrumb(-1);
    }

    onRowClick(category: DotCategory): void {
        this.store.navigateToChildren(category);
    }

    onSortOrderKeyDown(event: KeyboardEvent): void {
        if (event.key === 'Enter') {
            (event.target as HTMLElement).blur();
        }
    }

    openRowMenu(event: MouseEvent, category: DotCategory): void {
        event.preventDefault();
        this.rowMenuItems = [
            {
                label: this.#dotMessageService.get('categories.edit'),
                command: () => this.openEditDialog(category)
            },
            {
                label: this.#dotMessageService.get('categories.permissions'),
                command: () => this.openPermissionsDialog(category)
            },
            { separator: true },
            {
                label: this.#dotMessageService.get('categories.delete'),
                command: () => this.confirmDeleteSingle(category)
            }
        ];
        this.$rowMenu()?.show(event);
    }

    openPermissionsDialog(category: DotCategory): void {
        this.#dialogService.open(DotPermissionsIframeDialogComponent, {
            header: this.#dotMessageService.get('categories.permissions'),
            width: 'min(92vw, 75rem)',
            contentStyle: { overflow: 'hidden' },
            data: {
                url: this.#buildPermissionsUrl(category.inode)
            } satisfies DotPermissionsIframeDialogData,
            closable: true,
            closeOnEscape: true,
            modal: true,
            appendTo: 'body',
            draggable: false,
            resizable: false,
            position: 'center'
        });
    }

    #buildPermissionsUrl(inode: string): string {
        const params = new URLSearchParams({
            categoryInode: inode,
            popup: 'true'
        });
        return `/html/portlet/ext/categories/permissions.jsp?${params.toString()}`;
    }

    confirmDeleteSingle(category: DotCategory): void {
        this.#confirmationService.confirm({
            message: this.#dotMessageService.get('categories.confirm.delete.message', '1'),
            header: this.#dotMessageService.get('categories.confirm.delete.header'),
            acceptLabel: this.#dotMessageService.get('categories.delete'),
            rejectLabel: this.#dotMessageService.get('categories.cancel'),
            acceptButtonStyleClass: 'p-button-primary',
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => {
                this.store.setSelectedCategories([category]);
                this.store.deleteCategories();
            }
        });
    }

    openCreateDialog(): void {
        const breadcrumbs = this.store.breadcrumbs();
        const parentName = breadcrumbs.length
            ? (breadcrumbs[breadcrumbs.length - 1].label as string)
            : null;
        const ref = this.#dialogService.open(DotCategoriesCreateComponent, {
            header: this.#dotMessageService.get('categories.add.category'),
            width: '700px',
            data: { parentName },
            closable: true,
            closeOnEscape: true,
            draggable: false
        });

        ref?.onClose.pipe(take(1)).subscribe((result: DotCategoryForm) => {
            if (result) {
                this.store.createCategory(result);
            }
        });
    }

    openEditDialog(category: DotCategory): void {
        const ref = this.#dialogService.open(DotCategoriesCreateComponent, {
            header: this.#dotMessageService.get('categories.edit.category'),
            width: '700px',
            data: { category },
            closable: true,
            closeOnEscape: true,
            draggable: false
        });

        ref?.onClose.pipe(take(1)).subscribe((result: DotCategoryForm) => {
            if (result) {
                const updateForm: DotCategoryUpdateForm = { ...result, inode: category.inode };
                this.store.updateCategory(updateForm);
            }
        });
    }

    exportCategories(): void {
        this.store.exportCategories();
    }

    openImportDialog(): void {
        const ref = this.#dialogService.open(DotCategoriesImportComponent, {
            header: this.#dotMessageService.get('categories.import'),
            width: '700px',
            contentStyle: { height: '460px' },
            data: { parentInode: this.store.parentInode() },
            closable: true,
            closeOnEscape: true,
            draggable: false
        });

        ref?.onClose.pipe(take(1)).subscribe((result: DotCategoryImportResult) => {
            if (result) {
                this.store.loadCategories();
                const isSuccess = result.fails.length === 0;
                this.#dotMessageDisplayService.push({
                    life: 5000,
                    severity: isSuccess ? DotMessageSeverity.SUCCESS : DotMessageSeverity.WARNING,
                    message: isSuccess
                        ? this.#dotMessageService.get(
                              'categories.import.success',
                              `${result.successCount}`
                          )
                        : this.#dotMessageService.get(
                              'categories.import.partial-success',
                              `${result.successCount}`,
                              `${result.fails.length}`
                          ),
                    type: DotMessageType.SIMPLE_MESSAGE
                });
            }
        });
    }

    confirmDelete(): void {
        const count = this.store.selectedCategories().length;

        this.#confirmationService.confirm({
            message: this.#dotMessageService.get('categories.confirm.delete.message', `${count}`),
            header: this.#dotMessageService.get('categories.confirm.delete.header'),
            acceptLabel: this.#dotMessageService.get('categories.delete'),
            rejectLabel: this.#dotMessageService.get('categories.cancel'),
            acceptButtonStyleClass: 'p-button-primary',
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => this.store.deleteCategories()
        });
    }
}
