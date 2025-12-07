import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';

import { TabViewChangeEvent, TabViewModule } from 'primeng/tabview';
import { TooltipModule } from 'primeng/tooltip';

import { DEFAULT_VARIANT_ID } from '@dotcms/dotcms-models';

import { DotUvePaletteListComponent } from './components/dot-uve-palette-list/dot-uve-palette-list.component';
import { DotUVEPaletteListTypes } from './models';

import { UVEStore } from '../../../store/dot-uve.store';
import { UVE_PALETTE_TABS } from '../../../store/features/editor/models';

@Component({
    selector: 'dot-uve-palette',
    imports: [TabViewModule, DotUvePaletteListComponent, TooltipModule],
    templateUrl: './dot-uve-palette.component.html',
    styleUrl: './dot-uve-palette.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePaletteComponent {
    uveStore = inject(UVEStore);

    $languageId = input.required<number>({ alias: 'languageId' });
    $pagePath = input.required<string>({ alias: 'pagePath' });
    $variantId = input<string>(DEFAULT_VARIANT_ID, { alias: 'variantId' });

    protected readonly TABS_MAP = UVE_PALETTE_TABS;
    protected readonly $isStyleEditorEnabled = this.uveStore.$isStyleEditorEnabled;
    readonly $currentIndex = signal(0);
    // readonly PALETTE_TABS = PALETTE_TABS;
    readonly DotUVEPaletteListTypes = DotUVEPaletteListTypes;

    constructor() {
        // Temporal
        effect(() => {
            this.$currentIndex.set(this.uveStore.palette.currentTab());
        });
    }

    /*
     *  Only trigged when the user changes the tab manually.
     * @memberof DotUvePaletteComponent
     */
    onTabChange(event: TabViewChangeEvent) {
        this.$currentIndex.set(event.index);
    }
}
