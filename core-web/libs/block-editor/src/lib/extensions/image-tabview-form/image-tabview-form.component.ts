import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { DEFAULT_LANG_ID } from '@dotcms/block-editor';
import { DotCMSContentlet } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-image-tabview-form',
    templateUrl: './image-tabview-form.component.html',
    styleUrls: ['./image-tabview-form.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ImageTabviewFormComponent {
    @Input() languageId = DEFAULT_LANG_ID;
    @Input() onSelectImage: (payload: DotCMSContentlet | string) => void;
}
