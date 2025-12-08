import { ChangeDetectionStrategy, Component, EventEmitter, input, Output } from '@angular/core';

import { TabViewChangeEvent, TabViewModule } from 'primeng/tabview';
import { TooltipModule } from 'primeng/tooltip';

import { DEFAULT_VARIANT_ID } from '@dotcms/dotcms-models';

import { DotUvePaletteListComponent } from './components/dot-uve-palette-list/dot-uve-palette-list.component';
import { DotUVEPaletteListTypes } from './models';

import { UVE_PALETTE_TABS } from '../../../store/features/editor/models';

@Component({
    selector: 'dot-uve-palette',
    imports: [TabViewModule, DotUvePaletteListComponent, TooltipModule],
    templateUrl: './dot-uve-palette.component.html',
    styleUrl: './dot-uve-palette.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePaletteComponent {
    $pagePath = input.required<string>({ alias: 'pagePath' });
    $languageId = input.required<number>({ alias: 'languageId' });
    $variantId = input<string>(DEFAULT_VARIANT_ID, { alias: 'variantId' });
    $activeTab = input<UVE_PALETTE_TABS>(UVE_PALETTE_TABS.CONTENT_TYPES, { alias: 'activeTab' });

    @Output() onTabChange = new EventEmitter<UVE_PALETTE_TABS>();

    protected readonly TABS_MAP = UVE_PALETTE_TABS;
    // protected readonly $isStyleEditorEnabled = this.uveStore.$isStyleEditorEnabled;
    protected readonly DotUVEPaletteListTypes = DotUVEPaletteListTypes;

    /*
     *  Only trigged when the user changes the tab manually.
     * @memberof DotUvePaletteComponent
     */
    protected handleTabChange(event: TabViewChangeEvent) {
        this.onTabChange.emit(event.index);
    }
}
