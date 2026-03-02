import { signalState } from '@ngrx/signals';

import { NgClass } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Output,
    computed,
    inject
} from '@angular/core';

import { TabsModule } from 'primeng/tabs';
import { TooltipModule } from 'primeng/tooltip';

import { DotPageLayoutService } from '@dotcms/data-access';

import { DotRowReorderComponent } from './components/dot-row-reorder/dot-row-reorder.component';
import { DotUvePaletteListComponent } from './components/dot-uve-palette-list/dot-uve-palette-list.component';
import { DotUVEPaletteListTypes } from './models';

import { UVEStore } from '../../../store/dot-uve.store';
import { UVE_PALETTE_TABS } from '../../../store/features/editor/models';

interface TabHeaderConfig {
    value: UVE_PALETTE_TABS;
    icon: string;
    tooltip: string;
}

/**
 * Standalone palette component used by the EMA editor to display and switch
 * between different UVE-related resources (content types, components, styles, etc.).
 *
 * Container component that uses signalState for local UI state (tab selection)
 * and reads shared state from UVEStore.
 */
@Component({
    selector: 'dot-uve-palette',
    imports: [
        NgClass,
        TabsModule,
        TooltipModule,
        DotRowReorderComponent,
        DotUvePaletteListComponent
    ],
    templateUrl: './dot-uve-palette.component.html',
    styleUrl: './dot-uve-palette.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePaletteComponent {
    protected readonly uveStore = inject(UVEStore);
    protected readonly dotPageLayoutService = inject(DotPageLayoutService);
    protected readonly $tabHeaders = computed<TabHeaderConfig[]>(() => {
        const tabs: TabHeaderConfig[] = [
            { value: UVE_PALETTE_TABS.CONTENT_TYPES, icon: 'pi-stop', tooltip: 'Content types' },
            { value: UVE_PALETTE_TABS.WIDGETS, icon: 'pi-th-large', tooltip: 'Widgets' },
            { value: UVE_PALETTE_TABS.FAVORITES, icon: 'pi-star', tooltip: 'Favorites' },
            { value: UVE_PALETTE_TABS.LAYERS, icon: 'pi-table', tooltip: 'Layers' }
        ];
        return tabs;
    });

    /**
     * Tabs PT so we can style Prime's internal root element with Tailwind instead of ::ng-deep SCSS.
     */
    readonly tabsPt = {
        root: { class: 'h-full min-h-0' }
    };

    /**
     * Emits whenever the active tab in the palette changes.
     */
    @Output() onTabChange = new EventEmitter<UVE_PALETTE_TABS>();

    protected readonly TABS_MAP = UVE_PALETTE_TABS;
    protected readonly DotUVEPaletteListTypes = DotUVEPaletteListTypes;

    /**
     * Local component UI state using NgRx signalState (recommended pattern).
     * This keeps tab selection local to the component instead of polluting the global store.
     */
    readonly #localState = signalState({
        currentTab: UVE_PALETTE_TABS.CONTENT_TYPES
    });

    /**
     * Computed signals that read from UVEStore for shared state.
     * Made public for testing purposes.
     */
    readonly $pagePath = computed(() => this.uveStore.pageURI());
    readonly $languageId = computed(() => this.uveStore.pageLanguageId());
    readonly $variantId = computed(() => this.uveStore.pageVariantId());

    /**
     * Active tab - read from local state, not global store.
     * Made public for testing purposes.
     */
    readonly $activeTab = this.#localState.currentTab;

    /**
     * Emits when a tree node is selected to scroll to the corresponding element.
     */
    @Output() onNodeSelect = new EventEmitter<{ selector: string; type: string }>();

    constructor() {
        // Tab management is now handled locally without effects
    }

    /**
     * Called whenever the tab changes, either by user interaction or via the `activeIndex` property.
     * Updates local component state using patchState instead of dispatching to global store.
     *
     * @param value The new tab value.
     */
    protected handleTabChange(value: string | number | undefined): void {
        if (value !== undefined && value !== null) {
            const tab = value as UVE_PALETTE_TABS;
            this.#localState.patchState({ currentTab: tab });
            this.onTabChange.emit(tab);
        }
    }
}
