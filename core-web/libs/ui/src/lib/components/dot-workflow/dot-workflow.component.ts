import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    model,
    output,
    input,
    inject,
    signal,
    effect,
    forwardRef,
    computed,
    ViewChild,
    ChangeDetectorRef
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, FormsModule } from '@angular/forms';

import { SelectModule, Select } from 'primeng/select';

import { DotContentTypeService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-workflow',
    imports: [CommonModule, FormsModule, SelectModule],
    templateUrl: './dot-workflow.component.html',
    styleUrl: './dot-workflow.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotWorkflowComponent),
            multi: true
        }
    ]
})
export class DotWorkflowComponent implements ControlValueAccessor {
    private contentTypeService = inject(DotContentTypeService);
    private cdr = inject(ChangeDetectorRef);

    @ViewChild('selectRef') selectComponent?: Select;

    placeholder = input<string>('');
    disabled = input<boolean>(false);
    value = model<DotCMSContentType | null>(null);

    // ControlValueAccessor disabled state (can be set by form control)
    $isDisabled = signal<boolean>(false);

    // Combined disabled state (input disabled OR form control disabled)
    $disabled = computed(() => this.disabled() || this.$isDisabled());

    // Custom output for explicit change events
    onChange = output<DotCMSContentType | null>();

    contentTypes = signal<DotCMSContentType[]>([]);
    loading = signal<boolean>(false);
    totalRecords = signal<number>(0);
    private loadedPageSize = signal<number>(0);
    private readonly initialPageSize = 40; // Load 40 items initially (covers 90% of customers)
    private lastLazyLoadRequest = 0; // Track last request to prevent rapid calls

    // ControlValueAccessor callback functions
    private onChangeCallback = (_value: DotCMSContentType | null) => {
        // Implementation provided by registerOnChange
    };

    private onTouchedCallback = () => {
        // Implementation provided by registerOnTouched
    };

    constructor() {
        // Sync model signal changes with ControlValueAccessor
        effect(() => {
            const currentValue = this.value();
            this.onChangeCallback(currentValue);
        });
    }

    onContentTypeChange(contentType: DotCMSContentType | null): void {
        this.value.set(contentType);
        this.onTouchedCallback();
        this.onChange.emit(contentType);
    }

    // ControlValueAccessor implementation
    writeValue(value: DotCMSContentType | null): void {
        this.value.set(value);

        // If we have a value, ensure it's in the contentTypes array
        // This is especially important when the field is disabled
        if (value) {
            this.ensureContentTypeInList(value);
        }
    }

    /**
     * Ensures the given content type is in the contentTypes list.
     * Since writeValue receives the full DotCMSContentType object, we can just add it directly.
     *
     * @private
     * @param contentType The content type to ensure is in the list
     */
    private ensureContentTypeInList(contentType: DotCMSContentType): void {
        const currentContentTypes = this.contentTypes();
        const exists = currentContentTypes.some(
            (ct) => ct.id === contentType.id || ct.variable === contentType.variable
        );

        if (!exists) {
            // We have the full object, so just add it directly to the list
            this.contentTypes.set([...currentContentTypes, contentType]);
        }
    }

    registerOnChange(fn: (value: DotCMSContentType | null) => void): void {
        this.onChangeCallback = fn;
    }

    registerOnTouched(fn: () => void): void {
        this.onTouchedCallback = fn;
    }

    setDisabledState(isDisabled: boolean): void {
        this.$isDisabled.set(isDisabled);
    }

    /**
     * Handles dropdown show event to ensure virtual scroll is properly initialized
     * When the dropdown opens, we need to ensure the first page is loaded and
     * trigger a virtual scroll refresh to fix the empty list issue
     */
    onDropdownShow(): void {
        const currentItems = this.contentTypes();

        // If we have no items loaded, load the first page
        if (currentItems.length === 0) {
            this.onLazyLoad({ first: 0, rows: this.initialPageSize });
        } else {
            // Reset the scroller's viewport to the top when reopening
            // This fixes the issue where items appear empty until scrolling after reopening
            // We use a longer timeout to ensure the overlay and scroller are fully initialized
            setTimeout(() => {
                // Access the scroller through the Select component and reset to index 0
                // This forces the virtual scroll to recalculate its viewport from the top
                if (this.selectComponent?.scroller) {
                    this.selectComponent.scroller.scrollToIndex(0, 'auto');
                }

                // Force virtual scroll to recalculate by updating the options array reference
                // This triggers PrimeNG's change detection to refresh the virtual scroll viewport
                this.contentTypes.set([...currentItems]);

                // Trigger change detection to ensure the view updates
                this.cdr.markForCheck();
            }, 50);
        }
    }

    /**
     * Handles lazy loading of content types from PrimeNG Select
     * Only loads what's needed for the current viewport, not all items at once
     *
     * @param event Lazy load event with first (offset) and rows (page size)
     */
    onLazyLoad(event: { first: number; rows: number }): void {
        // Validate event parameters
        const first = Number(event?.first) || 0;
        const rows = Number(event?.rows) || 40;
        const currentCount = this.contentTypes().length;
        const totalEntries = this.totalRecords();

        // Calculate what we need for the current viewport
        const itemsNeeded = first + rows * 2; // 2x viewport buffer for smooth scrolling

        // If we already have all items, no need to load
        if (totalEntries > 0 && currentCount >= totalEntries) {
            return;
        }

        // If we already have enough items for the current viewport, don't load more
        if (currentCount >= itemsNeeded) {
            return;
        }

        // Prevent rapid repeated calls (debounce)
        const now = Date.now();
        if (now - this.lastLazyLoadRequest < 100) {
            return; // Ignore calls within 100ms of each other
        }
        this.lastLazyLoadRequest = now;

        // Only load if we actually need more items for the viewport
        this.loadContentTypesLazy(event);
    }

    /**
     * Loads content types with pagination support
     * Uses proper pagination with page numbers and per_page.
     * Always requests 40 items per page consistently.
     *
     * @private
     * @param event Lazy load event with first (offset) and rows (page size)
     */
    private loadContentTypesLazy(event: { first: number; rows: number }): void {
        if (this.loading()) {
            return;
        }

        // Validate event parameters
        const first = Number(event?.first) || 0;
        const rows = Number(event?.rows) || 40;

        const currentCount = this.contentTypes().length;
        const totalEntries = this.totalRecords();

        // Calculate what we need: current viewport position + buffer
        const itemsNeeded = first + rows * 2; // 2x viewport buffer

        // Calculate which page we need based on current offset
        // Page number is 1-indexed: offset 0-39 = page 1, 40-79 = page 2, etc.
        const currentPage = Math.floor(first / this.initialPageSize) + 1;
        const lastPageNeeded = Math.floor(itemsNeeded / this.initialPageSize) + 1;

        // Find the first missing page starting from current page
        let pageToLoad = currentPage;
        for (let page = currentPage; page <= lastPageNeeded; page++) {
            const pageStartIndex = (page - 1) * this.initialPageSize;
            const pageEndIndex = pageStartIndex + this.initialPageSize;

            // Check if we have this page loaded
            if (currentCount < pageEndIndex) {
                pageToLoad = page;
                break;
            }
        }

        // If we know the total, check if we're requesting beyond it
        if (totalEntries > 0) {
            const maxPage = Math.ceil(totalEntries / this.initialPageSize);
            if (pageToLoad > maxPage) {
                return; // Already have all pages
            }
        }

        // Check if we already have this page loaded
        const pageStartIndex = (pageToLoad - 1) * this.initialPageSize;
        const pageEndIndex = pageStartIndex + this.initialPageSize;
        if (currentCount >= pageEndIndex) {
            return; // We already have this page
        }

        this.loading.set(true);
        this.contentTypeService
            .getContentTypesWithPagination({
                page: pageToLoad, // Page number (1-indexed)
                per_page: this.initialPageSize // Always 40 items per page
            })
            .subscribe({
                next: ({ contentTypes, pagination }) => {
                    const currentContentTypes = this.contentTypes();

                    // Update total records from pagination (only set once)
                    if (this.totalRecords() === 0 && pagination.totalEntries) {
                        this.totalRecords.set(pagination.totalEntries);
                    }

                    // Append new content types, avoiding duplicates
                    const existingIds = new Set(
                        currentContentTypes.map((ct) => ct.id || ct.variable)
                    );
                    const newContentTypes = contentTypes.filter(
                        (ct) => !existingIds.has(ct.id || ct.variable)
                    );

                    if (newContentTypes.length > 0) {
                        // Merge and sort to maintain order
                        const merged = [...currentContentTypes, ...newContentTypes];
                        // Sort by name to maintain consistent order
                        merged.sort((a, b) => (a.name || '').localeCompare(b.name || ''));
                        this.contentTypes.set(merged);
                        this.loadedPageSize.set(merged.length);
                    }

                    this.loading.set(false);

                    // After loading, ensure current value is in the list
                    const currentValue = this.value();
                    if (currentValue) {
                        this.ensureContentTypeInList(currentValue);
                    }

                    // If we still need more pages for the viewport, load the next page
                    const updatedCount = this.contentTypes().length;
                    const stillNeeded = first + rows * 2;
                    if (updatedCount < stillNeeded &&
                        (totalEntries === 0 || updatedCount < totalEntries)) {
                        // Load the next page if needed
                        setTimeout(() => {
                            this.loadContentTypesLazy(event);
                        }, 0);
                    }
                },
                error: () => {
                    this.loading.set(false);
                }
            });
    }
}
