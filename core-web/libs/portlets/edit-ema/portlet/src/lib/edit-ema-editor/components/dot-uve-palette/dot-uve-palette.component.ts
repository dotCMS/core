import { ChangeDetectionStrategy, Component, input, signal } from '@angular/core';

import { TabViewModule } from 'primeng/tabview';
import { TooltipModule } from 'primeng/tooltip';

import { DEFAULT_VARIANT_ID } from '@dotcms/dotcms-models';

import { DotUvePaletteListComponent } from './components/dot-uve-palette-list/dot-uve-palette-list.component';
import {
    BASE_TYPES_FOR_FAVORITES,
    BASETYPES_FOR_CONTENT,
    BASETYPES_FOR_WIDGET,
    UVE_PALETTE_LIST_TYPES
} from './utils';

@Component({
    selector: 'dot-uve-palette',
    imports: [TabViewModule, DotUvePaletteListComponent, TooltipModule],
    templateUrl: './dot-uve-palette.component.html',
    styleUrl: './dot-uve-palette.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePaletteComponent {
    $languageId = input.required<number>({ alias: 'languageId' });
    $pagePath = input.required<string>({ alias: 'pagePath' });
    $variantId = input<string>(DEFAULT_VARIANT_ID, { alias: 'variantId' });

    readonly $currentIndex = signal(0);
    readonly UVE_PALETTE_LIST_TYPES = UVE_PALETTE_LIST_TYPES;

    readonly BASE_TYPES_FOR_CONTENT = BASETYPES_FOR_CONTENT;
    readonly BASE_TYPES_FOR_WIDGET = BASETYPES_FOR_WIDGET;
    readonly BASE_TYPES_FOR_FAVORITES = BASE_TYPES_FOR_FAVORITES;
}
