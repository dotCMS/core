import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { Subject } from 'rxjs';
import { Mock, vi } from 'vitest';

import { ConfirmationService, MenuItemCommandEvent } from 'primeng/api';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageDisplayService, DotMessageService } from '@dotcms/data-access';
import { DotMessageSeverity, DotMessageType, DotTag } from '@dotcms/dotcms-models';
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
            value: vi.fn().mockImplementation((query) => ({
                matches: false,
                media: query,
                onchange: null,
                addListener: vi.fn(),
                removeListener: vi.fn(),
                addEventListener: vi.fn(),
                removeEventListener: vi.fn(),
                dispatchEvent: vi.fn()
            }))
        });
    });

    const createComponent = createComponentFactory({
        component: DotTagsListComponent,
        componentProviders: [
            mockProvider(DotTagsListStore, {
                tags: vi.fn().mockReturnValue(MOCK_TAGS),
                selectedTags: vi.fn().mockReturnValue(MOCK_TAGS),
                showExportAll: vi.fn().mockReturnValue(false),
                filter: vi.fn().mockReturnValue(''),
                showGlobal: vi.fn().mockReturnValue(false),
                page: vi.fn().mockReturnValue(1),
                rows: vi.fn().mockReturnValue(25),
                totalRecords: vi.fn().mockReturnValue(100),
                status: vi.fn().mockReturnValue('loaded'),
                sortField: vi.fn().mockReturnValue('tagname'),
                sortOrder: vi.fn().mockReturnValue('ASC'),
                setFilter: vi.fn(),
                setShowGlobal: vi.fn(),
                setPagination: vi.fn(),
                setSort: vi.fn(),
                setSelectedTags: vi.fn(),
                createTag: vi.fn(),
                updateTag: vi.fn(),
                deleteTags: vi.fn(),
                exportSelected: vi.fn(),
                exportAll: vi.fn(),
                loadTags: vi.fn()
            }),
            mockProvider(DialogService),
            mockProvider(DotMessageDisplayService),
            ConfirmationService
        ],
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'tags.export': 'Export',
                    'tags.export.all': 'Export All',
                    'tags.export.selected': 'Export Selected',
                    'tags.empty.state.title': 'No tags yet',
                    'tags.empty.state.description': 'Create a tag to get started.',
                    'tags.delete': 'Delete',
                    'tags.cancel': 'Cancel',
                    'tags.confirm.delete.message': 'tags.confirm.delete.message',
                    'tags.confirm.delete.header': 'tags.confirm.delete.header',
                    'tags.import.success': 'Imported {0} tags successfully.',
                    'tags.import.partial-success': 'Imported {0} of {1} tags. {2} failed.',
                    'tags.show.global': 'Show Global Tags'
                })
            }
        ]
    });

    beforeEach(() => {
        vi.useFakeTimers();
        spectator = createComponent();
        store = spectator.inject(DotTagsListStore, true);
        vi.clearAllMocks();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    describe('Search', () => {
        it('should debounce search by 300ms', () => {
            spectator.component.onSearch('test');
            vi.advanceTimersByTime(299);
            expect(store.setFilter).not.toHaveBeenCalled();

            vi.advanceTimersByTime(1);
            expect(store.setFilter).toHaveBeenCalledWith('test');
        });

        it('should reset debounce timer on rapid typing', () => {
            spectator.component.onSearch('a');
            vi.advanceTimersByTime(100);
            spectator.component.onSearch('ab');
            vi.advanceTimersByTime(100);
            spectator.component.onSearch('abc');
            vi.advanceTimersByTime(300);

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

    describe('Table layout', () => {
        it('should have table wrapper for sticky pagination', () => {
            expect(spectator.query(byTestId('tags-table-wrapper'))).toBeTruthy();
        });
    });

    describe('Empty and loading state', () => {
        it('should show loading skeleton rows in body when status is loading and tags exist (e.g. pagination)', () => {
            (store.status as Mock).mockReturnValue('loading');
            (store.tags as Mock).mockReturnValue(MOCK_TAGS);
            spectator.detectChanges();

            const loadingRows = spectator.queryAll(byTestId('tags-loading-row'));
            expect(loadingRows.length).toBe(MOCK_TAGS.length);
            expect(spectator.queryAll('p-skeleton').length).toBeGreaterThan(0);

            // Restore defaults for subsequent tests
            (store.status as Mock).mockReturnValue('loaded');
        });

        it('should show empty state when no tags (emptymessage)', () => {
            (store.tags as Mock).mockReturnValue([]);
            (store.selectedTags as Mock).mockReturnValue([]);
            (store.status as Mock).mockReturnValue('loaded');
            spectator.detectChanges();

            const emptyState = spectator.query(byTestId('tags-empty-state'));
            expect(emptyState).toBeTruthy();
            expect(emptyState?.textContent).toContain('No tags yet');
            expect(emptyState?.textContent).toContain('Create a tag to get started.');

            // Restore defaults for subsequent tests
            (store.tags as Mock).mockReturnValue(MOCK_TAGS);
            (store.selectedTags as Mock).mockReturnValue(MOCK_TAGS);
        });
    });

    describe('Button Interactions', () => {
        describe('Split Button', () => {
            it('should render split button with Add Tag label', () => {
                (store.selectedTags as Mock).mockReturnValue([]);
                spectator.detectChanges();
                const btnHost = spectator.query(byTestId('tag-add-split-btn'));
                expect(btnHost).toBeTruthy();
                const button = btnHost?.querySelector('button');
                expect(button).toBeTruthy();
                expect(button?.textContent).toContain('add');
            });

            it('should have Import option in dropdown menu', () => {
                const menuItems = spectator.component.addTagMenuItems;
                expect(menuItems).toHaveLength(1);
                expect(menuItems[0].label).toBe('tags.import');
            });

            it('should call openCreateDialog when split button main action clicked', () => {
                (store.selectedTags as Mock).mockReturnValue([]);
                spectator.detectChanges();
                const spy = vi.spyOn(spectator.component, 'openCreateDialog');
                const btnHost = spectator.query(byTestId('tag-add-split-btn'));
                const button = btnHost?.querySelector('button');
                spectator.click(button!);
                expect(spy).toHaveBeenCalled();
            });

            it('should call openImportDialog when Import menu item is clicked', () => {
                const spy = vi.spyOn(spectator.component, 'openImportDialog');
                const menuItem = spectator.component.addTagMenuItems[0];
                menuItem.command?.({} as unknown as MenuItemCommandEvent);
                expect(spy).toHaveBeenCalled();
            });
        });

        describe('Conditional Buttons Visibility', () => {
            it('should hide Delete and Export split-button when nothing is selected', () => {
                (store.selectedTags as Mock).mockReturnValue([]);
                spectator = createComponent();
                store = spectator.inject(DotTagsListStore, true);
                spectator.detectChanges();
                expect(spectator.query(byTestId('tag-delete-btn'))).toBeFalsy();
                expect(spectator.query(byTestId('tag-export-split-btn'))).toBeFalsy();
            });

            it('should show Delete and Export split-button when tags are selected', () => {
                (store.selectedTags as Mock).mockReturnValue([MOCK_TAGS[0]]);
                spectator = createComponent();
                store = spectator.inject(DotTagsListStore, true);
                spectator.detectChanges();
                expect(spectator.query(byTestId('tag-delete-btn'))).toBeTruthy();
                expect(spectator.query(byTestId('tag-export-split-btn'))).toBeTruthy();
            });

            it('should show the Add split button regardless of selection', () => {
                (store.selectedTags as Mock).mockReturnValue([]);
                spectator = createComponent();
                store = spectator.inject(DotTagsListStore, true);
                spectator.detectChanges();
                expect(spectator.query(byTestId('tag-add-split-btn'))).toBeTruthy();

                (store.selectedTags as Mock).mockReturnValue(MOCK_TAGS);
                spectator = createComponent();
                store = spectator.inject(DotTagsListStore, true);
                spectator.detectChanges();
                expect(spectator.query(byTestId('tag-add-split-btn'))).toBeTruthy();
            });
        });

        describe('Button Actions', () => {
            it('should call confirmDelete when Delete button clicked', () => {
                (store.selectedTags as Mock).mockReturnValue(MOCK_TAGS);
                spectator = createComponent();
                store = spectator.inject(DotTagsListStore, true);
                spectator.detectChanges();
                const spy = vi.spyOn(spectator.component, 'confirmDelete');
                const btnHost = spectator.query(byTestId('tag-delete-btn'));
                expect(btnHost).toBeTruthy();
                const button = btnHost?.querySelector('button');
                expect(button).toBeTruthy();
                spectator.click(button!);
                expect(spy).toHaveBeenCalled();
            });

            it('should call store.exportSelected when Export split-button main action clicked', () => {
                (store.selectedTags as Mock).mockReturnValue(MOCK_TAGS);
                spectator = createComponent();
                store = spectator.inject(DotTagsListStore, true);
                spectator.detectChanges();
                const mainButton = spectator
                    .query(byTestId('tag-export-split-btn'))
                    ?.querySelector('button');
                expect(mainButton).toBeTruthy();
                spectator.click(mainButton!);
                expect(store.exportSelected).toHaveBeenCalled();
            });
        });

        describe('Export menu items', () => {
            it('should expose Export Selected and Export All entries', () => {
                expect(spectator.component.$exportMenuItems()).toHaveLength(2);
                expect(spectator.component.$exportMenuItems()[0].label).toBe('Export Selected');
                expect(spectator.component.$exportMenuItems()[1].label).toBe('Export All');
            });

            it('should disable Export All when showExportAll is false', () => {
                (store.showExportAll as Mock).mockReturnValue(false);
                spectator = createComponent();
                expect(spectator.component.$exportMenuItems()[1].disabled).toBe(true);
            });

            it('should enable Export All when showExportAll is true', () => {
                (store.showExportAll as Mock).mockReturnValue(true);
                spectator = createComponent();
                expect(spectator.component.$exportMenuItems()[1].disabled).toBe(false);
            });

            it('should invoke store.exportSelected when Export Selected menu item runs', () => {
                spectator.component
                    .$exportMenuItems()[0]
                    .command?.({} as unknown as MenuItemCommandEvent);
                expect(store.exportSelected).toHaveBeenCalled();
            });

            it('should invoke store.exportAll when Export All menu item runs', () => {
                spectator.component
                    .$exportMenuItems()[1]
                    .command?.({} as unknown as MenuItemCommandEvent);
                expect(store.exportAll).toHaveBeenCalled();
            });
        });

        describe('Show Global Tags toggle', () => {
            it('should render the Show Global Tags checkbox with label', () => {
                spectator.detectChanges();
                expect(spectator.query(byTestId('tag-show-global-checkbox'))).toBeTruthy();
                expect(spectator.query('label[for="show-global-tags"]')?.textContent).toContain(
                    'Show Global Tags'
                );
            });

            it('should call store.setShowGlobal when checkbox is toggled', () => {
                spectator.detectChanges();
                const input = spectator
                    .query(byTestId('tag-show-global-checkbox'))
                    ?.querySelector('input[type="checkbox"]') as HTMLInputElement | null;
                expect(input).toBeTruthy();
                input!.click();
                spectator.detectChanges();
                expect(store.setShowGlobal).toHaveBeenCalledWith(true);
            });
        });
    });

    describe('Row Click', () => {
        it('should call openEditDialog when tag row clicked', () => {
            const spy = vi.spyOn(spectator.component, 'openEditDialog');
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
            const openSpy = vi.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as unknown as DynamicDialogRef);

            spectator.component.openCreateDialog();

            expect(openSpy).toHaveBeenCalledWith(
                expect.anything(),
                expect.objectContaining({
                    header: 'tags.add.tag',
                    width: '700px',
                    closable: true,
                    closeOnEscape: true,
                    draggable: false,
                    position: 'center'
                })
            );
        });

        it('should open dialog and call store.createTag on close', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            vi.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as unknown as DynamicDialogRef);

            spectator.component.openCreateDialog();
            onClose.next({ name: 'new-tag', siteId: 'site1' });
            onClose.complete();

            expect(store.createTag).toHaveBeenCalledWith({ name: 'new-tag', siteId: 'site1' });
        });

        it('should not call store.createTag when dialog is cancelled', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            vi.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as unknown as DynamicDialogRef);

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
            const openSpy = vi.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as unknown as DynamicDialogRef);

            const tag = MOCK_TAGS[0];
            spectator.component.openEditDialog(tag);

            expect(openSpy).toHaveBeenCalledWith(
                expect.anything(),
                expect.objectContaining({
                    header: 'tags.edit.tag',
                    width: '700px',
                    data: { tag },
                    closable: true,
                    closeOnEscape: true,
                    draggable: false,
                    position: 'center'
                })
            );
        });

        it('should open dialog with tag data and call store.updateTag on close', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            const openSpy = vi.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as unknown as DynamicDialogRef);

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
            vi.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as unknown as DynamicDialogRef);

            spectator.component.openEditDialog(MOCK_TAGS[0]);
            onClose.next(undefined);
            onClose.complete();

            expect(store.updateTag).not.toHaveBeenCalled();
        });
    });

    describe('confirmDelete', () => {
        it('should show confirmation dialog with closable and closeOnEscape options', () => {
            const confirmationService = spectator.inject(ConfirmationService, true);
            const confirmSpy = vi.spyOn(confirmationService, 'confirm');

            spectator.component.confirmDelete();

            expect(confirmSpy).toHaveBeenCalledWith(
                expect.objectContaining({
                    message: 'tags.confirm.delete.message',
                    header: 'tags.confirm.delete.header',
                    acceptLabel: 'Delete',
                    rejectLabel: 'Cancel',
                    defaultFocus: 'reject',
                    closable: true,
                    closeOnEscape: true,
                    position: 'center'
                })
            );
        });

        it('should show confirmation dialog and call store.deleteTags on accept', () => {
            const confirmationService = spectator.inject(ConfirmationService, true);
            const confirmSpy = vi.spyOn(confirmationService, 'confirm');

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
            const openSpy = vi.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as unknown as DynamicDialogRef);

            spectator.component.openImportDialog();

            expect(openSpy).toHaveBeenCalledWith(
                expect.anything(),
                expect.objectContaining({
                    header: 'tags.import.header',
                    width: '700px',
                    closable: true,
                    closeOnEscape: true,
                    draggable: false,
                    position: 'center'
                })
            );
        });

        it('should open import dialog and call store.loadTags on close', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            vi.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as unknown as DynamicDialogRef);

            spectator.component.openImportDialog();
            onClose.next({ successCount: 5, failureCount: 0, totalRows: 5 });
            onClose.complete();

            expect(store.loadTags).toHaveBeenCalled();
        });

        it('should not call store.loadTags when import dialog is cancelled', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            vi.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as unknown as DynamicDialogRef);

            spectator.component.openImportDialog();
            onClose.next(undefined);
            onClose.complete();

            expect(store.loadTags).not.toHaveBeenCalled();
        });

        it('should push a SUCCESS message when all tags imported successfully', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            vi.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as unknown as DynamicDialogRef);
            const displayService = spectator.inject(DotMessageDisplayService, true);

            spectator.component.openImportDialog();
            onClose.next({ successCount: 5, failureCount: 0, totalRows: 5 });
            onClose.complete();

            expect(displayService.push).toHaveBeenCalledWith(
                expect.objectContaining({
                    life: 5000,
                    severity: DotMessageSeverity.SUCCESS,
                    type: DotMessageType.SIMPLE_MESSAGE
                })
            );
        });

        it('should push a WARNING message when import has failures', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            vi.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as unknown as DynamicDialogRef);
            const displayService = spectator.inject(DotMessageDisplayService, true);

            spectator.component.openImportDialog();
            onClose.next({ successCount: 3, failureCount: 2, totalRows: 5 });
            onClose.complete();

            expect(displayService.push).toHaveBeenCalledWith(
                expect.objectContaining({
                    life: 5000,
                    severity: DotMessageSeverity.WARNING,
                    type: DotMessageType.SIMPLE_MESSAGE
                })
            );
        });

        it('should not push a message when import dialog is cancelled', () => {
            const onClose = new Subject<unknown>();
            const dialogService = spectator.inject(DialogService, true);
            vi.spyOn(dialogService, 'open').mockReturnValue({
                onClose
            } as unknown as DynamicDialogRef);
            const displayService = spectator.inject(DotMessageDisplayService, true);

            spectator.component.openImportDialog();
            onClose.next(undefined);
            onClose.complete();

            expect(displayService.push).not.toHaveBeenCalled();
        });
    });
});
