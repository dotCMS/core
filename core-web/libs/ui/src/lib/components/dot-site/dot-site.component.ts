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

import { DotSiteService } from '@dotcms/data-access';
import { DotSite } from '@dotcms/dotcms-models';

interface ParsedSelectLazyLoadEvent extends SelectLazyLoadEvent {
    itemsNeeded: number;
}

type ParsedLazyLoadResult = ParsedSelectLazyLoadEvent | null;

/**
 * Represents the state for the DotSiteComponent.
 */
interface DotSiteState {
    /**
     * The list of loaded sites (from lazy loading).
     */
    sites: DotSite[];

    /**
     * The currently pinned option (shown at top of list).
     * This is the selected value that may not exist in the lazy-loaded pages yet.
     */
    pinnedOption: DotSite | null;

    /**
     * Indicates whether the sites are currently being loaded.
     */
    loading: boolean;

    /**
     * The total number of records available on the backend (0 if unknown or not loaded).
     */
    totalRecords: number;

    /**
     * The current filter value used to filter sites.
     */
    filterValue: string;
}

@Component({
    selector: 'dot-site',
    imports: [
        CommonModule,
        FormsModule,
        SelectModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule
    ],
    templateUrl: './dot-site.component.html',
    styles: [
        `
            :host {
                display: contents;
            }
        `
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotSiteComponent),
            multi: true
        }
    ]
})
export class DotSiteComponent implements ControlValueAccessor, OnInit, OnDestroy {
    private siteService = inject(DotSiteService);

    @HostListener('focus')
    onHostFocus(): void {
        this.select?.focusInputViewChild?.nativeElement?.focus();
    }

    @ViewChild('select') select: Select | undefined;

    /**
     * TODO: Remove this hardcoded value once the API endpoint /api/v1/site
     * returns the correct totalEntries in the pagination response.
     * Currently, the API returns totalEntries equal to the page size (e.g., 40)
     * instead of the actual total count (e.g., 501).
     * Update this value when the API is fixed to use pagination.totalEntries instead.
     */
    private readonly HARDCODED_TOTAL_ENTRIES = 501;

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
     * Two-way model binding for the selected site.
     * Accepts a string (site identifier), a DotSite object, or null if no site is selected.
     * Used to drive the currently selected value in the dropdown.
     */
    value = model<string | DotSite | null>(null);

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
     * Output event emitted whenever the selected site changes.
     * Emits the site identifier or null.
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
     * Reactive state of the component including loaded sites, loading status, total results, and active filter.
     * Readonly to prevent accidental re-assignment.
     */
    readonly $state = signalState<DotSiteState>({
        sites: [],
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
    $options = computed<DotSite[]>(() => {
        const loaded = this.$state.sites();
        const pinned = this.$state.pinnedOption();
        const filterValue = this.$state.filterValue().trim().toLowerCase();

        // No pinned option - just return loaded options
        if (!pinned) {
            return loaded;
        }

        // If filtering, only show pinned if it matches the filter
        if (filterValue) {
            const pinnedName = pinned.hostname.toLowerCase();
            const matchesFilter = pinnedName.includes(filterValue);

            if (!matchesFilter) {
                return loaded;
            }
        }

        // Filter out pinned from loaded to avoid duplicates, then prepend pinned
        const filtered = loaded.filter((s) => s.identifier !== pinned.identifier);

        return [pinned, ...filtered];
    });

    /**
     * The number of items to load per page when fetching sites.
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
     * Stores the timeout ID for the debounce when filtering sites.
     * Used to delay filter service calls until typing stabilizes.
     * Null when no debounce is pending.
     * @private
     */
    private filterDebounceTimeout: ReturnType<typeof setTimeout> | null = null;

    constructor() {
        // Sync model signal changes with ControlValueAccessor and fetch site object to set state
        effect(() => {
            const identifier = this.extractIdentifier(this.value());

            this.onChangeCallback(identifier);

            if (!identifier) {
                patchState(this.$state, { pinnedOption: null });
                return;
            }

            const currentPinned = this.$state.pinnedOption();
            if (currentPinned?.identifier === identifier) {
                return;
            }

            patchState(this.$state, { loading: true });
            this.siteService.getSiteById(identifier).subscribe({
                next: (site) => {
                    patchState(this.$state, { pinnedOption: site, loading: false });
                },
                error: () => {
                    patchState(this.$state, { pinnedOption: null, loading: false });
                }
            });
        });
    }

    ngOnInit(): void {
        if (this.$state.sites().length === 0) {
            this.onLazyLoad({ first: 0, last: this.pageSize - 1 });
        }
    }

    ngOnDestroy(): void {
        if (this.filterDebounceTimeout) {
            clearTimeout(this.filterDebounceTimeout);
            this.filterDebounceTimeout = null;
        }
    }

    // ControlValueAccessor callback functions
    private onChangeCallback = (_value: string | null) => {
        // Implementation provided by registerOnChange
    };

    private onTouchedCallback = () => {
        // Implementation provided by registerOnTouched
    };

    /**
     * Handles the event when the selected site changes.
     * - Updates the pinned option to the new selection
     * - Updates the model value with the selected site identifier.
     * - Calls the registered onTouched callback (for ControlValueAccessor interface).
     * - Emits the onChange output event to notify consumers of the new value.
     *
     * @param site The selected site, or null if cleared
     */
    onSiteChange(site: DotSite | null): void {
        // Update pinned option to the new selection
        patchState(this.$state, { pinnedOption: site });

        const identifier = site?.identifier ?? null;
        this.value.set(identifier);
        this.onTouchedCallback();
        this.onChange.emit(identifier);
    }

    /**
     * Handles lazy loading of sites from PrimeNG Select
     *
     * @param event Lazy load event with first (offset) and last (last index)
     */
    onLazyLoad(event: SelectLazyLoadEvent): void {
        const parsed = this.parseLazyLoadEvent(event);

        // Skip if event is invalid (contains NaN values from PrimeNG Scroller initialization)
        if (!parsed) {
            return;
        }

        const currentCount = this.$state.sites().length;
        const totalEntries = this.$state.totalRecords();

        if (!this.shouldLoadMore(parsed.itemsNeeded, currentCount, totalEntries)) {
            return;
        }

        this.loadSitesLazy(parsed, totalEntries);
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
                sites: [],
                totalRecords: 0
            });

            // Load first page with the new filter
            this.loadSitesLazy(
                { first: 0, last: this.pageSize - 1, itemsNeeded: this.pageSize },
                0
            );

            this.filterDebounceTimeout = null;
        }, 300);
    }

    /**
     * Resets the filter and reloads all sites
     */
    resetFilter(): void {
        if (this.filterDebounceTimeout) {
            clearTimeout(this.filterDebounceTimeout);
            this.filterDebounceTimeout = null;
        }

        patchState(this.$state, {
            filterValue: '',
            sites: [],
            totalRecords: 0
        });
        this.loadedPages.clear();

        this.loadSitesLazy({ first: 0, last: this.pageSize - 1, itemsNeeded: this.pageSize }, 0);
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
    writeValue(value: string | DotSite | null): void {
        // Extract identifier and set the value - the effect will handle fetching the site and updating state
        const identifier = this.extractIdentifier(value);
        this.value.set(identifier);
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
     * Sets sites with automatic sorting by name
     *
     * @private
     * @param sites The sites to set
     */
    private setSites(sites: DotSite[]): void {
        const sorted = [...sites].sort((a, b) =>
            (a.hostname || '').localeCompare(b.hostname || '')
        );
        patchState(this.$state, { sites: sorted });
    }

    /**
     * Extracts the identifier from a value that can be a string, DotSite object, or null.
     * Handles backward compatibility with DotSite objects.
     *
     * @private
     * @param value The value to extract identifier from (string, DotSite, or null)
     * @returns The identifier string or null
     */
    private extractIdentifier(value: string | DotSite | null): string | null {
        if (!value) {
            return null;
        }

        if (typeof value === 'string') {
            return value;
        }

        // Validate that the object has an identifier property
        if (!('identifier' in value) || typeof value.identifier !== 'string') {
            console.warn(
                `DotSiteComponent: Invalid site object provided. Expected object with 'identifier' property of type string, but received:`,
                value
            );
            return null;
        }

        return value.identifier;
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
     * Checks if we need to load more sites based on current state
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
     * Loads sites with pagination support
     * Converts PrimeNG's offset-based lazy loading to page-based API calls
     * Loads all missing pages from 1 up to the required page
     *
     * @private
     * @param parsed Parsed event values
     * @param totalEntries Total number of entries available (0 if unknown)
     */
    private loadSitesLazy(parsed: ParsedSelectLazyLoadEvent, totalEntries: number): void {
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

        this.siteService
            .getSites({
                page: pageToLoad,
                per_page: this.pageSize,
                ...(filter ? { filter } : {})
            })
            .subscribe({
                next: ({ sites }) => {
                    const currentSites = this.$state.sites();
                    const isFirstPage = pageToLoad === 1;

                    // Update total records from pagination
                    // TODO: When API is fixed, replace HARDCODED_TOTAL_ENTRIES with pagination.totalEntries
                    if (isFirstPage || this.$state.totalRecords() === 0) {
                        patchState(this.$state, { totalRecords: this.HARDCODED_TOTAL_ENTRIES });
                    }

                    // Replace on first page (initial load or filter change clears list first)
                    // Merge on subsequent pages (lazy loading pagination)
                    if (isFirstPage) {
                        this.setSites(sites);
                    } else {
                        // For subsequent pages, merge with existing (lazy loading)
                        const existingIdentifiers = new Set(currentSites.map((s) => s.identifier));
                        const newSites = sites.filter(
                            (s) => !existingIdentifiers.has(s.identifier)
                        );

                        if (newSites.length > 0) {
                            this.setSites([...currentSites, ...newSites]);
                        }
                    }

                    this.loadedPages.add(pageToLoad);

                    if (remainingPages.length > 0) {
                        this.loadPagesSequentially(remainingPages);
                    } else {
                        patchState(this.$state, { loading: false });
                    }
                },
                error: () => {
                    patchState(this.$state, { loading: false });
                }
            });
    }
}
