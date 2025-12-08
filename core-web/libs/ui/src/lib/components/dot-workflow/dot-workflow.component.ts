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
    computed
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, FormsModule } from '@angular/forms';

import { SelectModule } from 'primeng/select';

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
    private readonly pageSize = 40;

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
     * Handles dropdown show event to ensure initial data is loaded
     */
    onDropdownShow(): void {
        // Load first page if no items are loaded
        if (this.contentTypes().length === 0) {
            this.onLazyLoad({ first: 0, rows: this.pageSize });
        }
    }

    /**
     * Handles lazy loading of content types from PrimeNG Select
     *
     * @param event Lazy load event with first (offset) and either rows (page size) or last (last index)
     */
    onLazyLoad(event: { first: number; rows?: number; last?: number }): void {
        const first = Number(event?.first) || 0;
        // PrimeNG 21 uses 'last' instead of 'rows' - last is the last index (inclusive)
        const last = event?.last !== undefined ? Number(event.last) : undefined;
        const rows = event?.rows !== undefined ? Number(event.rows) : undefined;

        // Calculate items needed: if 'last' is provided, use it; otherwise use 'rows'
        const itemsNeeded = last !== undefined ? last + 1 : (rows !== undefined ? first + rows : this.pageSize);

        const currentCount = this.contentTypes().length;
        const totalEntries = this.totalRecords();

        // If we already have all items, no need to load
        if (totalEntries > 0 && currentCount >= totalEntries) {
            return;
        }

        // If we already have enough items, no need to load
        if (currentCount >= itemsNeeded) {
            return;
        }

        this.loadContentTypesLazy(event);
    }

    /**
     * Loads content types with pagination support
     * Converts PrimeNG's offset-based lazy loading to page-based API calls
     *
     * @private
     * @param event Lazy load event with first (offset) and either rows (page size) or last (last index)
     */
    private loadContentTypesLazy(event: { first: number; rows?: number; last?: number }): void {
        if (this.loading()) {
            return;
        }

        const first = Number(event?.first) || 0;
        const last = event?.last !== undefined ? Number(event.last) : undefined;
        const rows = event?.rows !== undefined ? Number(event.rows) : undefined;

        // Calculate how many items we need total
        const itemsNeeded = last !== undefined ? last + 1 : (rows !== undefined ? first + rows : this.pageSize);

        const currentCount = this.contentTypes().length;
        const totalEntries = this.totalRecords();

        // If we already have enough items for the requested range, return
        if (currentCount >= itemsNeeded) {
            return;
        }

        // If we know the total and we have all items, return
        if (totalEntries > 0 && currentCount >= totalEntries) {
            return;
        }

        // Calculate which page contains the last index we need
        // Page number is 1-indexed: offset 0-39 = page 1, 40-79 = page 2, etc.
        const lastIndexNeeded = last !== undefined ? last : (itemsNeeded - 1);
        const pageToLoad = Math.floor(lastIndexNeeded / this.pageSize) + 1;

        // If we know the total, check if we're requesting beyond it
        if (totalEntries > 0) {
            const maxPage = Math.ceil(totalEntries / this.pageSize);
            if (pageToLoad > maxPage) {
                return; // Already have all pages
            }
        }

        // Check if we already have this page loaded
        // Page N contains items from index (N-1)*pageSize to N*pageSize - 1
        const pageEndIndex = pageToLoad * this.pageSize;

        // If we already have enough items to cover this page, return
        if (currentCount >= pageEndIndex) {
            return;
        }

        this.loading.set(true);
        this.contentTypeService
            .getContentTypesWithPagination({
                page: pageToLoad,
                per_page: this.pageSize
            })
            .subscribe({
                next: ({ contentTypes, pagination }) => {
                    const currentContentTypes = this.contentTypes();

                    // Update total records from pagination
                    if (pagination.totalEntries) {
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
                        merged.sort((a, b) => (a.name || '').localeCompare(b.name || ''));
                        this.contentTypes.set(merged);
                    }

                    this.loading.set(false);

                    // After loading, ensure current value is in the list
                    const currentValue = this.value();
                    if (currentValue) {
                        this.ensureContentTypeInList(currentValue);
                    }
                },
                error: () => {
                    this.loading.set(false);
                }
            });
    }
}
