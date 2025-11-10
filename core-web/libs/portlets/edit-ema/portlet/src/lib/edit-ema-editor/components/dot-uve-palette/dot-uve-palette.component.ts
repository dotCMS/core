import { JsonPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';

import { TabViewChangeEvent, TabViewModule } from 'primeng/tabview';
import { TooltipModule } from 'primeng/tooltip';

import { DEFAULT_VARIANT_ID } from '@dotcms/dotcms-models';

import { DotUvePaletteListComponent } from './components/dot-uve-palette-list/dot-uve-palette-list.component';
import { DotUVEPaletteListTypes } from './models';

import { UVEStore } from '../../../store/dot-uve.store';
import { PALETTE_TABS } from '../../../store/features/editor/models';

@Component({
    selector: 'dot-uve-palette',
    imports: [TabViewModule, DotUvePaletteListComponent, TooltipModule, JsonPipe],
    templateUrl: './dot-uve-palette.component.html',
    styleUrl: './dot-uve-palette.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePaletteComponent {
    $languageId = input.required<number>({ alias: 'languageId' });
    $pagePath = input.required<string>({ alias: 'pagePath' });
    $currentTab = input.required<PALETTE_TABS>({ alias: 'currentTab' });
    $styleConfig = input<Record<string, unknown>>({}, { alias: 'styleConfig' });
    $variantId = input<string>(DEFAULT_VARIANT_ID, { alias: 'variantId' });

    uveStore = inject(UVEStore);

    readonly PALETTE_TABS = PALETTE_TABS;
    readonly DotUVEPaletteListTypes = DotUVEPaletteListTypes;

    onTabChange(event: TabViewChangeEvent) {
        this.uveStore.setPaletteCurrentTab(event.index);
    }
}
