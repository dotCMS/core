import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotCMSContentlet, EditorAssetTypes } from '@dotcms/dotcms-models';
import { DotAssetSearchComponent } from '@dotcms/ui';

@Component({
    selector: 'dot-asset-search-dialog',
    standalone: true,
    imports: [DotAssetSearchComponent],
    templateUrl: './dot-asset-search-dialog.component.html',
    styleUrl: './dot-asset-search-dialog.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAssetSearchDialogComponent {
    private readonly ref = inject(DynamicDialogRef);

    protected editorAssetType: EditorAssetTypes;

    constructor(private readonly config: DynamicDialogConfig) {
        this.editorAssetType = this.config.data?.assetType;
    }

    onSelectAsset(asset: DotCMSContentlet): void {
        this.ref.close(asset);
    }
}
