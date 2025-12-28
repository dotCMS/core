import { ChangeDetectionStrategy, Component, EventEmitter, computed, inject, Output } from '@angular/core';

import { TabViewChangeEvent, TabViewModule } from 'primeng/tabview';
import { TooltipModule } from 'primeng/tooltip';

import { DotPageLayoutService } from '@dotcms/data-access';
import { StyleEditorFormSchema } from '@dotcms/uve';

import { DotRowReorderComponent } from './components/dot-row-reorder/dot-row-reorder.component';
import { DotUvePaletteListComponent } from './components/dot-uve-palette-list/dot-uve-palette-list.component';
import { DotUveStyleEditorFormComponent } from './components/dot-uve-style-editor-form/dot-uve-style-editor-form.component';
import { DotUVEPaletteListTypes } from './models';

import { UVEStore } from '../../../store/dot-uve.store';
import { UVE_PALETTE_TABS } from '../../../store/features/editor/models';

/**
 * Standalone palette component used by the EMA editor to display and switch
 * between different UVE-related resources (content types, components, styles, etc.).
 *
 * Reads all state directly from UVEStore instead of receiving props.
 */
@Component({
    selector: 'dot-uve-palette',
    imports: [
        TabViewModule,
        DotUvePaletteListComponent,
        TooltipModule,
        DotUveStyleEditorFormComponent,
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
     * Computed signals that read directly from the UVEStore.
     * These replace the previous input properties.
     * Made public for testing purposes.
     */
    readonly $pagePath = computed(() => this.uveStore.$pageURI());
    readonly $languageId = computed(() => this.uveStore.$languageId());
    readonly $variantId = computed(() => this.uveStore.$variantId());
    readonly $activeTab = computed(() => this.uveStore.palette.currentTab());
    readonly $showStyleEditorTab = computed(() => this.uveStore.$isStyleEditorEnabled());
    readonly $styleSchema = computed<StyleEditorFormSchema | undefined>(() => this.uveStore.$styleSchema());

    /**
     * Emits when a tree node is selected to scroll to the corresponding element.
     */
    @Output() onNodeSelect = new EventEmitter<{ selector: string; type: string }>();

    /**
     * Called whenever the tab changes, either by user interaction or via the `activeIndex` property.
     * Directly updates the store instead of emitting an event.
     *
     * @param event TabView change event containing the new active index.
     */
    protected handleTabChange(event: TabViewChangeEvent) {
        this.uveStore.setPaletteTab(event.index);
    }

}
