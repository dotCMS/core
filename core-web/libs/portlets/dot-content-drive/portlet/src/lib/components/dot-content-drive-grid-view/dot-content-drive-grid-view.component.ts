import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    ElementRef,
    inject,
    input,
    output,
    Renderer2,
    signal,
    untracked
} from '@angular/core';

import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';

import {
    ContextMenuData,
    DotContentDriveItem,
    DotContentDrivePaginateEvent,
    DotLanguage
} from '@dotcms/dotcms-models';
import { DOT_DRAG_ITEM, DotMessagePipe } from '@dotcms/ui';

import { DotContentDriveGridCardComponent } from './dot-content-drive-grid-card/dot-content-drive-grid-card.component';
import { columnCountFromTops, GRID_MOVE_KEYS, nextGridIndex } from './grid-navigation';

/** Selector for the rendered cards, in reading order. */
const CARD_SELECTOR = '[data-testid="grid-card"]';

/**
 * Keys that only modify another keystroke. The browser sends a keydown for them on their own, and
 * that press must not end a Shift gesture that is still going.
 */
const MODIFIER_KEYS = new Set(['Shift', 'Control', 'Meta', 'Alt']);

/** Page sizes, the same as the table's. */
const ROWS_PER_PAGE_OPTIONS = [20, 40, 60];

/**
 * Content Drive's grid view (#37930): the current page of results as cards.
 *
 * The table's counterpart, driven by the same store state and reporting through the same outputs,
 * so the shell can wire both views to one set of handlers. It renders what it is given in the order
 * given (FR-003) and has no sort control of its own (FR-023).
 *
 * **Selection** is controlled: the grid renders `selection` and only reports changes. The checkbox
 * adds or removes one card and Shift on it selects the range from the anchor (FR-016). A click
 * anywhere else on a card opens the item (FR-015).
 *
 * **Keyboard** (US7): the grid is one tab stop. Left and Right move one card in reading order, Up
 * and Down move a row, Home and End go to the ends, Shift with any of them extends the selection
 * from the anchor, Enter and Space toggle the focused card, Shift+F10 or the ContextMenu key opens
 * its actions. Busy cards are skipped.
 */
@Component({
    selector: 'dot-content-drive-grid-view',
    imports: [DotContentDriveGridCardComponent, DotMessagePipe, PaginatorModule, SkeletonModule],
    templateUrl: './dot-content-drive-grid-view.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex h-full min-h-0 w-full flex-col' }
})
export class DotContentDriveGridViewComponent {
    readonly #host = inject<ElementRef<HTMLElement>>(ElementRef);
    readonly #renderer = inject(Renderer2);

    /** The page of results, in the order to show them. */
    $items = input<DotContentDriveItem[]>([], { alias: 'items' });

    /** The Drive's selection, shared with the table. */
    $selection = input<DotContentDriveItem[]>([], { alias: 'selection' });

    /** Whether results are loading: skeleton cards and a locked paginator. */
    $loading = input(false, { alias: 'loading' });

    /** Inodes (or identifiers) a bulk action is running on. */
    $busyRows = input<string[]>([], { alias: 'busyRows' });

    /** Total results, for the paginator. */
    $totalItems = input(0, { alias: 'totalItems' });

    /** Position of the first result on this page. */
    $offset = input(0, { alias: 'offset' });

    /** Results per page. */
    $rows = input(ROWS_PER_PAGE_OPTIONS[0], { alias: 'rows' });

    /** Languages by id, for the locale tags. */
    $languagesMap = input(new Map<number, DotLanguage>(), { alias: 'languagesMap' });

    /**
     * Key (`inode`) of the item that should hold the tab stop, typically the one the table had when
     * the user switched views. Ignored when that item is not on the page.
     */
    $activeKey = input<string | null>(null, { alias: 'activeKey' });

    /** A grid the user cannot act on: no tab stop, no keys, no menus, no drag, nothing opens. */
    $readOnly = input(false, { alias: 'readOnly' });

    /** The new selection, after a checkbox, a range or a key changed it. */
    selectionChange = output<DotContentDriveItem[]>();

    /** The page asked for, in the shape the table reports. */
    paginate = output<DotContentDrivePaginateEvent>();

    /** An item to open: a folder to enter or content to edit. */
    open = output<DotContentDriveItem>();

    /** An item whose actions menu should open, from a right-click, the ⋮ button or the keyboard. */
    rightClick = output<ContextMenuData>();

    /** Items being dragged. */
    dragStart = output<DotContentDriveItem[]>();

    /** The drag ended, dropped or not. */
    dragEnd = output<void>();

    /** The card dragged items were dropped on. */
    drop = output<DotContentDriveItem>();

    /** The cards scrolled. */
    scroll = output<Event>();

    /** The item that now holds the tab stop, so the other view can pick it up. */
    activeKeyChange = output<string | null>();

    protected readonly rowsPerPageOptions = ROWS_PER_PAGE_OPTIONS;

    protected readonly $busySet = computed(() => new Set(this.$busyRows()));

    protected readonly $selectedKeys = computed(
        () => new Set(this.$selection().map((item) => this.keyOf(item)))
    );

    /** One placeholder per result the page will hold. */
    protected readonly $skeletonCards = computed(() => Array.from({ length: this.$rows() }));

    /** The card that has the tab stop, by position on the page. */
    protected readonly $activeIndex = signal(0);

    /** The tab stop actually rendered: none in a read-only grid, never past the last card. */
    protected readonly $tabStopIndex = computed(() =>
        this.$readOnly() ? -1 : Math.min(this.$activeIndex(), this.$items().length - 1)
    );

    /** The card being dragged over, by key. */
    protected readonly $dragOverKey = signal<string | null>(null);

    /**
     * Where a range extends from, by position on the page. Moves with every plain arrow, tick and
     * Enter/Space; stays put while Shift extends.
     */
    #anchorIndex: number | null = null;

    /**
     * The selection as it stood when the current Shift gesture began. A range adds to it rather than
     * replacing it, and is recomputed from it on every step, so shrinking the range gives back
     * exactly what it took. `null` when no gesture is going.
     */
    #rangeBase: DotContentDriveItem[] | null = null;

    /**
     * Set when a page change is accepted, until `loading` catches up. Closes the gap in which a fast
     * second click would ask for another page, as the table does.
     */
    readonly #pageChangeAccepted = signal(false);

    /** Locks the paginator while results load. */
    protected readonly $paginatorLocked = computed(
        () => this.$loading() || this.#pageChangeAccepted()
    );

    constructor() {
        // Places the tab stop when the page or the requested item changes. Leaves it alone when the
        // requested item is the one that already has it, which is what happens when this grid's own
        // move comes back through the shell — resetting the anchor there would break a Shift range.
        effect(() => {
            const items = this.$items();
            const key = this.$activeKey();

            untracked(() => {
                const index = key ? items.findIndex((item) => this.keyOf(item) === key) : -1;

                if (index !== -1 && index === this.$activeIndex()) {
                    return;
                }

                this.$activeIndex.set(index === -1 ? 0 : index);
                this.#anchorIndex = index === -1 ? null : index;
                this.#rangeBase = null;
            });
        });

        effect(() => {
            if (this.$loading()) {
                this.#pageChangeAccepted.set(false);
            }
        });
    }

    /**
     * The key the Drive selects by: `inode`, so two language versions of one item are two cards.
     * Folders carry an inode backfilled from their identifier.
     *
     * @param item A listed item.
     */
    protected keyOf(item: DotContentDriveItem): string {
        return item.inode ?? item.identifier;
    }

    /**
     * Whether a bulk action is running on the item. Checks both keys, as the table does, because a
     * run may have registered either.
     *
     * @param item A listed item.
     */
    protected isBusy(item: DotContentDriveItem): boolean {
        const busy = this.$busySet();

        return busy.has(item.inode) || busy.has(item.identifier);
    }

    /**
     * How many cards fit in a row right now, measured from the rendered cards. Public so tests,
     * which have no layout engine, can stand in for it.
     */
    measureColumnCount(): number {
        return columnCountFromTops(this.#cards().map((card) => card.offsetTop));
    }

    /** Scrolls the card that holds the tab stop into view, if it is off screen. */
    revealActive(): void {
        this.#cards()[this.$tabStopIndex()]?.scrollIntoView({ block: 'nearest' });
    }

    /**
     * Opens the item, unless the grid is read-only or a bulk action holds it.
     *
     * @param item The clicked item.
     */
    protected onOpen(item: DotContentDriveItem): void {
        if (this.$readOnly() || this.isBusy(item)) {
            return;
        }

        this.open.emit(item);
    }

    /**
     * Keeps the tab stop on the card that took focus, by mouse or keyboard.
     *
     * @param index The card's position on the page.
     */
    protected onCardFocus(index: number): void {
        this.#setActive(index);
    }

    /**
     * Handles a checkbox click: a plain one adds or removes the card, a Shift one selects the range
     * from the anchor. With nothing selected there is no range to extend, so Shift acts as a plain
     * tick, as it does in the table.
     *
     * @param event The click on the checkbox.
     * @param index The card's position on the page.
     */
    protected onCheckboxClick(event: MouseEvent, index: number): void {
        const item = this.$items()[index];

        if (!item || this.isBusy(item)) {
            return;
        }

        this.#setActive(index);

        if (event.shiftKey && this.$selection().length) {
            this.#rangeBase ??= this.$selection();
            this.#extendSelectionTo(index);

            return;
        }

        this.#rangeBase = null;
        this.#anchorIndex = index;
        this.#toggle(item);
    }

    /**
     * Grid keyboard handling, delegated from the list (US7).
     *
     * @param event A keydown inside the list.
     */
    protected onKeydown(event: KeyboardEvent): void {
        if (this.$readOnly() || MODIFIER_KEYS.has(event.key)) {
            return;
        }

        const index = this.#indexOf(event.target);

        if (index === -1) {
            return;
        }

        const items = this.$items();

        if (GRID_MOVE_KEYS.has(event.key)) {
            // The arrows would otherwise scroll the listing (FR-032).
            event.preventDefault();

            if (event.shiftKey) {
                this.#rangeBase ??= this.$selection();
                this.#anchorIndex ??= index;
            } else {
                this.#rangeBase = null;
            }

            const next = nextGridIndex({
                index,
                key: event.key,
                columns: this.measureColumnCount(),
                count: items.length,
                isSkipped: (candidate) => this.isBusy(items[candidate])
            });

            if (next === index) {
                return;
            }

            this.#cards()[next]?.focus();
            this.#setActive(next);

            if (event.shiftKey) {
                this.#extendSelectionTo(next);
            } else {
                this.#anchorIndex = next;
            }

            return;
        }

        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();

            if (this.isBusy(items[index])) {
                return;
            }

            this.#rangeBase = null;
            this.#anchorIndex = index;
            this.#toggle(items[index]);

            return;
        }

        if (event.key === 'ContextMenu' || (event.key === 'F10' && event.shiftKey)) {
            event.preventDefault();
            this.#openMenuFromKeyboard(index);
        }
    }

    /**
     * Asks for the item's actions menu, as a right-click on its table row does. Left to the browser
     * in a read-only grid.
     *
     * @param event The right-click, or the ⋮ click.
     * @param item The card's item.
     */
    protected onContextMenu(event: MouseEvent, item: DotContentDriveItem): void {
        if (this.$readOnly()) {
            return;
        }

        event.preventDefault();
        this.rightClick.emit({ event, contentlet: item });
    }

    /**
     * Starts dragging: every selected item when the dragged card is one of them, otherwise just the
     * card (FR-021), with the same drag payload marker the table and the folder tree read.
     *
     * @param event The drag start.
     * @param item The dragged card's item.
     */
    protected onDragStart(event: DragEvent, item: DotContentDriveItem): void {
        if (this.$readOnly() || this.isBusy(item) || !event.dataTransfer) {
            return;
        }

        event.stopPropagation();

        const selection = this.$selection();
        const dragged = this.$selectedKeys().has(this.keyOf(item)) ? selection : [item];

        event.dataTransfer.effectAllowed = 'move';
        event.dataTransfer.setData(DOT_DRAG_ITEM, '');

        const image = this.#createDragImage(dragged);

        if (image) {
            event.dataTransfer.setDragImage(image, 40, 40);
        }

        this.dragStart.emit(dragged);
    }

    /**
     * Accepts an internal drag over a card and highlights it. File drags are left alone so they
     * reach the upload drop zone around the grid.
     *
     * @param event The drag over.
     * @param item The card's item.
     */
    protected onDragOver(event: DragEvent, item: DotContentDriveItem): void {
        if (!event.dataTransfer?.types.includes(DOT_DRAG_ITEM)) {
            return;
        }

        event.preventDefault();
        this.$dragOverKey.set(this.keyOf(item));
    }

    /**
     * Reports an internal drop on a card. A drop carrying files is left to bubble to the upload drop
     * zone (FR-022).
     *
     * @param event The drop.
     * @param item The card dropped on.
     */
    protected onDrop(event: DragEvent, item: DotContentDriveItem): void {
        const hasFiles = !!event.dataTransfer?.files?.length;
        const isInternal = event.dataTransfer?.types.includes(DOT_DRAG_ITEM);

        if (hasFiles || !isInternal) {
            return;
        }

        event.preventDefault();
        event.stopPropagation();
        this.$dragOverKey.set(null);
        this.drop.emit(item);
    }

    /** Clears the drag highlight and reports the end of the drag. */
    protected onDragEnd(): void {
        this.$dragOverKey.set(null);
        this.dragEnd.emit();
    }

    /**
     * Reports a page change in the shape the table reports it (`page` counted from 1). Ignored while
     * a page is loading, so a fast second click cannot queue another search.
     *
     * @param state The paginator's new state.
     */
    onPageChange(state: PaginatorState): void {
        if (this.$paginatorLocked()) {
            return;
        }

        this.#pageChangeAccepted.set(true);
        this.paginate.emit({
            first: state.first ?? 0,
            rows: state.rows ?? this.$rows(),
            page: (state.page ?? 0) + 1
        });
    }

    /** The rendered cards, in reading order. */
    #cards(): HTMLElement[] {
        return Array.from(this.#host.nativeElement.querySelectorAll<HTMLElement>(CARD_SELECTOR));
    }

    /** The position of the card an event came from, or -1. */
    #indexOf(target: EventTarget | null): number {
        const card = (target as HTMLElement | null)?.closest<HTMLElement>(CARD_SELECTOR);

        return card ? this.#cards().indexOf(card) : -1;
    }

    /** Moves the tab stop and tells the shell, when it actually moved. */
    #setActive(index: number): void {
        if (index === this.$activeIndex()) {
            return;
        }

        this.$activeIndex.set(index);

        const item = this.$items()[index];
        this.activeKeyChange.emit(item ? this.keyOf(item) : null);
    }

    /** Adds the item to the selection, or removes it if it is there. */
    #toggle(item: DotContentDriveItem): void {
        const key = this.keyOf(item);
        const selection = this.$selection();

        this.selectionChange.emit(
            this.$selectedKeys().has(key)
                ? selection.filter((selected) => this.keyOf(selected) !== key)
                : [...selection, item]
        );
    }

    /**
     * Selects everything between the anchor and `index`, on top of what was selected before the
     * Shift gesture began. Busy cards are left out of the range.
     */
    #extendSelectionTo(index: number): void {
        const items = this.$items();
        const anchor = this.#anchorIndex ?? index;
        const range = items
            .slice(Math.min(anchor, index), Math.max(anchor, index) + 1)
            .filter((item) => !this.isBusy(item));
        const inRange = new Set(range.map((item) => this.keyOf(item)));
        const kept = (this.#rangeBase ?? []).filter((item) => !inRange.has(this.keyOf(item)));

        this.selectionChange.emit([...kept, ...range]);
    }

    /**
     * Opens the actions menu for the focused card. The menu places itself at a pointer position, so
     * one is made up at the card's top-left corner, where the keyboard user's attention already is.
     */
    #openMenuFromKeyboard(index: number): void {
        const card = this.#cards()[index];
        const item = this.$items()[index];

        if (!card || !item) {
            return;
        }

        const rect = card.getBoundingClientRect();
        const event = new MouseEvent('contextmenu', {
            clientX: rect.left + 16,
            clientY: rect.top + 16,
            bubbles: true,
            cancelable: true
        });

        this.onContextMenu(event, item);
    }

    /**
     * Builds the picture the pointer carries while dragging: up to three previews stacked, and a
     * count when there are more items. Reuses the table's drag-image styles.
     */
    #createDragImage(items: DotContentDriveItem[]): HTMLElement | null {
        const container = this.#renderer.createElement('div') as HTMLElement;
        this.#renderer.addClass(container, 'drag-image-container');

        items.slice(0, 3).forEach((item, index) => {
            const media = this.#host.nativeElement.querySelector<HTMLElement>(
                `[data-grid-id="${item.identifier}"]`
            );
            const source =
                media?.tagName === 'I'
                    ? media
                    : media?.querySelector('img, i, .material-symbols-outlined');

            if (!source) {
                return;
            }

            const wrapper = this.#renderer.createElement('div') as HTMLElement;
            this.#renderer.addClass(wrapper, 'drag-image-item');
            this.#renderer.addClass(wrapper, `drag-image-item-${index}`);
            this.#renderer.appendChild(wrapper, source.cloneNode(true));
            this.#renderer.appendChild(container, wrapper);
        });

        if (items.length > 1) {
            const badge = this.#renderer.createElement('div') as HTMLElement;
            this.#renderer.addClass(badge, 'drag-image-badge');
            this.#renderer.setProperty(badge, 'textContent', String(items.length));
            this.#renderer.appendChild(container, badge);
        }

        this.#renderer.appendChild(document.body, container);
        // The browser copies the image when the drag starts, so it can go straight away.
        setTimeout(() => this.#renderer.removeChild(document.body, container), 0);

        return container;
    }
}
