import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { By } from '@angular/platform-browser';

import { DotFormatDateService, DotLanguagesService, DotMessageService } from '@dotcms/data-access';
import { DotcmsConfigService } from '@dotcms/dotcms-js';
import { DotLanguage } from '@dotcms/dotcms-models';
import { DotcmsConfigServiceMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotFolderListViewComponent } from './dot-folder-list-view.component';

import { DOT_DRAG_ITEM, HEADER_COLUMNS } from '../shared/constants';
import { mockItems } from '../shared/mocks';

// Mock DragEvent since it's not available in Jest environment
class DragEventMock extends Event {
    override preventDefault = jest.fn();
    override stopPropagation = jest.fn();
    dataTransfer: {
        effectAllowed?: string;
        setData?: ReturnType<typeof jest.fn>;
        setDragImage?: ReturnType<typeof jest.fn>;
    } | null = null;

    constructor(type: string) {
        super(type);
        this.dataTransfer = {
            effectAllowed: '',
            setData: jest.fn(),
            setDragImage: jest.fn()
        };
    }
}

// Override global DragEvent with our mock
(global as unknown as { DragEvent: typeof DragEventMock }).DragEvent = DragEventMock;

// Helper function to create properly mocked drag event
function createDragStartEvent(): DragEvent {
    return new DragEvent('dragstart');
}

const mockLanguages: DotLanguage[] = [
    {
        id: 1,
        language: 'English',
        languageCode: 'en',
        countryCode: 'US',
        country: 'United States'
    },
    {
        id: 2,
        language: 'Spanish',
        languageCode: 'es',
        countryCode: 'ES',
        country: 'Spain'
    }
];

describe('DotFolderListViewComponent', () => {
    let spectator: Spectator<DotFolderListViewComponent>;

    const createComponent = createComponentFactory({
        component: DotFolderListViewComponent,
        imports: [],
        providers: [
            mockProvider(DotMessageService, new MockDotMessageService({})),
            mockProvider(DotcmsConfigService, new DotcmsConfigServiceMock()),
            mockProvider(DotFormatDateService),
            mockProvider(DotLanguagesService, {
                get: jest.fn(() => of(mockLanguages))
            }),
            provideHttpClient()
        ],
        declarations: [],
        detectChanges: true
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    describe('Input Properties', () => {
        it('should set items input property', () => {
            spectator.setInput('items', mockItems);

            expect(spectator.component.$items()).toEqual(mockItems);
        });

        it('should set totalItems input property', () => {
            const mockTotalItems = 10;

            spectator.setInput('totalItems', mockTotalItems);

            expect(spectator.component.$totalItems()).toEqual(mockTotalItems);
        });

        it('should set loading input property', () => {
            spectator.setInput('loading', true);

            expect(spectator.component.$loading()).toBe(true);
        });
    });

    describe('Languages Service', () => {
        it('should call languages service on init', () => {
            const languagesService = spectator.inject(DotLanguagesService);

            expect(languagesService.get).toHaveBeenCalled();
        });

        it('should populate languagesMap with languages from service', () => {
            const languagesMap = spectator.component.state.languagesMap();

            expect(languagesMap.size).toBe(2);
            expect(languagesMap.get(1)).toEqual(mockLanguages[0]);
            expect(languagesMap.get(2)).toEqual(mockLanguages[1]);
        });

        it('should convert languages array to Map with id as key', () => {
            const languagesMap = spectator.component.state.languagesMap();

            expect(languagesMap.get(1)?.language).toBe('English');
            expect(languagesMap.get(1)?.languageCode).toBe('en');
            expect(languagesMap.get(2)?.language).toBe('Spanish');
            expect(languagesMap.get(2)?.languageCode).toBe('es');
        });
    });

    describe('Output Properties', () => {
        it('should emit selectionChange event when selection changes', () => {
            spectator.setInput('items', mockItems);

            const selectionChangeSpy = jest.spyOn(spectator.component.selectionChange, 'emit');
            const table = spectator.debugElement.query(By.css('[data-testId="table"]'));

            spectator.triggerEventHandler(table, 'selectionChange', mockItems);

            expect(selectionChangeSpy).toHaveBeenCalledWith(mockItems);
        });

        it('should emit paginate event when page changes', () => {
            const paginateSpy = jest.spyOn(spectator.component.paginate, 'emit');
            const table = spectator.debugElement.query(By.css('[data-testId="table"]'));

            spectator.setInput('loading', false);

            spectator.triggerEventHandler(table, 'onPage', { first: 10, rows: 10 });

            expect(paginateSpy).toHaveBeenCalledWith({ first: 10, rows: 10 });
        });

        it('should emit sort event when sort changes', () => {
            const sortSpy = jest.spyOn(spectator.component.sort, 'emit');
            const table = spectator.debugElement.query(By.css('[data-testId="table"]'));

            spectator.triggerEventHandler(table, 'onSort', { field: 'title', order: 1 });

            expect(sortSpy).toHaveBeenCalledWith({ field: 'title', order: 1 });
        });
    });

    describe('DOM', () => {
        it('should show the table', () => {
            const table = spectator.query(byTestId('table'));

            expect(table).toBeTruthy();
        });

        describe('Header', () => {
            it('should show the header', () => {
                const header = spectator.query(byTestId('header-row'));

                expect(header).toBeTruthy();
            });

            it('should show sortable columns with sort icon', () => {
                const sortableColumnsCount = HEADER_COLUMNS.filter((col) => col.sortable).length;
                const sortableColumns = spectator.queryAll(byTestId('header-column-sortable'));
                const sortIcons = spectator.queryAll(byTestId('sort-icon'));

                expect(sortableColumns.length).toBe(sortableColumnsCount);
                expect(sortIcons.length).toBe(sortableColumnsCount);
            });

            it('should show not sortable columns', () => {
                const notSortableColumnsCount = HEADER_COLUMNS.filter(
                    (col) => !col.sortable
                ).length;
                const notSortableColumns = spectator.queryAll(
                    byTestId('header-column-not-sortable')
                );

                expect(notSortableColumns.length).toBe(notSortableColumnsCount);
            });

            it('should have one checkbox column', () => {
                const checkboxColumn = spectator.query(byTestId('header-checkbox'));

                expect(checkboxColumn).toBeTruthy();
            });

            it('should have a checkbox column', () => {
                const checkboxColumn = spectator.query(byTestId('header-checkbox'));

                expect(checkboxColumn).toBeTruthy();
            });
        });
    });

    describe('Styles and Pagination', () => {
        it('should have empty-table class when items list is empty', () => {
            spectator.setInput('items', []);
            spectator.setInput('totalItems', 0);
            spectator.detectChanges();

            // Verify the styleClass computed signal contains 'empty-table'
            // PrimeNG applies styleClass to the p-table component's root element
            // We need to find the element that has the styleClass applied
            const tableElement = spectator.query(byTestId('table'));
            // PrimeNG may apply the class to a parent wrapper, so we check the element or its parent
            const elementWithClass =
                tableElement?.closest('.empty-table') ||
                spectator.query('.empty-table') ||
                (tableElement?.parentElement?.classList.contains('empty-table')
                    ? tableElement.parentElement
                    : null);

            expect(elementWithClass).toBeTruthy();
        });

        it('should not show pagination when there are 20 or fewer total items', () => {
            spectator.setInput('totalItems', 20);
            spectator.detectChanges();

            // Verify pagination is not shown by checking that paginator element doesn't exist
            // or by verifying the styleClass doesn't indicate pagination
            const paginator = spectator.query('.p-paginator');
            expect(paginator).toBeFalsy();
        });

        it('should set first value when calling onPage', () => {
            spectator.setInput('totalItems', 50); // Enable pagination
            spectator.detectChanges();

            const mockEvent = { first: 20, rows: 20 };
            spectator.component.onPage(mockEvent);
            spectator.detectChanges();

            expect(spectator.component.state.currentPageFirstRowIndex()).toBe(20);
        });

        it('should reset first to 0 when showPagination becomes true', () => {
            // Start with no pagination (totalItems <= MIN_ROWS_PER_PAGE)
            spectator.setInput('totalItems', 15);
            spectator.detectChanges();

            // Set first to some value
            const mockEvent = { first: 20, rows: 20 };
            spectator.component.onPage(mockEvent);
            spectator.detectChanges();

            // Now enable pagination by setting totalItems > MIN_ROWS_PER_PAGE
            spectator.setInput('totalItems', 50);
            spectator.detectChanges();

            expect(spectator.component.state.currentPageFirstRowIndex()).toBe(0);
        });
    });

    describe('Loading', () => {
        it('should show the loading row', () => {
            spectator.setInput('items', mockItems);
            spectator.setInput('loading', true);
            spectator.detectChanges();

            const loadingRow = spectator.query(byTestId('loading-row'));

            expect(loadingRow).toBeTruthy();
        });

        it('should not show the loading row', () => {
            spectator.setInput('items', mockItems);
            spectator.setInput('loading', false);
            spectator.detectChanges();

            const loadingRow = spectator.query(byTestId('loading-row'));

            expect(loadingRow).toBeNull();
        });
    });

    describe('Item Row', () => {
        const firstItem = mockItems[0];
        beforeEach(() => {
            spectator.setInput('items', mockItems);
            spectator.setInput('loading', false);
            spectator.detectChanges();
        });

        it('should show the item row', () => {
            const itemRow = spectator.query(byTestId('item-row'));

            expect(itemRow).toBeTruthy();
        });

        it('should have a checkbox column', () => {
            const checkboxColumn = spectator.query(byTestId('header-checkbox'));

            expect(checkboxColumn).toBeTruthy();
        });

        it('should have a title column', () => {
            const titleColumn = spectator.query(byTestId('item-title'));

            expect(titleColumn.textContent.trim()).toBe(firstItem.title);
        });

        it('should have a status column', () => {
            const statusColumn = spectator.query(byTestId('item-status'));

            expect(statusColumn).toBeTruthy();
        });

        it('should have a language column', () => {
            const languageColumn = spectator.query(byTestId('item-language'));

            expect(languageColumn).toBeTruthy();
        });

        it('should have a content type column', () => {
            const contentTypeColumn = spectator.query(byTestId('item-content-type'));

            expect(contentTypeColumn).toBeTruthy();
        });

        it('should have a mod user name column', () => {
            const modUserNameColumn = spectator.query(byTestId('item-mod-user-name'));

            expect(modUserNameColumn.textContent.trim()).toBe(firstItem.modUserName);
        });

        it('should have a mod date column', () => {
            const modDateColumn = spectator.query(byTestId('item-mod-date'));

            expect(modDateColumn).toBeTruthy();
        });

        it('should have a contentlet thumbnail', () => {
            const contentletThumbnail = spectator.query(byTestId('contentlet-thumbnail'));

            expect(contentletThumbnail).toBeTruthy();
        });

        it('should have a contentlet title', () => {
            const contentletTitle = spectator.query(byTestId('item-title'));

            expect(contentletTitle.textContent.trim()).toBe(firstItem.title);
        });

        it('should have item title text with truncate-text class', () => {
            const itemTitleText = spectator.query(byTestId('item-title-text'));

            expect(itemTitleText).toBeTruthy();
            expect(itemTitleText.classList.contains('truncate-text')).toBe(true);
        });

        it('should not have max-width: 100% style on item-title td', () => {
            const itemTitleTd = spectator.query(byTestId('item-title'));
            const computedStyle = window.getComputedStyle(itemTitleTd);

            expect(computedStyle.maxWidth).not.toBe('100%');
        });

        describe('Lock Icon', () => {
            it('should show lock icon when item is locked', () => {
                const lockedItem = { ...mockItems[0], locked: true };
                spectator.setInput('items', [lockedItem]);
                spectator.setInput('loading', false);
                spectator.detectChanges();

                const lockIcon = spectator.query(byTestId('lock-icon'));
                const lockOpenIcon = spectator.query(byTestId('lock-open-icon'));

                expect(lockIcon).toBeTruthy();
                expect(lockOpenIcon).toBeFalsy();
            });

            it('should show open lock icon when item is unlocked', () => {
                const unlockedItem = { ...mockItems[0], locked: false };
                spectator.setInput('items', [unlockedItem]);
                spectator.setInput('loading', false);
                spectator.detectChanges();

                const lockIcon = spectator.query(byTestId('lock-icon'));
                const lockOpenIcon = spectator.query(byTestId('lock-open-icon'));

                expect(lockIcon).toBeFalsy();
                expect(lockOpenIcon).toBeTruthy();
            });
        });

        describe('Status', () => {
            it('should have a published status', () => {
                const statusColumn = spectator.query(byTestId('item-status'));

                expect(statusColumn.textContent.trim()).toBe('Published');
            });

            it('should have a archived status', () => {
                spectator.setInput('items', [
                    {
                        ...firstItem,
                        live: false,
                        archived: true
                    }
                ]);
                spectator.detectChanges();

                const statusColumn = spectator.query(byTestId('item-status'));

                expect(statusColumn.textContent.trim()).toBe('Archived');
            });

            it('should have a draft status', () => {
                spectator.setInput('items', [
                    {
                        ...firstItem,
                        live: false,
                        archived: false,
                        working: true
                    }
                ]);
                spectator.detectChanges();

                const statusColumn = spectator.query(byTestId('item-status'));

                expect(statusColumn.textContent.trim()).toBe('Draft');
            });
        });
    });

    describe('Selection Management', () => {
        it('should clear selected items when items input changes', () => {
            const firstItem = mockItems[0];
            const secondItem = mockItems[1];

            spectator.setInput('items', mockItems);
            spectator.detectChanges();

            // Set some selected items
            spectator.component.selectedItems = [firstItem, secondItem];
            expect(spectator.component.selectedItems.length).toBe(2);

            // Change items input
            const newItems = [mockItems[2], mockItems[3]];
            spectator.setInput('items', newItems);
            spectator.detectChanges();

            // Selected items should be cleared
            expect(spectator.component.selectedItems).toEqual([]);
        });

        it('should clear selected items even when items array is empty', () => {
            const firstItem = mockItems[0];

            spectator.setInput('items', mockItems);
            spectator.detectChanges();

            // Set some selected items
            spectator.component.selectedItems = [firstItem];
            expect(spectator.component.selectedItems.length).toBe(1);

            // Change to empty items
            spectator.setInput('items', []);
            spectator.detectChanges();

            // Selected items should be cleared
            expect(spectator.component.selectedItems).toEqual([]);
        });
    });

    describe('Drag Events', () => {
        const firstItem = mockItems[0];
        const secondItem = mockItems[1];
        let dragStartSpy: ReturnType<typeof jest.spyOn>;

        beforeEach(() => {
            spectator.setInput('items', mockItems);
            spectator.setInput('loading', false);
            spectator.detectChanges();

            dragStartSpy = jest.spyOn(spectator.component.dragStart, 'emit');
        });

        afterEach(() => {
            jest.clearAllMocks();
        });

        describe('onDragStart', () => {
            describe('single drag', () => {
                it('should handle drag of a single item not in selection', () => {
                    const dragEvent = createDragStartEvent();

                    spectator.component.selectedItems = [];
                    spectator.component.onDragStart(dragEvent, firstItem);

                    expect(dragEvent.stopPropagation).toHaveBeenCalled();
                    expect(dragEvent.dataTransfer?.effectAllowed).toBe('move');
                    expect(dragEvent.dataTransfer?.setData).toHaveBeenCalledWith(DOT_DRAG_ITEM, '');
                    expect(dragStartSpy).toHaveBeenCalledWith([firstItem]);
                });

                it('should not proceed when dataTransfer is null', () => {
                    const dragEvent = createDragStartEvent();
                    // Override dataTransfer to null for testing
                    Object.defineProperty(dragEvent, 'dataTransfer', {
                        value: null,
                        writable: true
                    });

                    spectator.component.selectedItems = [];
                    spectator.component.onDragStart(dragEvent, firstItem);

                    expect(dragEvent.stopPropagation).not.toHaveBeenCalled();
                    expect(dragStartSpy).not.toHaveBeenCalled();
                });
            });

            describe('multiple selection', () => {
                it('should handle drag of multiple selected items', () => {
                    const dragEvent = createDragStartEvent();

                    spectator.component.selectedItems = [firstItem, secondItem];
                    spectator.component.onDragStart(dragEvent, firstItem);

                    expect(dragEvent.stopPropagation).toHaveBeenCalled();
                    expect(dragEvent.dataTransfer?.effectAllowed).toBe('move');
                    expect(dragEvent.dataTransfer?.setData).toHaveBeenCalledWith(DOT_DRAG_ITEM, '');
                    expect(dragStartSpy).toHaveBeenCalledWith([firstItem, secondItem]);
                });

                it('should drag all selected items when dragging one of them', () => {
                    const dragEvent = createDragStartEvent();

                    spectator.component.selectedItems = [firstItem, secondItem];
                    // Drag the second item which is in selection
                    spectator.component.onDragStart(dragEvent, secondItem);

                    expect(dragStartSpy).toHaveBeenCalledWith([firstItem, secondItem]);
                });
            });

            describe('multiple selection but single drag', () => {
                it('should not drag selected items when dragging a different item', () => {
                    const dragEvent = createDragStartEvent();
                    const thirdItem = mockItems[2];

                    spectator.component.selectedItems = [firstItem, secondItem];
                    spectator.component.onDragStart(dragEvent, thirdItem);

                    // Should emit only the third item, not the selected items
                    expect(dragStartSpy).toHaveBeenCalledWith([thirdItem]);
                    expect(dragStartSpy).not.toHaveBeenCalledWith([firstItem, secondItem]);
                });
            });
        });

        describe('onDragEnd', () => {
            it('should emit dragEnd with void', () => {
                const dragEndSpy = jest.spyOn(spectator.component.dragEnd, 'emit');

                spectator.component.onDragEnd();

                expect(dragEndSpy).toHaveBeenCalledWith();
            });

            it('should reset isDragging state to false', () => {
                const event = createDragStartEvent();
                const item = mockItems[0];

                // Start dragging first
                spectator.component.onDragStart(event, item);
                expect(spectator.component.state.isDragging()).toBe(true);

                // Then end dragging
                spectator.component.onDragEnd();

                expect(spectator.component.state.isDragging()).toBe(false);
            });
        });

        describe('isDragging state management', () => {
            beforeEach(() => {
                spectator.setInput('items', mockItems);
                spectator.setInput('loading', false);
                spectator.detectChanges();
            });

            it('should initialize isDragging state as false', () => {
                expect(spectator.component.state.isDragging()).toBe(false);
            });

            it('should set isDragging to true when drag starts', () => {
                const event = createDragStartEvent();
                const item = mockItems[0];

                spectator.component.onDragStart(event, item);

                expect(spectator.component.state.isDragging()).toBe(true);
            });

            it('should set isDragging to false when drag ends', () => {
                const event = createDragStartEvent();
                const item = mockItems[0];

                // Start dragging
                spectator.component.onDragStart(event, item);
                expect(spectator.component.state.isDragging()).toBe(true);

                // End dragging
                spectator.component.onDragEnd();
                expect(spectator.component.state.isDragging()).toBe(false);
            });

            it('should maintain isDragging state through complete drag lifecycle', () => {
                const event = createDragStartEvent();
                const firstItem = mockItems[0];

                // Initial state
                expect(spectator.component.state.isDragging()).toBe(false);

                // Start first drag
                spectator.component.onDragStart(event, firstItem);
                expect(spectator.component.state.isDragging()).toBe(true);

                // End first drag
                spectator.component.onDragEnd();
                expect(spectator.component.state.isDragging()).toBe(false);

                // Start second drag
                spectator.component.onDragStart(event, firstItem);
                expect(spectator.component.state.isDragging()).toBe(true);

                // End second drag
                spectator.component.onDragEnd();
                expect(spectator.component.state.isDragging()).toBe(false);
            });

            it('should apply is-dragging class to row when isDragging is true', () => {
                const event = createDragStartEvent();
                const item = mockItems[0];

                spectator.component.onDragStart(event, item);
                spectator.detectChanges();

                const row = spectator.query(byTestId('item-row')) as HTMLElement;
                expect(row.classList.contains('is-dragging')).toBe(true);
                expect(spectator.component.state.isDragging()).toBe(true);
            });

            it('should remove is-dragging class from row when isDragging is false', () => {
                const event = createDragStartEvent();
                const item = mockItems[0];

                // Start dragging
                spectator.component.onDragStart(event, item);
                spectator.detectChanges();

                let row = spectator.query(byTestId('item-row')) as HTMLElement;
                expect(row.classList.contains('is-dragging')).toBe(true);
                expect(spectator.component.state.isDragging()).toBe(true);

                // End dragging
                spectator.component.onDragEnd();
                spectator.detectChanges();

                row = spectator.query(byTestId('item-row')) as HTMLElement;
                expect(row.classList.contains('is-dragging')).toBe(false);
                expect(spectator.component.state.isDragging()).toBe(false);
            });

            it('should reflect isDragging state changes in the DOM immediately', () => {
                const event = createDragStartEvent();
                const item = mockItems[0];

                // Verify initial state in DOM
                let row = spectator.query(byTestId('item-row')) as HTMLElement;
                expect(row.classList.contains('is-dragging')).toBe(false);

                // Start drag and verify state + DOM
                spectator.component.onDragStart(event, item);
                spectator.detectChanges();

                row = spectator.query(byTestId('item-row')) as HTMLElement;
                expect(spectator.component.state.isDragging()).toBe(true);
                expect(row.classList.contains('is-dragging')).toBe(true);

                // End drag and verify state + DOM
                spectator.component.onDragEnd();
                spectator.detectChanges();

                row = spectator.query(byTestId('item-row')) as HTMLElement;
                expect(spectator.component.state.isDragging()).toBe(false);
                expect(row.classList.contains('is-dragging')).toBe(false);
            });
        });
    });

    describe('Double Click Events', () => {
        beforeEach(() => {
            spectator.setInput('items', mockItems);
            spectator.setInput('loading', false);
            spectator.detectChanges();
        });

        it('should emit doubleClick event when row is double clicked', () => {
            const doubleClickSpy = jest.spyOn(spectator.component.doubleClick, 'emit');
            const row = spectator.query(byTestId('item-row'));

            spectator.dispatchFakeEvent(row, 'dblclick');

            expect(doubleClickSpy).toHaveBeenCalledWith(mockItems[0]);
        });

        it('should emit doubleClick event when thumbnail is clicked', () => {
            const doubleClickSpy = jest.spyOn(spectator.component, 'onDoubleClick');
            const thumbnail = spectator.query(byTestId('contentlet-thumbnail'));

            spectator.click(thumbnail);

            expect(doubleClickSpy).toHaveBeenCalledWith(mockItems[0]);
        });

        it('should emit doubleClick event when title text is clicked', () => {
            const doubleClickSpy = jest.spyOn(spectator.component, 'onDoubleClick');
            const titleText = spectator.query(byTestId('item-title-text'));

            spectator.click(titleText);

            expect(doubleClickSpy).toHaveBeenCalledWith(mockItems[0]);
        });

        it('should call onDoubleClick with correct item when thumbnail is clicked', () => {
            const emitSpy = jest.spyOn(spectator.component.doubleClick, 'emit');
            const thumbnail = spectator.query(byTestId('contentlet-thumbnail'));

            spectator.click(thumbnail);

            expect(emitSpy).toHaveBeenCalledWith(mockItems[0]);
        });

        it('should call onDoubleClick with correct item when title text is clicked', () => {
            const emitSpy = jest.spyOn(spectator.component.doubleClick, 'emit');
            const titleText = spectator.query(byTestId('item-title-text'));

            spectator.click(titleText);

            expect(emitSpy).toHaveBeenCalledWith(mockItems[0]);
        });
    });
});
