import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { Subject } from 'rxjs';

import { ConfirmationService } from 'primeng/api';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotTag } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotTagsListComponent } from './dot-tags-list.component';
import { DotTagsListStore } from './store/dot-tags-list.store';

const MOCK_TAGS: DotTag[] = [
    { id: '1', label: 'tag1', siteId: 'site1', siteName: 'Site 1', persona: false },
    { id: '2', label: 'tag2', siteId: 'site2', siteName: 'Site 2', persona: false }
];

describe('DotTagsListComponent', () => {
    let spectator: Spectator<DotTagsListComponent>;
    let store: InstanceType<typeof DotTagsListStore>;

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
        component: DotTagsListComponent,
        componentProviders: [
            mockProvider(DotTagsListStore, {
                tags: jest.fn().mockReturnValue(MOCK_TAGS),
                selectedTags: jest.fn().mockReturnValue(MOCK_TAGS),
                filter: jest.fn().mockReturnValue(''),
                page: jest.fn().mockReturnValue(1),
                rows: jest.fn().mockReturnValue(25),
                totalRecords: jest.fn().mockReturnValue(100),
                status: jest.fn().mockReturnValue('loaded'),
                sortField: jest.fn().mockReturnValue('tagname'),
                sortOrder: jest.fn().mockReturnValue('ASC'),
                setFilter: jest.fn(),
                setPagination: jest.fn(),
                setSort: jest.fn(),
                setSelectedTags: jest.fn(),
                createTag: jest.fn(),
                updateTag: jest.fn(),
                deleteTags: jest.fn(),
                exportSelectedTags: jest.fn(),
                loadTags: jest.fn()
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
        store = spectator.inject(DotTagsListStore, true);
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
                sortField: 'tagname',
                sortOrder: 1
            });

            expect(store.setPagination).toHaveBeenCalledWith(3, 25);
            expect(store.setSort).toHaveBeenCalledWith('tagname', 'ASC');
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
                sortField: 'tagname',
                sortOrder: -1
            });

            expect(store.setSort).toHaveBeenCalledWith('tagname', 'DESC');
        });
    });

    describe('Button Interactions', () => {
        describe('Split Button', () => {
            it('should render split button with Add Tag label', () => {
                spectator.detectChanges();
                const btnHost = spectator.query(byTestId('tag-add-split-btn'));
                expect(btnHost).toBeTruthy();
                const button = btnHost?.querySelector('button');
                expect(button).toBeTruthy();
                expect(button?.textContent).toContain('tags.add.tag');
            });

            it('should have Import option in dropdown menu', () => {
                const menuItems = spectator.component.addTagMenuItems;
                expect(menuItems).toHaveLength(1);
                expect(menuItems[0].label).toBe('tags.import');
                expect(menuItems[0].icon).toBe('pi pi-upload');
            });

            it('should call openCreateDialog when split button main action clicked', () => {
                const spy = jest.spyOn(spectator.component, 'openCreateDialog');
                const btnHost = spectator.query(byTestId('tag-add-split-btn'));
                const button = btnHost?.querySelector('button');
                spectator.click(button!);
                expect(spy).toHaveBeenCalled();
            });

            it('should call openImportDialog when Import menu item is clicked', () => {
                const spy = jest.spyOn(spectator.component, 'openImportDialog');
                const menuItem = spectator.component.addTagMenuItems[0];
                menuItem.command?.();
                expect(spy).toHaveBeenCalled();
            });
        });

        describe('Conditional Buttons Visibility', () => {
            it('should show Delete and Export buttons when tags are selected', () => {
                spectator.detectChanges();
                const deleteBtn = spectator.query(byTestId('tag-delete-btn'));
                const exportBtn = spectator.query(byTestId('tag-export-btn'));

                expect(deleteBtn).toBeTruthy();
                expect(exportBtn).toBeTruthy();
            });

            it('should hide Delete and Export buttons when no tags are selected', () => {
                (store.selectedTags as jest.Mock).mockReturnValue([]);
                spectator.detectChanges();

                const deleteBtn = spectator.query(byTestId('tag-delete-btn'));
                const exportBtn = spectator.query(byTestId('tag-export-btn'));

                expect(deleteBtn).toBeFalsy();
                expect(exportBtn).toBeFalsy();

                // Restore mock for subsequent tests
                (store.selectedTags as jest.Mock).mockReturnValue(MOCK_TAGS);
            });

            it('should show buttons when selection changes from 0 to 1', () => {
                // Recreate component with no selection
                (store.selectedTags as jest.Mock).mockReturnValue([]);
                spectator = createComponent();
                store = spectator.inject(DotTagsListStore, true);
                spectator.detectChanges();
                expect(spectator.query(byTestId('tag-delete-btn'))).toBeFalsy();
                expect(spectator.query(byTestId('tag-export-btn'))).toBeFalsy();

                // Recreate component with 1 item selected
                (store.selectedTags as jest.Mock).mockReturnValue([MOCK_TAGS[0]]);
                spectator = createComponent();
                store = spectator.inject(DotTagsListStore, true);
                spectator.detectChanges();
                expect(spectator.query(byTestId('tag-delete-btn'))).toBeTruthy();
                expect(spectator.query(byTestId('tag-export-btn'))).toBeTruthy();
            });

            it('should show buttons when multiple tags are selected', () => {
                // Ensure mock returns 2 items
                (store.selectedTags as jest.Mock).mockReturnValue(MOCK_TAGS);
                spectator.detectChanges();

                const deleteBtn = spectator.query(byTestId('tag-delete-btn'));
                const exportBtn = spectator.query(byTestId('tag-export-btn'));

                expect(deleteBtn).toBeTruthy();
                expect(exportBtn).toBeTruthy();
                expect(store.selectedTags().length).toBe(2);
            });
        });

        describe('Button Actions', () => {
            it('should call confirmDelete when Delete button clicked', () => {
                spectator.detectChanges();
                const spy = jest.spyOn(spectator.component, 'confirmDelete');
                const btnHost = spectator.query(byTestId('tag-delete-btn'));
                expect(btnHost).toBeTruthy();
                const button = btnHost?.querySelector('button');
                expect(button).toBeTruthy();
                spectator.click(button!);
                expect(spy).toHaveBeenCalled();
            });

            it('should call exportSelectedTags when Export button clicked', () => {
                spectator.detectChanges();
                const btnHost = spectator.query(byTestId('tag-export-btn'));
                expect(btnHost).toBeTruthy();
                const button = btnHost?.querySelector('button');
                expect(button).toBeTruthy();
                spectator.click(button!);
                expect(store.exportSelectedTags).toHaveBeenCalled();
            });
        });
    });

    describe('Row Click', () => {
        it('should call openEditDialog when tag row clicked', () => {
            const spy = jest.spyOn(spectator.component, 'openEditDialog');
            spectator.detectChanges();
            const row = spectator.query(byTestId('tag-row'));
            spectator.click(row!);
            expect(spy).toHaveBeenCalled();
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
                    header: 'tags.add.tag',
                    width: '400px',
                    closable: true,
                    closeOnEscape: true
                })
            );
        });

        it('should open dialog and call store.createTag on close', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as DynamicDialogRef);

            spectator.component.openCreateDialog();
            onClose.next({ name: 'new-tag', siteId: 'site1' });
            onClose.complete();

            expect(store.createTag).toHaveBeenCalledWith({ name: 'new-tag', siteId: 'site1' });
        });

        it('should not call store.createTag when dialog is cancelled', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as DynamicDialogRef);

            spectator.component.openCreateDialog();
            onClose.next(undefined);
            onClose.complete();

            expect(store.createTag).not.toHaveBeenCalled();
        });
    });

    describe('openEditDialog', () => {
        it('should open dialog with closable and closeOnEscape options', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            const openSpy = jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as DynamicDialogRef);

            const tag = MOCK_TAGS[0];
            spectator.component.openEditDialog(tag);

            expect(openSpy).toHaveBeenCalledWith(
                expect.anything(),
                expect.objectContaining({
                    header: 'tags.edit.tag',
                    width: '400px',
                    data: { tag },
                    closable: true,
                    closeOnEscape: true
                })
            );
        });

        it('should open dialog with tag data and call store.updateTag on close', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            const openSpy = jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as DynamicDialogRef);

            const tag = MOCK_TAGS[0];
            spectator.component.openEditDialog(tag);

            expect(openSpy).toHaveBeenCalledWith(
                expect.anything(),
                expect.objectContaining({
                    header: 'tags.edit.tag',
                    data: { tag }
                })
            );

            onClose.next({ name: 'updated-tag', siteId: 'site1' });
            onClose.complete();

            expect(store.updateTag).toHaveBeenCalledWith(tag, {
                name: 'updated-tag',
                siteId: 'site1'
            });
        });

        it('should not call store.updateTag when dialog is cancelled', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as DynamicDialogRef);

            spectator.component.openEditDialog(MOCK_TAGS[0]);
            onClose.next(undefined);
            onClose.complete();

            expect(store.updateTag).not.toHaveBeenCalled();
        });
    });

    describe('confirmDelete', () => {
        it('should show confirmation dialog with closable and closeOnEscape options', () => {
            const confirmationService = spectator.inject(ConfirmationService, true);
            const confirmSpy = jest.spyOn(confirmationService, 'confirm');

            spectator.component.confirmDelete();

            expect(confirmSpy).toHaveBeenCalledWith(
                expect.objectContaining({
                    message: 'tags.confirm.delete.message',
                    header: 'tags.confirm.delete.header',
                    defaultFocus: 'reject',
                    closable: true,
                    closeOnEscape: true
                })
            );
        });

        it('should show confirmation dialog and call store.deleteTags on accept', () => {
            const confirmationService = spectator.inject(ConfirmationService, true);
            const confirmSpy = jest.spyOn(confirmationService, 'confirm');

            spectator.component.confirmDelete();

            expect(confirmSpy).toHaveBeenCalledWith(
                expect.objectContaining({
                    message: 'tags.confirm.delete.message',
                    header: 'tags.confirm.delete.header',
                    defaultFocus: 'reject'
                })
            );

            const acceptFn = confirmSpy.mock.calls[0][0].accept as () => void;
            acceptFn();

            expect(store.deleteTags).toHaveBeenCalled();
        });
    });

    describe('openImportDialog', () => {
        it('should open import dialog with closable and closeOnEscape options', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            const openSpy = jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as DynamicDialogRef);

            spectator.component.openImportDialog();

            expect(openSpy).toHaveBeenCalledWith(
                expect.anything(),
                expect.objectContaining({
                    header: 'tags.import.header',
                    width: '500px',
                    closable: true,
                    closeOnEscape: true
                })
            );
        });

        it('should open import dialog and call store.loadTags on close', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as DynamicDialogRef);

            spectator.component.openImportDialog();
            onClose.next(true);
            onClose.complete();

            expect(store.loadTags).toHaveBeenCalled();
        });

        it('should not call store.loadTags when import dialog is cancelled', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            jest.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as DynamicDialogRef);

            spectator.component.openImportDialog();
            onClose.next(undefined);
            onClose.complete();

            expect(store.loadTags).not.toHaveBeenCalled();
        });
    });
});
