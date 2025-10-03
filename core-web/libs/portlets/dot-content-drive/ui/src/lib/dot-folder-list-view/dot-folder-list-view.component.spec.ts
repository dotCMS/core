import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator/jest';

import { provideHttpClient } from '@angular/common/http';
import { By } from '@angular/platform-browser';

import { DotFormatDateService, DotMessageService } from '@dotcms/data-access';
import { DotcmsConfigService } from '@dotcms/dotcms-js';
import { DotcmsConfigServiceMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotFolderListViewComponent } from './dot-folder-list-view.component';

import { mockItems } from '../shared/mocks';

describe('DotFolderListViewComponent', () => {
    let spectator: Spectator<DotFolderListViewComponent>;

    const createComponent = createComponentFactory({
        component: DotFolderListViewComponent,
        imports: [],
        providers: [
            mockProvider(DotMessageService, new MockDotMessageService({})),
            mockProvider(DotcmsConfigService, new DotcmsConfigServiceMock()),
            mockProvider(DotFormatDateService),
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

            it('should show 2 sortable columns with sort icon', () => {
                const sortableColumns = spectator.queryAll(byTestId('header-column-sortable'));
                const sortIcons = spectator.queryAll(byTestId('sort-icon'));

                expect(sortableColumns.length).toBe(2);
                expect(sortIcons.length).toBe(2);
            });

            it('should show 3 not sortable columns', () => {
                const notSortableColumns = spectator.queryAll(
                    byTestId('header-column-not-sortable')
                );

                expect(notSortableColumns.length).toBe(4);
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

            const tableDebugEl = spectator.debugElement.query(By.css('[data-testId="table"]'));
            expect(tableDebugEl.attributes['ng-reflect-style-class']).toContain('empty-table');
        });

        it('should not show pagination when there are 20 or fewer total items', () => {
            spectator.setInput('totalItems', 20);
            spectator.detectChanges();

            const tableDebugEl = spectator.debugElement.query(By.css('[data-testId="table"]'));
            expect(tableDebugEl.attributes['ng-reflect-paginator']).toBe('false');
        });

        it('should set first value when calling onPage', () => {
            spectator.setInput('totalItems', 50); // Enable pagination
            spectator.detectChanges();

            const mockEvent = { first: 20, rows: 20 };
            spectator.component.onPage(mockEvent);
            spectator.detectChanges();

            const tableDebugEl = spectator.debugElement.query(By.css('[data-testId="table"]'));
            expect(tableDebugEl.attributes['ng-reflect-first']).toBe('20');
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

            const tableDebugEl = spectator.debugElement.query(By.css('[data-testId="table"]'));
            expect(tableDebugEl.attributes['ng-reflect-first']).toBe('0');
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

        it('should have a base type column', () => {
            const baseTypeColumn = spectator.query(byTestId('item-base-type'));

            expect(baseTypeColumn).toBeTruthy();
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
            expect(itemTitleText).toHaveClass('truncate-text');
        });

        it('should not have max-width: 100% style on item-title td', () => {
            const itemTitleTd = spectator.query(byTestId('item-title'));
            const computedStyle = window.getComputedStyle(itemTitleTd);

            expect(computedStyle.maxWidth).not.toBe('100%');
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
});
