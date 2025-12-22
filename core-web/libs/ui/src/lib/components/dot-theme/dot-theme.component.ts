import { signalState, patchState } from '@ngrx/signals';
import { Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    output,
    input,
    inject,
    signal,
    effect,
    forwardRef,
    computed,
    OnDestroy,
    ViewChild,
    contentChild,
    TemplateRef,
    DestroyRef
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, FormsModule } from '@angular/forms';

import { LazyLoadEvent } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DataView, DataViewModule } from 'primeng/dataview';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { PopoverModule } from 'primeng/popover';
import { RadioButtonModule } from 'primeng/radiobutton';

import { switchMap } from 'rxjs/operators';

import { DotThemesService } from '@dotcms/data-access';
import { DotTheme, DotPagination } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { DotSiteComponent } from '../dot-site/dot-site.component';

interface DotThemeState {
    themes: DotTheme[];
    loading: boolean;
    error: string | null;
    pagination: DotPagination | null;
    selectedTheme: DotTheme | null;
    hostId: string | null;
    searchValue: string;
}

@Component({
    selector: 'dot-theme',
    imports: [
        CommonModule,
        FormsModule,
        DataViewModule,
        RadioButtonModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        DotSiteComponent,
        ButtonModule,
        PopoverModule,
        CardModule
    ],
    host: {
        class: 'block'
    },
    templateUrl: './dot-theme.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotThemeComponent),
            multi: true
        }
    ]
})
export class DotThemeComponent implements ControlValueAccessor, OnDestroy {
    private readonly themesService = inject(DotThemesService);
    private readonly globalStore = inject(GlobalStore);
    private readonly destroyRef = inject(DestroyRef);

    @ViewChild('dataView') dataView: DataView | undefined;

    /**
     * Placeholder text to be shown in the site selector when empty.
     */
    placeholder = input<string>('');

    /**
     * Whether the component is disabled.
     * Settable via component input.
     */
    disabled = input<boolean>(false);

    /**
     * Internal value signal for the selected theme identifier.
     * Used internally by ControlValueAccessor; external updates via writeValue().
     * @internal
     */
    private readonly $value = signal<string | null>(null);

    /**
     * Public getter for the current value (for template binding).
     * @internal
     */
    value = this.$value.asReadonly();

    /**
     * Disabled state from the ControlValueAccessor interface.
     * True when the form control disables this component.
     * @internal
     */
    $isDisabled = signal<boolean>(false);

    /**
     * Combined disabled state: true if either the input or ControlValueAccessor disabled state is true.
     * Also clears pending search debounce when disabled.
     */
    $disabled = computed(() => {
        const isDisabled = this.disabled() || this.$isDisabled();
        if (isDisabled && this.filterDebounceTimeout) {
            clearTimeout(this.filterDebounceTimeout);
            this.filterDebounceTimeout = null;
        }
        return isDisabled;
    });

    /**
     * Output event emitted whenever the selected theme changes.
     * Emits the theme identifier or null.
     */
    onChange = output<string | null>();

    /**
     * CSS class(es) applied to the component wrapper.
     * Default is 'w-full'.
     */
    class = input<string>('w-full');

    /**
     * The HTML id attribute for the component.
     * Can be customized; defaults to an empty string.
     */
    id = input<string>('');

    /**
     * Optional custom template for the button trigger.
     * If not provided, the default button will be used.
     */
    buttonTemplate = contentChild<TemplateRef<unknown>>('buttonTemplate');

    /**
     * Reactive state of the component including loaded themes, loading status, pagination, and filters.
     */
    readonly $state = signalState<DotThemeState>({
        themes: [],
        loading: false,
        error: null,
        pagination: null,
        selectedTheme: null,
        hostId: null,
        searchValue: ''
    });

    /**
     * Current page's themes for DataView (when using lazy="true").
     * Contains only the themes for the current page being displayed.
     */
    readonly $currentPageThemes = computed(() => {
        return this.$state.themes();
    });

    /**
     * The selected theme's title for display in the button label.
     * Falls back to "Primary" if no theme is selected.
     */
    readonly $selectedThemeTitle = computed(() => {
        return this.$state.selectedTheme()?.title || 'Select a theme';
    });

    /**
     * The number of items to load per page when fetching themes.
     */
    readonly pageSize = 6;

    /**
     * Stores the timeout ID for the debounce when filtering themes.
     * Used to delay filter service calls until typing stabilizes.
     * Null when no debounce is pending.
     * @private
     */
    private filterDebounceTimeout: ReturnType<typeof setTimeout> | null = null;

    /**
     * Subject for canceling pending theme load requests when new requests are made.
     * Used to prevent race conditions from rapid pagination or search changes.
     * @private
     */
    private readonly loadRequest$ = new Subject<{ page: number; hostId: string; search?: string }>();

    /**
     * Tracks the identifier of a theme currently being fetched individually.
     * Used to prevent duplicate fetches and race conditions.
     * @private
     */
    private pendingThemeFetch: string | null = null;

    constructor() {
        /**
         * State Update Flow:
         *
         * 1. Effect (lines 210-220): Watches $value() changes and syncs selectedTheme.
         *    - Finds theme in loaded themes or current selectedTheme
         *    - If not found and identifier exists, triggers individual fetch
         *
         * 2. loadRequest$ subscription (lines 235-272): Handles paginated theme list loads.
         *    - Cancels previous requests via switchMap
         *    - Updates themes list and syncs selectedTheme if value matches loaded themes
         *    - Effect will re-run if selectedTheme changes, but won't duplicate fetch
         *
         * 3. fetchThemeByIdentifier() (lines 496-515): Fetches individual theme.
         *    - Only called when theme not in loaded list
         *    - Checks value hasn't changed before updating (prevents stale updates)
         *
         * Race condition prevention:
         * - loadRequest$ uses switchMap to cancel previous requests
         * - fetchThemeByIdentifier checks pendingThemeFetch to prevent duplicates
         * - Both check $value() before updating selectedTheme to ensure consistency
         */

        // Sync internal value signal changes with state (but don't trigger callbacks - those are for user interaction only)
        // The effect handles finding and setting selectedTheme whenever $value changes
        effect(() => {
            const identifier = this.$value();
            const theme = this.findThemeByIdentifier(identifier);
            patchState(this.$state, { selectedTheme: theme });

            // If theme not found in loaded themes and we have an identifier, fetch it
            // Skip if already fetching this theme to prevent duplicate requests
            if (identifier && !theme && this.pendingThemeFetch !== identifier) {
                this.fetchThemeByIdentifier(identifier);
            }
        });

        // Watch for current site changes from global store
        // Only initializes hostId when it's null (initial load). Once user selects a site,
        // this effect will not overwrite their selection even if GlobalStore changes.
        effect(() => {
            const currentSiteId = this.globalStore.currentSiteId();
            const currentHostId = this.$state.hostId();

            // Only set hostId if it's null and we have a valid site ID (initialization only)
            if (currentSiteId && currentHostId === null) {
                patchState(this.$state, { hostId: currentSiteId });
                // Load themes on initial setup
                this.loadThemes(1, currentSiteId);
            }
        });

        // Set up request cancellation for theme loading
        // Uses switchMap to cancel previous requests when new ones arrive
        this.loadRequest$
            .pipe(
                switchMap(({ page, hostId, search }) => {
                    patchState(this.$state, { loading: true, error: null });
                    return this.themesService.getThemes({
                        hostId,
                        page,
                        per_page: this.pageSize,
                        ...(search ? { searchParam: search } : {})
                    });
                }),
                takeUntilDestroyed(this.destroyRef)
            )
            .subscribe({
                next: ({ themes, pagination }) => {
                    const normalizedThemes = themes.map((theme) => this.normalizeThemePath(theme));

                    // Sync selectedTheme if we have a value set
                    // Check current value to prevent race conditions (value might have changed during load)
                    const currentValue = this.$value();
                    const selectedTheme = currentValue
                        ? this.findThemeByIdentifier(currentValue, normalizedThemes)
                        : null;

                    // If we found the theme in the loaded list, clear any pending individual fetch
                    if (selectedTheme && this.pendingThemeFetch === currentValue) {
                        this.pendingThemeFetch = null;
                    }

                    patchState(this.$state, {
                        themes: normalizedThemes,
                        pagination,
                        loading: false,
                        error: null,
                        selectedTheme
                    });
                },
                error: (error) => {
                    patchState(this.$state, {
                        loading: false,
                        error: error?.message || 'Failed to load themes'
                    });
                }
            });
    }

    ngOnDestroy(): void {
        if (this.filterDebounceTimeout) {
            clearTimeout(this.filterDebounceTimeout);
            this.filterDebounceTimeout = null;
        }
        // Complete the subject to clean up the subscription
        this.loadRequest$.complete();
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
     * Updates hostId and reloads themes with the new hostId.
     * Clears search value and selected value when site changes.
     *
     * @param siteId The selected site identifier, or null if cleared
     */
    onSiteChange(siteId: string | null): void {
        if (!siteId) {
            // Clear all state when site is cleared
            this.$value.set(null);
            patchState(this.$state, {
                hostId: null,
                themes: [],
                pagination: null,
                selectedTheme: null,
                searchValue: ''
            });
            return;
        }

        // Clear search value and selected value when switching sites
        // This prevents trying to fetch themes from the wrong site context
        this.$value.set(null);
        patchState(this.$state, { hostId: siteId, searchValue: '' });
        this.loadThemes(1, siteId);
    }

    /**
     * Handles search input changes with debouncing.
     * Resets loaded data and loads the first page with the new search filter.
     *
     * @param search The search text value
     */
    onSearchChange(search: string): void {
        if (this.filterDebounceTimeout) {
            clearTimeout(this.filterDebounceTimeout);
        }

        patchState(this.$state, { searchValue: search });

        this.filterDebounceTimeout = setTimeout(() => {
            const hostId = this.$state.hostId();
            if (!hostId) {
                this.filterDebounceTimeout = null;
                return;
            }

            patchState(this.$state, {
                themes: [],
                pagination: null
            });

            // Load first page with the new filter
            this.loadThemes(1, hostId, search.trim() || undefined);

            this.filterDebounceTimeout = null;
        }, 300);
    }

    /**
     * Handles user selection of a theme.
     * This method is called when the user interacts with the radio buttons.
     * It updates the value (which triggers effect to sync selectedTheme) and emits events.
     *
     * @param identifier The theme identifier selected by the user, or null to clear selection
     */
    onThemeSelect(identifier: string | null): void {
        // Prevent selection when disabled
        if (this.$disabled()) {
            return;
        }

        // Update the internal value signal (effect will handle finding and setting selectedTheme)
        this.$value.set(identifier);

        // Emit callbacks and events ONLY on user interaction (not from writeValue)
        this.onChangeCallback(identifier);
        this.onChange.emit(identifier);
        this.onTouchedCallback();
    }

    /**
     * Handles lazy loading of themes from PrimeNG DataView
     * With lazy="true", DataView expects only the current page's data.
     * We always load the requested page to ensure data is fresh.
     *
     * @param event Lazy load event with first (offset) and rows (page size)
     */
    onLazyLoad(event: LazyLoadEvent): void {
        const hostId = this.$state.hostId();
        if (!hostId) {
            return;
        }

        // Validate event parameters
        const first = Number(event?.first) || 0;
        const rows = Number(event?.rows) || this.pageSize;

        // Convert offset to page number (1-indexed)
        const page = Math.floor(first / rows) + 1;

        // Always load the requested page (even if we've loaded it before)
        // This ensures data is fresh when navigating back/forward
        const searchValue = this.$state.searchValue().trim() || undefined;
        this.loadThemes(page, hostId, searchValue);
    }


    /**
     * Gets the thumbnail URL for a theme.
     *
     * @param theme The theme object
     * @returns The thumbnail URL string
     */
    getThemeThumbnailUrl(theme: DotTheme): string {
        if (!theme.themeThumbnail) {
            return '';
        }

        // SYSTEM_THEME uses thumbnail as-is
        if (theme.identifier === 'SYSTEM_THEME') {
            return theme.themeThumbnail;
        }

        // Other themes use the dotAsset URL format
        return `/dA/${theme.themeThumbnail}/720/theme.png`;
    }

    /**
     * Loads themes from the service with pagination support.
     * Uses request cancellation to prevent race conditions from rapid calls.
     *
     * @private
     * @param page The page number to load (1-indexed)
     * @param hostId The host ID to filter themes
     * @param search Optional search parameter
     */
    private loadThemes(page: number, hostId: string, search?: string): void {
        this.loadRequest$.next({ page, hostId, search });
    }

    // ControlValueAccessor implementation
    writeValue(value: string | null): void {
        // Update internal value signal (effect will handle finding and setting selectedTheme)
        // NOTE: This does NOT emit onChange/onChangeCallback - only user interaction does
        this.$value.set(value);
        // Effect handles the rest: finding theme, fetching if needed, updating selectedTheme
    }

    registerOnChange(fn: (value: string | null) => void): void {
        this.onChangeCallback = fn;
    }

    registerOnTouched(fn: () => void): void {
        this.onTouchedCallback = fn;
    }

    setDisabledState(isDisabled: boolean): void {
        this.$isDisabled.set(isDisabled);
        // Note: Debounce cleanup is handled by $disabled computed signal
    }

    /**
     * Finds a theme by identifier from the provided themes array or current state.
     * Falls back to checking selectedTheme if not found in themes array.
     *
     * @private
     * @param identifier The theme identifier to find, or null
     * @param themes Optional themes array to search. If not provided, uses current state themes.
     * @returns The found theme or null
     */
    private findThemeByIdentifier(
        identifier: string | null,
        themes?: DotTheme[]
    ): DotTheme | null {
        if (!identifier) {
            return null;
        }

        const themesToSearch = themes || this.$state.themes();
        const foundTheme = themesToSearch.find((t) => t.identifier === identifier);

        if (foundTheme) {
            return foundTheme;
        }

        // Fallback: check if current selectedTheme matches
        const currentSelected = this.$state.selectedTheme();
        if (currentSelected?.identifier === identifier) {
            return currentSelected;
        }

        return null;
    }

    /**
     * Fetches a theme by identifier from the service.
     * Used when writeValue() is called with an identifier not in loaded themes.
     * Tracks pending fetches to prevent duplicate requests.
     *
     * @private
     * @param identifier The theme identifier to fetch
     */
    private fetchThemeByIdentifier(identifier: string): void {
        // Mark as pending to prevent duplicate fetches
        this.pendingThemeFetch = identifier;

        this.themesService
            .get(identifier)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
                next: (fetchedTheme) => {
                    const transformedTheme = this.normalizeThemePath(fetchedTheme);
                    // Only update if the value hasn't changed (user might have selected something else)
                    // and we're still the pending fetch (prevents race with loadRequest$)
                    if (this.$value() === identifier && this.pendingThemeFetch === identifier) {
                        patchState(this.$state, { selectedTheme: transformedTheme });
                    }
                    // Always clear pending flag if this was the pending fetch
                    if (this.pendingThemeFetch === identifier) {
                        this.pendingThemeFetch = null;
                    }
                },
                error: () => {
                    // If fetch fails, clear selection if value still matches
                    if (this.$value() === identifier && this.pendingThemeFetch === identifier) {
                        patchState(this.$state, { selectedTheme: null });
                    }
                    // Always clear pending flag on error
                    if (this.pendingThemeFetch === identifier) {
                        this.pendingThemeFetch = null;
                    }
                }
            });
    }

    /**
     * Normalizes theme path by removing '/application' prefix and leading slash.
     * This transformation is needed because the API returns paths like '/application/themes/theme-name'
     * but the UI displays them as 'themes/theme-name'.
     *
     * @private
     * @param theme The theme to normalize
     * @returns Theme with normalized path
     */
    private normalizeThemePath(theme: DotTheme): DotTheme {
        return {
            ...theme,
            path: theme.path.replace(/^\/application/, '').replace(/^\//, '')
        };
    }
}
