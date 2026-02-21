import { Subject } from 'rxjs';

import { ChangeDetectionStrategy, Component, DestroyRef, inject, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { ConfirmationService, MenuItem } from 'primeng/api';
import { BreadcrumbModule } from 'primeng/breadcrumb';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { Menu, MenuModule } from 'primeng/menu';
import { SplitButtonModule } from 'primeng/splitbutton';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ToolbarModule } from 'primeng/toolbar';

import { debounceTime, distinctUntilChanged, take } from 'rxjs/operators';

import { DotCategoryForm, DotMessageService } from '@dotcms/data-access';
import { DotCategory } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotCategoriesListStore } from './store/dot-categories-list.store';

import { DotCategoriesCreateComponent } from '../dot-categories-create/dot-categories-create.component';
import { DotCategoriesImportComponent } from '../dot-categories-import/dot-categories-import.component';

@Component({
    selector: 'dot-categories-permissions-placeholder',
    standalone: true,
    imports: [DotMessagePipe],
    template: `
        <p class="p-4 text-center">{{ 'categories.permissions.placeholder' | dm }}</p>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
class DotCategoriesPermissionsPlaceholderComponent {}

@Component({
    selector: 'dot-categories-list',
    standalone: true,
    imports: [
        FormsModule,
        TableModule,
        ButtonModule,
        InputTextModule,
        IconFieldModule,
        InputIconModule,
        ConfirmDialogModule,
        BreadcrumbModule,
        MenuModule,
        SplitButtonModule,
        ToolbarModule,
        DotMessagePipe
    ],
    templateUrl: './dot-categories-list.component.html',
    providers: [DotCategoriesListStore, DialogService, ConfirmationService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'w-full h-full min-h-0 grid grid-cols-1 grid-rows-[min-content_min-content_1fr]'
    }
})
export class DotCategoriesListComponent {
    readonly store = inject(DotCategoriesListStore);

    private readonly dialogService = inject(DialogService);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly destroyRef = inject(DestroyRef);

    private searchSubject = new Subject<string>();

    readonly homeItem = { icon: 'pi pi-home' };
    readonly rowMenu = viewChild<Menu>('rowMenu');
    rowMenuItems: MenuItem[] = [];

    readonly addCategoryMenuItems: MenuItem[] = [
        {
            label: this.dotMessageService.get('categories.import'),
            icon: 'pi pi-upload',
            command: () => this.openImportDialog()
        }
    ];

    constructor() {
        this.searchSubject
            .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
            .subscribe((value) => this.store.setFilter(value));
    }

    onSearch(value: string): void {
        this.searchSubject.next(value);
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

    onBreadcrumbClick(index: number): void {
        this.store.navigateToBreadcrumb(index);
    }

    onHomeClick(): void {
        this.store.navigateToBreadcrumb(-1);
    }

    onRowClick(category: DotCategory): void {
        this.store.navigateToChildren(category);
    }

    openRowMenu(event: Event, category: DotCategory): void {
        this.rowMenuItems = [
            {
                label: this.dotMessageService.get('categories.edit'),
                command: () => this.openEditDialog(category)
            },
            {
                label: this.dotMessageService.get('categories.permissions'),
                command: () => this.openPermissionsDialog()
            },
            {
                label: this.dotMessageService.get('categories.delete'),
                command: () => this.confirmDeleteSingle(category)
            }
        ];
        this.rowMenu()?.toggle(event);
    }

    openPermissionsDialog(): void {
        this.dialogService.open(DotCategoriesPermissionsPlaceholderComponent, {
            header: this.dotMessageService.get('categories.permissions'),
            width: '500px',
            closable: true,
            closeOnEscape: true
        });
    }

    confirmDeleteSingle(category: DotCategory): void {
        this.confirmationService.confirm({
            message: this.dotMessageService.get('categories.confirm.delete.message', '1'),
            header: this.dotMessageService.get('categories.confirm.delete.header'),
            acceptButtonStyleClass: 'p-button-outlined',
            rejectButtonStyleClass: 'p-button-primary',
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
        const ref = this.dialogService.open(DotCategoriesCreateComponent, {
            header: this.dotMessageService.get('categories.add.category'),
            width: '500px',
            data: { parentName },
            closable: true,
            closeOnEscape: true
        });

        ref?.onClose.pipe(take(1)).subscribe((result: DotCategoryForm) => {
            if (result) {
                this.store.createCategory(result);
            }
        });
    }

    openEditDialog(category: DotCategory): void {
        const ref = this.dialogService.open(DotCategoriesCreateComponent, {
            header: this.dotMessageService.get('categories.edit.category'),
            width: '500px',
            data: { category },
            closable: true,
            closeOnEscape: true
        });

        ref?.onClose.pipe(take(1)).subscribe((result: DotCategoryForm) => {
            if (result) {
                this.store.updateCategory({ ...result, inode: category.inode });
            }
        });
    }

    exportCategories(): void {
        this.store.exportCategories();
    }

    openImportDialog(): void {
        const ref = this.dialogService.open(DotCategoriesImportComponent, {
            header: this.dotMessageService.get('categories.import'),
            width: '500px',
            data: { parentInode: this.store.parentInode() },
            closable: true,
            closeOnEscape: true
        });

        ref?.onClose.pipe(take(1)).subscribe((imported: boolean) => {
            if (imported) {
                this.store.loadCategories();
            }
        });
    }

    confirmDelete(): void {
        const count = this.store.selectedCategories().length;

        this.confirmationService.confirm({
            message: this.dotMessageService.get('categories.confirm.delete.message', `${count}`),
            header: this.dotMessageService.get('categories.confirm.delete.header'),
            acceptButtonStyleClass: 'p-button-outlined',
            rejectButtonStyleClass: 'p-button-primary',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            accept: () => this.store.deleteCategories()
        });
    }
}
