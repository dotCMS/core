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
    OnInit,
    OnDestroy,
    ViewChild,
    HostListener
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, FormsModule } from '@angular/forms';

import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SelectLazyLoadEvent, SelectModule, Select } from 'primeng/select';

import { DotContentTypeService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';

interface ParsedSelectLazyLoadEvent extends SelectLazyLoadEvent {
    itemsNeeded: number;
}

type ParsedLazyLoadResult = ParsedSelectLazyLoadEvent | null;

/**
 * Represents the state for the DotContentTypeComponent.
 */
interface DotContentTypeState {
    /**
     * The list of loaded content types (from lazy loading).
     */
    contentTypes: DotCMSContentType[];

    /**
     * The currently pinned option (shown at top of list).
     * This is the selected value that may not exist in the lazy-loaded pages yet.
     */
    pinnedOption: DotCMSContentType | null;

    /**
     * Indicates whether the content types are currently being loaded.
     */
    loading: boolean;

    /**
     * The total number of records available on the backend (0 if unknown or not loaded).
     */
    totalRecords: number;

    /**
     * The current filter value used to filter content types.
     */
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
export class DotContentTypeComponent implements ControlValueAccessor, OnInit, OnDestroy {
    private contentTypeService = inject(DotContentTypeService);

    @HostListener('focus')
    onHostFocus(): void {
        this.select?.focusInputViewChild?.nativeElement?.focus();
    }

    @ViewChild('select') select: Select | undefined;

    /**
     * Placeholder text to be shown in the select input when empty.
     */
    placeholder = input<string>('');

    /**
     * Whether the select is disabled.
     * Settable via component input.
     */
    disabled = input<boolean>(false);

    /**
     * Two-way model binding for the selected content type.
     * Accepts a string (content type variable), a DotCMSContentType object, or null if no content type is selected.
     * Used to drive the currently selected value in the dropdown.
     */
    value = model<string | DotCMSContentType | null>(null);

    /**
     * Disabled state from the ControlValueAccessor interface.
     * True when the form control disables this component.
     * @internal
     */
    $isDisabled = signal<boolean>(false);

    /**
     * Combined disabled state: true if either the input or ControlValueAccessor disabled state is true.
     * Used to control the actual disabled property of the select.
     */
    $disabled = computed(() => this.disabled() || this.$isDisabled());

    /**
     * Output event emitted whenever the selected content type changes.
     * Emits the content type variable or null.
     */
    onChange = output<string | null>();

    /**
     * Output event emitted when the select dropdown overlay is shown.
     * Can be used by parent components to respond to overlay display (e.g., for analytics, UI adjustments).
     */
    onShow = output<void>();

    /**
     * Output event emitted when the select dropdown overlay is hidden.
     * Allows parent components to react to overlay closure (e.g., cleaning up, focusing).
     */
    onHide = output<void>();

    /**
     * CSS class(es) applied to the select component wrapper.
     * Default is 'w-full'.
     */
    class = input<string>('w-full');

    /**
     * The HTML id attribute for the select input.
     * Can be customized; defaults to an empty string.
     */
    id = input<string>('');

    /**
     * Reactive state of the component including loaded types, loading status, total results, and active filter.
     * Readonly to prevent accidental re-assignment.
     */
    readonly $state = signalState<DotContentTypeState>({
        contentTypes: [],
        pinnedOption: null,
        loading: false,
        totalRecords: 0,
        filterValue: ''
    });

    /**
     * Computed options for the select dropdown.
     * Combines pinned option (if any) with lazy-loaded options.
     * The pinned option is always at the top and filtered out from the lazy-loaded list to avoid duplicates.
     */
    $options = computed(() => {
        const loaded = this.$state.contentTypes();
        const pinned = this.$state.pinnedOption();
        const filterValue = this.$state.filterValue().trim().toLowerCase();

        // No pinned option - just return loaded options
        if (!pinned) {
            return loaded;
        }

        // If filtering, only show pinned if it matches the filter
        if (filterValue) {
            const pinnedName = (pinned.name || '').toLowerCase();
            const pinnedVariable = (pinned.variable || '').toLowerCase();
            const matchesFilter = pinnedName.includes(filterValue) || pinnedVariable.includes(filterValue);

            if (!matchesFilter) {
                return loaded;
            }
        }

        // Filter out pinned from loaded to avoid duplicates, then prepend pinned
        const filtered = loaded.filter(ct => ct.variable !== pinned.variable);

        return [pinned, ...filtered];
    });

    /**
     * The number of items to load per page when fetching content types.
     * @private
     */
    private readonly pageSize = 40;

    /**
     * Set of page numbers that have already been loaded from the backend.
     * Used to prevent redundant page fetching.
     * @private
     */
    private loadedPages = new Set<number>();

    /**
     * Stores the timeout ID for the debounce when filtering content types.
     * Used to delay filter service calls until typing stabilizes.
     * Null when no debounce is pending.
     * @private
     */
    private filterDebounceTimeout: ReturnType<typeof setTimeout> | null = null;

    constructor() {
        // Sync model signal changes with ControlValueAccessor and fetch content type object to set state
        effect(() => {
            const variable = this.extractVariable(this.value());

            // Sync with ControlValueAccessor
            this.onChangeCallback(variable);

            if (!variable) {
                patchState(this.$state, { pinnedOption: null });
                return;
            }

            // Skip fetch if we already have this content type pinned
            const currentPinned = this.$state.pinnedOption();
            if (currentPinned?.variable === variable) {
                return;
            }

            patchState(this.$state, { loading: true });
            this.contentTypeService.getContentType(variable).subscribe({
                next: (contentType) => {
                    // Pin the initial value so it appears at the top of the list
                    patchState(this.$state, { pinnedOption: contentType, loading: false });

                    // Ensure it's in the contentTypes array
                    // This is especially important when the field is disabled
                    this.ensureContentTypeInList(contentType);
                },
                error: () => {
                    // If fetch fails, clear pinned option
                    patchState(this.$state, { pinnedOption: null, loading: false });
                }
            });
        });
    }

    // ControlValueAccessor callback functions
    private onChangeCallback = (_value: string | null) => {
        // Implementation provided by registerOnChange
    };

    private onTouchedCallback = () => {
        // Implementation provided by registerOnTouched
    };

    ngOnInit(): void {
        if (this.$state.contentTypes().length === 0) {
            this.onLazyLoad({ first: 0, last: this.pageSize - 1 });
        }
    }

    ngOnDestroy(): void {
        if (this.filterDebounceTimeout) {
            clearTimeout(this.filterDebounceTimeout);
            this.filterDebounceTimeout = null;
        }
    }

    /**
     * Handles the event when the selected content type changes.
     * - Updates the pinned option to the new selection
     * - Updates the model value with the selected content type variable.
     * - Calls the registered onTouched callback (for ControlValueAccessor interface).
     * - Emits the onChange output event to notify consumers of the new value.
     *
     * @param contentType The selected content type, or null if cleared
     */
    onContentTypeChange(contentType: DotCMSContentType | null): void {
        // Update pinned option to the new selection
        patchState(this.$state, { pinnedOption: contentType });

        const variable = contentType?.variable ?? null;
        this.value.set(variable);
        this.onTouchedCallback();
        this.onChange.emit(variable);
    }

    /**
     * Handles ngModelChange from PrimeNG Select
     * Extracts the variable from the ContentType object and stores it in the model
     *
     * @param contentType The ContentType object from PrimeNG Select, or null if cleared
     */
    onModelChange(contentType: DotCMSContentType | null): void {
        const variable = contentType?.variable ?? null;
        this.value.set(variable);
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

    /**
     * Handles the event when the select overlay is shown.
     * Initializes the virtual scroller to ensure options are displayed correctly.
     */
    onSelectShow(): void {
        this.onShow.emit();
        // Initialize virtual scroller state to fix display issue with custom filter template
        requestAnimationFrame(() => {
            if (this.select?.scroller) {
                this.select.scroller.setInitialState();
                this.select.scroller.viewInit();
            }
        });
    }

    /**
     * Handles the event when the select overlay is hidden.
     */
    onSelectHide(): void {
        this.onHide.emit();
    }

    // ControlValueAccessor implementation
    writeValue(value: string | DotCMSContentType | null): void {
        // Extract variable and set the value - the effect will handle fetching the content type and updating state
        const variable = this.extractVariable(value);
        this.value.set(variable);
    }

    registerOnChange(fn: (value: string | null) => void): void {
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
     * Extracts the variable from a value that can be a string, DotCMSContentType object, or null.
     * Handles backward compatibility with DotCMSContentType objects.
     *
     * @private
     * @param value The value to extract variable from (string, DotCMSContentType, or null)
     * @returns The variable string or null
     */
    private extractVariable(value: string | DotCMSContentType | null): string | null {
        if (!value) {
            return null;
        }

        if (typeof value === 'string') {
            return value;
        }

        // Validate that the object has a variable property
        if (!('variable' in value) || typeof value.variable !== 'string') {
            console.warn(
                `DotContentTypeComponent: Invalid content type object provided. Expected object with 'variable' property of type string, but received:`,
                value
            );
            return null;
        }

        return value.variable;
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
        // If we already have enough items to satisfy the request, no need to load
        if (currentCount >= itemsNeeded) {
            return false;
        }

        // If we know the total and we have all items, no need to load
        // But be cautious: if totalEntries equals currentCount and currentCount equals pageSize,
        // the API might be returning incorrect totalEntries (it might just be the current page count)
        // So only trust totalEntries if it's significantly larger than the page size
        if (totalEntries > 0 && currentCount >= totalEntries) {
            // If totalEntries is suspiciously equal to pageSize, don't trust it
            // Continue loading to see if there are more items
            if (totalEntries <= this.pageSize && currentCount === this.pageSize) {
                return true; // Keep loading, API might have wrong total
            }
            return false;
        }

        return true;
    }

    /**
     * Loads content types with pagination support
     * Converts PrimeNG's offset-based lazy loading to page-based API calls
     * Loads all missing pages from 1 up to the required page
     *
     * @private
     * @param parsed Parsed event values
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

        // If we know the total, check if we're requesting beyond it
        // But be cautious: if totalEntries equals pageSize, the API might be wrong
        if (totalEntries > 0 && totalEntries > this.pageSize) {
            const maxPage = Math.ceil(totalEntries / this.pageSize);
            if (pageToLoad > maxPage) {
                return;
            }
        }

        // Find all missing pages from 1 to pageToLoad
        const pagesToLoad: number[] = [];
        for (let page = 1; page <= pageToLoad; page++) {
            if (!this.loadedPages.has(page)) {
                pagesToLoad.push(page);
            }
        }

        // If all required pages are already loaded, return
        if (pagesToLoad.length === 0) {
            return;
        }

        // Load all missing pages sequentially
        this.loadPagesSequentially(pagesToLoad);
    }

    /**
     * Loads multiple pages sequentially
     *
     * @private
     * @param pages Array of page numbers to load
     */
    private loadPagesSequentially(pages: number[]): void {
        if (pages.length === 0) {
            return;
        }

        patchState(this.$state, { loading: true });
        const filter = this.$state.filterValue().trim();
        const pageToLoad = pages[0];
        const remainingPages = pages.slice(1);

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
                    if (isFirstPage || this.$state.totalRecords() === 0) {
                        if (pagination.totalEntries) {
                            patchState(this.$state, { totalRecords: pagination.totalEntries });
                        }
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

                    // If there are more pages to load, continue loading them
                    if (remainingPages.length > 0) {
                        this.loadPagesSequentially(remainingPages);
                    } else {
                        patchState(this.$state, { loading: false });

                        // Ensure current value is in the list only when not filtering
                        // When filtering, don't add selected value if it doesn't match the filter
                        // This allows "No results found" to display correctly
                        const currentValue = this.value();
                        if (currentValue && typeof currentValue === 'string' && !isFiltering) {
                            const currentContentTypes = this.$state.contentTypes();
                            const alreadyInList = currentContentTypes.some((ct) => ct.variable === currentValue);

                            // Only fetch if not already in the list to avoid duplicate API calls
                            // (the constructor effect also fetches when value changes)
                            if (!alreadyInList) {
                                patchState(this.$state, { loading: true });
                                this.contentTypeService.getContentType(currentValue).subscribe({
                                    next: (contentType) => {
                                        this.ensureContentTypeInList(contentType);
                                        patchState(this.$state, { loading: false });
                                    },
                                    error: () => {
                                        patchState(this.$state, { loading: false });
                                    }
                                });
                            }
                        }
                    }
                },
                error: () => {
                    patchState(this.$state, { loading: false });
                }
            });
    }
}
