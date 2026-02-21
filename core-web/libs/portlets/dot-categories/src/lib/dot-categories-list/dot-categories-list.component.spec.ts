import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { Subject } from 'rxjs';

import { ConfirmationService } from 'primeng/api';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotCategory } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotCategoriesListComponent } from './dot-categories-list.component';
import { DotCategoriesListStore } from './store/dot-categories-list.store';

const MOCK_CATEGORIES: DotCategory[] = [
    {
        categoryName: 'Category 1',
        key: 'cat1',
        categoryVelocityVarName: 'cat1Var',
        sortOrder: 0,
        active: true,
        inode: 'inode-1',
        identifier: 'id-1',
        type: 'Category',
        childrenCount: 3,
        description: '',
        keywords: '',
        iDate: Date.now(),
        owner: 'system'
    } as DotCategory,
    {
        categoryName: 'Category 2',
        key: 'cat2',
        categoryVelocityVarName: 'cat2Var',
        sortOrder: 1,
        active: true,
        inode: 'inode-2',
        identifier: 'id-2',
        type: 'Category',
        childrenCount: 0,
        description: '',
        keywords: '',
        iDate: Date.now(),
        owner: 'system'
    } as DotCategory
];

describe('DotCategoriesListComponent', () => {
    let spectator: Spectator<DotCategoriesListComponent>;
    let store: InstanceType<typeof DotCategoriesListStore>;

    // Mock window.matchMedia for PrimeNG SplitButton
    beforeAll(() => {
        Object.defineProperty(window, 'matchMedia', {
            writable: true,
            value: jest.fn().mockImplementation((query) => ({
                matches: false,
                media: query,
                onchange: null,
                addListener: jest.fn(),
                removeListener: jest.fn(),
                addEventListener: jest.fn(),
                removeEventListener: jest.fn(),
                dispatchEvent: jest.fn()
            }))
        });
    });

    const createComponent = createComponentFactory({
        component: DotCategoriesListComponent,
        componentProviders: [
            mockProvider(DotCategoriesListStore, {
                categories: jest.fn().mockReturnValue(MOCK_CATEGORIES),
                selectedCategories: jest.fn().mockReturnValue(MOCK_CATEGORIES),
                filter: jest.fn().mockReturnValue(''),
                page: jest.fn().mockReturnValue(1),
                rows: jest.fn().mockReturnValue(25),
                totalRecords: jest.fn().mockReturnValue(100),
                status: jest.fn().mockReturnValue('loaded'),
                sortField: jest.fn().mockReturnValue('category_name'),
                sortOrder: jest.fn().mockReturnValue('ASC'),
                breadcrumbs: jest.fn().mockReturnValue([]),
                parentInode: jest.fn().mockReturnValue(null),
                setFilter: jest.fn(),
                setPagination: jest.fn(),
                setSort: jest.fn(),
                setSelectedCategories: jest.fn(),
                createCategory: jest.fn(),
                updateCategory: jest.fn(),
                deleteCategories: jest.fn(),
                exportCategories: jest.fn(),
                importCategories: jest.fn(),
                loadCategories: jest.fn(),
                navigateToChildren: jest.fn(),
                navigateToBreadcrumb: jest.fn()
            }),
            mockProvider(DialogService),
            ConfirmationService
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({})
            }
        ]
    });

    beforeEach(() => {
        jest.useFakeTimers();
        spectator = createComponent();
        store = spectator.inject(DotCategoriesListStore, true);
        jest.clearAllMocks();
    });

    afterEach(() => {
        jest.useRealTimers();
    });

    describe('Search', () => {
        it('should debounce search by 300ms', () => {
            spectator.component.onSearch('test');
            jest.advanceTimersByTime(299);
            expect(store.setFilter).not.toHaveBeenCalled();

            jest.advanceTimersByTime(1);
            expect(store.setFilter).toHaveBeenCalledWith('test');
        });

        it('should reset debounce timer on rapid typing', () => {
            spectator.component.onSearch('a');
            jest.advanceTimersByTime(100);
            spectator.component.onSearch('ab');
            jest.advanceTimersByTime(100);
            spectator.component.onSearch('abc');
            jest.advanceTimersByTime(300);

            expect(store.setFilter).toHaveBeenCalledTimes(1);
            expect(store.setFilter).toHaveBeenCalledWith('abc');
        });
    });

    describe('Lazy Load', () => {
        it('should compute page correctly and call setPagination and setSort', () => {
            spectator.component.onLazyLoad({
                first: 50,
                rows: 25,
                sortField: 'category_name',
                sortOrder: 1
            });

            expect(store.setPagination).toHaveBeenCalledWith(3, 25);
            expect(store.setSort).toHaveBeenCalledWith('category_name', 'ASC');
        });

        it('should not call setSort when sortField is not provided', () => {
            spectator.component.onLazyLoad({ first: 0, rows: 25 });

            expect(store.setPagination).toHaveBeenCalledWith(1, 25);
            expect(store.setSort).not.toHaveBeenCalled();
        });

        it('should handle DESC sort order', () => {
            spectator.component.onLazyLoad({
                first: 0,
                rows: 25,
                sortField: 'category_name',
                sortOrder: -1
            });

            expect(store.setSort).toHaveBeenCalledWith('category_name', 'DESC');
        });
    });

    describe('Button Interactions', () => {
        describe('Add Split Button', () => {
            it('should render Add Category split button', () => {
                spectator.detectChanges();
                const btnHost = spectator.query(byTestId('category-add-split-btn'));
                expect(btnHost).toBeTruthy();
                const button = btnHost?.querySelector('button');
                expect(button).toBeTruthy();
                expect(button?.textContent).toContain('categories.add.category');
            });

            it('should call openCreateDialog when main button clicked', () => {
                const spy = jest.spyOn(spectator.component, 'openCreateDialog');
                const btnHost = spectator.query(byTestId('category-add-split-btn'));
                const button = btnHost?.querySelector('button');
                spectator.click(button!);
                expect(spy).toHaveBeenCalled();
            });

            it('should have Import menu item that calls openImportDialog', () => {
                const spy = jest.spyOn(spectator.component, 'openImportDialog');
                const menuItems = spectator.component.addCategoryMenuItems;
                expect(menuItems).toHaveLength(1);
                expect(menuItems[0].label).toBe('categories.import');
                expect(menuItems[0].icon).toBe('pi pi-upload');

                menuItems[0].command!({} as never);
                expect(spy).toHaveBeenCalled();
            });
        });

        describe('Conditional Buttons Visibility', () => {
            it('should show Delete button when categories are selected', () => {
                spectator.detectChanges();
                const deleteBtn = spectator.query(byTestId('category-delete-btn'));

                expect(deleteBtn).toBeTruthy();
            });

            it('should hide Delete button when no categories are selected', () => {
                (store.selectedCategories as jest.Mock).mockReturnValue([]);
                spectator.detectChanges();

                const deleteBtn = spectator.query(byTestId('category-delete-btn'));

                expect(deleteBtn).toBeFalsy();

                (store.selectedCategories as jest.Mock).mockReturnValue(MOCK_CATEGORIES);
            });
        });

        describe('Button Actions', () => {
            it('should call confirmDelete when Delete button clicked', () => {
                spectator.detectChanges();
                const spy = jest.spyOn(spectator.component, 'confirmDelete');
                const btnHost = spectator.query(byTestId('category-delete-btn'));
                expect(btnHost).toBeTruthy();
                const button = btnHost?.querySelector('button');
                expect(button).toBeTruthy();
                spectator.click(button!);
                expect(spy).toHaveBeenCalled();
            });
        });
    });

    describe('Row Click', () => {
        it('should call navigateToChildren when row clicked', () => {
            spectator.detectChanges();
            spectator.component.onRowClick(MOCK_CATEGORIES[0]);
            expect(store.navigateToChildren).toHaveBeenCalledWith(MOCK_CATEGORIES[0]);
        });

        it('should call navigateToChildren even when category has no children', () => {
            spectator.detectChanges();
            spectator.component.onRowClick(MOCK_CATEGORIES[1]);
            expect(store.navigateToChildren).toHaveBeenCalledWith(MOCK_CATEGORIES[1]);
        });
    });

    describe('Actions Menu', () => {
        it('should render actions button in each row', () => {
            spectator.detectChanges();
            const actionBtns = spectator.queryAll(byTestId('category-actions-btn'));
            expect(actionBtns.length).toBe(MOCK_CATEGORIES.length);
        });

        it('should set menu items and toggle menu on openRowMenu', () => {
            spectator.component.openRowMenu(new Event('click'), MOCK_CATEGORIES[0]);

            const items = spectator.component.rowMenuItems;
            expect(items).toHaveLength(3);
            expect(items[0].label).toBe('categories.edit');
            expect(items[1].label).toBe('categories.permissions');
            expect(items[2].label).toBe('categories.delete');
        });

        it('should call openEditDialog from edit menu item', () => {
            const spy = jest.spyOn(spectator.component, 'openEditDialog');
            spectator.component.openRowMenu(new Event('click'), MOCK_CATEGORIES[0]);
            spectator.component.rowMenuItems[0].command!({} as never);
            expect(spy).toHaveBeenCalledWith(MOCK_CATEGORIES[0]);
        });

        it('should call openPermissionsDialog from permissions menu item', () => {
            const spy = jest.spyOn(spectator.component, 'openPermissionsDialog');
            spectator.component.openRowMenu(new Event('click'), MOCK_CATEGORIES[0]);
            spectator.component.rowMenuItems[1].command!({} as never);
            expect(spy).toHaveBeenCalled();
        });

        it('should call confirmDeleteSingle from delete menu item', () => {
            const spy = jest.spyOn(spectator.component, 'confirmDeleteSingle');
            spectator.component.openRowMenu(new Event('click'), MOCK_CATEGORIES[0]);
            spectator.component.rowMenuItems[2].command!({} as never);
            expect(spy).toHaveBeenCalledWith(MOCK_CATEGORIES[0]);
        });
    });

    describe('confirmDeleteSingle', () => {
        it('should show confirmation dialog and delete on accept', () => {
            const confirmationService = spectator.inject(ConfirmationService, true);
            const confirmSpy = jest.spyOn(confirmationService, 'confirm');

            spectator.component.confirmDeleteSingle(MOCK_CATEGORIES[0]);

            expect(confirmSpy).toHaveBeenCalledWith(
                expect.objectContaining({
                    closable: true,
                    closeOnEscape: true
                })
            );

            const acceptFn = confirmSpy.mock.calls[0][0].accept as () => void;
            acceptFn();

            expect(store.setSelectedCategories).toHaveBeenCalledWith([MOCK_CATEGORIES[0]]);
            expect(store.deleteCategories).toHaveBeenCalled();
        });
    });

    describe('openPermissionsDialog', () => {
        it('should open dialog with permissions header', () => {
            const dialogService = spectator.inject(DialogService, true);
            const openSpy = jest.spyOn(dialogService, 'open').mockReturnValue(null as never);

            spectator.component.openPermissionsDialog();

            expect(openSpy).toHaveBeenCalledWith(
                expect.anything(),
                expect.objectContaining({
                    header: 'categories.permissions',
                    closable: true,
                    closeOnEscape: true
                })
            );
        });
    });

    describe('openCreateDialog', () => {
        it('should open dialog with closable and closeOnEscape options', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            const openSpy = jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as DynamicDialogRef);

            spectator.component.openCreateDialog();

            expect(openSpy).toHaveBeenCalledWith(
                expect.anything(),
                expect.objectContaining({
                    header: 'categories.add.category',
                    width: '500px',
                    data: { parentName: null },
                    closable: true,
                    closeOnEscape: true
                })
            );
        });

        it('should pass parentName to dialog when navigated into a parent', () => {
            (store.breadcrumbs as jest.Mock).mockReturnValue([
                { label: 'Parent Category', id: 'parent-inode' }
            ]);
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            const openSpy = jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as DynamicDialogRef);

            spectator.component.openCreateDialog();

            expect(openSpy).toHaveBeenCalledWith(
                expect.anything(),
                expect.objectContaining({
                    data: { parentName: 'Parent Category' }
                })
            );
            (store.breadcrumbs as jest.Mock).mockReturnValue([]);
        });

        it('should open dialog and call store.createCategory on close', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as DynamicDialogRef);

            spectator.component.openCreateDialog();
            onClose.next({ categoryName: 'New Category', key: 'new-cat' });
            onClose.complete();

            expect(store.createCategory).toHaveBeenCalledWith({
                categoryName: 'New Category',
                key: 'new-cat'
            });
        });

        it('should not call store.createCategory when dialog is cancelled', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as DynamicDialogRef);

            spectator.component.openCreateDialog();
            onClose.next(undefined);
            onClose.complete();

            expect(store.createCategory).not.toHaveBeenCalled();
        });
    });

    describe('openEditDialog', () => {
        it('should open dialog with closable and closeOnEscape options', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            const openSpy = jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as DynamicDialogRef);

            const category = MOCK_CATEGORIES[0];
            spectator.component.openEditDialog(category);

            expect(openSpy).toHaveBeenCalledWith(
                expect.anything(),
                expect.objectContaining({
                    header: 'categories.edit.category',
                    width: '500px',
                    data: { category },
                    closable: true,
                    closeOnEscape: true
                })
            );
        });

        it('should open dialog and call store.updateCategory on close', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as DynamicDialogRef);

            const category = MOCK_CATEGORIES[0];
            spectator.component.openEditDialog(category);

            onClose.next({ categoryName: 'Updated', key: 'updated-key' });
            onClose.complete();

            expect(store.updateCategory).toHaveBeenCalledWith({
                categoryName: 'Updated',
                key: 'updated-key',
                inode: 'inode-1'
            });
        });

        it('should not call store.updateCategory when dialog is cancelled', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as DynamicDialogRef);

            spectator.component.openEditDialog(MOCK_CATEGORIES[0]);
            onClose.next(undefined);
            onClose.complete();

            expect(store.updateCategory).not.toHaveBeenCalled();
        });
    });

    describe('Export Button', () => {
        it('should render Export button', () => {
            spectator.detectChanges();
            const btnHost = spectator.query(byTestId('category-export-btn'));
            expect(btnHost).toBeTruthy();
            const button = btnHost?.querySelector('button');
            expect(button).toBeTruthy();
            expect(button?.textContent).toContain('categories.export');
        });

        it('should call exportCategories when Export button clicked', () => {
            const spy = jest.spyOn(spectator.component, 'exportCategories');
            spectator.detectChanges();
            const btnHost = spectator.query(byTestId('category-export-btn'));
            const button = btnHost?.querySelector('button');
            spectator.click(button!);
            expect(spy).toHaveBeenCalled();
        });

        it('should call store.exportCategories', () => {
            spectator.component.exportCategories();
            expect(store.exportCategories).toHaveBeenCalled();
        });
    });

    describe('openImportDialog', () => {
        it('should open dialog with correct config', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            const openSpy = jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as DynamicDialogRef);

            spectator.component.openImportDialog();

            expect(openSpy).toHaveBeenCalledWith(
                expect.anything(),
                expect.objectContaining({
                    width: '500px',
                    data: { parentInode: null },
                    closable: true,
                    closeOnEscape: true
                })
            );
        });

        it('should call store.loadCategories when dialog closes with true', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as DynamicDialogRef);

            spectator.component.openImportDialog();
            onClose.next(true);
            onClose.complete();

            expect(store.loadCategories).toHaveBeenCalled();
        });

        it('should not call store.loadCategories when dialog closes with false', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as DynamicDialogRef);

            spectator.component.openImportDialog();
            onClose.next(false);
            onClose.complete();

            expect(store.loadCategories).not.toHaveBeenCalled();
        });
    });

    describe('confirmDelete', () => {
        it('should show confirmation dialog with closable and closeOnEscape options', () => {
            const confirmationService = spectator.inject(ConfirmationService, true);
            const confirmSpy = jest.spyOn(confirmationService, 'confirm');

            spectator.component.confirmDelete();

            expect(confirmSpy).toHaveBeenCalledWith(
                expect.objectContaining({
                    message: 'categories.confirm.delete.message',
                    header: 'categories.confirm.delete.header',
                    defaultFocus: 'reject',
                    closable: true,
                    closeOnEscape: true
                })
            );
        });

        it('should call store.deleteCategories on accept', () => {
            const confirmationService = spectator.inject(ConfirmationService, true);
            const confirmSpy = jest.spyOn(confirmationService, 'confirm');

            spectator.component.confirmDelete();

            const acceptFn = confirmSpy.mock.calls[0][0].accept as () => void;
            acceptFn();

            expect(store.deleteCategories).toHaveBeenCalled();
        });
    });
});
