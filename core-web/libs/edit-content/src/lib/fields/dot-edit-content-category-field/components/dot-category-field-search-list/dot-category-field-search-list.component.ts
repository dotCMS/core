import { CommonModule } from '@angular/common';
import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    effect,
    ElementRef,
    EventEmitter,
    inject,
    input,
    Output,
    Renderer2,
    signal,
    ViewChild
} from '@angular/core';

import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';

import { DotCategory } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import {
    DotCategoryFieldKeyValueObj,
    DotTableHeaderCheckboxSelectEvent,
    DotTableRowSelectEvent
} from '../../models/dot-category-field.models';
import { getParentPath } from '../../utils/category-field.utils';
import { DotTableSkeletonComponent } from '../dot-table-skeleton/dot-table-skeleton.component';

@Component({
    selector: 'dot-category-field-search-list',
    standalone: true,
    imports: [CommonModule, TableModule, SkeletonModule, DotTableSkeletonComponent, DotMessagePipe],
    templateUrl: './dot-category-field-search-list.component.html',
    styleUrl: './dot-category-field-search-list.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCategoryFieldSearchListComponent implements AfterViewInit {
    /**
     * Represents a reference to a table container element in the DOM to calculate the
     * viewport to use in the virtual scroll
     */
    @ViewChild('tableContainer', { static: false }) tableContainer!: ElementRef;
    /**
     * The scrollHeight variable represents a signal with a default value of '0px'.
     * It can be used to track and manipulate the height of a scrollable element.
     */
    $scrollHeight = signal<string>('0px');
    /**
     * Represents the categories found with the filter
     */
    categories = input.required<DotCategory[]>();
    /**
     * Represent the selected items in the store
     */
    selected = input.required<DotCategoryFieldKeyValueObj[]>();
    /**
     * EventEmitter for emit the selected category(ies).
     */
    @Output() itemChecked = new EventEmitter<
        DotCategoryFieldKeyValueObj | DotCategoryFieldKeyValueObj[]
    >();
    /**
     * EventEmitter that emits events to remove a selected item(s).
     */
    @Output() removeItem = new EventEmitter<string | string[]>();
    /**
     * Represents a variable indicating if the component is in loading state.
     */
    isLoading = input.required<boolean>();
    /**
     * Computed variable to store the search results parsed.
     *
     */
    $searchResults = computed<DotCategoryFieldKeyValueObj[]>(() => {
        return this.categories().map((item) => {
            const path = getParentPath(item);

            return { key: item.key, value: item.categoryName, path: path, inode: item.inode };
        });
    });
    /**
     * Model of the items selected
     */
    itemsSelected: DotCategoryFieldKeyValueObj[];
    /**
     * Represents an array of temporary selected items.
     */
    temporarySelectedAll: string[] = [];
    #renderer = inject(Renderer2);
    #destroyRef = inject(DestroyRef);

    readonly #effectRef = effect(() => {
        // Todo: find a better way to update this
        this.itemsSelected = this.selected();
    });

    /**
     * This method is called when an item is selected.
     *
     * @param {$event: DotTableRowSelectEvent<DotCategoryFieldKeyValueObj>} $event - The event object containing the selected item data.
     * @return {void}
     */
    onSelectItem({ data }: DotTableRowSelectEvent<DotCategoryFieldKeyValueObj>): void {
        this.itemChecked.emit(data);
    }

    /**
     * Removes an item from the list.
     *
     * @param {DotTableRowSelectEvent<DotCategoryFieldKeyValueObj>} $event - The event that triggered the item removal.
     * @return {void}
     */
    onRemoveItem({ data: { key } }: DotTableRowSelectEvent<DotCategoryFieldKeyValueObj>): void {
        this.removeItem.emit(key);
    }

    /**
     * Handles the event when the header checkbox is toggled.
     *
     * @param {DotTableHeaderCheckboxSelectEvent} event - The event triggered when the header checkbox is toggled.
     *
     * @return {void}
     */
    onHeaderCheckboxToggle({ checked }: DotTableHeaderCheckboxSelectEvent): void {
        if (checked) {
            const values = this.$searchResults().map((item) => item.key);
            this.itemChecked.emit(this.$searchResults());
            this.temporarySelectedAll = [...values];
        } else {
            this.removeItem.emit(this.temporarySelectedAll);
            this.temporarySelectedAll = [];
        }
    }

    ngAfterViewInit(): void {
        this.setTableScrollHeight();
        this.#renderer.listen('window', 'resize', this.setTableScrollHeight.bind(this));

        this.#destroyRef.onDestroy(() => {
            this.#renderer.listen('window', 'resize', null);
            this.#effectRef.destroy();
        });
    }

    /**
     * Calculate the high of the container for the virtual scroll
     * @private
     */
    private setTableScrollHeight() {
        if (this.tableContainer) {
            const containerHeight = this.tableContainer.nativeElement.clientHeight;
            this.$scrollHeight.set(`${containerHeight}px`);
        }
    }
}
