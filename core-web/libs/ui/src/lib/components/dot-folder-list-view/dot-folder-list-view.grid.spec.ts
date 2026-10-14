import { byTestId, createHostFactory, mockProvider, SpectatorHost } from '@openng/spectator/vitest';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { provideHttpClient } from '@angular/common/http';
import { By } from '@angular/platform-browser';

import { Paginator } from 'primeng/paginator';

import { DotFormatDateService, DotLanguagesService, DotMessageService } from '@dotcms/data-access';
import { DotcmsConfigService } from '@dotcms/dotcms-js';
import {
    DotCMSContentlet,
    DotContentDriveBrowseItem,
    DotContentDriveFolder,
    DotLanguage
} from '@dotcms/dotcms-models';
import { DotcmsConfigServiceMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DOT_DRAG_ITEM } from './constants';
import { DotFolderListViewComponent } from './dot-folder-list-view.component';
import { DotFolderListViewLayout } from './models';

const FOLDER = {
    type: 'folder',
    identifier: 'id-folder',
    inode: 'inode-folder',
    name: 'activities',
    title: 'Activities',
    path: '/activities/',
    owner: 'Daniel Ortiz',
    permissions: []
} as unknown as DotContentDriveFolder;

const content = (name: string, extra: Record<string, unknown> = {}) =>
    ({
        identifier: `id-${name}`,
        inode: `inode-${name}`,
        title: name,
        contentType: 'Blog',
        baseType: 'CONTENT',
        languageId: 1,
        live: true,
        working: true,
        archived: false,
        hasLiveVersion: true,
        locked: false,
        modUserName: 'Maya Chen',
        modDate: '2026-10-01T10:00:00Z',
        ...extra
    }) as unknown as DotCMSContentlet;

const IMAGE = content('image');
const BLOG = content('blog');
const PAGE = content('page');
const ITEMS: DotContentDriveBrowseItem[] = [FOLDER, IMAGE, BLOG, PAGE];

const LANGUAGES: DotLanguage[] = [
    {
        id: 1,
        language: 'English',
        languageCode: 'en',
        countryCode: 'US',
        country: 'USA',
        isoCode: 'en-US'
    }
];

/** A drag event jsdom can carry: it has no `DataTransfer`, so one is stubbed on. */
function dragEvent(type: string, types: string[] = [DOT_DRAG_ITEM], files: File[] = []) {
    const event = new Event(type, { bubbles: true, cancelable: true }) as DragEvent;
    Object.defineProperty(event, 'dataTransfer', {
        value: { types, files, effectAllowed: '', setData: vi.fn(), setDragImage: vi.fn() }
    });

    return event;
}

/**
 * A stand-in for a caller's card: a title, a preview, a checkbox and a ⋮, wired to the context the
 * grid hands every card the way the Drive's card is.
 */
const HOST_TEMPLATE = `
    <dot-folder-list-view
        [layout]="layout"
        [items]="items"
        [selection]="selection"
        [loading]="loading"
        [busyRows]="busyRows"
        [totalItems]="totalItems"
        [offset]="offset"
        [rows]="rows"
        [readOnly]="readOnly"
        [sortField]="sortField"
        [sortOrder]="sortOrder"
        dataKey="inode"
        (selectionChange)="selection = $event">
        <ng-template
            #card
            let-item
            let-id="id"
            let-selected="selected"
            let-busy="busy"
            let-dragOver="dragOver"
            let-readOnly="readOnly"
            let-languagesMap="languagesMap"
            let-toggle="toggle"
            let-open="open"
            let-menu="menu">
            <div
                data-testid="test-card"
                [attr.data-selected]="selected"
                [attr.data-busy]="busy"
                [attr.data-drag-over]="dragOver"
                [attr.data-languages]="languagesMap.size"
                (click)="open($event)">
                <i class="pi pi-file" data-drag-preview [attr.data-preview]="item.inode"></i>
                <span [id]="id + '-label'" data-testid="grid-card-title">{{ item.title }}</span>
                <span [id]="id + '-description'">{{ item.contentType }}</span>
                <span
                    data-testid="grid-card-checkbox"
                    (click)="$event.stopPropagation(); toggle($event)"></span>
                @if (!readOnly) {
                    <button data-testid="grid-card-menu" (click)="$event.stopPropagation(); menu($event)"></button>
                }
            </div>
        </ng-template>
    </dot-folder-list-view>
`;

interface GridHostProps {
    layout: DotFolderListViewLayout;
    items: DotContentDriveBrowseItem[];
    selection: DotContentDriveBrowseItem[];
    loading: boolean;
    busyRows: string[];
    totalItems: number;
    offset: number;
    rows: number;
    readOnly: boolean;
    sortField: string;
    sortOrder: number;
}

describe('DotFolderListViewComponent — grid layout (#37930)', () => {
    let spectator: SpectatorHost<DotFolderListViewComponent, GridHostProps>;

    const createHost = createHostFactory({
        component: DotFolderListViewComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    Folder: 'Folder',
                    Page: 'Page',
                    'content-drive.grid-view.label': 'Content',
                    'content.drive.empty.state.title': 'This folder is empty',
                    'content.drive.empty.state.description': 'Nothing here yet'
                })
            },
            mockProvider(DotcmsConfigService, new DotcmsConfigServiceMock()),
            mockProvider(DotFormatDateService),
            mockProvider(DotLanguagesService, { get: vi.fn().mockReturnValue(of(LANGUAGES)) }),
            provideHttpClient()
        ],
        detectChanges: false
    });

    const render = (props: Partial<GridHostProps> = {}) => {
        spectator = createHost(HOST_TEMPLATE, {
            hostProps: {
                layout: 'grid',
                items: ITEMS,
                selection: [],
                loading: false,
                busyRows: [],
                totalItems: ITEMS.length,
                offset: 0,
                rows: 20,
                readOnly: false,
                sortField: 'modDate',
                sortOrder: -1,
                ...props
            }
        });
        spectator.detectChanges();
    };

    const cards = () => spectator.queryAll<HTMLElement>(byTestId('grid-card'));
    const card = (index: number) => cards()[index];
    const rows = () => spectator.queryAll<HTMLElement>(byTestId('item-row'));
    const titles = () =>
        cards().map((element) =>
            element.querySelector('[data-testid="grid-card-title"]')?.textContent?.trim()
        );
    const tabIndexes = () => cards().map((element) => element.getAttribute('tabindex'));
    const setLayout = (layout: DotFolderListViewLayout) => {
        spectator.setHostInput({ layout });
        spectator.detectChanges();
    };
    /** Switches, then lets the after-render work (anchor, tab stop) land, as the browser would. */
    const setLayoutAndSettle = async (layout: DotFolderListViewLayout) => {
        setLayout(layout);
        await spectator.fixture.whenStable();
        spectator.detectChanges();
    };

    let emitted: DotContentDriveBrowseItem[][];

    const renderSelectable = (props: Partial<GridHostProps> = {}) => {
        render(props);
        emitted = [];
        spectator.output<DotContentDriveBrowseItem[]>('selectionChange').subscribe((items) => {
            emitted.push(items);
            spectator.detectChanges();
        });
    };

    describe('the page as cards', () => {
        it('should draw the caller card for each item, in the order given, and no table', () => {
            render();

            expect(titles()).toEqual(ITEMS.map((item) => item.title));
            expect(spectator.query('p-table')).toBeNull();
        });

        it('should hand each card its item, its state and the languages', () => {
            render({ selection: [IMAGE], busyRows: [BLOG.inode] });

            const bodies = spectator.queryAll<HTMLElement>(byTestId('test-card'));

            expect(bodies.map((body) => body.dataset['selected'])).toEqual([
                'false',
                'true',
                'false',
                'false'
            ]);
            expect(bodies.map((body) => body.dataset['busy'])).toEqual([
                'false',
                'false',
                'true',
                'false'
            ]);
            expect(bodies[0].dataset['languages']).toBe('1');
        });

        it('should be announced as a multi-select list whose options the cards name', () => {
            render();

            const list = spectator.query(byTestId('grid-list'));
            const first = card(0);
            const labelId = first.getAttribute('aria-labelledby') as string;
            const descriptionId = first.getAttribute('aria-describedby') as string;

            expect(list?.getAttribute('role')).toBe('listbox');
            expect(list?.getAttribute('aria-multiselectable')).toBe('true');
            expect(list?.getAttribute('aria-label')).toBe('Content');
            expect(cards().every((element) => element.getAttribute('role') === 'option')).toBe(
                true
            );
            expect(first.querySelector(`[id="${labelId}"]`)?.textContent?.trim()).toBe(
                'Activities'
            );
            expect(first.querySelector(`[id="${descriptionId}"]`)).toBeTruthy();
        });

        it('should show skeleton cards, one per row of the page, while loading', () => {
            render({ loading: true, rows: 40 });

            expect(cards().length).toBe(0);
            expect(spectator.queryAll(byTestId('grid-skeleton-card')).length).toBe(40);
        });

        it('should show the table empty state when there are no items', () => {
            render({ items: [], totalItems: 0 });

            expect(spectator.query(byTestId('grid-empty-state'))?.textContent).toContain(
                'This folder is empty'
            );
        });

        it('should tell the cards of a read-only listing to offer no actions', () => {
            render({ readOnly: true });

            expect(spectator.queryAll(byTestId('grid-card-menu')).length).toBe(0);
        });

        it('should offer no sort control (US6)', () => {
            render();

            expect(spectator.query('p-sorticon, [psortablecolumn]')).toBeNull();
        });

        it('should keep everything inside the cards within its own scroll box', () => {
            // Without a positioned ancestor, a card's absolutely positioned screen reader labels
            // anchored to the drop zone outside, stretched it, and drew a second scrollbar.
            render();

            expect(spectator.query(byTestId('grid-scroll'))?.classList).toContain('relative');
            expect(cards().every((element) => element.classList.contains('relative'))).toBe(true);
        });

        it('should lay cards out at least 240px wide with a 20px gap', () => {
            render();

            const list = spectator.query(byTestId('grid-list')) as HTMLElement;

            expect(list.className).toContain(
                'grid-cols-[repeat(auto-fill,minmax(min(240px,100%),1fr))]'
            );
            expect(list.className).toContain('gap-[20px]');
        });
    });

    describe('selecting with the checkbox', () => {
        const tick = (index: number, init: MouseEventInit = {}) =>
            card(index)
                .querySelector('[data-testid="grid-card-checkbox"]')
                ?.dispatchEvent(new MouseEvent('click', { bubbles: true, ...init }));

        it('should add a ticked card and keep the rest (US3 #1, #4)', () => {
            renderSelectable({ selection: [FOLDER] });

            tick(1);

            expect(emitted.at(-1)).toEqual([FOLDER, IMAGE]);
        });

        it('should remove an unticked card and keep the rest', () => {
            renderSelectable({ selection: [FOLDER, IMAGE] });

            tick(0);

            expect(emitted.at(-1)).toEqual([IMAGE]);
        });

        it('should mark selected cards for screen readers', () => {
            render({ selection: [IMAGE] });

            expect(cards().map((element) => element.getAttribute('aria-selected'))).toEqual([
                'false',
                'true',
                'false',
                'false'
            ]);
        });

        it('should select the range from the anchor on a Shift-click (US3 #5)', () => {
            renderSelectable();

            tick(0);
            tick(2, { shiftKey: true });

            expect(emitted.at(-1)).toEqual([FOLDER, IMAGE, BLOG]);
        });

        it('should treat a Shift-click with nothing selected as a plain tick', () => {
            renderSelectable();

            tick(2, { shiftKey: true });

            expect(emitted.at(-1)).toEqual([BLOG]);
        });

        it('should not select a busy card (FR-014)', () => {
            renderSelectable({ busyRows: [IMAGE.inode] });

            tick(1);

            expect(emitted).toEqual([]);
        });

        it('should leave busy cards out of a range (US7 #7)', () => {
            renderSelectable({ busyRows: [IMAGE.inode] });

            tick(0);
            tick(2, { shiftKey: true });

            expect(emitted.at(-1)).toEqual([FOLDER, BLOG]);
        });

        it('should select one language version only (US3 #6)', () => {
            const spanish = { ...IMAGE, inode: 'inode-image-es', languageId: 2 };
            renderSelectable({ items: [IMAGE, spanish] });

            tick(1);

            expect(emitted.at(-1)).toEqual([spanish]);
        });
    });

    describe('a card does what a row does', () => {
        const plainClick = (index: number) =>
            card(index)
                .querySelector('[data-testid="grid-card-title"]')
                ?.dispatchEvent(new MouseEvent('click', { bubbles: true, detail: 1 }));

        it('should open the clicked item (US5 #1)', () => {
            render();
            const open = vi.fn();
            spectator.output('doubleClick').subscribe(open);

            plainClick(2);

            expect(open).toHaveBeenCalledWith(BLOG);
        });

        it('should not open anything in a read-only listing', () => {
            render({ readOnly: true });
            const open = vi.fn();
            spectator.output('doubleClick').subscribe(open);

            plainClick(2);

            expect(open).not.toHaveBeenCalled();
        });

        it('should not open a busy item', () => {
            render({ busyRows: [BLOG.inode] });
            const open = vi.fn();
            spectator.output('doubleClick').subscribe(open);

            plainClick(2);

            expect(open).not.toHaveBeenCalled();
        });

        it('should ask for the row menu on a right-click (US5 #2)', () => {
            render();
            const rightClick = vi.fn();
            spectator.output('rightClick').subscribe(rightClick);
            const event = new MouseEvent('contextmenu', { bubbles: true, cancelable: true });

            card(1).dispatchEvent(event);

            expect(rightClick).toHaveBeenCalledWith({ event, contentlet: IMAGE });
            expect(event.defaultPrevented).toBe(true);
        });

        it('should keep the browser menu in a read-only listing', () => {
            render({ readOnly: true });
            const rightClick = vi.fn();
            spectator.output('rightClick').subscribe(rightClick);
            const event = new MouseEvent('contextmenu', { bubbles: true, cancelable: true });

            card(1).dispatchEvent(event);

            expect(rightClick).not.toHaveBeenCalled();
            expect(event.defaultPrevented).toBe(false);
        });

        it('should ask for the row menu from the card ⋮ (US1 #11)', () => {
            render();
            const rightClick = vi.fn();
            spectator.output('rightClick').subscribe(rightClick);

            spectator.click(card(1).querySelector('[data-testid="grid-card-menu"]') as Element);

            expect(rightClick).toHaveBeenCalledWith(expect.objectContaining({ contentlet: IMAGE }));
        });

        it('should drag every selected item when a selected card is dragged (US5 #5)', () => {
            render({ selection: [IMAGE, BLOG] });
            const dragStart = vi.fn();
            spectator.output('dragStart').subscribe(dragStart);

            card(1).dispatchEvent(dragEvent('dragstart'));

            expect(dragStart).toHaveBeenCalledWith([IMAGE, BLOG]);
        });

        it('should drag only the card when it is not selected', () => {
            render({ selection: [IMAGE] });
            const dragStart = vi.fn();
            spectator.output('dragStart').subscribe(dragStart);

            card(3).dispatchEvent(dragEvent('dragstart'));

            expect(dragStart).toHaveBeenCalledWith([PAGE]);
        });

        it('should draw the drag picture from the dragged language version', () => {
            // Two versions share an identifier. Looking the preview up by identifier found the
            // first one on the page, whichever was dragged.
            const spanish = { ...IMAGE, inode: 'inode-image-es', languageId: 2 };
            render({ items: [IMAGE, spanish] });
            const appendChild = vi.spyOn(document.body, 'appendChild');

            card(1).dispatchEvent(dragEvent('dragstart'));

            const picture = appendChild.mock.calls.at(-1)?.[0] as HTMLElement;
            expect(
                Array.from(picture.querySelectorAll('[data-preview]')).map((element) =>
                    element.getAttribute('data-preview')
                )
            ).toEqual(['inode-image-es']);
            appendChild.mockRestore();
        });

        it('should not drag in a read-only listing', () => {
            render({ readOnly: true });

            expect(card(1).getAttribute('draggable')).toBe('false');
        });

        it('should take an internal drop on a folder card (US5 #4)', () => {
            render();
            const drop = vi.fn();
            spectator.output('drop').subscribe(drop);
            const over = dragEvent('dragover');

            card(0).dispatchEvent(over);
            card(0).dispatchEvent(dragEvent('drop'));

            expect(over.defaultPrevented).toBe(true);
            expect(drop).toHaveBeenCalledWith(FOLDER);
        });

        it('should neither accept nor highlight a drag over a content card', () => {
            render();
            const over = dragEvent('dragover');

            card(1).dispatchEvent(over);
            spectator.detectChanges();

            expect(over.defaultPrevented).toBe(false);
            expect(
                spectator.queryAll<HTMLElement>(byTestId('test-card'))[1].dataset['dragOver']
            ).toBe('false');
        });

        it('should clear the highlight when the drag leaves the card', () => {
            render();
            const body = () => spectator.queryAll<HTMLElement>(byTestId('test-card'))[0];

            card(0).dispatchEvent(dragEvent('dragover'));
            spectator.detectChanges();
            expect(body().dataset['dragOver']).toBe('true');

            card(0).dispatchEvent(dragEvent('dragleave'));
            spectator.detectChanges();
            expect(body().dataset['dragOver']).toBe('false');
        });

        it('should leave a file drop to the upload drop zone (US5 #6)', () => {
            render();
            const drop = vi.fn();
            spectator.output('drop').subscribe(drop);
            const fileDrop = dragEvent('drop', ['Files'], [new File(['x'], 'x.txt')]);

            card(0).dispatchEvent(fileDrop);

            expect(drop).not.toHaveBeenCalled();
            expect(fileDrop.defaultPrevented).toBe(false);
        });

        it('should report the end of a drag', () => {
            render();
            const dragEnd = vi.fn();
            spectator.output('dragEnd').subscribe(dragEnd);

            card(1).dispatchEvent(dragEvent('dragend'));

            expect(dragEnd).toHaveBeenCalled();
        });
    });

    describe('paging', () => {
        const paginator = () => spectator.debugElement.query(By.directive(Paginator));
        const nextButton = () =>
            spectator.query<HTMLButtonElement>('[data-testid="grid-paginator"] .p-paginator-next');

        it('should ask for the next page in the table page shape (US5 #3)', () => {
            render({ totalItems: 100 });
            const paginate = vi.fn();
            spectator.output('paginate').subscribe(paginate);

            spectator.triggerEventHandler(paginator(), 'onPageChange', {
                first: 20,
                rows: 20,
                page: 1,
                pageCount: 5
            });

            expect(paginate).toHaveBeenCalledWith(
                expect.objectContaining({ first: 20, rows: 20, page: 2 })
            );
        });

        it('should not ask for another page while one is loading', () => {
            render({ totalItems: 100, loading: true });
            const paginate = vi.fn();
            spectator.output('paginate').subscribe(paginate);

            spectator.triggerEventHandler(paginator(), 'onPageChange', {
                first: 20,
                rows: 20,
                page: 1,
                pageCount: 5
            });

            expect(paginate).not.toHaveBeenCalled();
        });

        it('should disable Next for real while a page loads, so the keyboard cannot reach it', () => {
            // A CSS-only lock let Enter through: PrimeNG moved its own page while the request was
            // dropped, and the paginator read "Page 3" over page 2's cards.
            render({ totalItems: 100 });
            expect(nextButton()?.disabled).toBe(false);

            spectator.setHostInput({ loading: true });
            spectator.detectChanges();

            expect(nextButton()?.disabled).toBe(true);
        });
    });

    describe('keyboard', () => {
        const press = (index: number, key: string, init: KeyboardEventInit = {}) => {
            const target = card(index);
            target.focus();
            target.dispatchEvent(
                new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true, ...init })
            );
            spectator.detectChanges();
        };
        const focusedIndex = () => cards().indexOf(document.activeElement as HTMLElement);

        it('should have exactly one tab stop, on the first card (US7 #1)', () => {
            renderSelectable();

            expect(tabIndexes()).toEqual(['0', '-1', '-1', '-1']);
        });

        it('should take no tab stop in a read-only listing (FR-034)', () => {
            renderSelectable({ readOnly: true });

            expect(tabIndexes()).toEqual(['-1', '-1', '-1', '-1']);
        });

        it('should move focus and the tab stop with the arrows, not the selection (US7 #2)', () => {
            renderSelectable();

            press(0, 'ArrowRight');

            expect(focusedIndex()).toBe(1);
            expect(tabIndexes()).toEqual(['-1', '0', '-1', '-1']);
            expect(emitted).toEqual([]);
        });

        it('should move a whole row with Up and Down (US7 #3)', () => {
            renderSelectable();
            vi.spyOn(spectator.component, 'measureColumnCount').mockReturnValue(2);

            press(0, 'ArrowDown');

            expect(focusedIndex()).toBe(2);
        });

        it('should skip a busy card (US7 #7)', () => {
            renderSelectable({ busyRows: [IMAGE.inode] });

            press(0, 'ArrowRight');

            expect(focusedIndex()).toBe(2);
        });

        it('should grow and shrink the selection with Shift+arrows (US7 #5)', () => {
            renderSelectable();

            press(0, 'ArrowRight', { shiftKey: true });
            expect(emitted.at(-1)).toEqual([FOLDER, IMAGE]);

            press(1, 'ArrowRight', { shiftKey: true });
            expect(emitted.at(-1)).toEqual([FOLDER, IMAGE, BLOG]);

            press(2, 'ArrowLeft', { shiftKey: true });
            expect(emitted.at(-1)).toEqual([FOLDER, IMAGE]);
        });

        it.each(['Enter', ' '])('should toggle the focused card with %j (US7 #10)', (key) => {
            renderSelectable();

            press(1, key);
            expect(emitted.at(-1)).toEqual([IMAGE]);

            press(1, key);
            expect(emitted.at(-1)).toEqual([]);
        });

        it('should open the actions menu with Shift+F10', () => {
            renderSelectable();
            const rightClick = vi.fn();
            spectator.output('rightClick').subscribe(rightClick);

            press(1, 'F10', { shiftKey: true });

            expect(rightClick).toHaveBeenCalledWith(expect.objectContaining({ contentlet: IMAGE }));
        });

        it('should ignore the keys in a read-only listing', () => {
            renderSelectable({ readOnly: true });

            press(0, 'Enter');
            press(0, 'ArrowRight', { shiftKey: true });

            expect(emitted).toEqual([]);
        });

        it('should keep a tab stop when a new page arrives (US7 #11)', () => {
            renderSelectable();
            press(0, 'ArrowRight');
            press(1, 'ArrowRight');

            spectator.setHostInput({ items: [FOLDER] });
            spectator.detectChanges();

            expect(tabIndexes()).toEqual(['0']);
        });
    });

    describe('switching layout', () => {
        const rowTabIndexes = () => rows().map((row) => row.tabIndex);

        it('should keep the tab stop on the item the grid was on (US7 #12)', async () => {
            renderSelectable();
            card(2).focus();
            spectator.detectChanges();

            await setLayoutAndSettle('table');

            expect(rowTabIndexes()).toEqual([-1, -1, 0, -1]);
        });

        it('should keep the tab stop on the row the table was on', () => {
            renderSelectable({ layout: 'table' });
            rows()[2].focus();
            spectator.detectChanges();

            setLayout('grid');

            expect(tabIndexes()).toEqual(['-1', '-1', '0', '-1']);
        });

        it('should follow the user through table → grid → table → grid', () => {
            // Each layout used to report its own item to the other, and a report the table had
            // already sent once was dropped, so the last trip landed on a stale item.
            renderSelectable({ layout: 'table' });
            rows()[0].focus();
            spectator.detectChanges();

            setLayout('grid');
            card(1).focus();
            spectator.detectChanges();

            setLayout('table');
            rows()[0].focus();
            spectator.detectChanges();

            setLayout('grid');

            expect(tabIndexes()).toEqual(['0', '-1', '-1', '-1']);
        });

        it('should keep the selection anchor, so a Shift-click extends the range from it', async () => {
            renderSelectable({ layout: 'table', selection: [IMAGE] });
            rows()[1].focus();
            spectator.detectChanges();

            await setLayoutAndSettle('grid');
            card(3)
                .querySelector('[data-testid="grid-card-checkbox"]')
                ?.dispatchEvent(new MouseEvent('click', { bubbles: true, shiftKey: true }));

            expect(emitted.at(-1)).toEqual([IMAGE, BLOG, PAGE]);
        });

        it('should scroll the item the user was on into view after a switch', async () => {
            renderSelectable();
            card(2).focus();
            spectator.detectChanges();
            const scrollIntoView = vi.fn();
            Element.prototype.scrollIntoView = scrollIntoView;

            setLayout('table');
            await spectator.fixture.whenStable();

            expect(scrollIntoView).toHaveBeenCalledWith({ block: 'nearest' });
            expect(scrollIntoView.mock.contexts.at(-1)).toBe(rows()[2]);
        });

        it('should restore the sort the caller holds when the table comes back', () => {
            render({ sortField: 'title', sortOrder: 1 });

            setLayout('table');

            const table = spectator.component.dataTable();
            expect(table?.sortField).toBe('title');
            expect(table?.sortOrder).toBe(1);
        });
    });

    describe('table layout', () => {
        beforeEach(() => render({ layout: 'table' }));

        it('should draw the table and no grid', () => {
            expect(spectator.query('p-table')).toBeTruthy();
            expect(spectator.query(byTestId('grid-list'))).toBeNull();
        });
    });
});
