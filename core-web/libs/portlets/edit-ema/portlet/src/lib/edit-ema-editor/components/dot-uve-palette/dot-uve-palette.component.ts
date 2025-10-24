import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';

import { TabViewModule } from 'primeng/tabview';

import { DEFAULT_VARIANT_ID, DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';

import { DotUvePaletteListComponent } from './components/dot-uve-palette-list/dot-uve-palette-list.component';

import { DotUVEPaletteListType } from '../../../shared/models';

@Component({
    selector: 'dot-uve-palette',
    imports: [TabViewModule, DotUvePaletteListComponent],
    templateUrl: './dot-uve-palette.component.html',
    styleUrl: './dot-uve-palette.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePaletteComponent {
    $languageId = input.required<number>({ alias: 'languageId' });
    $pagePath = input.required<string>({ alias: 'pagePath' });
    $variantId = input<string>(DEFAULT_VARIANT_ID, { alias: 'variantId' });

    readonly TYPES_ARRAY: Array<DotUVEPaletteListType> = [
        DotCMSBaseTypesContentTypes.CONTENT,
        DotCMSBaseTypesContentTypes.WIDGET,
        'FAVORITES'
    ];

    readonly $currentIndex = signal(0);
    readonly $type = computed(() => {
        return this.TYPES_ARRAY[this.$currentIndex()];
    });
}
