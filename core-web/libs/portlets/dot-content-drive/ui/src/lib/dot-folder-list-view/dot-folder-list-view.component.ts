import { patchState, signalState } from '@ngrx/signals';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    CUSTOM_ELEMENTS_SCHEMA,
    effect,
    inject,
    input,
    OnInit,
    output,
    Renderer2
} from '@angular/core';

import { LazyLoadEvent, SortEvent } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';

import { DotLanguagesService } from '@dotcms/data-access';
import { ContextMenuData, DotContentDriveItem, DotLanguage } from '@dotcms/dotcms-models';
import {
    DotContentletStatusPipe,
    DotLocaleTagPipe,
    DotMessagePipe,
    DotRelativeDatePipe
} from '@dotcms/ui';

import { DOT_DRAG_ITEM, HEADER_COLUMNS } from '../shared/constants';

@Component({
    selector: 'dot-folder-list-view',
    imports: [
        ButtonModule,
        ChipModule,
        DotContentletStatusPipe,
        DotMessagePipe,
        DotRelativeDatePipe,
        SkeletonModule,
        TableModule,
        DotLocaleTagPipe
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    templateUrl: './dot-folder-list-view.component.html',
    styleUrl: './dot-folder-list-view.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotFolderListViewComponent implements OnInit {
    private readonly renderer = inject(Renderer2);
    private readonly dotLanguagesService = inject(DotLanguagesService);

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
    paginate = output<LazyLoadEvent>();

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
     * An array of selected items.
     *
     * @type {DotContentDriveItem[]}
     * @alias selectedItems
     */
    selectedItems = [];

    readonly MIN_ROWS_PER_PAGE = 20;
    protected readonly rowsPerPageOptions = [this.MIN_ROWS_PER_PAGE, 40, 60];
    protected readonly HEADER_COLUMNS = HEADER_COLUMNS;
    protected readonly SKELETON_SPAN = HEADER_COLUMNS.length + 1;
    protected readonly $showPagination = computed(
        () => this.$totalItems() > this.MIN_ROWS_PER_PAGE
    );

    /**
     * Computed style class for the table.
     *
     * @type {ComputedSignal<string>}
     * @alias styleClass
     */
    protected readonly $styleClass = computed(() =>
        this.$items().length === 0 ? 'dotTable empty-table' : 'dotTable'
    );

    /**
     * State of the component.
     */
    readonly state = signalState({
        isDragging: false,
        currentPageFirstRowIndex: 0,
        languagesMap: new Map<number, DotLanguage>()
    });

    /**
     * Effect that handles pagination state management
     */
    protected readonly firstEffect = effect(() => {
        const showPagination = this.$showPagination();
        if (showPagination) {
            patchState(this.state, { currentPageFirstRowIndex: 0 });
        }
    });

    /**
     * Effect that cleans the selected items when the items change
     */
    protected readonly $cleanSelectedItems = effect(() => {
        this.$items();
        this.selectedItems = [];
    });

    ngOnInit(): void {
        // We should be getting this from the Global Store
        // But it gets out of scope for the ticket.
        this.dotLanguagesService.get().subscribe((languages) => {
            const languagesMap = new Map<number, DotLanguage>();
            languages.forEach((language) => {
                languagesMap.set(language.id, language);
            });

            patchState(this.state, { languagesMap });
        });
    }

    /**
     * Handles right click on a content item to show context menu
     * @param event The mouse event
     * @param contentlet The content item that was right clicked
     */
    onContextMenu(event: Event, contentlet: DotContentDriveItem) {
        event.preventDefault();
        this.rightClick.emit({ event, contentlet });
    }

    /**
     * Handles pagination events from the PrimeNG Table
     * @param event The lazy load event containing pagination info
     */
    onPage(event: LazyLoadEvent) {
        patchState(this.state, { currentPageFirstRowIndex: event.first });
        this.paginate.emit(event);
    }

    /**
     * Handles selection changes in the table and emits selected items
     */
    onSelectionChange() {
        this.selectionChange.emit(this.selectedItems);
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
                `[data-id="${item.identifier}"]`
            ) as HTMLElement;

            if (!thumbnail) {
                return;
            }

            const wrapper = this.renderer.createElement('div');
            this.renderer.addClass(wrapper, 'drag-image-item');
            this.renderer.addClass(wrapper, `drag-image-item-${idx}`);

            // Check if first child is an img - if so, copy its HTML
            const firstChild = thumbnail.firstElementChild;

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
     * Handles drag end on a content item
     */
    onDragEnd() {
        // Reset dragging state to false
        patchState(this.state, { isDragging: false });
        this.dragEnd.emit();
    }
}
