import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { EditorAssetTypes } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-asset-preview',
    templateUrl: './dot-asset-preview.component.html',
    styleUrls: ['./dot-asset-preview.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class DotAssetPreviewComponent {
    @Input()
    type: EditorAssetTypes;

    @Input()
    file: File;

    @Input()
    src: string | ArrayBuffer;
}
