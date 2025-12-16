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
    ViewChild
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, FormsModule } from '@angular/forms';

import { LazyLoadEvent } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DataView, DataViewModule } from 'primeng/dataview';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { PopoverModule } from 'primeng/popover';
import { RadioButtonModule } from 'primeng/radiobutton';

import { DotThemesService } from '@dotcms/data-access';
import { DotTheme, DotPagination } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { DotSiteComponent } from '../dot-site/dot-site.component';

interface DotThemeState {
    themes: DotTheme[];
    loading: boolean;
    pagination: DotPagination | null;
    selectedThemeId: string | null;
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
        PopoverModule
    ],
    templateUrl: './dot-theme.component.html',
    styleUrl: './dot-theme.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotThemeComponent),
            multi: true
        }
    ]
})
export class DotThemeComponent implements ControlValueAccessor, OnInit, OnDestroy {
    private readonly themesService = inject(DotThemesService);
    private readonly globalStore = inject(GlobalStore);

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
     * Two-way model binding for the selected theme identifier.
     * Accepts a string (theme identifier) or null if no theme is selected.
     */
    value = model<string | null>(null);

    /**
     * Disabled state from the ControlValueAccessor interface.
     * True when the form control disables this component.
     * @internal
     */
    $isDisabled = signal<boolean>(false);

    /**
     * Combined disabled state: true if either the input or ControlValueAccessor disabled state is true.
     */
    $disabled = computed(() => this.disabled() || this.$isDisabled());

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
     * Reactive state of the component including loaded themes, loading status, pagination, and filters.
     */
    readonly $state = signalState<DotThemeState>({
        themes: [],
        loading: false,
        pagination: null,
        selectedThemeId: null,
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
     * Set of page numbers that have already been loaded from the backend.
     * Used to prevent redundant page fetching.
     * @private
     */
    private loadedPages = new Set<number>();

    constructor() {
        // Sync model signal changes with ControlValueAccessor and state
        effect(() => {
            const identifier = this.value();
            patchState(this.$state, { selectedThemeId: identifier });
            this.onChangeCallback(identifier);
        });

        // Watch for current site changes from global store
        // This handles both initial load (when store loads asynchronously) and subsequent changes
        effect(() => {
            const currentSiteId = this.globalStore.currentSiteId();
            const currentHostId = this.$state.hostId();

            // Update hostId if it's different and we have a valid site ID
            if (currentSiteId && currentSiteId !== currentHostId) {
                patchState(this.$state, { hostId: currentSiteId });
                // Only load themes if we don't have any loaded yet (initial load)
                // If themes are already loaded, user might have changed the site selector
                if (this.$state.themes().length === 0) {
                    this.loadThemes(1, currentSiteId);
                }
            }
        });
    }

    ngOnInit(): void {
        // Effect will handle the initial load when store becomes available
        // But also check immediately in case store is already loaded
        const currentSiteId = this.globalStore.currentSiteId();
        if (currentSiteId && !this.$state.hostId()) {
            patchState(this.$state, { hostId: currentSiteId });
            this.loadThemes(1, currentSiteId);
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
     * Updates hostId and reloads themes with the new hostId.
     *
     * @param siteId The selected site identifier, or null if cleared
     */
    onSiteChange(siteId: string | null): void {
        if (!siteId) {
            patchState(this.$state, {
                hostId: null,
                themes: [],
                pagination: null
            });
            this.loadedPages.clear();
            return;
        }

        patchState(this.$state, { hostId: siteId });
        this.loadedPages.clear();
        this.loadThemes(1, siteId, this.$state.searchValue());
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

            this.loadedPages.clear();
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
     * Handles theme selection via radio button onChange event.
     *
     * @param theme The selected theme
     */
    onThemeSelect(theme: DotTheme): void {
        patchState(this.$state, { selectedThemeId: theme.identifier });
        this.value.set(theme.identifier);
        this.onTouchedCallback();
        this.onChange.emit(theme.identifier);
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
     *
     * @private
     * @param page The page number to load (1-indexed)
     * @param hostId The host ID to filter themes
     * @param search Optional search parameter
     */
    private loadThemes(page: number, hostId: string, search?: string): void {
        if (this.$state.loading()) {
            return;
        }

        patchState(this.$state, { loading: true });

        this.themesService
            .getThemes({
                hostId,
                page,
                per_page: this.pageSize,
                ...(search ? { searchParam: search } : {})
            })
            .subscribe({
                next: ({ themes, pagination }) => {
                    const t = themes.map((theme) => ({
                        ...theme,
                        path: theme.path.replace('/application', '').replace('/', '')
                    }));
                    patchState(this.$state, { themes: t, pagination, loading: false });

                    this.loadedPages.add(page);
                },
                error: () => {
                    patchState(this.$state, { loading: false });
                }
            });
    }

    // ControlValueAccessor implementation
    writeValue(value: string | null): void {
        this.value.set(value);
        patchState(this.$state, { selectedThemeId: value });
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
}
