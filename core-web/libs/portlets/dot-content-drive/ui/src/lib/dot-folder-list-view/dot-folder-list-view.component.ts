import { patchState, signalState } from '@ngrx/signals';

import { DatePipe, NgTemplateOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    input,
    OnInit,
    output,
    Renderer2,
    signal,
    viewChild
} from '@angular/core';

import { LazyLoadEvent, SortEvent } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { Table, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';

import { take } from 'rxjs/operators';

import { DotLanguagesService } from '@dotcms/data-access';
import {
    ContextMenuData,
    DotContentDriveItem,
    DotContentDrivePaginateEvent,
    DotLanguage
} from '@dotcms/dotcms-models';
import {
    DotContentletStatusBadgeComponent,
    DotContentThumbnailComponent,
    DotLocaleTagPipe,
    DotMessagePipe,
    DotRelativeDatePipe
} from '@dotcms/ui';

import {
    DOT_DRAG_ITEM,
    DotFolderListViewFixedColumn,
    HEADER_COLUMNS,
    SYSTEM_HOST_IDENTIFIER
} from '../shared/constants';
import {
    DOT_FOLDER_LIST_VIEW_COLUMN_TYPE,
    DotFolderListViewColumn,
    DotFolderListViewColumnField
} from '../shared/models';

/**
 * Canonical position of the "type" column. Extra columns follow it, in the header and in the body
 * alike — read from the constant rather than hardcoded so the two cannot drift.
 */
const TYPE_COLUMN_ORDER =
    HEADER_COLUMNS.find((column) => column.field === 'contentType')?.order ?? Infinity;

@Component({
    selector: 'dot-folder-list-view',
    imports: [
        ButtonModule,
        TagModule,
        DotContentletStatusBadgeComponent,
        DotContentThumbnailComponent,
        DotMessagePipe,
        DotRelativeDatePipe,
        SkeletonModule,
        TableModule,
        DotLocaleTagPipe,
        NgTemplateOutlet,
        DatePipe
    ],
    templateUrl: './dot-folder-list-view.component.html',
    styleUrls: ['./dot-folder-list-view.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'w-full h-full min-h-0 block' }
})
export class DotFolderListViewComponent implements OnInit {
    private readonly renderer = inject(Renderer2);
    private readonly dotLanguagesService = inject(DotLanguagesService);

    dataTable = viewChild<Table>('dataTable');

    /**
     * A signal that takes an array of DotContentDriveItem objects.
     *
     * @type {InputSignal<DotContentDriveItem[]>}
     * @alias items
     */
    $items = input<DotContentDriveItem[]>([], { alias: 'items' });

    /**
     * A signal that takes the total number of items.
     *
     * @type {InputSignal<number>}
     * @alias totalItems
     */
    $totalItems = input<number>(0, { alias: 'totalItems' });

    /**
     * A signal that takes the loading state.
     *
     * @type {InputSignal<boolean>}
     * @alias loading
     */
    $loading = input<boolean>(false, { alias: 'loading' });

    /**
     * A signal that takes the offset.
     *
     * @type {InputSignal<number>}
     * @alias offset
     */
    $offset = input<number>(0, { alias: 'offset' });

    /**
     * Extra, caller-provided columns appended after the fixed "type" column. Agnostic of how they
     * are sourced (Content Drive derives them from the selected content type's "Show In List" fields).
     *
     * @type {InputSignal<DotFolderListViewColumn[]>}
     * @alias extraColumns
     */
    $extraColumns = input<DotFolderListViewColumn[]>([], { alias: 'extraColumns' });

    /**
     * An output that emits the selected items.
     *
     * @type {Output<DotContentDriveItem[]>}
     * @alias selectionChange
     */
    selectionChange = output<DotContentDriveItem[]>();

    /**
     * An output that emits the pagination event.
     *
     * @type {Output<LazyLoadEvent>}
     * @alias paginate
     */
    paginate = output<DotContentDrivePaginateEvent>();

    /**
     * An output that emits the sort event.
     *
     * @type {Output<SortEvent>}
     * @alias sort
     */
    sort = output<SortEvent>();

    /**
     * An output that emits the right click event.
     *
     * @type {Output<ContextMenuData>}
     * @alias rightClick
     */
    rightClick = output<ContextMenuData>();

    /**
     * An output that emits the double click event.
     *
     * @type {Output<DotContentDriveItem>}
     * @alias doubleClick
     */
    doubleClick = output<DotContentDriveItem>();

    /**
     * An output that emits the drag start event.
     *
     * @type {Output<DotContentDriveItem[]>}
     * @alias dragStart
     */
    dragStart = output<DotContentDriveItem[]>();

    /**
     * An output that emits the drag end event.
     *
     * @type {Output<void>}
     * @alias dragEnd
     */
    dragEnd = output<void>();

    /**
     * An output that emits the drop event.
     *
     * @type {Output<DotContentDriveItem>} the target value
     * @alias drop
     */
    drop = output<DotContentDriveItem>();

    /**
     * An output that emits the scroll event.
     *
     * @type {Output<Event>}
     * @alias scroll
     */
    scroll = output<Event>();

    /**
     * Field that identifies a row. Language variants of one contentlet share an `identifier` and
     * differ only by `inode`, so a caller listing variants separately must key on `inode`.
     *
     * @type {InputSignal<string>}
     * @alias dataKey
     */
    $dataKey = input<string>('identifier', { alias: 'dataKey' });

    /**
     * Where the rows come from. `true` (default) pages server-side: the table holds one page and
     * emits `paginate` for the next. `false` pages a list the caller already holds in full.
     *
     * @type {InputSignal<boolean>}
     * @alias lazy
     */
    $lazy = input<boolean>(true, { alias: 'lazy' });

    /**
     * Which of the fixed columns to render, by field. Empty (the default) renders them all.
     *
     * The full set assumes the portlet's width; in a dialog it overflows and squeezes the title to
     * an ellipsis. Caller-provided `extraColumns` are unaffected — those were asked for explicitly.
     *
     * @type {InputSignal<string[]>}
     * @alias visibleColumns
     */
    $visibleColumns = input<DotFolderListViewColumnField[]>([], { alias: 'visibleColumns' });

    /**
     * Freezes the selection while the caller is acting on it. Distinct from `loading`, which is
     * about rows arriving: this stops the picked set changing mid-flight and desyncing from what
     * was submitted.
     *
     * @type {InputSignal<boolean>}
     * @alias disabled
     */
    $disabled = input<boolean>(false, { alias: 'disabled' });

    /**
     * Inodes whose lock is held by somebody other than the current user, marked so a bulk action
     * that may be refused on them is visible before firing. The caller decides — the judgement needs
     * the user's admin role, which this table has no business knowing.
     *
     * @type {InputSignal<string[]>}
     * @alias lockedByOthers
     */
    $lockedByOthers = input<string[]>([], { alias: 'lockedByOthers' });

    /**
     * Strips the row affordances that assume a browsing grid — drag, context menu, open-on-click and
     * the kebab. Checkboxes stay. Used when the table is a confirmation list inside a dialog.
     *
     * @type {InputSignal<boolean>}
     * @alias readOnly
     */
    $readOnly = input<boolean>(false, { alias: 'readOnly' });

    /** Flagged inodes as a set — one lookup per row instead of a scan. */
    protected readonly $lockedByOthersSet = computed(() => new Set(this.$lockedByOthers()));

    /**
     * Caller-owned checked set — makes the table **controlled**: it renders this and only reports
     * changes through `selectionChange`, never applying them itself. Omit for the uncontrolled
     * table, which keeps its own set and clears it whenever `items` changes.
     *
     * @type {InputSignal<DotContentDriveItem[] | undefined>}
     * @alias selection
     */
    $selection = input<DotContentDriveItem[] | undefined>(undefined, { alias: 'selection' });

    /** Checked set while uncontrolled. Ignored as long as `selection` is provided. */
    readonly #internalSelection = signal<DotContentDriveItem[]>([]);

    /**
     * Bumped on every selection change the table reports. Exists so the sync effect below re-runs
     * after a click whose outcome the parent declines — where the effective selection is unchanged
     * and a value-based dependency alone would never fire.
     */
    readonly #selectionRevision = signal(0);

    /** What the table renders: the caller's set when provided, otherwise our own. */
    protected readonly $tableSelection = computed<DotContentDriveItem[]>(
        () => this.$selection() ?? this.#internalSelection()
    );

    /**
     * The effective selection — the caller's set when one is provided, otherwise our own.
     *
     * Read-only on purpose. A setter used to exist and it was a trap: while controlled it wrote the
     * internal set, which the getter then ignored in favour of the caller's, so an assignment
     * vanished without a word. The only way in is `onSelectionChange`, the same entry point the
     * table itself uses.
     *
     * @type {DotContentDriveItem[]}
     * @alias selectedItems
     */
    get selectedItems(): DotContentDriveItem[] {
        return this.$tableSelection();
    }

    readonly MIN_ROWS_PER_PAGE = 20;
    protected readonly rowsPerPageOptions = [this.MIN_ROWS_PER_PAGE, 40, 60];

    /**
     * Extra columns after de-duplication by `field` key: drops any that collide with a fixed column
     * (e.g. a "title" field) or repeat another extra. Header, body and colspan all consume this so
     * they never drift. De-dupe is by field key, not label.
     */
    protected readonly $safeExtraColumns = computed<DotFolderListViewColumn[]>(() => {
        // Widened to `string` deliberately: the fixed fields are a closed union, but the extras
        // this de-dupes against are caller-provided and can carry any field name.
        const seen = new Set<string>(HEADER_COLUMNS.map((column) => column.field));

        return this.$extraColumns().filter((column) => {
            if (seen.has(column.field)) {
                return false;
            }
            seen.add(column.field);

            return true;
        });
    });

    /**
     * Rough `ch` per `rem` (16px font, ~8px advance), used ONLY to compare a fixed per-type width
     * against a header-derived one and pick the wider. Never used to compute a rendered size, so the
     * approximation cannot drift a column's actual width.
     */
    private readonly REM_TO_CH = 2;
    /** Character-count clamps for content-based (text/number) column widths, in `ch`. */
    private readonly EXTRA_COL_MIN_CH = 8;
    private readonly EXTRA_COL_MAX_CH = 32;

    private readonly EXTRA_COL_PAD_CH = 3;
    /** Column types exposed to the template's `@switch`, so cases aren't magic strings. */
    protected readonly COLUMN_TYPE = DOT_FOLDER_LIST_VIEW_COLUMN_TYPE;

    /**
     * Whether the row is an asset shared across every site, i.e. one living on SYSTEM_HOST.
     *
     * The "Show Shared Assets" filter is seeded on, so shared assets are mixed into every folder
     * listing by default and are otherwise indistinguishable from the site's own content.
     *
     * `'host' in item` is the narrowing check, not a stylistic choice: a row is a contentlet or a
     * folder, and only the contentlet carries `host` (a folder has `hostId`), so the guard both
     * satisfies the union and excludes folders in one step.
     *
     * @param item The row to test.
     * @return {*} {boolean} `true` when the row lives on SYSTEM_HOST.
     */
    protected isSharedAsset(item: DotContentDriveItem): boolean {
        return 'host' in item && item.host === SYSTEM_HOST_IDENTIFIER;
    }

    /** Fixed widths per non-text column type (predictable, so no measuring needed). */
    private readonly EXTRA_COL_TYPE_WIDTH: Partial<
        Record<DotFolderListViewColumn['type'], string>
    > = {
        [DOT_FOLDER_LIST_VIEW_COLUMN_TYPE.DATE]: '12rem',
        [DOT_FOLDER_LIST_VIEW_COLUMN_TYPE.DATETIME]: '16rem',
        [DOT_FOLDER_LIST_VIEW_COLUMN_TYPE.TIME]: '9rem',
        [DOT_FOLDER_LIST_VIEW_COLUMN_TYPE.BOOLEAN]: '7rem',
        // Fixed thumbnail column — wider than the 4.5rem thumbnail box so a sortable header
        // (label + sort icon) fits on one line; never measured from content.
        [DOT_FOLDER_LIST_VIEW_COLUMN_TYPE.IMAGE]: '9rem'
    };

    /**
     * De-duplicated extra columns with a resolved width. The table sizes them itself so consumers
     * only pass field/header/type: an explicit `width` wins; otherwise text/number columns size to
     * their content (from the current rows), and other types use a fixed per-type width.
     */
    protected readonly $sizedExtraColumns = computed<DotFolderListViewColumn[]>(() => {
        const items = this.$items();

        return this.$safeExtraColumns().map((column) => ({
            ...column,
            width: this.#resolveExtraColumnWidth(column, items)
        }));
    });

    /**
     * Full column set the header/skeleton render: the fixed columns with the (de-duplicated, sized)
     * extra columns spliced in right after the "type" column. The body keeps its hardcoded cells and
     * renders only the extra cells generically in the same position.
     */
    /**
     * The fixed columns being rendered. Filtered off `HEADER_COLUMNS` rather than off the caller's
     * list, so display order stays the table's.
     */
    protected readonly $fixedColumns = computed<DotFolderListViewFixedColumn[]>(() => {
        const requested = this.$visibleColumns();

        return requested.length
            ? this.#fillWidth(HEADER_COLUMNS.filter((column) => requested.includes(column.field)))
            : HEADER_COLUMNS;
    });

    protected readonly $columns = computed<DotFolderListViewColumn[]>(() => {
        const extras = this.$sizedExtraColumns();
        const fixed = this.$fixedColumns();

        if (!extras.length) {
            return fixed;
        }

        const columns: DotFolderListViewColumn[] = [...fixed];
        // Anchored to where the "type" column *sits in the canonical order*, not to whether it is
        // rendered — the body's extra-column loop occupies that slot either way. Keying off the
        // rendered index instead appended the extras when Type was hidden, putting every extra cell
        // one heading early.
        const afterType = columns.findIndex((column) => column.order > TYPE_COLUMN_ORDER);
        const insertAt = afterType === -1 ? columns.length : afterType;
        columns.splice(insertAt, 0, ...extras);

        return columns;
    });

    /**
     * Fields actually being rendered. The body checks against this so its cells can never disagree
     * with the header — a mismatch shifts every cell out from under its heading.
     */
    protected readonly $visibleColumnSet = computed<Set<DotFolderListViewColumnField>>(
        () => new Set(this.$fixedColumns().map((column) => column.field))
    );

    /** Total column count including the leading checkbox column — drives colspan/skeleton span. */
    protected readonly $columnSpan = computed(() => this.$columns().length + 1);

    /**
     * Whether to render the paginator. Always while lazy — the table cannot know whether the server
     * has more. Non-lazy it holds the whole list, so below a page the paginator is dead weight.
     *
     * Replaces a `$showPagination` that was declared but never bound.
     */
    protected readonly $paginator = computed(
        () => this.$lazy() || this.$items().length > this.MIN_ROWS_PER_PAGE
    );

    /**
     * Row count the paginator divides into pages. `totalItems` is the server's count and is
     * meaningless to a caller holding every row, so a non-lazy table counts what it was given.
     */
    protected readonly $recordCount = computed(() =>
        this.$lazy() ? this.$totalItems() : this.$items().length
    );

    readonly $loadingRows = signal<number[]>(Array.from({ length: this.MIN_ROWS_PER_PAGE }));

    /**
     * Computed pass-through configuration for empty table.
     */
    readonly $ptConfig = computed(() => {
        const extras = this.$sizedExtraColumns();

        return {
            // Take the paginator out of play while a search is in flight, so the controls cannot be
            // clicked into queueing more searches. `pointer-events-none` covers the rows-per-page
            // dropdown too, which triggers a fetch of its own.
            paginator: {
                class: this.$loading() ? 'pointer-events-none opacity-60' : ''
            },
            table: {
                style: {
                    'table-layout': 'fixed',
                    // Grow the table past the container by the sum of the extra columns' widths, so
                    // each keeps its readable width and the results scroll horizontally instead of
                    // squeezing every column.
                    ...(extras.length > 0 && {
                        'min-width': `calc(100% + ${extras.map((column) => column.width).join(' + ')})`
                    }),
                    ...(this.$items().length === 0 && { height: '100%', width: '100%' })
                }
            }
        };
    });

    /**
     * State of the component.
     */
    readonly state = signalState({
        isDragging: false,
        languagesMap: new Map<number, DotLanguage>(),
        dragOverRowId: null as string | null
    });

    /**
     * Pushes the effective selection into the table, which one-way binding alone does not.
     *
     * PrimeNG's table is built for `[(selection)]`: toggling a checkbox sets
     * `preventSelectionSetterPropagation`, on the assumption that the value coming back is the one
     * it just emitted and so needs no re-keying. With a one-way binding that assumption breaks —
     * a parent that declines a change sends no new value at all, and one that normalises sends a
     * different array whose first arrival is swallowed by that flag. Either way the checkbox ends up
     * showing something other than the set that would be fired.
     *
     * Re-asserting here makes the rendered boxes follow this component's model unconditionally,
     * which is what "controlled" has to mean. Idempotent in the uncontrolled case, where the value
     * being re-asserted is the one PrimeNG just produced.
     */
    protected readonly $syncTableSelection = effect(() => {
        const selection = this.$tableSelection();
        // Read so a click re-runs this even when the effective selection did not change — the
        // decline case, where the parent sends nothing back but PrimeNG has already moved.
        this.#selectionRevision();

        const table = this.dataTable();

        if (!table) {
            return;
        }

        table.selection = selection;
        // Both are needed: the first rebuilds the row-key lookup `isSelected` reads, the second
        // tells the already-rendered checkboxes to re-read it.
        table.updateSelectionKeys();
        table.tableService.onSelectionChange();
    });

    /**
     * Effect that cleans the selected items when the items change.
     *
     * Only ever touches the uncontrolled set — `$tableSelection` prefers the caller's, so this can
     * no longer discard a selection the parent owns (in the action preview the rows and the
     * selection are the same data, and clearing here would empty the payload about to be fired).
     */
    protected readonly $cleanSelectedItems = effect(() => {
        this.$items();
        this.#internalSelection.set([]);
    });

    /**
     * Bound scroll handler to ensure the same reference is used for add/remove event listener
     */
    private readonly boundScrollHandler = this.scrollHandler.bind(this);

    /**
     * Rescales a subset of the fixed columns so their percentage widths add back up to 100%,
     * keeping their proportions to one another.
     *
     * `HEADER_COLUMNS` is authored to fill the table exactly. Take three of seven and the rest is
     * leftover — which `table-layout: fixed` shares across *every* column, the 3rem checkbox one
     * included. The visible result is a gutter between the checkbox and the first heading rather
     * than a wider title column.
     *
     * Left alone when any visible column carries a non-percentage width: there is nothing
     * meaningful to rescale a `12rem` against a `32%`.
     */
    #fillWidth(columns: DotFolderListViewFixedColumn[]): DotFolderListViewFixedColumn[] {
        const widths = columns.map((column) =>
            column.width?.endsWith('%') ? Number.parseFloat(column.width) : NaN
        );

        if (widths.some((width) => !Number.isFinite(width))) {
            return columns;
        }

        const total = widths.reduce((sum, width) => sum + width, 0);

        if (!total) {
            return columns;
        }

        return columns.map((column, index) => ({
            ...column,
            width: `${((widths[index] / total) * 100).toFixed(2)}%`
        }));
    }

    /**
     * Resolves an extra column's width. Explicit width wins; otherwise text/number columns size to
     * their content — the average value length across the current rows (never narrower than the
     * header), padded and clamped, in `ch` so it scales with the cell font. Other types use a fixed
     * per-type width. Average (not max) keeps one long outlier from blowing the column out; overflow
     * truncates and the table scrolls horizontally.
     */
    #resolveExtraColumnWidth(
        column: DotFolderListViewColumn,
        items: DotContentDriveItem[]
    ): string {
        if (column.width) {
            return column.width;
        }

        const lengths = items
            .map((item) => (item as Record<string, unknown>)?.[column.field])
            .filter((value) => value !== null && value !== undefined && value !== '')
            .map((value) => String(value).length);

        const averageLength = lengths.length
            ? Math.round(lengths.reduce((sum, length) => sum + length, 0) / lengths.length)
            : 0;

        const fixed = column.type && this.EXTRA_COL_TYPE_WIDTH[column.type];

        // A fixed-type column renders formatted text (a date, an icon, a thumbnail) whose width does
        // not depend on the raw value, so it must not be measured from content — only from its
        // header, which is the one part that can outgrow the fixed width.
        const contentLength = fixed ? 0 : averageLength;

        // Both are clamped here, and neither has to be exact: a header is guaranteed to fit by
        // `min-width: max-content` on the header cell, which the browser measures precisely. Sizing it
        // from a character count instead over-reserved badly — `1ch` is the width of "0", wider than
        // the average character in a proportional font, so a 41-character label asked for ~40% more
        // room than it needed.
        const chars = Math.min(
            Math.max(
                Math.max(column.header?.length ?? 0, contentLength) + this.EXTRA_COL_PAD_CH,
                this.EXTRA_COL_MIN_CH
            ),
            this.EXTRA_COL_MAX_CH
        );

        if (!fixed) {
            return `${chars}ch`;
        }

        // The per-type width is a floor, not an override: a boolean column pinned at 7rem could not
        // show a header longer than that. Resolved to whichever is wider rather than a CSS `max()`,
        // because these widths are also summed into the table's `min-width: calc(100% + …)` and a
        // single length keeps that expression parseable.
        return chars > parseFloat(fixed) * this.REM_TO_CH ? `${chars}ch` : fixed;
    }

    ngOnInit(): void {
        // Only the locale column reads the map, so a caller that hides it — the action preview,
        // which is a fresh instance of this grid on every drill-in — should not pay for the request.
        if (!this.$visibleColumnSet().has('languageId')) {
            return;
        }

        // We should be getting this from the Global Store
        // But it gets out of scope for the ticket.
        this.dotLanguagesService
            .get()
            .pipe(take(1))
            .subscribe((languages) => {
                const languagesMap = new Map<number, DotLanguage>();
                languages.forEach((language) => {
                    languagesMap.set(language.id, language);
                });

                patchState(this.state, { languagesMap });
            });
    }

    /**
     * Initializes the component after the view has been initialized
     */
    ngAfterViewInit(): void {
        const tableBody = this.getTableBody();

        if (tableBody) {
            tableBody.addEventListener('scroll', this.boundScrollHandler);
        }
    }

    /**
     * Destroys the component
     */
    ngOnDestroy(): void {
        const tableBody = this.getTableBody();
        if (tableBody) {
            tableBody.removeEventListener('scroll', this.boundScrollHandler);
        }
    }

    /**
     * Gets the table body element
     * @returns The table body element
     */
    private getTableBody(): HTMLElement | null {
        return this.dataTable()?.el.nativeElement.querySelector('.p-datatable-table-container');
    }

    /**
     * Handles scroll events from the table body
     * @param event The scroll event
     */
    private scrollHandler(event: Event) {
        this.scroll.emit(event);
    }

    /**
     * Handles right click on a content item to show context menu
     * @param event The mouse event
     * @param contentlet The content item that was right clicked
     */
    onContextMenu(event: Event, contentlet: DotContentDriveItem) {
        // Returns before `preventDefault` so a read-only table leaves the browser's own menu alone
        // rather than suppressing it and offering nothing in its place.
        if (this.$readOnly()) {
            return;
        }

        event.preventDefault();
        this.rightClick.emit({ event, contentlet });
    }

    /**
     * Handles pagination events from the PrimeNG Table
     * @param event The lazy load event containing pagination info
     */
    onPage(event: LazyLoadEvent) {
        // Lazy only. `paginate` means "fetch me this page" — nothing a caller holding every row can
        // act on, and the skeleton rows it primes would never be shown. The table pages itself.
        if (!this.$lazy()) {
            return;
        }

        // A search is already in flight. The paginator stays mounted while the rows are replaced by
        // skeletons, so without this every further click fires another search for a page the user
        // cannot see yet, and the last response to arrive wins regardless of which page was asked
        // for last. The controls are also visually disabled via `$ptConfig`, but this is the part
        // that has to hold: a keyboard activation or a fast double click does not wait for CSS.
        if (this.$loading()) {
            return;
        }

        const page = event.first && event.rows ? Math.floor(event.first / event.rows) + 1 : 1;
        this.paginate.emit({ ...event, page });
        this.$loadingRows.set([...Array(event.rows)]);
    }

    /**
     * Handles selection changes in the table and emits selected items.
     *
     * Records the set for the uncontrolled case and always reports it. A controlled table renders
     * the caller's set until the caller echoes this back, so the parent can decline a change.
     *
     * @param items The table's new selection
     */
    onSelectionChange(items: DotContentDriveItem[]) {
        this.#internalSelection.set(items ?? []);
        this.selectionChange.emit(items ?? []);
        // Runs after PrimeNG has finished mutating its own state — it drops the row's selection key
        // *after* emitting, so anything restored from inside this handler would be undone.
        this.#selectionRevision.update((revision) => revision + 1);
    }

    /**
     * Handles sort events from the PrimeNG Table
     * @param event The sort event containing sort field and order
     */
    onSort(event: SortEvent) {
        this.sort.emit(event);
    }

    /**
     * Handles double click on a content item
     * @param contentlet The content item that was double clicked
     */
    onDoubleClick(contentlet: DotContentDriveItem) {
        // Guarded here rather than per template binding: the row's dblclick, the title and the
        // thumbnail all land on this, and opening an item from a dialog would navigate away from it.
        if (this.$readOnly()) {
            return;
        }

        this.doubleClick.emit(contentlet);
    }

    /**
     * Handles drag start on a content item
     * @param event The drag start event
     * @param contentlet The content item that was dragged
     */
    onDragStart(event: DragEvent, contentlet: DotContentDriveItem) {
        if (!event.dataTransfer) return;

        event.stopPropagation();

        // Set dragging state to true
        patchState(this.state, { isDragging: true });

        // Check if the dragged item is in the current selection
        const selected = this.selectedItems;
        const isDraggingSelectedItem = selected.some(
            (item) => item.identifier === contentlet.identifier
        );

        // Determine which items are being dragged
        const itemsToDrag = isDraggingSelectedItem && selected.length > 0 ? selected : [contentlet];

        event.dataTransfer.effectAllowed = 'move';
        event.dataTransfer.setData(DOT_DRAG_ITEM, '');

        // Create drag image from actual rendered content (img/icon)
        const dragImage = this.createDragImage(itemsToDrag.slice(0, 3), itemsToDrag.length);
        if (dragImage) {
            event.dataTransfer.setDragImage(dragImage, 40, 40);
        }

        // Emit the drag start event with the items being dragged
        this.dragStart.emit(itemsToDrag);
    }

    /**
     * Handles drag over a content item to show hover effect
     * @param event The drag over event
     * @param targetItem The content item being dragged over
     */
    onDragOver(event: DragEvent, targetItem: DotContentDriveItem) {
        // Only handle internal drags (item to item)
        const isInternalDrag = event.dataTransfer?.types.includes(DOT_DRAG_ITEM);
        if (isInternalDrag) {
            event.preventDefault();
            patchState(this.state, { dragOverRowId: targetItem.identifier });
        }
    }

    /**
     * Handles drop on a content item
     * Only handles internal drags (item to item). External file drops are allowed to bubble up to the dropzone.
     * @param event The drop event
     * @param targetItem The content item that was dropped
     */
    onDrop(event: DragEvent, targetItem: DotContentDriveItem) {
        // If this is an external file drop, let it bubble up to the dropzone
        const hasFiles = event.dataTransfer?.files && event.dataTransfer.files.length > 0;
        const isInternalDrag = event.dataTransfer?.types.includes(DOT_DRAG_ITEM);

        // Only handle internal drags (item to item), not file drops
        if (hasFiles || !isInternalDrag) {
            return; // Let the event bubble up to the dropzone
        }

        event.preventDefault();
        event.stopPropagation();
        patchState(this.state, { dragOverRowId: null });
        this.drop.emit(targetItem);
    }

    /**
     * Handles drag end on a content item
     */
    onDragEnd() {
        // Reset dragging state to false and clear drag over
        patchState(this.state, { isDragging: false, dragOverRowId: null });
        this.dragEnd.emit();
    }

    /**
     * Creates drag image from actual rendered thumbnails (img/icon elements)
     * @param items The items to create the drag image from
     * @param totalCount The total number of items
     * @returns The drag image element
     */
    private createDragImage(items: DotContentDriveItem[], totalCount: number): HTMLElement | null {
        const container = this.renderer.createElement('div');
        this.renderer.addClass(container, 'drag-image-container');

        items.forEach((item, idx) => {
            if (!item?.identifier) {
                return;
            }

            // Find the thumbnail element
            // Note: Using querySelector here as Renderer2 doesn't provide query methods
            // This is acceptable since drag operations are client-side only
            const thumbnail = document.querySelector(
                `[data-table-id="${item.identifier}"]`
            ) as HTMLElement;

            if (!thumbnail) {
                return;
            }

            const wrapper = this.renderer.createElement('div');
            this.renderer.addClass(wrapper, 'drag-image-item');
            this.renderer.addClass(wrapper, `drag-image-item-${idx}`);

            // Check if the thumbnail is an icon or an image - if so, copy its HTML
            const firstChild = thumbnail.tagName === 'I' ? thumbnail : thumbnail.firstElementChild;

            if (!firstChild) {
                return;
            }

            const childIsImage = firstChild.tagName.toLowerCase() === 'img';
            const hasShadowRoot = firstChild.shadowRoot;

            if (!childIsImage && !hasShadowRoot) {
                const clone = firstChild.cloneNode(true) as HTMLElement;
                this.renderer.appendChild(wrapper, clone);
            } else {
                this.renderer.setProperty(
                    wrapper,
                    'innerHTML',
                    childIsImage ? firstChild.outerHTML : firstChild.shadowRoot?.innerHTML || ''
                );
            }

            this.renderer.appendChild(container, wrapper);
        });

        // Add badge if multiple items
        if (totalCount > 1) {
            const badge = this.renderer.createElement('div');
            this.renderer.addClass(badge, 'drag-image-badge');
            this.renderer.setProperty(badge, 'textContent', totalCount.toString());
            this.renderer.appendChild(container, badge);
        }

        this.renderer.appendChild(document.body, container);
        // This will remove the container from the dom after the drag captures the images
        setTimeout(() => this.renderer.removeChild(document.body, container), 0);

        return container;
    }

    /**
     * Handles first change event from the PrimeNG Table
     * Basically primeNG Table handles the change of the first on every OnChange
     * Making it lose the reference if you do a sort and do not handle this manually
     *
     * Check this issue to know if we are able to remove this function
     * since its a legacy issue that they are basically ignoring.
     * https://github.com/primefaces/primeng/issues/11898#issuecomment-1831076132
     */
    protected onFirstChange() {
        // Lazy only. `$offset` is the parent's cursor into a server-paged list; a non-lazy caller
        // holds every row, has no cursor and never binds it — so re-pinning `first` to a permanent
        // `0` snapped the table back to page one on every page change, leaving everything past the
        // first page unreachable.
        if (!this.$lazy()) {
            return;
        }

        const dataTable = this.dataTable();

        if (dataTable) {
            dataTable.first = this.$offset();
        }
    }
}
