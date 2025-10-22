import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { DotCMSContentType } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-uve-palette-item',
    imports: [CommonModule],
    templateUrl: './dot-uve-palette-item.component.html',
    styleUrl: './dot-uve-palette-item.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePaletteItemComponent {
    @Input() contenttype!: DotCMSContentType;
}
