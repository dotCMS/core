import { ChangeDetectionStrategy, Component, signal } from '@angular/core';

import { DotCMSContentType } from '@dotcms/dotcms-models';

import { MOCK_CONTENT_TYPES } from '../../utils';
import { DotUvePaletteItemComponent } from '../dot-uve-palette-item/dot-uve-palette-item.component';

@Component({
    selector: 'dot-uve-palette-list',
    imports: [DotUvePaletteItemComponent],
    templateUrl: './dot-uve-palette-list.component.html',
    styleUrl: './dot-uve-palette-list.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePaletteListComponent {
    contenttypes = signal<DotCMSContentType[]>(MOCK_CONTENT_TYPES);
}
