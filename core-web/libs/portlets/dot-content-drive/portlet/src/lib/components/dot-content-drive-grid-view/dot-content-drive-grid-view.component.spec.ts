import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { DotMessageService } from '@dotcms/data-access';
import { DotContentDriveItem } from '@dotcms/dotcms-models';
import { DOT_DRAG_ITEM } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveGridViewComponent } from './dot-content-drive-grid-view.component';
import {
    GRID_FOLDER,
    GRID_IMAGE,
    GRID_ITEMS,
    GRID_LANGUAGES,
    GRID_LOCKED_BLOG,
    GRID_MESSAGES,
    GRID_ORPHAN_PAGE
} from './grid-view.mocks';

/** A drag event jsdom can carry: it has no `DataTransfer`, so one is stubbed on. */
function dragEvent(type: string, types: string[] = [DOT_DRAG_ITEM], files: File[] = []) {
    const event = new Event(type, { bubbles: true, cancelable: true }) as DragEvent;
    Object.defineProperty(event, 'dataTransfer', {
        value: {
            types,
            files,
            effectAllowed: '',
            setData: vi.fn(),
            setDragImage: vi.fn()
        }
    });

    return event;
}

describe('DotContentDriveGridViewComponent', () => {
    let spectator: Spectator<DotContentDriveGridViewComponent>;

    const createComponent = createComponentFactory({
        component: DotContentDriveGridViewComponent,
        providers: [
            { provide: DotMessageService, useValue: new MockDotMessageService(GRID_MESSAGES) }
        ],
        detectChanges: false
    });

    const render = (inputs: Record<string, unknown> = {}) => {
        spectator = createComponent({
            props: {
                items: GRID_ITEMS,
                totalItems: GRID_ITEMS.length,
                languagesMap: GRID_LANGUAGES,
                ...inputs
            } as never
        });
        spectator.detectChanges();
    };

    const cards = () => spectator.queryAll<HTMLElement>(byTestId('grid-card'));
    const titles = () =>
        cards().map((card) =>
            card.querySelector('[data-testid="grid-card-title"]')?.textContent?.trim()
        );

    describe('US1 — the page as cards', () => {
        it('should draw one card per item, in the order given', () => {
            render();

            expect(titles()).toEqual(['activities', ...GRID_ITEMS.slice(1).map((i) => i.title)]);
        });

        it('should be announced as a multi-select list of content', () => {
            render();

            const list = spectator.query(byTestId('grid-list'));

            expect(list?.getAttribute('role')).toBe('listbox');
            expect(list?.getAttribute('aria-multiselectable')).toBe('true');
            expect(list?.getAttribute('aria-label')).toBe('Content');
            expect(cards().every((card) => card.getAttribute('role') === 'option')).toBe(true);
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

        it('should keep the ⋮ off every card in a read-only grid', () => {
            render({ readOnly: true });

            expect(spectator.queryAll(byTestId('grid-card-menu')).length).toBe(0);
        });

        it('should offer no sort control (US6)', () => {
            render();

            expect(spectator.query('p-sorticon, [psortablecolumn]')).toBeNull();
        });

        it('should show the pagination control', () => {
            render();

            expect(spectator.query('p-paginator')).toBeTruthy();
        });
    });

    describe('US2 — tidy layout', () => {
        beforeEach(() => render());

        it('should keep everything inside the cards within its own scroll box', () => {
            // Without a positioned ancestor, the cards' absolutely positioned screen reader labels
            // anchored to the drop zone outside, stretched it, and drew a second scrollbar.
            expect(spectator.query(byTestId('grid-scroll'))?.classList).toContain('relative');
            expect(cards().every((card) => card.classList.contains('relative'))).toBe(true);
        });

        it('should lay cards out at least 240px wide with a 20px gap', () => {
            const list = spectator.query(byTestId('grid-list')) as HTMLElement;

            expect(list.className).toContain(
                'grid-cols-[repeat(auto-fill,minmax(min(240px,100%),1fr))]'
            );
            expect(list.className).toContain('gap-[20px]');
        });
    });

    describe('US3 — selecting with the checkbox', () => {
        let emitted: DotContentDriveItem[][];

        const tick = (index: number, init: MouseEventInit = {}) =>
            cards()
                [index].querySelector('[data-testid="grid-card-checkbox"]')
                ?.dispatchEvent(new MouseEvent('click', { bubbles: true, ...init }));

        const renderSelectable = (inputs: Record<string, unknown> = {}) => {
            render(inputs);
            emitted = [];
            spectator.output<DotContentDriveItem[]>('selectionChange').subscribe((items) => {
                emitted.push(items);
                // Controlled, as the shell binds it: the store echoes the selection back.
                spectator.setInput('selection', items);
                spectator.detectChanges();
            });
        };

        it('should add a ticked card and keep the rest (US3 #1, #4)', () => {
            renderSelectable({ selection: [GRID_FOLDER] });

            tick(1);

            expect(emitted.at(-1)).toEqual([GRID_FOLDER, GRID_IMAGE]);
        });

        it('should remove an unticked card and keep the rest', () => {
            renderSelectable({ selection: [GRID_FOLDER, GRID_IMAGE] });

            tick(0);

            expect(emitted.at(-1)).toEqual([GRID_IMAGE]);
        });

        it('should mark selected cards for screen readers', () => {
            render({ selection: [GRID_IMAGE] });

            expect(cards().map((card) => card.getAttribute('aria-selected'))).toEqual([
                'false',
                'true',
                'false',
                'false'
            ]);
        });

        it('should select the range from the anchor on a Shift-click (US3 #5, US7 #6)', () => {
            renderSelectable();

            tick(0);
            tick(2, { shiftKey: true });

            expect(emitted.at(-1)).toEqual([GRID_FOLDER, GRID_IMAGE, GRID_LOCKED_BLOG]);
        });

        it('should treat a Shift-click with nothing selected as a plain tick', () => {
            renderSelectable();

            tick(2, { shiftKey: true });

            expect(emitted.at(-1)).toEqual([GRID_LOCKED_BLOG]);
        });

        it('should not select a busy card (FR-014, FR-028)', () => {
            renderSelectable({ busyRows: [GRID_IMAGE.inode] });

            tick(1);

            expect(emitted).toEqual([]);
        });

        it('should leave busy cards out of a range (US7 #7)', () => {
            renderSelectable({ busyRows: [GRID_IMAGE.inode] });

            tick(0);
            tick(2, { shiftKey: true });

            expect(emitted.at(-1)).toEqual([GRID_FOLDER, GRID_LOCKED_BLOG]);
        });

        it('should select one language version only (US3 #6)', () => {
            const spanish = { ...GRID_IMAGE, inode: 'inode-image-es', languageId: 2 };
            renderSelectable({ items: [GRID_IMAGE, spanish] });

            tick(1);

            expect(emitted.at(-1)).toEqual([spanish]);
        });
    });

    describe('US5 — a card does what a row does', () => {
        const card = (index: number) => cards()[index];
        const plainClick = (index: number) =>
            card(index)
                .querySelector('[data-testid="grid-card-title"]')
                ?.dispatchEvent(new MouseEvent('click', { bubbles: true, detail: 1 }));

        it('should open the clicked item (US5 #1)', () => {
            render();
            const open = vi.fn();
            spectator.output('open').subscribe(open);

            plainClick(2);

            expect(open).toHaveBeenCalledWith(GRID_LOCKED_BLOG);
        });

        it('should not open anything in a read-only grid', () => {
            render({ readOnly: true });
            const open = vi.fn();
            spectator.output('open').subscribe(open);

            plainClick(2);

            expect(open).not.toHaveBeenCalled();
        });

        it('should not open a busy item', () => {
            render({ busyRows: [GRID_LOCKED_BLOG.inode] });
            const open = vi.fn();
            spectator.output('open').subscribe(open);

            plainClick(2);

            expect(open).not.toHaveBeenCalled();
        });

        it('should ask for the row menu on a right-click (US5 #2)', () => {
            render();
            const rightClick = vi.fn();
            spectator.output('rightClick').subscribe(rightClick);
            const event = new MouseEvent('contextmenu', { bubbles: true, cancelable: true });

            card(1).dispatchEvent(event);

            expect(rightClick).toHaveBeenCalledWith({ event, contentlet: GRID_IMAGE });
            expect(event.defaultPrevented).toBe(true);
        });

        it('should keep the browser menu in a read-only grid', () => {
            render({ readOnly: true });
            const rightClick = vi.fn();
            spectator.output('rightClick').subscribe(rightClick);
            const event = new MouseEvent('contextmenu', { bubbles: true, cancelable: true });

            card(1).dispatchEvent(event);

            expect(rightClick).not.toHaveBeenCalled();
            expect(event.defaultPrevented).toBe(false);
        });

        it('should ask for the row menu from the ⋮ button (US1 #11)', () => {
            render();
            const rightClick = vi.fn();
            spectator.output('rightClick').subscribe(rightClick);

            spectator.click(
                card(1).querySelector('[data-testid="grid-card-menu"] button') as Element
            );

            expect(rightClick).toHaveBeenCalledWith(
                expect.objectContaining({ contentlet: GRID_IMAGE })
            );
        });

        it('should drag every selected item when a selected card is dragged (US5 #5)', () => {
            render({ selection: [GRID_IMAGE, GRID_LOCKED_BLOG] });
            const dragStart = vi.fn();
            spectator.output('dragStart').subscribe(dragStart);

            card(1).dispatchEvent(dragEvent('dragstart'));

            expect(dragStart).toHaveBeenCalledWith([GRID_IMAGE, GRID_LOCKED_BLOG]);
        });

        it('should drag only the card when it is not selected', () => {
            render({ selection: [GRID_IMAGE] });
            const dragStart = vi.fn();
            spectator.output('dragStart').subscribe(dragStart);

            card(3).dispatchEvent(dragEvent('dragstart'));

            expect(dragStart).toHaveBeenCalledWith([GRID_ORPHAN_PAGE]);
        });

        it('should not drag in a read-only grid', () => {
            render({ readOnly: true });

            expect(card(1).getAttribute('draggable')).toBe('false');
        });

        it('should take an internal drop on a card (US5 #4)', () => {
            render();
            const drop = vi.fn();
            spectator.output('drop').subscribe(drop);
            const over = dragEvent('dragover');

            card(0).dispatchEvent(over);
            card(0).dispatchEvent(dragEvent('drop'));

            expect(over.defaultPrevented).toBe(true);
            expect(drop).toHaveBeenCalledWith(GRID_FOLDER);
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

        it('should ask for the next page with the table page shape (US5 #3)', () => {
            render({ totalItems: 100 });
            const paginate = vi.fn();
            spectator.output('paginate').subscribe(paginate);

            spectator.component.onPageChange({ first: 20, rows: 20, page: 1, pageCount: 5 });

            expect(paginate).toHaveBeenCalledWith({ first: 20, rows: 20, page: 2 });
        });

        it('should not ask for another page while one is loading', () => {
            render({ totalItems: 100, loading: true });
            const paginate = vi.fn();
            spectator.output('paginate').subscribe(paginate);

            spectator.component.onPageChange({ first: 20, rows: 20, page: 1, pageCount: 5 });

            expect(paginate).not.toHaveBeenCalled();
        });
    });

    describe('US7 — keyboard', () => {
        const tabIndexes = () => cards().map((card) => card.getAttribute('tabindex'));
        const press = (index: number, key: string, init: KeyboardEventInit = {}) => {
            const card = cards()[index];
            card.focus();
            card.dispatchEvent(
                new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true, ...init })
            );
            spectator.detectChanges();
        };
        const focusedIndex = () => cards().indexOf(document.activeElement as HTMLElement);

        let emitted: DotContentDriveItem[][];

        const renderKeyboard = (inputs: Record<string, unknown> = {}) => {
            render(inputs);
            emitted = [];
            spectator.output<DotContentDriveItem[]>('selectionChange').subscribe((items) => {
                emitted.push(items);
                spectator.setInput('selection', items);
                spectator.detectChanges();
            });
        };

        it('should have exactly one tab stop, on the first card (US7 #1)', () => {
            renderKeyboard();

            expect(tabIndexes()).toEqual(['0', '-1', '-1', '-1']);
        });

        it('should take no tab stop in a read-only grid (FR-034)', () => {
            renderKeyboard({ readOnly: true });

            expect(tabIndexes()).toEqual(['-1', '-1', '-1', '-1']);
        });

        it('should move focus and the tab stop with the arrows, not the selection (US7 #2)', () => {
            renderKeyboard();
            const activeKey = vi.fn();
            spectator.output('activeKeyChange').subscribe(activeKey);

            press(0, 'ArrowRight');

            expect(focusedIndex()).toBe(1);
            expect(tabIndexes()).toEqual(['-1', '0', '-1', '-1']);
            expect(emitted).toEqual([]);
            expect(activeKey).toHaveBeenLastCalledWith(GRID_IMAGE.inode);
        });

        it('should move a whole row with Up and Down (US7 #3)', () => {
            renderKeyboard();
            vi.spyOn(spectator.component, 'measureColumnCount').mockReturnValue(2);

            press(0, 'ArrowDown');

            expect(focusedIndex()).toBe(2);
        });

        it('should skip a busy card (US7 #7)', () => {
            renderKeyboard({ busyRows: [GRID_IMAGE.inode] });

            press(0, 'ArrowRight');

            expect(focusedIndex()).toBe(2);
        });

        it('should grow and shrink the selection with Shift+arrows (US7 #5)', () => {
            renderKeyboard();

            press(0, 'ArrowRight', { shiftKey: true });
            expect(emitted.at(-1)).toEqual([GRID_FOLDER, GRID_IMAGE]);

            press(1, 'ArrowRight', { shiftKey: true });
            expect(emitted.at(-1)).toEqual([GRID_FOLDER, GRID_IMAGE, GRID_LOCKED_BLOG]);

            press(2, 'ArrowLeft', { shiftKey: true });
            expect(emitted.at(-1)).toEqual([GRID_FOLDER, GRID_IMAGE]);
        });

        it.each(['Enter', ' '])('should toggle the focused card with %j (US7 #10)', (key) => {
            renderKeyboard();

            press(1, key);
            expect(emitted.at(-1)).toEqual([GRID_IMAGE]);

            press(1, key);
            expect(emitted.at(-1)).toEqual([]);
        });

        it('should open the actions menu with Shift+F10', () => {
            renderKeyboard();
            const rightClick = vi.fn();
            spectator.output('rightClick').subscribe(rightClick);

            press(1, 'F10', { shiftKey: true });

            expect(rightClick).toHaveBeenCalledWith(
                expect.objectContaining({ contentlet: GRID_IMAGE })
            );
        });

        it('should ignore the keys in a read-only grid', () => {
            renderKeyboard({ readOnly: true });

            press(0, 'Enter');
            press(0, 'ArrowRight', { shiftKey: true });

            expect(emitted).toEqual([]);
        });

        it('should put the tab stop on the item it is told about (US7 #12)', () => {
            renderKeyboard({ activeKey: GRID_LOCKED_BLOG.inode });

            expect(tabIndexes()).toEqual(['-1', '-1', '0', '-1']);
        });

        it('should fall back to the first card when that item is not on the page', () => {
            renderKeyboard({ activeKey: 'gone' });

            expect(tabIndexes()).toEqual(['0', '-1', '-1', '-1']);
        });

        it('should keep a tab stop when a new page arrives (US7 #11)', () => {
            renderKeyboard();
            press(0, 'ArrowRight');
            press(1, 'ArrowRight');

            spectator.setInput('items', [GRID_FOLDER]);
            spectator.detectChanges();

            expect(tabIndexes()).toEqual(['0']);
        });

        it('should scroll the tab stop into view when asked', () => {
            renderKeyboard({ activeKey: GRID_LOCKED_BLOG.inode });
            const scroll = vi.fn();
            cards()[2].scrollIntoView = scroll;

            spectator.component.revealActive();

            expect(scroll).toHaveBeenCalledWith({ block: 'nearest' });
        });
    });
});
