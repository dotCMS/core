import { patchState, signalState } from '@ngrx/signals';

import { DatePipe, NgTemplateOutlet } from '@angular/common';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    input,
    OnDestroy,
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
    DotContentDriveBrowseItem,
    DotContentDriveItem,
    DotContentDrivePaginateEvent,
    DotLanguage
} from '@dotcms/dotcms-models';

import {
    DOT_DRAG_ITEM,
    DotFolderListViewFixedColumn,
    HEADER_COLUMNS,
    rescaleToWidthBudget
} from './constants';
import {
    DOT_FOLDER_LIST_VIEW_COLUMN_TYPE,
    DotFolderListViewColumn,
    DotFolderListViewColumnField,
    DotFolderListViewSelectionMode
} from './models';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';
import { DotLocaleTagPipe } from '../../pipes/dot-locale-tag/dot-locale-tag.pipe';
import { DotRelativeDatePipe } from '../../pipes/dot-relative-date/dot-relative-date.pipe';
import { DotContentThumbnailComponent } from '../dot-content-thumbnail/dot-content-thumbnail.component';
import { DotContentletStatusBadgeComponent } from '../dot-contentlet-status-badge/dot-contentlet-status-badge.component';
import { SYSTEM_HOST_ID } from '../dot-folder-tree/constants';

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
export class DotFolderListViewComponent implements OnInit, AfterViewInit, OnDestroy {
    private readonly renderer = inject(Renderer2);
    private readonly dotLanguagesService = inject(DotLanguagesService);

    dataTable = viewChild<Table>('dataTable');

    /**
     * A signal that takes an array of DotContentDriveItem objects.
     *
     * @alias items
     */
    /**
     * Rows to render.
     *
     * Wider than {@link DotContentDriveItem} because the asset picker's browse mode can list menu
     * links. Everything the list *acts on* — the context menu, drag, drop — stays narrow: a link
     * has no workflow actions, so it is displayed and selectable but never actionable.
     */
    $items = input<DotContentDriveBrowseItem[]>([], { alias: 'items' });

    /**
     * A signal that takes the total number of items.
     *
     * @alias totalItems
     */
    $totalItems = input<number>(0, { alias: 'totalItems' });

    /**
     * A signal that takes the loading state.
     *
     * @alias loading
     */
    $loading = input<boolean>(false, { alias: 'loading' });

    /**
     * A signal that takes the offset.
     *
     * @alias offset
     */
    $offset = input<number>(0, { alias: 'offset' });

    /**
     * Extra, caller-provided columns appended after the fixed "type" column. Agnostic of how they
     * are sourced (Content Drive derives them from the selected content type's "Show In List" fields).
     *
     * @alias extraColumns
     */
    $extraColumns = input<DotFolderListViewColumn[]>([], { alias: 'extraColumns' });

    /**
     * Table selection mode. Defaults to `multiple` so Content Drive behavior is unchanged.
     * AssetPicker passes `single`.
     *
     * @alias selectionMode
     */
    $selectionMode = input<DotFolderListViewSelectionMode>('multiple', { alias: 'selectionMode' });

    /**
     * Whether rows offer per-row actions: the kebab column and the right-click menu.
     *
     * Defaults to `true` so Content Drive — where a row is something you manage (publish, move,
     * delete) — is unchanged. The AssetPicker turns it off: there a row is something you *pick*, and
     * the actions would either do nothing (nothing is listening) or take the editor out of the flow.
     * It also gates `onContextMenu`, so right-click keeps the browser's own menu instead of being
     * swallowed for a menu that never opens.
     *
     * @alias showActions
     */
    $showActions = input(true, { alias: 'showActions' });

    /**
     * Whether clicking a row's title or thumbnail **opens** the item rather than selecting the row.
     *
     * Defaults to `true` for Content Drive, where the title is a distinct affordance: it navigates
     * to the editor, so it swallows the click to keep the row from being selected underneath.
     *
     * The AssetPicker turns it off. There is nothing to open there — a row exists to be picked — so
     * swallowing the click left the row selectable only through its padding, with the radio out of
     * step with what the store had already recorded. With this off the click bubbles to
     * `pSelectableRow` and the whole row selects. Travels with {@link $showActions}: together they
     * are what separates a picker from a manager.
     *
     * @alias titleOpensItem
     */
    $titleOpensItem = input(true, { alias: 'titleOpensItem' });

    /**
     * An output that emits the selected items.
     *
     * @alias selectionChange
     */
    selectionChange = output<DotContentDriveBrowseItem[]>();

    /**
     * An output that emits the pagination event.
     *
     * @alias paginate
     */
    paginate = output<DotContentDrivePaginateEvent>();

    /**
     * An output that emits the sort event.
     *
     * @alias sort
     */
    sort = output<SortEvent>();

    /**
     * An output that emits the right click event.
     *
     * @alias rightClick
     */
    rightClick = output<ContextMenuData>();

    /**
     * An output that emits the double click event.
     *
     * @alias doubleClick
     */
    doubleClick = output<DotContentDriveBrowseItem>();

    /**
     * An output that emits the drag start event.
     *
     * @alias dragStart
     */
    dragStart = output<DotContentDriveItem[]>();

    /**
     * An output that emits the drag end event.
     *
     * @alias dragEnd
     */
    dragEnd = output<void>();

    /**
     * An output that emits the drop event, carrying the target value.
     *
     * @alias drop
     */
    drop = output<DotContentDriveItem>();

    /**
     * An output that emits the scroll event.
     *
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
     * @type {InputSignal<DotContentDriveItem | DotContentDriveItem[] | null | undefined>}
     * @alias selection
     */
    $selection = input<DotContentDriveBrowseItem | DotContentDriveBrowseItem[] | null | undefined>(
        undefined,
        {
            alias: 'selection'
        }
    );

    /** Checked set while uncontrolled. Ignored as long as `selection` is provided. */
    readonly #internalSelection = signal<DotContentDriveBrowseItem[]>([]);

    /**
     * Bumped on every selection change the table reports. Exists so the sync effect below re-runs
     * after a click whose outcome the parent declines — where the effective selection is unchanged
     * and a value-based dependency alone would never fire.
     */
    readonly #selectionRevision = signal(0);

    /**
     * The effective selection — the caller's set when one is provided, otherwise our own.
     *
     * PrimeNG binds an array in `multiple` mode and a single row (or `null`) in `single` mode.
     * The controlled input is normalized to an array internally and projected back into whichever
     * shape the current mode expects.
     *
     * @alias selectedItems
     */
    get selectedItems(): DotContentDriveBrowseItem | DotContentDriveBrowseItem[] | null {
        const items =
            this.$selection() !== undefined
                ? this.#asSelectedArray(this.$selection())
                : this.#internalSelection();

        return this.$selectionMode() === 'multiple' ? items : (items[0] ?? null);
    }

    set selectedItems(selection: DotContentDriveBrowseItem | DotContentDriveBrowseItem[] | null) {
        this.#internalSelection.set(this.#asSelectedArray(selection));
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

    /** Character-count clamps for content-based (text/number) column widths, in `ch`. */
    private readonly EXTRA_COL_MIN_CH = 8;
    private readonly EXTRA_COL_MAX_CH = 32;

    private readonly EXTRA_COL_PAD_CH = 3;
    /**
     * Room a header needs for its sort indicator, on top of its label.
     *
     * Measured in the rendered portlet: the icon plus the space before it is ~23px against a `1ch` of
     * 8.67px in the header font, so 3 covers it. Without it, the estimate for a header like "Bool
     * Radio" landed at 112.7px against a 113px need, short by a hair, which is all it takes to spill
     * into the next header since nothing clips it.
     *
     * Reserved only when the icon is actually drawn. The template gates it on
     * `sortable && !readOnly`, so keying this off `sortable` alone reserved width nothing rendered
     * into whenever a read-only table carried extra columns.
     */
    private readonly EXTRA_COL_SORT_ICON_CH = 3;
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
    protected isSharedAsset(item: DotContentDriveBrowseItem): boolean {
        return 'host' in item && item.host === SYSTEM_HOST_ID;
    }

    /**
     * Fixed floor per non-text column type (predictable, so no measuring needed), counted in `ch`:
     * the same unit the header estimate below uses, so the two can be compared exactly.
     *
     * They were authored in `rem` and are converted here, which is the fix for the reported bug. A
     * `rem` floor and a `ch` estimate cannot be compared without knowing both the root font size and
     * the header font's advance width, and the conversion that did it (2 `ch` per `rem`) assumed a
     * 16px root while dotCMS sets 14px. That scored a 7rem boolean column as 14ch when it is really
     * 11.3ch, so the floor kept beating headers that did not fit inside it. One unit removes the
     * conversion, and with it that whole class of error.
     *
     * The counts hold the widths the `rem` values rendered at (14px root, `1ch` of 8.67px in the
     * header font) to within a few px: 19ch for 12rem, 26ch for 16rem, 15ch for 9rem, 11ch for 7rem.
     */
    private readonly EXTRA_COL_TYPE_CH: Partial<
        Record<NonNullable<DotFolderListViewColumn['type']>, number>
    > = {
        [DOT_FOLDER_LIST_VIEW_COLUMN_TYPE.DATE]: 19,
        [DOT_FOLDER_LIST_VIEW_COLUMN_TYPE.DATETIME]: 26,
        [DOT_FOLDER_LIST_VIEW_COLUMN_TYPE.TIME]: 15,
        [DOT_FOLDER_LIST_VIEW_COLUMN_TYPE.BOOLEAN]: 11,
        // Fixed thumbnail column, wider than the 4.5rem thumbnail box and never measured from
        // content. A sortable header gets its own room from the estimate below.
        [DOT_FOLDER_LIST_VIEW_COLUMN_TYPE.IMAGE]: 15
    };

    /**
     * De-duplicated extra columns with a resolved width. The table sizes them itself so consumers
     * only pass field/header/type: an explicit `width` wins; otherwise text/number columns size to
     * their content (from the current rows), and other types use a fixed per-type width.
     */
    protected readonly $sizedExtraColumns = computed<DotFolderListViewColumn[]>(() => {
        const items = this.$items();
        // Read unconditionally so this stays a dependency even when no extra column is sortable, and
        // resolved here rather than inside the resolver so the width is derived from what the header
        // actually renders: the template draws a sort icon only for `sortable && !readOnly`.
        const readOnly = this.$readOnly();

        return this.$safeExtraColumns().map((column) => ({
            ...column,
            width: this.#resolveExtraColumnWidth(column, items, !!column.sortable && !readOnly)
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
     *
     * Whatever is rendered has its percentages rescaled to the budget the full set carries; see
     * {@link rescaleToWidthBudget} for why the leftover cannot just be left unclaimed. The full set
     * with actions passes through untouched, since its percentages already add up to that total.
     */
    protected readonly $fixedColumns = computed<DotFolderListViewFixedColumn[]>(() => {
        const requested = this.$visibleColumns();
        const shown = (
            requested.length
                ? HEADER_COLUMNS.filter((column) => requested.includes(column.field))
                : HEADER_COLUMNS
        ).filter((column) => this.$showActions() || column.field !== 'actions');

        // Keyed off what is actually rendered rather than off whether a subset was requested: the
        // full set with the actions column hidden totals 91%, not the budget, and would hand the
        // leftover to the checkbox column just like a subset does. `rescaleToWidthBudget` is a
        // no-op when the total already matches, so the full set with actions passes through.
        return rescaleToWidthBudget(shown);
    });

    protected readonly $columns = computed<DotFolderListViewColumn[]>(() => {
        const fixed = this.$fixedColumns();
        const extras = this.$sizedExtraColumns();

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

    /** Total column count including the leading checkbox/radio column — drives colspan/skeleton span. */
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
        const selection = this.selectedItems;
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
     * Only ever touches the uncontrolled set — the `selectedItems` getter prefers the caller's
     * `selection` input when one is present, so this can no longer discard a selection the parent
     * owns (in the action preview the rows and the selection are the same data, and clearing here
     * would empty the payload about to be fired).
     */
    protected readonly $cleanSelectedItems = effect(() => {
        this.$items();
        this.selectedItems = this.$selectionMode() === 'multiple' ? [] : null;
    });

    /**
     * Bound scroll handler to ensure the same reference is used for add/remove event listener
     */
    private readonly boundScrollHandler = this.scrollHandler.bind(this);

    /**
     * Normalizes PrimeNG selection (array in multiple mode, object/null in single) to an array.
     */
    #asSelectedArray(
        selection: DotContentDriveBrowseItem | DotContentDriveBrowseItem[] | null | undefined
    ): DotContentDriveBrowseItem[] {
        if (!selection) {
            return [];
        }

        return Array.isArray(selection) ? selection : [selection];
    }

    /**
     * Resolves an extra column's width. Explicit width wins; otherwise text/number columns size to
     * their content — the average value length across the current rows (never narrower than the
     * header), padded and clamped, in `ch` so it scales with the cell font. Other types keep their
     * per-type width as a floor, widening to the header when the header needs more. Average (not max)
     * keeps one long outlier from blowing the column out; overflow truncates in the cells and the
     * table scrolls horizontally.
     *
     * `showsSortIcon` says whether this header will draw a sort indicator, so the room for one is
     * reserved only when it will be used.
     */
    #resolveExtraColumnWidth(
        column: DotFolderListViewColumn,
        items: DotContentDriveBrowseItem[],
        showsSortIcon: boolean
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

        const typeFloor = column.type ? this.EXTRA_COL_TYPE_CH[column.type] : undefined;
        const hasTypeFloor = typeFloor !== undefined;

        // A floored column renders formatted text (a date, an icon, a thumbnail) whose width does not
        // depend on the raw value, so it must not be measured from content, only from its header,
        // which is the one part that can outgrow the floor.
        const contentLength = hasTypeFloor ? 0 : averageLength;

        // `1ch` is the width of "0", wider than the average character in a proportional font, so a
        // per-character count reserves a little more than the label strictly needs. That slack is
        // load-bearing: together with the pad it is the only room the cell padding and the sort
        // indicator get. `min-width: max-content` on the header cell reads like the real guarantee,
        // but it is inert under `table-layout: fixed`: measured in the rendered portlet, a 7rem header
        // cell whose label and sort icon needed 113px was laid out at 98px, min-width and all, and the
        // surplus drew straight over the next heading.
        //
        // The floor takes part in the same clamp, so the wider of the two always wins and nothing has
        // to be converted between units.
        return `${Math.min(
            Math.max(
                Math.max(column.header?.length ?? 0, contentLength) +
                    this.EXTRA_COL_PAD_CH +
                    (showsSortIcon ? this.EXTRA_COL_SORT_ICON_CH : 0),
                hasTypeFloor ? typeFloor : this.EXTRA_COL_MIN_CH
            ),
            this.EXTRA_COL_MAX_CH
        )}ch`;
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
    onContextMenu(event: Event, contentlet: DotContentDriveBrowseItem) {
        if (!this.$showActions() || this.$readOnly()) {
            return;
        }

        // A menu link carries no workflow state, so every action the menu offers is meaningless for
        // one. Content Drive never lists links, so this only ever fires in the asset picker.
        if (!isActionable(contentlet)) {
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
     * @param selection The table's new selection
     */
    onSelectionChange(selection: DotContentDriveBrowseItem | DotContentDriveBrowseItem[] | null) {
        this.selectedItems = selection;
        this.selectionChange.emit(this.#asSelectedArray(selection));
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
    onDoubleClick(contentlet: DotContentDriveBrowseItem) {
        // Guarded here rather than per template binding: the row's dblclick, the title and the
        // thumbnail all land on this, and opening an item from a dialog would navigate away from it.
        if (this.$readOnly()) {
            return;
        }

        this.doubleClick.emit(contentlet);
    }

    /**
     * Handles a click on the row's title or thumbnail.
     *
     * When the title opens the item it has to swallow the click, or the row would be selected on
     * the way out too. When it does not, the click is left alone so it reaches `pSelectableRow` and
     * the whole row — content included, not just its padding — selects.
     *
     * @param event The click event
     * @param contentlet The content item whose title was clicked
     */
    onTitleClick(event: Event, contentlet: DotContentDriveBrowseItem) {
        if (!this.$titleOpensItem()) {
            return;
        }

        this.onDoubleClick(contentlet);
        event.stopPropagation();
    }

    /**
     * Handles drag start on a content item
     * @param event The drag start event
     * @param contentlet The content item that was dragged
     */
    onDragStart(event: DragEvent, contentlet: DotContentDriveBrowseItem) {
        // The `draggable` attribute already keeps the row still, but it only covers user-initiated
        // drags: a programmatic `dragstart` would still reach this and emit a move nothing can drop.
        if (this.$readOnly()) {
            return;
        }

        if (!event.dataTransfer) return;

        event.stopPropagation();

        // Set dragging state to true
        patchState(this.state, { isDragging: true });

        // Check if the dragged item is in the current selection
        const selected = this.#asSelectedArray(this.selectedItems);
        const isDraggingSelectedItem = selected.some(
            (item) => item.identifier === contentlet.identifier
        );

        // Determine which items are being dragged. Menu links are filtered out: they have no
        // permissions and nowhere to be moved to, so they are never part of a drag payload.
        const itemsToDrag = (
            isDraggingSelectedItem && selected.length > 0 ? selected : [contentlet]
        ).filter(isActionable);

        if (!itemsToDrag.length) {
            return;
        }

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
    onDragOver(event: DragEvent, targetItem: DotContentDriveBrowseItem) {
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
    onDrop(event: DragEvent, targetItem: DotContentDriveBrowseItem) {
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

        if (!isActionable(targetItem)) {
            return;
        }

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
    private createDragImage(
        items: DotContentDriveBrowseItem[],
        totalCount: number
    ): HTMLElement | null {
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

/**
 * Whether a row is something the shared folder actions can act on.
 *
 * Everything except a menu link: links have no workflow state, no permissions of their own and no
 * editor to open, so they are displayed and selectable but never actionable.
 */
function isActionable(item: DotContentDriveBrowseItem): item is DotContentDriveItem {
    const row = item as { type?: string; extension?: string };

    return row.type !== 'link' && row.extension !== 'link';
}
