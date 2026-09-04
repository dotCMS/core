import { SafeResourceUrl } from '@angular/platform-browser';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { EditorAssetTypes } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-asset-preview',
    templateUrl: './dot-asset-preview.component.html',
    styleUrls: ['./dot-asset-preview.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class DotAssetPreviewComponent {
    @Input()
    type!: EditorAssetTypes;

    @Input()
    // PrimeNG's FileUpload decorates the File with `objectURL`, which `File` does not declare.
    file!: File & { objectURL?: string };

    @Input()
    src!: string | ArrayBuffer | SafeResourceUrl;
}
