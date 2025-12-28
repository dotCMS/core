import { ChangeDetectionStrategy, Component, EventEmitter, inject, input, Output } from '@angular/core';

import { TabViewChangeEvent, TabViewModule } from 'primeng/tabview';
import { TooltipModule } from 'primeng/tooltip';

import { DotPageLayoutService } from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID } from '@dotcms/dotcms-models';
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
 * It exposes inputs to control the current page, language, variant and active tab,
 * and emits events when the active tab changes.
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
    /**
     * Absolute path of the page currently being edited.
     */
    $pagePath = input.required<string>({ alias: 'pagePath' });

    /**
     * Identifier of the language in which the page is being edited.
     */
    $languageId = input.required<number>({ alias: 'languageId' });

    /**
     * Variant identifier of the page/contentlet; defaults to `DEFAULT_VARIANT_ID`.
     */
    $variantId = input<string>(DEFAULT_VARIANT_ID, { alias: 'variantId' });

    /**
     * Currently active palette tab.
     */
    $activeTab = input<UVE_PALETTE_TABS>(UVE_PALETTE_TABS.CONTENT_TYPES, { alias: 'activeTab' });

    /**
     * Whether the style editor tab should be shown in the palette.
     */
    $showStyleEditorTab = input<boolean>(false, { alias: 'showStyleEditorTab' });

    /**
     * The Style Schema to use for the current selected contentlet.
     */
    $styleSchema = input<StyleEditorFormSchema>(undefined, { alias: 'styleSchema' });

    /**
     * Emits whenever the active tab in the palette changes.
     */
    @Output() onTabChange = new EventEmitter<UVE_PALETTE_TABS>();

    /**
     * Emits when a tree node is selected to scroll to the corresponding element.
     */
    @Output() onNodeSelect = new EventEmitter<{ selector: string; type: string }>();

    protected readonly uveStore = inject(UVEStore);
    protected readonly TABS_MAP = UVE_PALETTE_TABS;
    protected readonly DotUVEPaletteListTypes = DotUVEPaletteListTypes;

    protected readonly dotPageLayoutService = inject(DotPageLayoutService);


    /**
     * Called whenever the tab changes, either by user interaction or via the `activeIndex` property.
     *
     * @param event TabView change event containing the new active index.
     */
    protected handleTabChange(event: TabViewChangeEvent) {
        this.onTabChange.emit(event.index);
    }

}
