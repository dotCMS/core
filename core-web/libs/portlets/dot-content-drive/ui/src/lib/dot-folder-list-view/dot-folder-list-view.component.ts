import {
    ChangeDetectionStrategy,
    Component,
    computed,
    CUSTOM_ELEMENTS_SCHEMA,
    effect,
    input,
    output,
    signal
} from '@angular/core';

import { LazyLoadEvent, SortEvent } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';

import { ContextMenuData, DotContentDriveItem } from '@dotcms/dotcms-models';
import { DotContentletStatusPipe, DotMessagePipe, DotRelativeDatePipe } from '@dotcms/ui';

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
        TableModule
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    templateUrl: './dot-folder-list-view.component.html',
    styleUrl: './dot-folder-list-view.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotFolderListViewComponent {
    $items = input<DotContentDriveItem[]>([], { alias: 'items' });
    $totalItems = input<number>(0, { alias: 'totalItems' });
    $loading = input<boolean>(false, { alias: 'loading' });

    selectionChange = output<DotContentDriveItem[]>();
    paginate = output<LazyLoadEvent>();
    sort = output<SortEvent>();
    rightClick = output<ContextMenuData>();
    doubleClick = output<DotContentDriveItem>();
    dragStart = output<DotContentDriveItem[]>();

    selectedItems = [];

    readonly MIN_ROWS_PER_PAGE = 20;
    protected readonly rowsPerPageOptions = [this.MIN_ROWS_PER_PAGE, 40, 60];
    protected readonly HEADER_COLUMNS = HEADER_COLUMNS;
    protected readonly SKELETON_SPAN = HEADER_COLUMNS.length + 1;
    protected readonly $showPagination = computed(
        () => this.$totalItems() > this.MIN_ROWS_PER_PAGE
    );
    protected readonly $styleClass = computed(() =>
        this.$items().length === 0 ? 'dotTable empty-table' : 'dotTable'
    );

    /**
     * Index of the first row to be displayed in the current page.
     * Used by PrimeNG Table for pagination state management.
     */
    protected readonly $currentPageFirstRowIndex = signal<number>(0);

    /**
     * Effect that handles pagination state management
     */
    protected readonly firstEffect = effect(() => {
        const showPagination = this.$showPagination();
        if (showPagination) {
            this.$currentPageFirstRowIndex.set(0);
        }
    });

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
        this.$currentPageFirstRowIndex.set(event.first);
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
     */
    private createDragImage(items: DotContentDriveItem[], totalCount: number): HTMLElement | null {
        const container = document.createElement('div');
        container.className = 'drag-image-container';

        items.forEach((item, idx) => {
            if (!item?.identifier) return;

            // Find the thumbnail element
            const thumbnail = document.querySelector(
                `[data-id="${item.identifier}"]`
            ) as HTMLElement;

            if (thumbnail) {
                const wrapper = document.createElement('div');
                wrapper.className = `drag-image-item drag-image-item-${idx}`;

                // Check if first child is an img - if so, copy its HTML
                const firstChild = thumbnail.firstElementChild;
                if (firstChild && firstChild.tagName.toLowerCase() === 'img') {
                    wrapper.innerHTML = firstChild.outerHTML;
                } else if (firstChild) {
                    // For custom elements with shadow DOM, try to get the actual content
                    const shadowRoot = (firstChild as Element).shadowRoot;
                    if (shadowRoot) {
                        // Copy shadow root content
                        wrapper.innerHTML = shadowRoot.innerHTML;
                    } else {
                        // Fallback: clone the element
                        const clone = firstChild.cloneNode(true) as HTMLElement;
                        wrapper.appendChild(clone);
                    }
                }

                container.appendChild(wrapper);
            }
        });

        // Add badge if multiple items
        if (totalCount > 1) {
            const badge = document.createElement('div');
            badge.className = 'drag-image-badge';
            badge.textContent = String(totalCount);
            container.appendChild(badge);
        }

        document.body.appendChild(container);
        setTimeout(() => document.body.removeChild(container), 0);

        return container;
    }
}
