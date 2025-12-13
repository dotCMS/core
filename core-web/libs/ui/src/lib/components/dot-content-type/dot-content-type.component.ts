import { signalState, patchState } from '@ngrx/signals';

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

import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SelectLazyLoadEvent, SelectModule } from 'primeng/select';

import { DotContentTypeService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';

interface ParsedSelectLazyLoadEvent extends SelectLazyLoadEvent {
    itemsNeeded: number;
}

type ParsedLazyLoadResult = ParsedSelectLazyLoadEvent | null;

interface DotContentTypeState {
    contentTypes: DotCMSContentType[];
    loading: boolean;
    totalRecords: number;
    filterValue: string;
}

@Component({
    selector: 'dot-content-type',
    imports: [
        CommonModule,
        FormsModule,
        SelectModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule
    ],
    templateUrl: './dot-content-type.component.html',
    styleUrl: './dot-content-type.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotContentTypeComponent),
            multi: true
        }
    ]
})
export class DotContentTypeComponent implements ControlValueAccessor, OnInit {
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

    readonly $state = signalState<DotContentTypeState>({
        contentTypes: [],
        loading: false,
        totalRecords: 0,
        filterValue: ''
    });

    private readonly pageSize = 40;
    private loadedPages = new Set<number>();
    private filterDebounceTimeout: ReturnType<typeof setTimeout> | null = null;

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
        if (this.$state.contentTypes().length === 0) {
            this.onLazyLoad({ first: 0, last: this.pageSize - 1 });
        }
    }

    /**
     * Handles the event when the selected content type changes.
     * - Updates the model value with the selected content type.
     * - Calls the registered onTouched callback (for ControlValueAccessor interface).
     * - Emits the onChange output event to notify consumers of the new value.
     *
     * @param contentType The selected content type, or null if cleared
     */
    onContentTypeChange(contentType: DotCMSContentType | null): void {
        this.value.set(contentType);
        this.onTouchedCallback();
        this.onChange.emit(contentType);
    }

    /**
     * Handles lazy loading of content types from PrimeNG Select
     *
     * @param event Lazy load event with first (offset) and last (last index)
     */
    onLazyLoad(event: SelectLazyLoadEvent): void {
        const parsed = this.parseLazyLoadEvent(event);

        // Skip if event is invalid (contains NaN values from PrimeNG Scroller initialization)
        if (!parsed) {
            return;
        }

        const currentCount = this.$state.contentTypes().length;
        const totalEntries = this.$state.totalRecords();

        if (!this.shouldLoadMore(parsed.itemsNeeded, currentCount, totalEntries)) {
            return;
        }

        this.loadContentTypesLazy(parsed, totalEntries);
    }

    /**
     * Handles filter changes from the custom filter template
     * Resets loaded data and loads the first page with the new filter
     * Uses debouncing to avoid excessive API calls while typing
     *
     * @param filter The filter text value
     */
    onFilterChange(filter: string): void {
        if (!filter || filter.trim() === '') {
            this.resetFilter();
            return;
        }

        if (this.filterDebounceTimeout) {
            clearTimeout(this.filterDebounceTimeout);
        }

        patchState(this.$state, { filterValue: filter });

        this.filterDebounceTimeout = setTimeout(() => {
            this.loadedPages.clear();
            patchState(this.$state, {
                contentTypes: [],
                totalRecords: 0
            });

            // Load first page with the new filter
            this.loadContentTypesLazy(
                { first: 0, last: this.pageSize - 1, itemsNeeded: this.pageSize },
                0
            );

            this.filterDebounceTimeout = null;
        }, 300);
    }

    /**
     * Resets the filter and reloads all content types
     */
    resetFilter(): void {
        if (this.filterDebounceTimeout) {
            clearTimeout(this.filterDebounceTimeout);
            this.filterDebounceTimeout = null;
        }

        patchState(this.$state, {
            filterValue: '',
            contentTypes: [],
            totalRecords: 0
        });
        this.loadedPages.clear();

        this.loadContentTypesLazy(
            { first: 0, last: this.pageSize - 1, itemsNeeded: this.pageSize },
            0
        );
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
     * Sets content types with automatic sorting by name
     *
     * @private
     * @param contentTypes The content types to set
     */
    private setContentTypes(contentTypes: DotCMSContentType[]): void {
        const sorted = [...contentTypes].sort((a, b) => (a.name || '').localeCompare(b.name || ''));
        patchState(this.$state, { contentTypes: sorted });
    }

    /**
     * Ensures the given content type is in the contentTypes list.
     *
     * @private
     * @param contentType The content type to ensure is in the list
     */
    private ensureContentTypeInList(contentType: DotCMSContentType): void {
        const currentContentTypes = this.$state.contentTypes();
        const exists = currentContentTypes.some((ct) => ct.variable === contentType.variable);

        if (!exists) {
            this.setContentTypes([...currentContentTypes, contentType]);
        }
    }

    /**
     * Parses and validates lazy load event, calculates items needed.
     * Returns null if event contains invalid values (NaN) from PrimeNG Scroller initialization.
     *
     * @private
     * @param event Lazy load event with first (offset) and last (last index)
     * @returns Parsed event object with calculated itemsNeeded, or null if event is invalid
     */
    private parseLazyLoadEvent(event: SelectLazyLoadEvent): ParsedLazyLoadResult {
        // Validate event: PrimeNG Scroller may emit events with NaN values during initialization
        // when calculateOptions() runs before items are loaded. This happens because it calculates
        // lazyLoadState.last as: Math.min(step, this._items.length). If this._items is null/undefined,
        // this results in Math.min(40, undefined) = NaN.
        if (
            event?.first === undefined ||
            isNaN(Number(event.first)) ||
            (event?.last !== undefined && isNaN(Number(event.last)))
        ) {
            return null;
        }

        const first = Number(event.first);

        // PrimeNG 21 uses 'last' instead of 'rows' - last is the last index (inclusive)
        const last = event.last !== undefined ? Number(event.last) : undefined;

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
    private shouldLoadMore(
        itemsNeeded: number,
        currentCount: number,
        totalEntries: number
    ): boolean {
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
     * Loads content types with pagination support
     * Converts PrimeNG's offset-based lazy loading to page-based API calls
     *
     * @private
     * @param parsed Parsed event values
     * @param currentCount Current number of items loaded
     * @param totalEntries Total number of entries available (0 if unknown)
     */
    private loadContentTypesLazy(parsed: ParsedSelectLazyLoadEvent, totalEntries: number): void {
        if (this.$state.loading()) {
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

        patchState(this.$state, { loading: true });
        const filter = this.$state.filterValue().trim();
        this.contentTypeService
            .getContentTypesWithPagination({
                page: pageToLoad,
                per_page: this.pageSize,
                ...(filter ? { filter } : {})
            })
            .subscribe({
                next: ({ contentTypes, pagination }) => {
                    const currentContentTypes = this.$state.contentTypes();
                    const isFiltering = filter.length > 0;
                    const isFirstPage = pageToLoad === 1;

                    // Update total records from pagination
                    if (pagination.totalEntries) {
                        patchState(this.$state, { totalRecords: pagination.totalEntries });
                    }

                    // Replace on first page (initial load or filter change clears list first)
                    // Merge on subsequent pages (lazy loading pagination)
                    if (isFirstPage) {
                        this.setContentTypes(contentTypes);
                    } else {
                        // For subsequent pages, merge with existing (lazy loading)
                        const existingVariables = new Set(currentContentTypes.map((ct) => ct.variable));
                        const newContentTypes = contentTypes.filter(
                            (ct) => !existingVariables.has(ct.variable)
                        );

                        if (newContentTypes.length > 0) {
                            this.setContentTypes([...currentContentTypes, ...newContentTypes]);
                        }
                    }

                    this.loadedPages.add(pageToLoad);
                    patchState(this.$state, { loading: false });

                    // Ensure current value is in the list only when not filtering
                    // When filtering, don't add selected value if it doesn't match the filter
                    // This allows "No results found" to display correctly
                    const currentValue = this.value();
                    if (currentValue && !isFiltering) {
                        this.ensureContentTypeInList(currentValue);
                    }
                },
                error: () => {
                    patchState(this.$state, { loading: false });
                }
            });
    }
}
