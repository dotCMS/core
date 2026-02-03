import { patchState, signalState } from '@ngrx/signals';

import { ChangeDetectionStrategy, Component, EventEmitter, Output, computed, inject } from '@angular/core';

import { TabViewChangeEvent, TabViewModule } from 'primeng/tabview';
import { TooltipModule } from 'primeng/tooltip';

import { DotPageLayoutService } from '@dotcms/data-access';

import { DotRowReorderComponent } from './components/dot-row-reorder/dot-row-reorder.component';
import { DotUvePaletteListComponent } from './components/dot-uve-palette-list/dot-uve-palette-list.component';
import { DotUVEPaletteListTypes } from './models';

import { UVEStore } from '../../../store/dot-uve.store';
import { UVE_PALETTE_TABS } from '../../../store/features/editor/models';

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
        TabViewModule,
        DotUvePaletteListComponent,
        TooltipModule,
        DotRowReorderComponent,
    ],
    templateUrl: './dot-uve-palette.component.html',
    styleUrl: './dot-uve-palette.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePaletteComponent {
    protected readonly uveStore = inject(UVEStore);
    protected readonly dotPageLayoutService = inject(DotPageLayoutService);

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
    readonly $pagePath = computed(() => this.uveStore.$pageURI());
    readonly $languageId = computed(() => this.uveStore.$languageId());
    readonly $variantId = computed(() => this.uveStore.$variantId());

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
     * @param event TabView change event containing the new active index.
     */
    protected handleTabChange(event: TabViewChangeEvent) {
        patchState(this.#localState, { currentTab: event.index });
    }

}
