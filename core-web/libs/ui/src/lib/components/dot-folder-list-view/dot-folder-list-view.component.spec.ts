import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { By } from '@angular/platform-browser';

import { LazyLoadEvent } from 'primeng/api';
import { Table, TableCheckbox, TableHeaderCheckbox } from 'primeng/table';

import { DotFormatDateService, DotLanguagesService, DotMessageService } from '@dotcms/data-access';
import { DotcmsConfigService } from '@dotcms/dotcms-js';
import { DotContentDriveItem, DotLanguage } from '@dotcms/dotcms-models';
import { DotcmsConfigServiceMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DOT_DRAG_ITEM, HEADER_COLUMNS } from './constants';
import { DotFolderListViewComponent } from './dot-folder-list-view.component';
import { mockItems } from './mocks';

// Mock DragEvent since it's not available in Jest environment
class DragEventMock extends Event {
    override preventDefault = jest.fn();
    override stopPropagation = jest.fn();
    dataTransfer: {
        effectAllowed?: string;
        setData?: ReturnType<typeof jest.fn>;
        setDragImage?: ReturnType<typeof jest.fn>;
        types?: string[];
        files?: FileList | File[];
    } | null = null;

    constructor(type: string) {
        super(type);
        this.dataTransfer = {
            effectAllowed: '',
            setData: jest.fn(),
            setDragImage: jest.fn(),
            types: [],
            files: []
        };
    }
}

// Override global DragEvent with our mock
(global as unknown as { DragEvent: typeof DragEventMock }).DragEvent = DragEventMock;

// Helper function to create properly mocked drag event
function createDragStartEvent(): DragEvent {
    return new DragEvent('dragstart');
}

// Helper function to create drag over event with internal drag type
function createDragOverEvent(types: string[] = [DOT_DRAG_ITEM]): DragEvent {
    const event = new DragEvent('dragover');
    Object.defineProperty(event, 'dataTransfer', {
        value: {
            types,
            files: []
        },
        writable: true
    });
    return event;
}

// Helper function to create drag over event with files
function createFileDragOverEvent(files: File[] = []): DragEvent {
    const event = new DragEvent('dragover');
    Object.defineProperty(event, 'dataTransfer', {
        value: {
            types: ['Files'],
            files
        },
        writable: true
    });
    return event;
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
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    Folder: 'Folder',
                    Published: 'Published',
                    Archived: 'Archived',
                    Revision: 'Revision',
                    Draft: 'Draft',
                    New: 'New'
                })
            },
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

        it('should set offset input property', () => {
            spectator.setInput('offset', 20);

            expect(spectator.component.$offset()).toBe(20);
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

            expect(paginateSpy).toHaveBeenCalledWith({ first: 10, rows: 10, page: 2 });
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
            const table = spectator.query(byTestId('table'))!;

            expect(table).toBeTruthy();
        });

        describe('Header', () => {
            it('should show the header', () => {
                const header = spectator.query(byTestId('header-row'))!;

                expect(header).toBeTruthy();
            });

            it('should show sortable columns with sort icon', () => {
                const sortableColumnsCount = HEADER_COLUMNS.filter((col) => col.sortable).length;
                const sortableColumns = spectator.queryAll(byTestId('header-column-sortable'))!;
                const sortIcons = spectator.queryAll(byTestId('sort-icon'))!;

                expect(sortableColumns.length).toBe(sortableColumnsCount);
                expect(sortIcons.length).toBe(sortableColumnsCount);
            });

            it('should show not sortable columns', () => {
                const notSortableColumnsCount = HEADER_COLUMNS.filter(
                    (col) => !col.sortable
                ).length;
                const notSortableColumns = spectator.queryAll(
                    byTestId('header-column-not-sortable')
                )!;

                expect(notSortableColumns.length).toBe(notSortableColumnsCount);
            });

            it('should have a checkbox column', () => {
                const checkboxColumn = spectator.query(byTestId('header-checkbox'))!;

                expect(checkboxColumn).toBeTruthy();
            });
        });
    });

    describe('Styles and Pagination', () => {
        it('should render table when items list is empty', () => {
            spectator.setInput('items', []);
            spectator.setInput('totalItems', 0);
            spectator.detectChanges();

            // Verify the table is still rendered when empty
            const tableElement = spectator.query(byTestId('table'))!;
            expect(tableElement).toBeTruthy();
        });

        it('should always show pagination regardless of totalItems', () => {
            spectator.setInput('totalItems', 20);
            spectator.detectChanges();

            // Paginator is always rendered ([paginator]="true")
            const paginator = spectator.query('.p-paginator')!;
            expect(paginator).toBeTruthy();
        });

        it('should emit paginate event when calling onPage', () => {
            spectator.setInput('totalItems', 50); // Enable pagination
            spectator.detectChanges();

            const paginateSpy = jest.spyOn(spectator.component.paginate, 'emit');
            const mockEvent = { first: 20, rows: 20 };
            spectator.component.onPage(mockEvent);
            spectator.detectChanges();

            expect(paginateSpy).toHaveBeenCalledWith({ ...mockEvent, page: 2 });
        });

        it('should sync table first value when firstChange event is emitted', () => {
            spectator.setInput('offset', 40);
            spectator.setInput('totalItems', 50); // Enable pagination so table renders
            spectator.detectChanges();

            // Mock the dataTable viewChild to return a mock table
            const mockTable = { first: 0 };
            Object.defineProperty(spectator.component, 'dataTable', {
                value: () => mockTable,
                writable: true
            });

            const table = spectator.debugElement.query(By.css('[data-testId="table"]'));
            spectator.triggerEventHandler(table, 'firstChange', null);

            expect(mockTable.first).toBe(40);
        });

        it('should not throw when firstChange event is emitted without table instance', () => {
            spectator.setInput('offset', 40);
            spectator.setInput('totalItems', 50);
            spectator.detectChanges();

            // Mock the dataTable viewChild to return undefined
            Object.defineProperty(spectator.component, 'dataTable', {
                value: () => undefined,
                writable: true
            });

            const table = spectator.debugElement.query(By.css('[data-testId="table"]'));

            expect(() => spectator.triggerEventHandler(table, 'firstChange', null)).not.toThrow();
        });
    });

    /**
     * Row identity. The browsing grid keys on `identifier`, but language variants of one contentlet
     * share an identifier and differ only by inode — and inodes are what bulk actions fire on. A
     * caller that acts per variant has to be able to key on `inode` instead.
     */
    describe('dataKey', () => {
        const variantA = { ...mockItems[0], identifier: 'shared-id', inode: 'inode-en' };
        const variantB = { ...mockItems[0], identifier: 'shared-id', inode: 'inode-es' };

        it('should key rows on identifier by default', () => {
            expect(spectator.query(Table)!.dataKey).toBe('identifier');
        });

        it('should treat rows sharing an identifier as the same row by default', () => {
            // Not a defect for the grid — it lists one row per identifier — but it is exactly why
            // the default cannot be reused where variants are listed separately.
            spectator.setInput('items', [variantA, variantB]);
            spectator.setInput('selection', [variantA]);
            spectator.detectChanges();

            const table = spectator.query(Table)!;

            expect(table.isSelected(variantA)).toBe(true);
            expect(table.isSelected(variantB)).toBe(true);
        });

        it('should distinguish rows sharing an identifier when keyed on inode', () => {
            spectator.setInput('items', [variantA, variantB]);
            spectator.setInput('dataKey', 'inode');
            spectator.setInput('selection', [variantA]);
            spectator.detectChanges();

            const table = spectator.query(Table)!;

            expect(table.isSelected(variantA)).toBe(true);
            expect(table.isSelected(variantB)).toBe(false);
        });
    });

    /**
     * Where the rows come from. The grid is lazy: it holds one page and asks the parent for the
     * next. A caller that already has every row in memory pages locally instead.
     */
    describe('lazy', () => {
        /** More rows than fit on a page, so paging is observable. */
        const manyItems = Array.from({ length: 25 }, (_, index) => ({
            ...mockItems[0],
            identifier: `id-${index}`,
            inode: `inode-${index}`,
            // Distinct titles so a test can tell *which* page it is looking at, not just how many
            // rows it has.
            title: `Item ${index}`
        }));

        it('should delegate paging to the parent by default', () => {
            expect(spectator.query(Table)!.lazy).toBe(true);
        });

        it('should honour the offset when paging an in-memory list', () => {
            // The discriminating behaviour. PrimeNG slices to `rows` either way, but pins the slice
            // to index 0 while lazy — the parent is expected to have fetched the right page. Only a
            // non-lazy table moves through a list it already holds.
            spectator.setInput('items', manyItems);
            spectator.setInput('offset', 20);
            spectator.setInput('lazy', false);
            spectator.setInput('loading', false);
            spectator.detectChanges();

            // Page two of twenty-five: the last five rows, not the first twenty over again.
            expect(spectator.queryAll(byTestId('item-row'))!.length).toBe(5);
        });

        it('should reach page two without the caller binding an offset', () => {
            // A non-lazy caller has no reason to bind `offset` — it holds every row and does not
            // fetch pages. `onFirstChange` used to pin `first` back to `$offset()` (still 0) on
            // every page change, so the paginator advanced and snapped straight back, leaving
            // everything past row 20 unreachable.
            spectator.setInput('items', manyItems);
            spectator.setInput('lazy', false);
            spectator.setInput('loading', false);
            spectator.detectChanges();

            spectator.click('.p-paginator-next');
            spectator.detectChanges();

            const titles = spectator
                .queryAll(byTestId('item-title-text'))
                .map((cell) => cell.textContent.trim());

            // Page two of twenty-five holds the last five rows.
            expect(titles.length).toBe(5);
            expect(titles[0]).toBe(manyItems[20].title);
        });

        it('should keep an in-memory list in the order it was given', () => {
            // The grid opens sorted by modDate desc, which is right for browsing a page of results.
            // A non-lazy caller has already chosen an order — the action preview's rows are the
            // user's selection, in the order the grid showed it — and PrimeNG sorts non-lazy data
            // on render, so that default silently reshuffled it.
            const chosenOrder = [
                {
                    ...mockItems[0],
                    identifier: 'older',
                    inode: 'older',
                    title: 'Chosen first',
                    modDate: '2020-01-01T00:00:00Z'
                },
                {
                    ...mockItems[0],
                    identifier: 'newer',
                    inode: 'newer',
                    title: 'Chosen second',
                    modDate: '2030-01-01T00:00:00Z'
                }
            ];

            spectator.setInput('items', chosenOrder);
            spectator.setInput('lazy', false);
            spectator.setInput('loading', false);
            spectator.detectChanges();

            expect(
                spectator
                    .queryAll(byTestId('item-title-text'))
                    .map((cell) => cell.textContent.trim())
            ).toEqual(['Chosen first', 'Chosen second']);
        });

        it('should not reorder the array the caller passed in', () => {
            // PrimeNG's non-lazy path sorts `value` **in place** (`sortSingle`), and the array a
            // caller hands over is the one it holds itself — for the action preview, the very array
            // behind `$previewItems()` and the parent's included set. Reordering it from outside
            // Angular would be a side effect on the caller's own state, not just a display choice.
            const given = [
                { ...mockItems[0], inode: 'older', title: 'Chosen first', modDate: '2020-01-01' },
                { ...mockItems[0], inode: 'newer', title: 'Chosen second', modDate: '2030-01-01' }
            ];
            const originalOrder = [...given];

            spectator.setInput('items', given);
            spectator.setInput('lazy', false);
            spectator.setInput('loading', false);
            spectator.detectChanges();

            expect(given).toEqual(originalOrder);
        });

        it('should not ask the parent to fetch a page it already holds', () => {
            // `paginate` means "fetch me this page from the server" — a request a caller holding
            // every row cannot satisfy, and it also resets the skeleton rows on each page turn.
            spectator.setInput('items', manyItems);
            spectator.setInput('lazy', false);
            spectator.setInput('loading', false);
            spectator.detectChanges();
            const paginateSpy = jest.spyOn(spectator.component.paginate, 'emit');

            spectator.click('.p-paginator-next');
            spectator.detectChanges();

            expect(paginateSpy).not.toHaveBeenCalled();
        });

        it('should still ask the parent to fetch a page while lazy', () => {
            spectator.setInput('items', manyItems);
            spectator.setInput('totalItems', 100);
            spectator.setInput('loading', false);
            spectator.detectChanges();
            const paginateSpy = jest.spyOn(spectator.component.paginate, 'emit');

            spectator.component.onPage({ first: 20, rows: 20 });

            expect(paginateSpy).toHaveBeenCalled();
        });

        it('should not fetch languages when the locale column is hidden', () => {
            // The preview is a second instance of this grid, built and torn down on every drill-in,
            // and it hides the locale column — so the map that request builds is never read.
            const languagesService = spectator.inject(DotLanguagesService, true);
            languagesService.get.mockClear();

            const scoped = createComponent({
                props: {
                    items: mockItems,
                    visibleColumns: ['title', 'live', 'contentType']
                } as unknown as NonNullable<Parameters<typeof createComponent>[0]>['props']
            });
            scoped.detectChanges();

            expect(languagesService.get).not.toHaveBeenCalled();
        });

        it('should fetch languages when the locale column is shown', () => {
            const languagesService = spectator.inject(DotLanguagesService, true);

            expect(languagesService.get).toHaveBeenCalled();
        });

        it('should count the in-memory list rather than the totalItems input', () => {
            // `totalItems` is the server's count and means nothing to a caller holding every row —
            // left at 0 here, which would collapse the paginator to a single page if it were used.
            spectator.setInput('items', manyItems);
            spectator.setInput('totalItems', 0);
            spectator.setInput('lazy', false);
            spectator.setInput('loading', false);
            spectator.detectChanges();

            expect(spectator.query(Table)!.totalRecords).toBe(manyItems.length);
        });

        it('should hide the paginator when an in-memory list fits on one page', () => {
            // Dead weight on a short confirmation list. The lazy grid always shows it, because it
            // cannot know whether the server has more.
            spectator.setInput('items', mockItems);
            spectator.setInput('lazy', false);
            spectator.setInput('loading', false);
            spectator.detectChanges();

            expect(spectator.query('.p-paginator')!).toBeNull();
        });

        it('should show the paginator when an in-memory list outgrows a page', () => {
            spectator.setInput('items', manyItems);
            spectator.setInput('lazy', false);
            spectator.setInput('loading', false);
            spectator.detectChanges();

            expect(spectator.query('.p-paginator')!).toBeTruthy();
        });
    });

    /**
     * Freezes the selection while the caller is mid-flight. `loading` is about rows arriving; this
     * is about the set the user has already picked being acted on, where letting them change it
     * would desync what the dialog shows from what was actually submitted.
     */
    describe('disabled', () => {
        beforeEach(() => {
            spectator.setInput('items', mockItems);
            spectator.setInput('loading', false);
        });

        it('should leave the checkboxes usable by default', () => {
            spectator.detectChanges();

            expect(spectator.query(TableCheckbox)!.disabled()).toBeFalsy();
            expect(spectator.query(TableHeaderCheckbox)!.disabled()).toBeFalsy();
        });

        it('should freeze the row and header checkboxes while disabled', () => {
            spectator.setInput('disabled', true);
            spectator.detectChanges();

            expect(spectator.query(TableCheckbox)!.disabled()).toBe(true);
            expect(spectator.query(TableHeaderCheckbox)!.disabled()).toBe(true);
        });

        it('should select a row by clicking it when not disabled', () => {
            spectator.detectChanges();
            const selectionChangeSpy = jest.spyOn(spectator.component.selectionChange, 'emit');

            spectator.click(byTestId('item-row'));

            expect(selectionChangeSpy).toHaveBeenCalled();
        });

        it('should not select a row by clicking it while disabled', () => {
            // Rows are selectable by click, not only by checkbox — freezing the boxes alone would
            // leave a way straight around the freeze.
            spectator.setInput('disabled', true);
            spectator.detectChanges();
            const selectionChangeSpy = jest.spyOn(spectator.component.selectionChange, 'emit');

            spectator.click(byTestId('item-row'));

            expect(selectionChangeSpy).not.toHaveBeenCalled();
        });
    });

    /**
     * Which of the fixed columns to render. The full set assumes the portlet's full width; inside a
     * dialog it overflows, squeezing the title to an ellipsis and pushing the rest out of view.
     */
    describe('visibleColumns', () => {
        /** Header cells excluding the leading checkbox column. */
        const headerCells = (): HTMLElement[] =>
            spectator.queryAll<HTMLElement>('thead th').slice(1);

        beforeEach(() => {
            spectator.setInput('items', mockItems);
            spectator.setInput('loading', false);
        });

        it('should render every fixed column by default', () => {
            spectator.detectChanges();

            expect(headerCells().length).toBe(HEADER_COLUMNS.length);
        });

        it('should render only the requested columns', () => {
            spectator.setInput('visibleColumns', ['title', 'live', 'modUser']);
            spectator.detectChanges();

            expect(headerCells().length).toBe(3);
        });

        it('should drop the body cells of the columns it hides', () => {
            // Header and body have to agree, or the cells shift out from under their headings.
            spectator.setInput('visibleColumns', ['title', 'live', 'modUser']);
            spectator.detectChanges();

            expect(spectator.query(byTestId('item-title'))!).toBeTruthy();
            expect(spectator.query(byTestId('item-status'))!).toBeTruthy();
            expect(spectator.query(byTestId('item-mod-user-name'))!).toBeTruthy();

            expect(spectator.query(byTestId('item-language'))!).toBeFalsy();
            expect(spectator.query(byTestId('item-content-type'))!).toBeFalsy();
            expect(spectator.query(byTestId('item-mod-date'))!).toBeFalsy();
            expect(spectator.query(byTestId('item-actions'))!).toBeFalsy();
        });

        it('should keep the requested columns in their canonical order', () => {
            // Display order stays the table's, not the order the caller happened to list.
            spectator.setInput('visibleColumns', ['modUser', 'title']);
            spectator.detectChanges();

            expect(headerCells().map((cell) => cell.textContent.trim())).toEqual([
                'name',
                'Edited-By'
            ]);
        });

        it('should keep the leading checkbox column whatever is hidden', () => {
            spectator.setInput('visibleColumns', ['title']);
            spectator.detectChanges();

            expect(spectator.query(byTestId('header-checkbox'))!).toBeTruthy();
            expect(spectator.query(byTestId('item-checkbox'))!).toBeTruthy();
        });

        it('should leave the title column unsized so it absorbs the leftover width', () => {
            // The one column without a width is what makes every other case below work: whatever
            // the sized columns and the 3rem checkbox one do not claim lands here.
            spectator.setInput('visibleColumns', ['title', 'live', 'contentType']);
            spectator.detectChanges();

            const [title, status, type] = headerCells();

            expect(title.style.width).toBe('');
            expect(status.style.width).toBe('10%');
            expect(type.style.width).toBe('15%');
        });

        it('should keep the sized columns at their authored widths whatever is hidden', () => {
            // No rescaling: dropping a column hands its share to the title, not to everyone.
            spectator.detectChanges();

            expect(headerCells().map((cell) => cell.style.width)).toEqual(
                HEADER_COLUMNS.map((column) => column.width ?? '')
            );
        });

        it('should leave room for the checkbox column instead of overflowing the table', () => {
            // Regression guard: percentages adding up to a full 100% put the `table-layout: fixed`
            // table 3rem past its container, which reads as a horizontal scrollbar that scrolls
            // nothing. Every subset has to stay under 100% so the checkbox column fits inside it.
            for (const columns of [
                [],
                ['title', 'live', 'contentType'],
                ['title', 'modUser', 'modDate']
            ]) {
                spectator.setInput('visibleColumns', columns);
                spectator.setInput('showActions', columns.length === 0);
                spectator.detectChanges();

                const total = headerCells()
                    .map((cell) => Number.parseFloat(cell.style.width) || 0)
                    .reduce((sum, width) => sum + width, 0);

                expect(total).toBeLessThan(100);
            }
        });

        it('should keep extra columns under their own heading when Type is hidden', () => {
            // The body anchors the extra-column loop at the Type slot whether or not Type renders,
            // so the header has to do the same. Appending them instead left the cells one place
            // early — an extra value sitting under the "Edited-By" heading.
            //
            // The case above hides everything after the title, which makes both orderings coincide;
            // it takes a visible column *after* Type to tell them apart.
            spectator.setInput('visibleColumns', ['title', 'modUser']);
            spectator.setInput('extraColumns', [
                { field: 'author', header: 'Author', order: 1, type: 'text' }
            ]);
            spectator.detectChanges();

            expect(headerCells().map((cell) => cell.textContent.trim())).toEqual([
                'name',
                'Author',
                'Edited-By'
            ]);

            // Same sequence in the body, which is the assertion that actually catches a mismatch.
            const cells = [...spectator.query(byTestId('item-row'))!.querySelectorAll('td')].map(
                (cell) => cell.getAttribute('data-testid') ?? cell.getAttribute('data-testId')
            );

            expect(cells).toEqual([
                'item-checkbox',
                'item-title',
                'item-extra-author',
                'item-mod-user-name'
            ]);
        });

        it('should still append caller-provided extra columns', () => {
            // Extras are explicitly passed in, so they are never what the caller meant to hide.
            spectator.setInput('visibleColumns', ['title']);
            spectator.setInput('extraColumns', [
                { field: 'author', header: 'Author', order: 1, type: 'text' }
            ]);
            spectator.detectChanges();

            expect(headerCells().length).toBe(2);
            expect(spectator.query('[data-testid="item-extra-author"]')!).toBeTruthy();
        });
    });

    /**
     * Sorting is a browsing affordance: it reorders a page of results the parent then re-fetches.
     * A confirmation list has nothing to re-fetch, and re-ordering the rows a user is checking off
     * only loses their place — so `readOnly` takes the sort controls with it.
     */
    describe('sorting under readOnly', () => {
        beforeEach(() => {
            spectator.setInput('items', mockItems);
            spectator.setInput('loading', false);
        });

        it('should offer sortable headers by default', () => {
            spectator.detectChanges();

            expect(spectator.queryAll(byTestId('sort-icon'))!.length).toBeGreaterThan(0);
            expect(spectator.queryAll(byTestId('header-column-sortable'))!.length).toBeGreaterThan(
                0
            );
        });

        it('should render no sort controls while readOnly', () => {
            spectator.setInput('readOnly', true);
            spectator.detectChanges();

            expect(spectator.queryAll(byTestId('sort-icon'))!.length).toBe(0);
            expect(spectator.queryAll(byTestId('header-column-sortable'))!.length).toBe(0);
        });

        it('should not emit sort when a header is clicked while readOnly', () => {
            spectator.setInput('readOnly', true);
            spectator.detectChanges();
            const sortSpy = jest.spyOn(spectator.component.sort, 'emit');

            spectator.click(spectator.queryAll('thead th')![1]);

            expect(sortSpy).not.toHaveBeenCalled();
        });
    });

    describe('Loading', () => {
        it('should show the loading row', () => {
            spectator.setInput('items', mockItems);
            spectator.setInput('loading', true);
            spectator.detectChanges();

            const loadingRow = spectator.query(byTestId('loading-row'))!;

            expect(loadingRow).toBeTruthy();
        });

        it('should not show the loading row', () => {
            spectator.setInput('items', mockItems);
            spectator.setInput('loading', false);
            spectator.detectChanges();

            const loadingRow = spectator.query(byTestId('loading-row'))!;

            expect(loadingRow).toBeNull();
        });

        it('should show loading row when loading is true and items is empty', () => {
            spectator.setInput('items', []);
            spectator.setInput('loading', true);
            spectator.detectChanges();

            const loadingRows = spectator.queryAll(byTestId('loading-row'))!;

            expect(loadingRows.length).toBeGreaterThan(0);
        });

        it('should show loading row instead of data rows when loading is true and items has data', () => {
            spectator.setInput('items', mockItems);
            spectator.setInput('loading', true);
            spectator.detectChanges();

            const loadingRows = spectator.queryAll(byTestId('loading-row'))!;
            const itemRows = spectator.queryAll(byTestId('item-row'))!;

            expect(loadingRows.length).toBe(mockItems.length);
            expect(itemRows.length).toBe(0);
        });

        it('should render loading row with checkbox-sized skeleton in first column', () => {
            spectator.setInput('items', mockItems);
            spectator.setInput('loading', true);
            spectator.detectChanges();

            const loadingRow = spectator.query(byTestId('loading-row'))!;
            const firstCell = loadingRow?.querySelector('td');
            const skeleton = firstCell?.querySelector('p-skeleton');

            expect(firstCell).toBeTruthy();
            expect(skeleton).toBeTruthy();
            expect(skeleton?.getAttribute('height')).toBe('1.5rem');
            expect(skeleton?.getAttribute('width')).toBe('1.5rem');
        });

        it('should set $loadingRows length to event.rows when onPage is called', () => {
            spectator.setInput('totalItems', 50);
            spectator.detectChanges();

            spectator.component.onPage({ first: 0, rows: 40 } as LazyLoadEvent);
            spectator.detectChanges();

            expect(spectator.component.$loadingRows().length).toBe(40);
        });
    });

    describe('onPage page number calculation', () => {
        it('should resolve page 1 when first=0 (falsy)', () => {
            spectator.setInput('totalItems', 50);
            spectator.detectChanges();

            const paginateSpy = jest.spyOn(spectator.component.paginate, 'emit');
            spectator.component.onPage({ first: 0, rows: 20 });

            // first=0 is falsy → page defaults to 1
            expect(paginateSpy).toHaveBeenCalledWith({ first: 0, rows: 20, page: 1 });
        });

        it('should resolve page 2 when first=20, rows=20', () => {
            spectator.setInput('totalItems', 50);
            spectator.detectChanges();

            const paginateSpy = jest.spyOn(spectator.component.paginate, 'emit');
            spectator.component.onPage({ first: 20, rows: 20 });

            expect(paginateSpy).toHaveBeenCalledWith({ first: 20, rows: 20, page: 2 });
        });

        it('should resolve page 3 when first=40, rows=20', () => {
            spectator.setInput('totalItems', 80);
            spectator.detectChanges();

            const paginateSpy = jest.spyOn(spectator.component.paginate, 'emit');
            spectator.component.onPage({ first: 40, rows: 20 });

            expect(paginateSpy).toHaveBeenCalledWith({ first: 40, rows: 20, page: 3 });
        });
    });

    describe('Empty state and pass-through config', () => {
        it('should set table height and width 100% in $ptConfig when items is empty', () => {
            spectator.setInput('items', []);
            spectator.detectChanges();

            const ptConfig = spectator.component.$ptConfig();
            const tableStyle = ptConfig.table?.style as { height?: string; width?: string };

            expect(tableStyle?.height).toBe('100%');
            expect(tableStyle?.width).toBe('100%');
        });

        it('should not set full size in $ptConfig when items has data', () => {
            spectator.setInput('items', mockItems);
            spectator.detectChanges();

            const ptConfig = spectator.component.$ptConfig();
            const tableStyle = ptConfig.table?.style as { height?: string; width?: string };

            expect(tableStyle?.height).toBeUndefined();
            expect(tableStyle?.width).toBeUndefined();
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
            const itemRow = spectator.query(byTestId('item-row'))!;

            expect(itemRow).toBeTruthy();
        });

        /**
         * Long values must clip, never widen the table or grow the row.
         *
         * The columns carry fixed percentage widths, which only hold under `table-layout: fixed` —
         * with the default `auto` layout a long title stretches the table past its container and
         * pushes the trailing columns behind a horizontal scrollbar.
         */
        describe('overflowing cell values', () => {
            const longTitle = 'Easy Snowboard Tricks You can Start Using Right Away';

            it('should lay the table out with fixed columns so the widths are honoured', () => {
                expect(spectator.query<HTMLTableElement>('table')!.style.tableLayout).toBe('fixed');
            });

            it('should truncate a long title rather than widen the table', () => {
                spectator.setInput('items', [{ ...firstItem, title: longTitle }]);
                spectator.detectChanges();

                const title = spectator.query(byTestId('item-title-text'))!;

                expect(title.classList.contains('truncate')).toBe(true);
            });

            it('should keep the full title reachable on hover once it is clipped', () => {
                // Truncating without this loses the information outright — the row shows an ellipsis
                // and there is no way to read the rest.
                spectator.setInput('items', [{ ...firstItem, title: longTitle }]);
                spectator.detectChanges();

                const title = spectator.query(byTestId('item-title-text'))!;

                expect(title.getAttribute('title')).toBe(longTitle);
            });

            it('should truncate a long content type instead of wrapping the row', () => {
                // The type column is a fixed 15%; without clipping a long variable name wraps to a
                // second line and the row grows taller than its neighbours.
                spectator.setInput('items', [
                    { ...firstItem, contentType: 'AVeryLongContentTypeVariableName' }
                ]);
                spectator.detectChanges();

                const contentType = spectator.query(byTestId('item-content-type'))!;

                expect(contentType.classList.contains('truncate')).toBe(true);
            });
        });

        it('should have a checkbox column', () => {
            const checkboxColumn = spectator.query(byTestId('header-checkbox'))!;

            expect(checkboxColumn).toBeTruthy();
        });

        it('should have a title column', () => {
            const titleColumn = spectator.query(byTestId('item-title'))!;
            const titleText = spectator.query(byTestId('item-title-text'))!;

            expect(titleColumn).toBeTruthy();
            expect(titleText.textContent.trim()).toBe(firstItem.title);
        });

        it('should have a status column', () => {
            const statusColumn = spectator.query(byTestId('item-status'))!;

            expect(statusColumn).toBeTruthy();
        });

        it('should have a language column', () => {
            const languageColumn = spectator.query(byTestId('item-language'))!;

            expect(languageColumn).toBeTruthy();
        });

        it('should have a content type column', () => {
            const contentTypeColumn = spectator.query(byTestId('item-content-type'))!;

            expect(contentTypeColumn).toBeTruthy();
        });

        it('should have a mod user name column', () => {
            const modUserNameColumn = spectator.query(byTestId('item-mod-user-name'))!;
            const modUserName = 'modUserName' in firstItem ? firstItem.modUserName : 'Unknown';

            expect(modUserNameColumn.textContent.trim()).toBe(modUserName);
        });

        it('should have a mod date column', () => {
            const modDateColumn = spectator.query(byTestId('item-mod-date'))!;

            expect(modDateColumn).toBeTruthy();
        });

        it('should have a contentlet thumbnail', () => {
            const contentletThumbnail = spectator.query(byTestId('contentlet-thumbnail'))!;

            expect(contentletThumbnail).toBeTruthy();
        });

        it('should show contentlet thumbnail instead of folder icon for non-folder items', () => {
            const contentletThumbnail = spectator.query(byTestId('contentlet-thumbnail'))!;
            const folderIcon = spectator.query(byTestId('folder-icon'))!;

            expect(contentletThumbnail).toBeTruthy();
            expect(folderIcon).toBeFalsy();
        });

        it('should have a contentlet title', () => {
            const contentletTitle = spectator.query(byTestId('item-title-text'))!;

            expect(contentletTitle.textContent.trim()).toBe(firstItem.title);
        });

        it('should have item title text with truncate class', () => {
            const itemTitleText = spectator.query(byTestId('item-title-text'))!;

            expect(itemTitleText).toBeTruthy();
            expect(itemTitleText.classList.contains('truncate')).toBe(true);
        });

        it('should not have max-width: 100% style on item-title td', () => {
            const itemTitleTd = spectator.query(byTestId('item-title'))!;
            const computedStyle = window.getComputedStyle(itemTitleTd);

            expect(computedStyle.maxWidth).not.toBe('100%');
        });

        describe('Lock Icon', () => {
            it('should show lock icon when item is locked', () => {
                const lockedItem = { ...mockItems[0], locked: true };
                spectator.setInput('items', [lockedItem]);
                spectator.setInput('loading', false);
                spectator.detectChanges();

                const lockIcon = spectator.query(byTestId('lock-icon'))!;
                const lockOpenIcon = spectator.query(byTestId('lock-open-icon'))!;

                expect(lockIcon).toBeTruthy();
                expect(lockOpenIcon).toBeFalsy();
            });

            it('should show open lock icon when item is unlocked', () => {
                const unlockedItem = { ...mockItems[0], locked: false };
                spectator.setInput('items', [unlockedItem]);
                spectator.setInput('loading', false);
                spectator.detectChanges();

                const lockIcon = spectator.query(byTestId('lock-icon'))!;
                const lockOpenIcon = spectator.query(byTestId('lock-open-icon'))!;

                expect(lockIcon).toBeFalsy();
                expect(lockOpenIcon).toBeTruthy();
            });

            /**
             * A lock the current user does not hold reads differently from one they do: only the
             * holder or a CMS Administrator can release it, so a bulk Unlock may be refused on
             * these rows and the user needs to see which before firing.
             *
             * Which rows those are is the caller's call, not the table's — the decision needs the
             * user's admin role, which lives in the portlet. An administrator releases every lock,
             * so their caller passes nothing and no row is marked.
             */
            describe('locks held by another user', () => {
                const lockedItem = { ...mockItems[0], locked: true, inode: 'locked-inode' };

                beforeEach(() => {
                    spectator.setInput('items', [lockedItem]);
                    spectator.setInput('loading', false);
                });

                it('should show the plain lock icon when no rows are marked', () => {
                    spectator.detectChanges();

                    expect(spectator.query(byTestId('lock-icon'))!).toBeTruthy();
                    expect(spectator.query(byTestId('lock-foreign-icon'))!).toBeFalsy();
                });

                it('should mark a locked row whose inode the caller flagged', () => {
                    spectator.setInput('lockedByOthers', [lockedItem.inode]);
                    spectator.detectChanges();

                    expect(spectator.query(byTestId('lock-foreign-icon'))!).toBeTruthy();
                    expect(spectator.query(byTestId('lock-icon'))!).toBeFalsy();
                });

                it('should explain the marker on hover', () => {
                    spectator.setInput('lockedByOthers', [lockedItem.inode]);
                    spectator.detectChanges();

                    expect(
                        spectator.query(byTestId('lock-foreign-icon'))!.getAttribute('title')
                    ).toBe('content-drive.list-view.locked-by-another-user');
                });

                it('should leave the current user’s own lock unmarked', () => {
                    // Same selection, different row: only the flagged inodes are marked, so a lock
                    // the user holds keeps reading as an ordinary lock.
                    spectator.setInput('lockedByOthers', ['some-other-inode']);
                    spectator.detectChanges();

                    expect(spectator.query(byTestId('lock-icon'))!).toBeTruthy();
                    expect(spectator.query(byTestId('lock-foreign-icon'))!).toBeFalsy();
                });

                it('should not mark an unlocked row even when its inode is flagged', () => {
                    // Defensive: a stale flag must not put a lock icon on a row that has none.
                    spectator.setInput('items', [{ ...lockedItem, locked: false }]);
                    spectator.setInput('lockedByOthers', [lockedItem.inode]);
                    spectator.detectChanges();

                    expect(spectator.query(byTestId('lock-open-icon'))!).toBeTruthy();
                    expect(spectator.query(byTestId('lock-foreign-icon'))!).toBeFalsy();
                });
            });
        });

        /**
         * The grid's row affordances make no sense in a modal confirmation list: there is nowhere to
         * drag to, the context menu acts on a grid that is not visible, and opening an item would
         * navigate away from the dialog. `readOnly` strips them; the checkboxes stay.
         */
        describe('readOnly', () => {
            beforeEach(() => {
                spectator.setInput('items', mockItems);
                spectator.setInput('loading', false);
                spectator.setInput('readOnly', true);
                spectator.detectChanges();
            });

            it('should not make rows draggable', () => {
                expect(spectator.query(byTestId('item-row'))!.getAttribute('draggable')).toBe(
                    'false'
                );
            });

            it('should not emit dragStart when a drag is started anyway', () => {
                // The attribute only stops the user; the handler has to stop everything else.
                const dragStartSpy = jest.spyOn(spectator.component.dragStart, 'emit');

                spectator.component.onDragStart(createDragStartEvent(), mockItems[0]);

                expect(dragStartSpy).not.toHaveBeenCalled();
            });

            it('should not render the kebab menu button', () => {
                expect(spectator.query(byTestId('kebab-menu-button'))!).toBeFalsy();
            });

            it('should not emit rightClick on context menu', () => {
                const rightClickSpy = jest.spyOn(spectator.component.rightClick, 'emit');

                spectator.dispatchFakeEvent(spectator.query(byTestId('item-row'))!, 'contextmenu');

                expect(rightClickSpy).not.toHaveBeenCalled();
            });

            it('should not emit doubleClick on a double click', () => {
                const doubleClickSpy = jest.spyOn(spectator.component.doubleClick, 'emit');

                spectator.dispatchFakeEvent(spectator.query(byTestId('item-row'))!, 'dblclick');

                expect(doubleClickSpy).not.toHaveBeenCalled();
            });

            it('should not open the item when its title is clicked', () => {
                // The title and thumbnail carry their own click-to-open handlers, separate from the
                // row's dblclick — both have to go or the dialog navigates out from under itself.
                const doubleClickSpy = jest.spyOn(spectator.component.doubleClick, 'emit');

                spectator.click(byTestId('item-title-text'));

                expect(doubleClickSpy).not.toHaveBeenCalled();
            });

            it('should still render the row checkboxes', () => {
                expect(spectator.query(byTestId('item-checkbox'))!).toBeTruthy();
            });

            it('should not toggle a row when a cell without its own click handler is clicked', () => {
                // Rows are `pSelectableRow`, and PrimeNG's `metaKeySelection` defaults to false, so a
                // plain click toggles. The checkbox, title and thumbnail cells all stop propagation;
                // the status and type cells do not. In a confirmation list that means glancing at a
                // status badge can silently drop that row from what Execute fires — with nothing
                // suggesting the row was clickable. The table this replaced toggled on the checkbox
                // alone.
                const selectionChangeSpy = jest.spyOn(spectator.component.selectionChange, 'emit');

                spectator.click(spectator.query(byTestId('item-status'))!);

                expect(selectionChangeSpy).not.toHaveBeenCalled();
            });
        });

        describe('Status', () => {
            it('should have a published status', () => {
                // Update firstItem to have all required properties for Published status
                spectator.setInput('items', [
                    {
                        ...firstItem,
                        live: true,
                        working: true,
                        hasLiveVersion: true
                    }
                ]);
                spectator.detectChanges();

                const statusColumn = spectator.query(byTestId('item-status'))!;

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

                const statusColumn = spectator.query(byTestId('item-status'))!;

                expect(statusColumn.textContent.trim()).toBe('Archived');
            });

            it('should have a draft status', () => {
                spectator.setInput('items', [
                    {
                        ...firstItem,
                        live: false,
                        archived: false,
                        working: false,
                        hasLiveVersion: false
                    }
                ]);
                spectator.detectChanges();

                const statusColumn = spectator.query(byTestId('item-status'))!;

                expect(statusColumn.textContent.trim()).toBe('Draft');
            });
        });

        describe('Folder-specific rendering', () => {
            const mockFolder: DotContentDriveItem = {
                __icon__: 'folderIcon',
                defaultFileType: 'FileAsset',
                description: 'Test folder',
                extension: 'folder',
                filesMasks: '*',
                hasTitleImage: false,
                hostId: 'host-123',
                iDate: Date.now(),
                identifier: 'folder-123',
                inode: 'folder-inode-123',
                mimeType: 'folder',
                modDate: Date.now(),
                name: 'Test Folder',
                owner: 'admin',
                parent: '/',
                path: '/documents/',
                permissions: [],
                showOnMenu: true,
                sortOrder: 0,
                title: 'Test Folder',
                type: 'folder'
            };

            beforeEach(() => {
                spectator.setInput('items', [mockFolder]);
                spectator.setInput('loading', false);
                spectator.detectChanges();
            });

            it('should not show lock icon for folders', () => {
                const lockIcon = spectator.query(byTestId('lock-icon'))!;
                const lockOpenIcon = spectator.query(byTestId('lock-open-icon'))!;

                expect(lockIcon).toBeFalsy();
                expect(lockOpenIcon).toBeFalsy();
            });

            it('should not show status badge for folders', () => {
                const statusColumn = spectator.query(byTestId('item-status'))!;
                const statusBadge = statusColumn?.querySelector('dot-contentlet-status-badge');

                expect(statusBadge).toBeFalsy();
                expect(statusColumn?.textContent?.trim()).toBe('');
            });

            it('should not show language tag for folders', () => {
                const languageColumn = spectator.query(byTestId('item-language'))!;
                const languageTag = languageColumn?.querySelector('p-tag');

                expect(languageTag).toBeFalsy();
                expect(languageColumn?.textContent?.trim()).toBe('');
            });

            it('should have a content type column for folders', () => {
                // Query the content type column (same pattern as regular items test)
                const contentTypeColumn = spectator.query(byTestId('item-content-type'))!;

                expect(contentTypeColumn).toBeTruthy();
            });

            it('should show owner instead of modUserName for folders', () => {
                const modUserNameColumn = spectator.query(byTestId('item-mod-user-name'))!;

                expect(modUserNameColumn?.textContent?.trim()).toBe('admin');
            });

            it('should show folder title', () => {
                const titleColumn = spectator.query(byTestId('item-title'))!;

                expect(titleColumn?.textContent?.trim()).toContain('Test Folder');
            });

            it('should show folder icon instead of contentlet thumbnail for folders', () => {
                const contentletThumbnail = spectator.query(byTestId('contentlet-thumbnail'))!;
                const folderIcon = spectator.query(byTestId('folder-icon'))!;

                expect(contentletThumbnail).toBeFalsy();
                expect(folderIcon).toBeTruthy();
            });

            it('should have kebab menu button for folders', () => {
                const kebabButton = spectator.query(byTestId('kebab-menu-button'))!;

                expect(kebabButton).toBeTruthy();
            });

            it('should emit rightClick when folder row is right clicked', () => {
                const rightClickSpy = jest.spyOn(spectator.component.rightClick, 'emit');
                const row = spectator.query(byTestId('item-row'))!;

                spectator.dispatchFakeEvent(row, 'contextmenu');

                expect(rightClickSpy).toHaveBeenCalledWith({
                    event: expect.any(Event),
                    contentlet: mockFolder
                });
            });

            it('should emit rightClick when folder kebab menu button is clicked', () => {
                const rightClickSpy = jest.spyOn(spectator.component.rightClick, 'emit');
                const kebabButton = spectator.debugElement.query(
                    By.css('[data-testId="kebab-menu-button"]')
                );

                spectator.triggerEventHandler(kebabButton, 'onClick', new Event('click'));

                expect(rightClickSpy).toHaveBeenCalledWith({
                    event: expect.any(Event),
                    contentlet: mockFolder
                });
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
            expect(spectator.component.selectedItems).toEqual([firstItem, secondItem]);

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
            expect(spectator.component.selectedItems).toEqual([firstItem]);

            // Change to empty items
            spectator.setInput('items', []);
            spectator.detectChanges();

            // Selected items should be cleared
            expect(spectator.component.selectedItems).toEqual([]);
        });

        /**
         * A caller-provided `selection` makes the table controlled: the parent owns the checked set
         * and this component only reports changes to it. Without an input the table stays
         * uncontrolled and keeps the behaviour the two cases above describe.
         *
         * The Action Center's action preview needs the controlled mode — it lets the user uncheck
         * rows before firing, and that set has to live in the dialog, not in the table.
         */
        describe('caller-provided selection', () => {
            const [firstItem, secondItem, thirdItem] = mockItems;

            /**
             * Checked state per row, read from what the checkbox renders from: `TableCheckbox`
             * assigns `this.checked = dt.isSelected(value)`. Not the `input` element's `checked`
             * property — that is a focus proxy PrimeNG never drives, so it reads false even for a
             * genuinely selected row.
             */
            const rowChecked = (): boolean[] => {
                const table = spectator.query(Table)!;

                return mockItems.map((item) => table.isSelected(item));
            };

            beforeEach(() => {
                spectator.setInput('items', mockItems);
                spectator.detectChanges();
            });

            it('should render the caller-provided selection as the checked set', () => {
                spectator.setInput('selection', [firstItem, secondItem]);
                spectator.detectChanges();

                expect(spectator.component.selectedItems).toEqual([firstItem, secondItem]);
            });

            it('should pass the caller-provided selection down to the table', () => {
                // Guards the template binding, not just the component field: an internal signal that
                // never reaches `p-table` would leave every checkbox unchecked.
                spectator.setInput('selection', [firstItem]);
                spectator.detectChanges();

                expect(spectator.query(Table)!.selection).toEqual([firstItem]);
            });

            it('should follow the caller-provided selection when it changes', () => {
                spectator.setInput('selection', [firstItem]);
                spectator.detectChanges();

                spectator.setInput('selection', [secondItem, thirdItem]);
                spectator.detectChanges();

                expect(spectator.component.selectedItems).toEqual([secondItem, thirdItem]);
            });

            it('should NOT discard a caller-provided selection when items change', () => {
                // The regression this whole mode exists to avoid. The clearing effect above is right
                // for the browsing grid — a new page of results has nothing to do with the old
                // selection — but in the preview the rows and the selection are the same data, so
                // clearing on an items change would empty the payload the user is about to fire.
                spectator.setInput('selection', [firstItem, secondItem]);
                spectator.detectChanges();

                spectator.setInput('items', [...mockItems]);
                spectator.detectChanges();

                expect(spectator.component.selectedItems).toEqual([firstItem, secondItem]);
            });

            it('should emit selectionChange without taking ownership of the set', () => {
                // Controlled means the parent decides: this reports the user's intent and waits to
                // be told the new set, rather than applying it locally and drifting from the parent.
                const selectionChangeSpy = jest.spyOn(spectator.component.selectionChange, 'emit');

                spectator.setInput('selection', [firstItem]);
                spectator.detectChanges();

                spectator.component.onSelectionChange([firstItem, secondItem]);

                expect(selectionChangeSpy).toHaveBeenCalledWith([firstItem, secondItem]);
                expect(spectator.component.selectedItems).toEqual([firstItem]);
            });

            it('should stay on the parent’s set when the parent declines a change', () => {
                // The point of controlled mode: the parent can refuse. PrimeNG mutates its own
                // selection on a checkbox click, and if the parent then hands back an unchanged
                // input, Angular sees no change and re-runs nothing — leaving the checkbox visually
                // ahead of the set that will actually be fired.
                spectator.setInput('selection', [firstItem, secondItem]);
                spectator.detectChanges();

                expect(rowChecked()).toEqual([true, true, false, false, false]);

                spectator.click(
                    spectator.queryAll(byTestId('item-row'))![0].querySelector('input')!
                );
                spectator.detectChanges();

                // Parent said nothing, so the row is still in — and the box has to say so. Asserted
                // on what the row renders, not on the model: the model was already right, and it was
                // the box drifting away from it.
                expect(spectator.component.selectedItems).toEqual([firstItem, secondItem]);
                expect(rowChecked()).toEqual([true, true, false, false, false]);
            });

            it('should follow the parent when it narrows the set to something else', () => {
                // The parent normalising rather than echoing: what it returns is what renders, not
                // what the click implied.
                spectator.setInput('selection', [firstItem, secondItem]);
                spectator.detectChanges();

                spectator.click(
                    spectator.queryAll(byTestId('item-row'))![0].querySelector('input')!
                );
                spectator.setInput('selection', [thirdItem]);
                spectator.detectChanges();

                expect(rowChecked()).toEqual([false, false, true, false, false]);
            });

            it('should drag the caller-provided selection', () => {
                // `onDragStart` reads the effective selection; it must see the caller's, not a
                // stale internal one. (The preview sets `readOnly`, but the input pair is
                // independent of that and the grid should stay coherent either way.)
                const dragStartSpy = jest.spyOn(spectator.component.dragStart, 'emit');

                spectator.setInput('selection', [firstItem, secondItem]);
                spectator.detectChanges();

                spectator.component.onDragStart(createDragStartEvent(), firstItem);

                expect(dragStartSpy).toHaveBeenCalledWith([firstItem, secondItem]);
            });
        });
    });

    describe('selectionMode', () => {
        it('should default to multiple and show header checkbox', () => {
            expect(spectator.component.$selectionMode()).toBe('multiple');
            expect(spectator.query(byTestId('header-checkbox'))).toBeTruthy();
            expect(spectator.query(byTestId('item-radio'))).toBeFalsy();
        });

        it('should hide header checkbox and show radios in single mode', () => {
            spectator.setInput('selectionMode', 'single');
            spectator.setInput('items', mockItems);
            spectator.setInput('loading', false);
            spectator.detectChanges();

            expect(spectator.query(byTestId('header-checkbox'))).toBeFalsy();
            expect(spectator.queryAll(byTestId('item-radio')).length).toBeGreaterThan(0);
        });

        it('should emit a one-item array when selection changes in single mode', () => {
            spectator.setInput('selectionMode', 'single');
            spectator.setInput('items', mockItems);
            spectator.detectChanges();

            const selectionChangeSpy = jest.spyOn(spectator.component.selectionChange, 'emit');
            const table = spectator.debugElement.query(By.css('[data-testId="table"]'));

            spectator.triggerEventHandler(table, 'selectionChange', mockItems[0]);

            expect(selectionChangeSpy).toHaveBeenCalledWith([mockItems[0]]);
            expect(spectator.component.selectedItems).toEqual(mockItems[0]);
        });

        it('should clear selection to null when items change in single mode', () => {
            spectator.setInput('selectionMode', 'single');
            spectator.setInput('items', mockItems);
            spectator.detectChanges();

            spectator.component.selectedItems = mockItems[0];
            spectator.setInput('items', [mockItems[1]]);
            spectator.detectChanges();

            expect(spectator.component.selectedItems).toBeNull();
        });
    });

    describe('showActions', () => {
        beforeEach(() => {
            spectator.setInput('items', mockItems);
            spectator.setInput('loading', false);
            spectator.detectChanges();
        });

        it('should default to showing row actions so Content Drive is unchanged', () => {
            expect(spectator.component.$showActions()).toBe(true);
            expect(spectator.query(byTestId('kebab-menu-button'))).toBeTruthy();
        });

        it('should drop the kebab column when actions are off', () => {
            // The AssetPicker's rows are things you pick, not things you manage.
            spectator.setInput('showActions', false);
            spectator.detectChanges();

            expect(spectator.query(byTestId('kebab-menu-button'))).toBeFalsy();
            expect(spectator.query(byTestId('item-actions'))).toBeFalsy();
        });

        it('should drop the actions header cell too, so the columns stay aligned', () => {
            const withActions = spectator.queryAll(
                '[data-testId="header-column-sortable"], [data-testId="header-column-not-sortable"]'
            ).length;

            spectator.setInput('showActions', false);
            spectator.detectChanges();

            expect(
                spectator.queryAll(
                    '[data-testId="header-column-sortable"], [data-testId="header-column-not-sortable"]'
                ).length
            ).toBe(withActions - 1);
        });

        it('should leave the browser context menu alone when actions are off', () => {
            spectator.setInput('showActions', false);
            spectator.detectChanges();

            const rightClickSpy = jest.spyOn(spectator.component.rightClick, 'emit');
            const event = new MouseEvent('contextmenu', { cancelable: true });

            spectator.component.onContextMenu(event, mockItems[0]);

            expect(rightClickSpy).not.toHaveBeenCalled();
            expect(event.defaultPrevented).toBe(false);
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

                    spectator.component.onSelectionChange([]);
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

                    spectator.component.onSelectionChange([]);
                    spectator.component.onDragStart(dragEvent, firstItem);

                    expect(dragEvent.stopPropagation).not.toHaveBeenCalled();
                    expect(dragStartSpy).not.toHaveBeenCalled();
                });
            });

            describe('multiple selection', () => {
                it('should handle drag of multiple selected items', () => {
                    const dragEvent = createDragStartEvent();

                    spectator.component.onSelectionChange([firstItem, secondItem]);
                    spectator.component.onDragStart(dragEvent, firstItem);

                    expect(dragEvent.stopPropagation).toHaveBeenCalled();
                    expect(dragEvent.dataTransfer?.effectAllowed).toBe('move');
                    expect(dragEvent.dataTransfer?.setData).toHaveBeenCalledWith(DOT_DRAG_ITEM, '');
                    expect(dragStartSpy).toHaveBeenCalledWith([firstItem, secondItem]);
                });

                it('should drag all selected items when dragging one of them', () => {
                    const dragEvent = createDragStartEvent();

                    spectator.component.onSelectionChange([firstItem, secondItem]);
                    // Drag the second item which is in selection
                    spectator.component.onDragStart(dragEvent, secondItem);

                    expect(dragStartSpy).toHaveBeenCalledWith([firstItem, secondItem]);
                });
            });

            describe('multiple selection but single drag', () => {
                it('should not drag selected items when dragging a different item', () => {
                    const dragEvent = createDragStartEvent();
                    const thirdItem = mockItems[2];

                    spectator.component.onSelectionChange([firstItem, secondItem]);
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

            it('should apply cursor-grabbing class to row when isDragging is true', () => {
                const event = createDragStartEvent();
                const item = mockItems[0];

                spectator.component.onDragStart(event, item);
                spectator.detectChanges();

                const row = spectator.query(byTestId('item-row'))! as HTMLElement;
                expect(row.classList.contains('cursor-grabbing')).toBe(true);
                expect(spectator.component.state.isDragging()).toBe(true);
            });

            it('should remove cursor-grabbing class from row when isDragging is false', () => {
                const event = createDragStartEvent();
                const item = mockItems[0];

                // Start dragging
                spectator.component.onDragStart(event, item);
                spectator.detectChanges();

                let row = spectator.query(byTestId('item-row'))! as HTMLElement;
                expect(row.classList.contains('cursor-grabbing')).toBe(true);
                expect(spectator.component.state.isDragging()).toBe(true);

                // End dragging
                spectator.component.onDragEnd();
                spectator.detectChanges();

                row = spectator.query(byTestId('item-row'))! as HTMLElement;
                expect(row.classList.contains('cursor-grabbing')).toBe(false);
                expect(spectator.component.state.isDragging()).toBe(false);
            });

            it('should reflect isDragging state changes in the DOM immediately', () => {
                const event = createDragStartEvent();
                const item = mockItems[0];

                // Verify initial state in DOM
                let row = spectator.query(byTestId('item-row'))! as HTMLElement;
                expect(row.classList.contains('cursor-grabbing')).toBe(false);

                // Start drag and verify state + DOM
                spectator.component.onDragStart(event, item);
                spectator.detectChanges();

                row = spectator.query(byTestId('item-row'))! as HTMLElement;
                expect(spectator.component.state.isDragging()).toBe(true);
                expect(row.classList.contains('cursor-grabbing')).toBe(true);

                // End drag and verify state + DOM
                spectator.component.onDragEnd();
                spectator.detectChanges();

                row = spectator.query(byTestId('item-row'))! as HTMLElement;
                expect(spectator.component.state.isDragging()).toBe(false);
                expect(row.classList.contains('cursor-grabbing')).toBe(false);
            });
        });

        describe('onDragOver', () => {
            beforeEach(() => {
                spectator.setInput('items', mockItems);
                spectator.setInput('loading', false);
                spectator.detectChanges();
            });

            it('should set dragOverRowId when dragging over a row with internal drag', () => {
                const row = spectator.query(byTestId('item-row'))! as HTMLElement;
                const dragOverEvent = createDragOverEvent();
                const preventDefaultSpy = jest.spyOn(dragOverEvent, 'preventDefault');

                row.dispatchEvent(dragOverEvent);
                spectator.detectChanges();

                expect(spectator.component.state.dragOverRowId()).toBe(firstItem.identifier);
                expect(preventDefaultSpy).toHaveBeenCalled();
            });

            it('should not set dragOverRowId when dragging over with file drop', () => {
                const row = spectator.query(byTestId('item-row'))! as HTMLElement;
                const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
                const dragOverEvent = createFileDragOverEvent([mockFile]);

                row.dispatchEvent(dragOverEvent);
                spectator.detectChanges();

                expect(spectator.component.state.dragOverRowId()).toBeNull();
            });

            it('should not set dragOverRowId when dataTransfer is null', () => {
                const row = spectator.query(byTestId('item-row'))! as HTMLElement;
                const dragOverEvent = new DragEvent('dragover');
                Object.defineProperty(dragOverEvent, 'dataTransfer', {
                    value: null,
                    writable: true
                });

                row.dispatchEvent(dragOverEvent);
                spectator.detectChanges();

                expect(spectator.component.state.dragOverRowId()).toBeNull();
            });

            it('should set dragOverRowId when dragOverRowId matches item identifier', () => {
                const row = spectator.query(byTestId('item-row'))! as HTMLElement;
                const dragOverEvent = createDragOverEvent();

                row.dispatchEvent(dragOverEvent);
                spectator.detectChanges();

                expect(spectator.component.state.dragOverRowId()).toBe(firstItem.identifier);
            });

            it('should update dragOverRowId when dragging over different rows', () => {
                const rows = spectator.queryAll(byTestId('item-row'))! as HTMLElement[];
                const dragOverEvent = createDragOverEvent();

                // Drag over second item
                rows[1].dispatchEvent(dragOverEvent);
                spectator.detectChanges();

                // dragOverRowId should be set to the second item
                expect(spectator.component.state.dragOverRowId()).toBe(secondItem.identifier);
            });
        });

        describe('onDrop', () => {
            beforeEach(() => {
                spectator.setInput('items', mockItems);
                spectator.setInput('loading', false);
                spectator.detectChanges();
            });

            it('should clear dragOverRowId when dropping on a row with internal drag', () => {
                const row = spectator.query(byTestId('item-row'))! as HTMLElement;
                const dropSpy = jest.spyOn(spectator.component.drop, 'emit');
                const dropEvent = new DragEvent('drop');
                Object.defineProperty(dropEvent, 'dataTransfer', {
                    value: {
                        types: [DOT_DRAG_ITEM],
                        files: [],
                        preventDefault: jest.fn(),
                        stopPropagation: jest.fn()
                    },
                    writable: true
                });

                // Set dragOverRowId first
                const dragOverEvent = createDragOverEvent();
                row.dispatchEvent(dragOverEvent);
                spectator.detectChanges();
                expect(spectator.component.state.dragOverRowId()).toBe(firstItem.identifier);

                // Now drop
                row.dispatchEvent(dropEvent);
                spectator.detectChanges();

                expect(spectator.component.state.dragOverRowId()).toBeNull();
                expect(dropSpy).toHaveBeenCalledWith(firstItem);
            });

            it('should not handle file drops and let them bubble up', () => {
                const row = spectator.query(byTestId('item-row'))! as HTMLElement;
                const dropSpy = jest.spyOn(spectator.component.drop, 'emit');
                const mockFile = new File(['test'], 'test.jpg', { type: 'image/jpeg' });
                const dropEvent = new DragEvent('drop');
                Object.defineProperty(dropEvent, 'dataTransfer', {
                    value: {
                        types: ['Files'],
                        files: [mockFile]
                    },
                    writable: true
                });

                row.dispatchEvent(dropEvent);
                spectator.detectChanges();

                expect(dropSpy).not.toHaveBeenCalled();
            });

            it('should not handle drops that are not internal drags', () => {
                const row = spectator.query(byTestId('item-row'))! as HTMLElement;
                const dropSpy = jest.spyOn(spectator.component.drop, 'emit');
                const dropEvent = new DragEvent('drop');
                Object.defineProperty(dropEvent, 'dataTransfer', {
                    value: {
                        types: ['text/plain'],
                        files: []
                    },
                    writable: true
                });

                row.dispatchEvent(dropEvent);
                spectator.detectChanges();

                expect(dropSpy).not.toHaveBeenCalled();
            });

            it('should clear dragOverRowId on drop even if it was set', () => {
                const row = spectator.query(byTestId('item-row'))! as HTMLElement;
                const dropEvent = new DragEvent('drop');
                Object.defineProperty(dropEvent, 'dataTransfer', {
                    value: {
                        types: [DOT_DRAG_ITEM],
                        files: [],
                        preventDefault: jest.fn(),
                        stopPropagation: jest.fn()
                    },
                    writable: true
                });

                // Set dragOverRowId first
                const dragOverEvent = createDragOverEvent();
                row.dispatchEvent(dragOverEvent);
                spectator.detectChanges();

                row.dispatchEvent(dropEvent);
                spectator.detectChanges();

                expect(spectator.component.state.dragOverRowId()).toBeNull();
            });
        });

        describe('onDragEnd', () => {
            beforeEach(() => {
                spectator.setInput('items', mockItems);
                spectator.setInput('loading', false);
                spectator.detectChanges();
            });

            it('should clear dragOverRowId when drag ends', () => {
                const row = spectator.query(byTestId('item-row'))! as HTMLElement;
                const dragOverEvent = createDragOverEvent();

                // Set dragOverRowId first
                row.dispatchEvent(dragOverEvent);
                spectator.detectChanges();
                expect(spectator.component.state.dragOverRowId()).toBe(firstItem.identifier);

                // End drag
                spectator.dispatchFakeEvent(row, 'dragend');
                spectator.detectChanges();

                expect(spectator.component.state.dragOverRowId()).toBeNull();
                expect(spectator.component.state.isDragging()).toBe(false);
            });

            it('should clear dragOverRowId and isDragging state together', () => {
                const row = spectator.query(byTestId('item-row'))! as HTMLElement;
                const dragStartEvent = createDragStartEvent();
                const dragOverEvent = createDragOverEvent();

                // Start drag
                row.dispatchEvent(dragStartEvent);
                spectator.detectChanges();
                // Drag over
                row.dispatchEvent(dragOverEvent);
                spectator.detectChanges();

                expect(spectator.component.state.isDragging()).toBe(true);
                expect(spectator.component.state.dragOverRowId()).toBe(firstItem.identifier);

                // End drag
                spectator.dispatchFakeEvent(row, 'dragend');
                spectator.detectChanges();

                expect(spectator.component.state.isDragging()).toBe(false);
                expect(spectator.component.state.dragOverRowId()).toBeNull();
            });
        });

        describe('dragOverRowId state management', () => {
            beforeEach(() => {
                spectator.setInput('items', mockItems);
                spectator.setInput('loading', false);
                spectator.detectChanges();
            });

            it('should initialize dragOverRowId as null', () => {
                expect(spectator.component.state.dragOverRowId()).toBeNull();
            });

            it('should update dragOverRowId when dragging over different items', () => {
                const rows = spectator.queryAll(byTestId('item-row'))! as HTMLElement[];
                const dragOverEvent = createDragOverEvent();

                // Drag over first item
                rows[0].dispatchEvent(dragOverEvent);
                spectator.detectChanges();
                expect(spectator.component.state.dragOverRowId()).toBe(firstItem.identifier);

                // Drag over second item
                rows[1].dispatchEvent(dragOverEvent);
                spectator.detectChanges();
                expect(spectator.component.state.dragOverRowId()).toBe(secondItem.identifier);
            });

            it('should reflect dragOverRowId state changes immediately', () => {
                const row = spectator.query(byTestId('item-row'))! as HTMLElement;
                const dragOverEvent = createDragOverEvent();

                // Verify initial state
                expect(spectator.component.state.dragOverRowId()).toBeNull();

                // Drag over first item
                row.dispatchEvent(dragOverEvent);
                spectator.detectChanges();

                expect(spectator.component.state.dragOverRowId()).toBe(firstItem.identifier);
            });
        });
    });

    describe('Context Menu Events', () => {
        beforeEach(() => {
            spectator.setInput('items', mockItems);
            spectator.setInput('loading', false);
            spectator.detectChanges();
        });

        it('should emit rightClick event when row is right clicked', () => {
            const rightClickSpy = jest.spyOn(spectator.component.rightClick, 'emit');
            const row = spectator.query(byTestId('item-row'))!;

            spectator.dispatchFakeEvent(row, 'contextmenu');

            expect(rightClickSpy).toHaveBeenCalledWith({
                event: expect.any(Event),
                contentlet: mockItems[0]
            });
        });

        it('should prevent default when context menu is triggered', () => {
            const mockEvent = { preventDefault: jest.fn() } as unknown as Event;

            spectator.component.onContextMenu(mockEvent, mockItems[0]);

            expect(mockEvent.preventDefault).toHaveBeenCalled();
        });

        it('should emit rightClick event when kebab menu button is clicked', () => {
            const rightClickSpy = jest.spyOn(spectator.component.rightClick, 'emit');
            const kebabButton = spectator.debugElement.query(
                By.css('[data-testId="kebab-menu-button"]')
            );

            // PrimeNG button uses onClick event, not click
            spectator.triggerEventHandler(kebabButton, 'onClick', new Event('click'));

            expect(rightClickSpy).toHaveBeenCalledWith({
                event: expect.any(Event),
                contentlet: mockItems[0]
            });
        });

        it('should call onContextMenu with correct item when kebab menu button is clicked', () => {
            const onContextMenuSpy = jest.spyOn(spectator.component, 'onContextMenu');
            const kebabButton = spectator.debugElement.query(
                By.css('[data-testId="kebab-menu-button"]')
            );

            // PrimeNG button uses onClick event, not click
            spectator.triggerEventHandler(kebabButton, 'onClick', new Event('click'));

            expect(onContextMenuSpy).toHaveBeenCalledWith(expect.any(Event), mockItems[0]);
        });

        it('should emit rightClick with correct item for different rows', () => {
            const rightClickSpy = jest.spyOn(spectator.component.rightClick, 'emit');
            const rows = spectator.queryAll(byTestId('item-row'))!;

            // Right click on second row
            spectator.dispatchFakeEvent(rows[1], 'contextmenu');

            expect(rightClickSpy).toHaveBeenCalledWith({
                event: expect.any(Event),
                contentlet: mockItems[1]
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
            const row = spectator.query(byTestId('item-row'))!;

            spectator.dispatchFakeEvent(row, 'dblclick');

            expect(doubleClickSpy).toHaveBeenCalledWith(mockItems[0]);
        });

        it('should emit doubleClick event when thumbnail is clicked', () => {
            const emitSpy = jest.spyOn(spectator.component.doubleClick, 'emit');
            const thumbnail = spectator.query(byTestId('contentlet-thumbnail'))!;

            spectator.click(thumbnail);

            expect(emitSpy).toHaveBeenCalledWith(mockItems[0]);
        });

        it('should emit doubleClick event when title text is clicked', () => {
            const emitSpy = jest.spyOn(spectator.component.doubleClick, 'emit');
            const titleText = spectator.query(byTestId('item-title-text'))!;

            spectator.click(titleText);

            expect(emitSpy).toHaveBeenCalledWith(mockItems[0]);
        });

        it('should swallow the title click so the row is not selected underneath', () => {
            // Content Drive's title is an "open" affordance, distinct from selecting the row.
            const event = new MouseEvent('click', { bubbles: true, cancelable: true });
            const stopPropagation = jest.spyOn(event, 'stopPropagation');

            spectator.component.onTitleClick(event, mockItems[0]);

            expect(stopPropagation).toHaveBeenCalled();
        });
    });

    describe('titleOpensItem', () => {
        beforeEach(() => {
            spectator.setInput('items', mockItems);
            spectator.setInput('loading', false);
            spectator.detectChanges();
        });

        it('should default to opening the item, leaving Content Drive untouched', () => {
            expect(spectator.component.$titleOpensItem()).toBe(true);
        });

        it('should not open the item when turned off', () => {
            spectator.setInput('titleOpensItem', false);
            spectator.detectChanges();

            const emitSpy = jest.spyOn(spectator.component.doubleClick, 'emit');
            spectator.click(spectator.query(byTestId('item-title-text'))!);

            expect(emitSpy).not.toHaveBeenCalled();
        });

        it('should let the title click reach the row so the whole row selects', () => {
            // The regression: the title swallowed the click, so only the cell padding selected the
            // row and the radio fell out of step with the picker's stored selection.
            spectator.setInput('titleOpensItem', false);
            spectator.detectChanges();

            const event = new MouseEvent('click', { bubbles: true, cancelable: true });
            const stopPropagation = jest.spyOn(event, 'stopPropagation');

            spectator.component.onTitleClick(event, mockItems[0]);

            expect(stopPropagation).not.toHaveBeenCalled();
        });

        it('should select the row when its title is clicked', () => {
            spectator.setInput('titleOpensItem', false);
            spectator.setInput('selectionMode', 'single');
            spectator.detectChanges();

            const selectionSpy = jest.spyOn(spectator.component.selectionChange, 'emit');
            spectator.click(spectator.query(byTestId('item-title-text'))!);

            expect(selectionSpy).toHaveBeenCalledWith([mockItems[0]]);
        });
    });

    describe('Scroll Events', () => {
        beforeEach(() => {
            spectator.setInput('items', mockItems);
            spectator.setInput('loading', false);
            spectator.detectChanges();
        });

        afterEach(() => {
            jest.clearAllMocks();
        });

        it('should emit scroll event when table body is scrolled', () => {
            const scrollSpy = jest.spyOn(spectator.component.scroll, 'emit');
            const tableBody = spectator.query('.p-datatable-table-container')! as HTMLElement;

            const scrollEvent = new Event('scroll');
            tableBody.dispatchEvent(scrollEvent);

            expect(scrollSpy).toHaveBeenCalledWith(scrollEvent);
        });

        it('should add scroll event listener on ngAfterViewInit and emit scroll events', () => {
            const tableBody = spectator.query('.p-datatable-table-container')! as HTMLElement;
            const addListenerSpy = jest.spyOn(tableBody, 'addEventListener');

            spectator.component.ngAfterViewInit();

            expect(addListenerSpy).toHaveBeenCalledWith('scroll', expect.any(Function));

            // Verify the listener emits scroll events
            const scrollSpy = jest.spyOn(spectator.component.scroll, 'emit');
            const scrollEvent = new Event('scroll');
            tableBody.dispatchEvent(scrollEvent);

            expect(scrollSpy).toHaveBeenCalledWith(scrollEvent);
        });

        it('should remove scroll event listener on ngOnDestroy and stop emitting', () => {
            const tableBody = spectator.query('.p-datatable-table-container')! as HTMLElement;
            const removeListenerSpy = jest.spyOn(tableBody, 'removeEventListener');

            spectator.component.ngOnDestroy();

            expect(removeListenerSpy).toHaveBeenCalledWith('scroll', expect.any(Function));

            // Verify scroll events are no longer emitted after destroy
            const scrollSpy = jest.spyOn(spectator.component.scroll, 'emit');
            const scrollEvent = new Event('scroll');
            tableBody.dispatchEvent(scrollEvent);

            expect(scrollSpy).not.toHaveBeenCalled();
        });

        it('should not throw when ngOnDestroy is called without table body', () => {
            // Mock dataTable to return null for el.nativeElement.querySelector
            Object.defineProperty(spectator.component, 'dataTable', {
                value: () => ({
                    el: {
                        nativeElement: {
                            querySelector: () => null
                        }
                    }
                }),
                writable: true
            });

            expect(() => spectator.component.ngOnDestroy()).not.toThrow();
        });

        it('should not throw when ngAfterViewInit is called without table body', () => {
            // Mock dataTable to return null for el.nativeElement.querySelector
            Object.defineProperty(spectator.component, 'dataTable', {
                value: () => ({
                    el: {
                        nativeElement: {
                            querySelector: () => null
                        }
                    }
                }),
                writable: true
            });

            expect(() => spectator.component.ngAfterViewInit()).not.toThrow();
        });

        it('should not add event listener when dataTable is undefined', () => {
            Object.defineProperty(spectator.component, 'dataTable', {
                value: () => undefined,
                writable: true
            });

            expect(() => spectator.component.ngAfterViewInit()).not.toThrow();
        });

        it('should not remove event listener when dataTable is undefined', () => {
            Object.defineProperty(spectator.component, 'dataTable', {
                value: () => undefined,
                writable: true
            });

            expect(() => spectator.component.ngOnDestroy()).not.toThrow();
        });
    });

    describe('Extra columns', () => {
        const rows = [
            {
                identifier: '1',
                type: 'content',
                title: 'A',
                contentType: 'Blog',
                myText: 'hello',
                myBool: true,
                myDate: 1700000000000,
                myDateTime: 1700000000000
            },
            {
                identifier: '2',
                type: 'content',
                title: 'B',
                contentType: 'Blog',
                myText: 'a longer value here',
                myBool: false,
                myDate: 1700000000000,
                myDateTime: 1700000000000
            }
        ] as unknown as (typeof mockItems)[number][];

        const notSortableHeaders = () =>
            spectator.queryAll(byTestId('header-column-not-sortable'))! as HTMLElement[];
        const headerByLabel = (label: string) =>
            notSortableHeaders().find((th) => th.textContent?.trim() === label);

        beforeEach(() => {
            spectator.setInput('items', rows);
            spectator.setInput('loading', false);
        });

        it('should render the extra column header and a cell per row after the Type column', () => {
            spectator.setInput('extraColumns', [
                { field: 'myText', header: 'My Text', order: 0, type: 'text' }
            ]);
            spectator.detectChanges();

            expect(headerByLabel('My Text')).toBeTruthy();

            const cells = spectator.queryAll(byTestId('item-extra-myText'))!;
            expect(cells.length).toBe(2);
            expect(cells[0].textContent?.trim()).toBe('hello');

            // Sits right after the Type cell in the row.
            const firstRowCells = Array.from(
                spectator.query(byTestId('item-row'))?.querySelectorAll('td') ?? []
            );
            const typeIdx = firstRowCells.findIndex(
                (td) => td.getAttribute('data-testid') === 'item-content-type'
            );
            expect(firstRowCells[typeIdx + 1].getAttribute('data-testid')).toBe(
                'item-extra-myText'
            );
        });

        it('should drop an extra column whose field key collides with a fixed column', () => {
            spectator.setInput('extraColumns', [
                { field: 'title', header: 'Dup', order: 0, type: 'text' }
            ]);
            spectator.detectChanges();

            expect(spectator.query(byTestId('item-extra-title'))!).toBeFalsy();
            expect(headerByLabel('Dup')).toBeFalsy();
        });

        it('should render a thumbnail cell for an image column (per-field asset)', () => {
            spectator.setInput('items', [
                {
                    identifier: '1',
                    type: 'content',
                    title: 'A',
                    contentType: 'Blog',
                    photo: '/dA/abc/photo/pic.png'
                }
            ] as unknown as (typeof mockItems)[number][]);
            spectator.setInput('extraColumns', [
                { field: 'photo', header: 'Photo', order: 0, type: 'image' }
            ]);
            spectator.detectChanges();

            expect(spectator.query(byTestId('item-extra-image-photo'))!).toBeTruthy();
        });

        it('should render distinct boolean icons for true/false and stay blank when absent', () => {
            spectator.setInput('items', [
                { identifier: '1', type: 'content', title: 'A', myBool: true },
                { identifier: '2', type: 'content', title: 'B', myBool: false },
                { identifier: '3', type: 'content', title: 'C' } // no value → blank, not "false"
            ] as unknown as (typeof mockItems)[number][]);
            spectator.setInput('extraColumns', [
                { field: 'myBool', header: 'Bool', order: 0, type: 'boolean' }
            ]);
            spectator.detectChanges();

            const cells = spectator.queryAll(byTestId('item-extra-myBool'))!;
            expect(
                cells[0].querySelector('[data-testId="item-extra-bool"]')?.textContent?.trim()
            ).toBe('check_circle');
            expect(
                cells[1].querySelector('[data-testId="item-extra-bool"]')?.textContent?.trim()
            ).toBe('cancel');
            expect(cells[2].querySelector('[data-testId="item-extra-bool"]')).toBeFalsy();
        });

        it('should include the time for datetime but not for date', () => {
            spectator.setInput('extraColumns', [
                { field: 'myDate', header: 'D', order: 0, type: 'date' },
                { field: 'myDateTime', header: 'DT', order: 1, type: 'datetime' }
            ]);
            spectator.detectChanges();

            const dateText = spectator.query(byTestId('item-extra-myDate'))?.textContent ?? '';
            const dateTimeText =
                spectator.query(byTestId('item-extra-myDateTime'))?.textContent ?? '';
            expect(dateText).not.toContain(':');
            expect(dateTimeText).toContain(':');
        });

        it('formats date/datetime/time columns in UTC, not the viewer timezone', () => {
            // dotCMS stores these fields against the server zone (UTC) and the editor shows them in
            // that zone, so the table's DatePipe pins ':UTC' — otherwise a UTC+/-N viewer sees a
            // shifted day/hour that mismatches the editor and the filter. Run under a non-UTC zone
            // and seed a near-midnight-UTC instant so the calendar day AND hour differ from UTC:
            // dropping ':UTC' would render 01/14 21:30 (EST) and fail these exact assertions.
            const originalTz = process.env.TZ;
            process.env.TZ = 'America/New_York'; // UTC-5 in January

            try {
                const epoch = Date.UTC(2026, 0, 15, 2, 30); // 2026-01-15 02:30 UTC → 2026-01-14 21:30 EST
                spectator.setInput('items', [
                    { identifier: '1', type: 'content', title: 'A', d: epoch, dt: epoch, t: epoch }
                ] as unknown as (typeof mockItems)[number][]);
                spectator.setInput('extraColumns', [
                    { field: 'd', header: 'D', order: 0, type: 'date' },
                    { field: 'dt', header: 'DT', order: 1, type: 'datetime' },
                    { field: 't', header: 'T', order: 2, type: 'time' }
                ]);
                spectator.detectChanges();

                expect(spectator.query(byTestId('item-extra-d'))?.textContent?.trim()).toBe(
                    '01/15/2026'
                );
                expect(spectator.query(byTestId('item-extra-dt'))?.textContent?.trim()).toBe(
                    '01/15/2026 2:30 AM'
                );
                expect(spectator.query(byTestId('item-extra-t'))?.textContent?.trim()).toBe(
                    '2:30 AM'
                );
            } finally {
                if (originalTz === undefined) {
                    delete process.env.TZ;
                } else {
                    process.env.TZ = originalTz;
                }
            }
        });

        it('should size a text column by content (ch) and a date column with a fixed width', () => {
            spectator.setInput('extraColumns', [
                { field: 'myText', header: 'T', order: 0, type: 'text' },
                { field: 'myDate', header: 'D', order: 1, type: 'date' }
            ]);
            spectator.detectChanges();

            expect(headerByLabel('T')?.style.width).toMatch(/ch$/);
            expect(headerByLabel('D')?.style.width).toBe('12rem');
        });

        it('should clamp a text column width to the max for very long values', () => {
            spectator.setInput('items', [
                { identifier: '1', type: 'content', title: 'A', long: 'x'.repeat(200) }
            ] as unknown as (typeof mockItems)[number][]);
            spectator.setInput('extraColumns', [
                { field: 'long', header: 'L', order: 0, type: 'text' }
            ]);
            spectator.detectChanges();

            expect(headerByLabel('L')?.style.width).toBe('32ch');
        });

        it('should leave the table unchanged when no extra columns are provided', () => {
            spectator.setInput('extraColumns', []);
            spectator.detectChanges();

            expect(spectator.query(byTestId('item-extra-myText'))!).toBeFalsy();
        });
    });
});
