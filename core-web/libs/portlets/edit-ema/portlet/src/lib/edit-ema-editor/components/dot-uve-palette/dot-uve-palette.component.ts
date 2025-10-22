import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { TabViewModule } from 'primeng/tabview';

import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-uve-palette',
    imports: [TabViewModule, DotMessagePipe],
    templateUrl: './dot-uve-palette.component.html',
    styleUrl: './dot-uve-palette.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePaletteComponent {
    $languageId = input.required<number>({ alias: 'languageId' });
    $variantId = input.required<string>({ alias: 'variantId' });
}
