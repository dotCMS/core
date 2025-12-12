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
    OnInit
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, FormsModule } from '@angular/forms';

import { SelectLazyLoadEvent, SelectModule } from 'primeng/select';

import { DotContentTypeService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';

interface ParsedSelectLazyLoadEvent extends SelectLazyLoadEvent {
    itemsNeeded: number;
}

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
export class DotWorkflowComponent implements ControlValueAccessor, OnInit {

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
    private loadedPages = new Set<number>();

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

    ngOnInit(): void {
        if (this.contentTypes().length === 0) {
            this.onLazyLoad({ first: 0, last: this.pageSize - 1 });
        }
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
     *
     * @private
     * @param contentType The content type to ensure is in the list
     */
    private ensureContentTypeInList(contentType: DotCMSContentType): void {
        const currentContentTypes = this.contentTypes();
        const exists = currentContentTypes.some((ct) => ct.variable === contentType.variable);

        if (!exists) {
            const updated = [...currentContentTypes, contentType];
            updated.sort((a, b) => (a.name || '').localeCompare(b.name || ''));
            this.contentTypes.set(updated);
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
     * Parses lazy load event and calculates items needed
     *
     * @private
     * @param event Lazy load event with first (offset) and last (last index)
     * @returns Object with parsed values and calculated itemsNeeded
     */
    private parseLazyLoadEvent(event: SelectLazyLoadEvent): ParsedSelectLazyLoadEvent {
        const first = Number(event?.first) || 0;
        // PrimeNG 21 uses 'last' instead of 'rows' - last is the last index (inclusive)
        const last = event?.last !== undefined ? Number(event.last) : undefined;

        // Calculate items needed: if 'last' is provided, use it; otherwise use page size
        const itemsNeeded = last !== undefined ? last + 1 : this.pageSize;

        return { first, last, itemsNeeded };
    }

    /**
     * Checks if we need to load more content types based on current state
     *
     * @private
     * @param itemsNeeded Total number of items needed
     * @param currentCount Current number of items loaded
     * @param totalEntries Total number of entries available (0 if unknown)
     * @returns true if we need to load more, false otherwise
     */
    private shouldLoadMore(itemsNeeded: number, currentCount: number, totalEntries: number): boolean {
        // If we already have all items, no need to load
        if (totalEntries > 0 && currentCount >= totalEntries) {
            return false;
        }

        // If we already have enough items, no need to load
        if (currentCount >= itemsNeeded) {
            return false;
        }

        return true;
    }

    /**
     * Handles lazy loading of content types from PrimeNG Select
     *
     * @param event Lazy load event with first (offset) and last (last index)
     */
    onLazyLoad(event: SelectLazyLoadEvent): void {
        const parsed = this.parseLazyLoadEvent(event);
        const currentCount = this.contentTypes().length;
        const totalEntries = this.totalRecords();

        if (!this.shouldLoadMore(parsed.itemsNeeded, currentCount, totalEntries)) {
            return;
        }

        this.loadContentTypesLazy(parsed, currentCount, totalEntries);
    }

    /**
     * Loads content types with pagination support
     * Converts PrimeNG's offset-based lazy loading to page-based API calls
     *
     * @private
     * @param parsed Parsed event values
     * @param currentCount Current number of items loaded
     * @param totalEntries Total number of entries available (0 if unknown)
     */
    private loadContentTypesLazy(
        parsed: ParsedSelectLazyLoadEvent,
        currentCount: number,
        totalEntries: number
    ): void {
        if (this.loading()) {
            return;
        }

        const { itemsNeeded, last } = parsed;

        // Calculate which page contains the last index we need
        // Page number is 1-indexed: offset 0-39 = page 1, 40-79 = page 2, etc.
        const lastIndexNeeded = last !== undefined ? last : itemsNeeded - 1;
        const pageToLoad = Math.floor(lastIndexNeeded / this.pageSize) + 1;

        // Check if we already loaded this page
        if (this.loadedPages.has(pageToLoad)) {
            return;
        }

        // If we know the total, check if we're requesting beyond it
        if (totalEntries > 0) {
            const maxPage = Math.ceil(totalEntries / this.pageSize);
            if (pageToLoad > maxPage) {
                return;
            }
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

                    // Filter out duplicates - variable is unique, so just check variable
                    const existingVariables = new Set(currentContentTypes.map((ct) => ct.variable));
                    const newContentTypes = contentTypes.filter((ct) => !existingVariables.has(ct.variable));

                    if (newContentTypes.length > 0) {
                        const merged = [...currentContentTypes, ...newContentTypes];
                        merged.sort((a, b) => (a.name || '').localeCompare(b.name || ''));
                        this.contentTypes.set(merged);
                    }

                    this.loadedPages.add(pageToLoad);
                    this.loading.set(false);

                    // Ensure current value is in the list
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
